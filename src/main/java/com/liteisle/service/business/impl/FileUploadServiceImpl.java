package com.liteisle.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liteisle.center.AsyncFileProcessingCenter;
import com.liteisle.common.domain.*;
import com.liteisle.common.dto.response.FileUploadAsyncResp;
import com.liteisle.common.enums.*;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.business.FileUploadService;
import com.liteisle.service.core.*;
import com.liteisle.util.HashUtil;
import com.liteisle.util.MimeTypeUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipFile;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final long MAX_MARKDOWN_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> MUSIC_EXTENSIONS = Set.of(
            "mp3", "flac", "wav", "aac", "ogg", "wma", "m4a"
    );
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "md", "pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx"
    );

    @Resource
    private StoragesService storagesService;
    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;
    @Resource
    private UsersService usersService;
    @Resource
    private TransferLogService transferLogService;
    @Resource
    private DocumentMetadataService documentMetadataService;
    @Resource
    private MusicMetadataService musicMetadataService;
    @Resource
    private AsyncFileProcessingCenter asyncFileProcessingCenter;
    //TODO 优化上传功能
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadAsyncResp uploadFile(MultipartFile file, Long folderId) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new LiteisleException("上传文件无效");
        }
        // 步骤 1: 解析和验证文件夹ID
        Long userId = UserContextHolder.getUserId();
        Long targetFolderId = resolveTargetFolderId(folderId, userId);
        // 步骤 2: 文件预处理
        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());

        // 2. 使用 FilenameUtils.getExtension() 来获取后缀
        String suffix = FilenameUtils.getExtension(originalFilename).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(suffix)) {
            // 处理没有后缀名的情况
            throw new LiteisleException("文件缺少后缀名");
        }
        FileTypeEnum fileType = determineFileType(suffix);

        //优化 防止读取-修改-写入
        // 将检查和更新合并到一个SQL语句中
        Users user = usersService.getById(userId);
        if (user == null) {
            throw new LiteisleException("用户不存在");
        }
        boolean updateSuccess = usersService.update(new UpdateWrapper<Users>()
                .setSql("storage_used = storage_used + " + file.getSize())
                .eq("id", userId)
                // 关键：在WHERE子句中直接检查空间是否足够
                .le("storage_used + " + file.getSize(), user.getStorageQuota())
        );

        if (!updateSuccess) {
            throw new LiteisleException("存储空间不足或更新失败");
        }

        // 步骤 3: 处理 Markdown 文件 (特殊路径)
        if (isMarkdownFile(suffix)) {
            return handleMarkdownUpload(file, targetFolderId, userId, suffix, originalFilename);
        }
        // --- 通用文件处理路径 ---
        Path stagedFile = null;
        try {
            // 步骤 4: 将 MultipartFile 流式落到应用托管临时文件，异步线程只持有路径
            stagedFile = stageUploadedFile(file, suffix);
            long stagedFileSize = java.nio.file.Files.size(stagedFile);
            validateFileSignature(stagedFile, suffix);

            // 步骤 5: 基于临时文件流式计算哈希
            String fileHash = calculateFileHash(stagedFile);

            // 步骤 6: 尝试秒传
            FileUploadAsyncResp fastUploadResponse =
                    handleFastUpload(fileHash, originalFilename, stagedFileSize, targetFolderId, userId, fileType, suffix);
            if (fastUploadResponse != null) {
                deleteQuietly(stagedFile);
                return fastUploadResponse; // 秒传成功，直接返回
            }

            // 步骤 7: 处理新文件上传
            return handleNewFileUpload(
                    stagedFile,
                    stagedFileSize,
                    originalFilename,
                    MimeTypeUtil.getMimeType(originalFilename),
                    fileHash,
                    targetFolderId,
                    userId,
                    fileType,
                    suffix
            );
        } catch (LiteisleException e) {
            deleteQuietly(stagedFile);
            throw e;
        } catch (Exception e) {
            deleteQuietly(stagedFile);
            log.error("处理上传文件失败", e);
            throw new LiteisleException("无法处理上传的文件");
        }
    }

    /**
     * 【修正版】处理新文件上传
     * 创建初始记录，并启动异步任务处理实际的文件上传和元数据提取。
     */
    private FileUploadAsyncResp handleNewFileUpload(
            Path stagedFile, long fileSize, String originalFilename, String mimeType, String fileHash, Long targetFolderId,
            Long userId, FileTypeEnum fileType, String suffix) {

        try {
            // 1. 创建初始 Files 记录 (状态: PROCESSING)
            Files fileRecord = new Files();
            fileRecord.setUserId(userId);
            fileRecord.setFolderId(targetFolderId);
            fileRecord.setFileName(originalFilename);
            fileRecord.setFileExtension(suffix);
            fileRecord.setFileType(fileType);
            fileRecord.setFileStatus(FileStatusEnum.PROCESSING);
            fileRecord.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis() * 100000));
            fileRecord.setCreateTime(new Date());
            fileRecord.setUpdateTime(new Date());
            boolean saveFile = filesService.save(fileRecord);
            if (!saveFile) {
                throw new LiteisleException("创建初始文件记录失败");
            }
            // 2. 创建初始传输日志 (状态: PROCESSING)
            TransferLog logRecord = new TransferLog();
            logRecord.setUserId(userId);
            logRecord.setTransferType(TransferTypeEnum.UPLOAD);
            logRecord.setFileId(fileRecord.getId());
            logRecord.setFolderId(targetFolderId);
            logRecord.setItemName(originalFilename);
            logRecord.setItemSize(fileSize);
            logRecord.setLogStatus(TransferStatusEnum.PROCESSING);
            logRecord.setErrorMessage(null);
            logRecord.setTransferDurationMs(null);
            logRecord.setClientIp(null);
            logRecord.setCreateTime(new Date());
            logRecord.setUpdateTime(new Date());
            boolean saveLog = transferLogService.save(logRecord);
            if (!saveLog) {
                throw new LiteisleException("创建初始传输日志失败");
            }

            // 3. 注册事务同步回调，在当前事务成功提交后执行异步任务
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("事务已提交，启动异步文件处理. FileId: {}, LogId: {}", fileRecord.getId(), logRecord.getId());
                    asyncFileProcessingCenter.processNewFile(
                            stagedFile,
                            originalFilename,
                            fileSize,
                            mimeType,
                            fileHash,
                            fileRecord.getId(),
                            logRecord.getId()
                    );
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        deleteQuietly(stagedFile);
                    }
                }
            });

            log.info("新文件上传任务已启动, fileId: {}, logId: {}. 立即返回响应。", fileRecord.getId(), logRecord.getId());

            // 4. 立即返回“处理中”的响应给前端
            return creatUploadResponse(fileRecord, logRecord);
        } catch (RuntimeException e) {
            deleteQuietly(stagedFile);
            throw e;
        }
    }


    private FileUploadAsyncResp handleFastUpload(
            String fileHash, String originalFilename, long fileSize, Long targetFolderId,
            Long userId, FileTypeEnum fileType, String suffix) {
        //获取原本的storageId以及music meta数据
        // 【关键修复 #1】查询时，必须确保文件是“存活”的 (reference_count > 0)
        Storages storages = storagesService.getOne(
                new QueryWrapper<Storages>()
                        .eq("file_hash", fileHash)
                        .gt("reference_count", 0)); // > 0, 避免与清理任务的竞争
        if (storages == null) {
            return null;
        }
        Long storagesId = storages.getId();
        //判断是否是music
        if (fileType.equals(FileTypeEnum.MUSIC)) {
            //是music
            //存files 复用原本的file_hash 也就是 storage_id
            Files originFile = filesService.getOne(
                    new QueryWrapper<Files>()
                            .eq("storage_id", storagesId)
                            .eq("file_type", FileTypeEnum.MUSIC)
                            .eq("file_status", FileStatusEnum.AVAILABLE)
                            .isNull("delete_time")
                            .last("LIMIT 1"));
            MusicMetadata originMusicMetadata = originFile == null ? null : musicMetadataService.getOne(
                    new QueryWrapper<MusicMetadata>().eq("file_id", originFile.getId()));

            Files file2Save = new Files();
            file2Save.setUserId(userId);
            file2Save.setFolderId(targetFolderId);
            file2Save.setFileType(FileTypeEnum.MUSIC);
            file2Save.setFileStatus(FileStatusEnum.AVAILABLE);
            file2Save.setFileExtension(suffix);
            file2Save.setStorageId(storagesId);
            file2Save.setFileName(originalFilename);
            file2Save.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis() * 100000));
            file2Save.setCreateTime(new Date());
            file2Save.setUpdateTime(new Date());
            boolean saveFile = filesService.save(file2Save);
            if (!saveFile) {
                throw new LiteisleException("创建文件失败");
            }
            //复制一份music meta存入数据库
            MusicMetadata musicMetadata = new MusicMetadata();
            musicMetadata.setFileId(file2Save.getId());
            musicMetadata.setArtist(originMusicMetadata == null ? "未知艺术家" : originMusicMetadata.getArtist());
            musicMetadata.setAlbum(originMusicMetadata == null ? "未知专辑" : originMusicMetadata.getAlbum());
            musicMetadata.setDuration(originMusicMetadata == null ? 0 : originMusicMetadata.getDuration());
            boolean saveMeta = musicMetadataService.save(musicMetadata);
            if (!saveMeta) {
                throw new LiteisleException("创建音乐元数据失败");
            }
            //创建 transfer log success
            TransferLog transferLog = new TransferLog();
            transferLog.setUserId(userId);
            transferLog.setTransferType(TransferTypeEnum.UPLOAD);
            transferLog.setFileId(file2Save.getId());
            transferLog.setLogStatus(TransferStatusEnum.SUCCESS);
            transferLog.setItemName(originalFilename);
            transferLog.setItemSize(fileSize);
            transferLog.setCreateTime(new Date());
            transferLog.setUpdateTime(new Date());
            boolean saveLog = transferLogService.save(transferLog);
            if (!saveLog) {
                throw new LiteisleException("创建上传日志失败");
            }
            //storages 引用加一
            boolean update = storagesService.update(new UpdateWrapper<Storages>()
                    .eq("id", storagesId)
                    .setSql("reference_count = reference_count + 1"));
            if (!update) {
                throw new LiteisleException("更新文件引用计数失败");
            }
            return creatUploadResponse(file2Save, transferLog);
        } else {
            //不是music
            //存files 复用原本的file_hash 也就是 storage_id
            Files file2Save = new Files();
            file2Save.setUserId(userId);
            file2Save.setFolderId(targetFolderId);
            file2Save.setFileType(FileTypeEnum.DOCUMENT);
            file2Save.setFileStatus(FileStatusEnum.AVAILABLE);
            file2Save.setFileExtension(suffix);
            file2Save.setStorageId(storagesId);
            file2Save.setFileName(originalFilename);
            file2Save.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis() * 100000));
            file2Save.setCreateTime(new Date());
            file2Save.setUpdateTime(new Date());
            boolean saveFile = filesService.save(file2Save);
            if (!saveFile) {
                throw new LiteisleException("创建文件失败");
            }
            //创建 transfer log success
            TransferLog transferLog = new TransferLog();
            transferLog.setUserId(userId);
            transferLog.setTransferType(TransferTypeEnum.UPLOAD);
            transferLog.setFileId(file2Save.getId());
            transferLog.setLogStatus(TransferStatusEnum.SUCCESS);
            transferLog.setItemName(originalFilename);
            transferLog.setItemSize(fileSize);
            transferLog.setCreateTime(new Date());
            transferLog.setUpdateTime(new Date());
            boolean saveLog = transferLogService.save(transferLog);
            if (!saveLog) {
                throw new LiteisleException("创建上传日志失败");
            }
            //storages 引用加一
            boolean update = storagesService.update(new UpdateWrapper<Storages>()
                    .eq("id", storagesId)
                    .setSql("reference_count = reference_count + 1"));
            if (!update) {
                throw new LiteisleException("更新文件引用计数失败");
            }
            return creatUploadResponse(file2Save, transferLog);
        }
    }

    private FileUploadAsyncResp handleMarkdownUpload(
            MultipartFile file, Long targetFolderId, Long userId, String suffix, String originalFilename) {
        String content;
        try {
            content = extractTextContent(file);
        } catch (IOException e) {
            throw new LiteisleException(e.getMessage());
        }
        //存入 files
        Files file2Save = new Files();
        file2Save.setUserId(userId);
        file2Save.setFolderId(targetFolderId);
        file2Save.setFileType(FileTypeEnum.DOCUMENT);
        file2Save.setFileStatus(FileStatusEnum.AVAILABLE);
        file2Save.setFileExtension(suffix);
        file2Save.setFileName(originalFilename);
        file2Save.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis() * 100000));
        file2Save.setCreateTime(new Date());
        file2Save.setUpdateTime(new Date());
        boolean save = filesService.save(file2Save);
        if (!save) {
            throw new LiteisleException("创建文件失败");
        }
        //存入 document meta
        DocumentMetadata documentMetadata = new DocumentMetadata();
        documentMetadata.setFileId(file2Save.getId());
        documentMetadata.setContent(content);
        documentMetadata.setVersion(0L);
        boolean saveMeta = documentMetadataService.save(documentMetadata);
        if (!saveMeta) {
            throw new LiteisleException("创建文件元数据失败");
        }
        //创建 transfer log
        TransferLog transferLog = new TransferLog();
        transferLog.setUserId(userId);
        transferLog.setTransferType(TransferTypeEnum.UPLOAD);
        transferLog.setFileId(file2Save.getId());
        transferLog.setLogStatus(TransferStatusEnum.SUCCESS);
        transferLog.setItemName(originalFilename);
        transferLog.setItemSize(file.getSize());
        transferLog.setCreateTime(new Date());
        transferLog.setUpdateTime(new Date());
        boolean saveTransferLog = transferLogService.save(transferLog);
        if (!saveTransferLog) {
            throw new LiteisleException("创建传输任务失败");
        }
        return creatUploadResponse(file2Save, transferLog);
    }

    private FileUploadAsyncResp creatUploadResponse(Files file2Save, TransferLog transferLog) {
        return new FileUploadAsyncResp(
                transferLog.getId(),
                file2Save.getId(),
                transferLog.getLogStatus(),
                new FileUploadAsyncResp.InitialFileData(
                        file2Save.getId(),
                        file2Save.getFileName(),
                        file2Save.getFileType(),
                        file2Save.getFileStatus(),
                        file2Save.getSortedOrder(),
                        file2Save.getCreateTime(),
                        file2Save.getUpdateTime()));
    }


    private String extractTextContent(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_MARKDOWN_FILE_SIZE_BYTES) {
            throw new LiteisleException("Markdown 文件不能超过 5MB");
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private Long resolveTargetFolderId(Long folderId, Long userId) {
        if (folderId != null && folderId > 0) {
            Folders targetFolder = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("id", folderId)
                    .eq("user_id", userId)
                    .isNull("delete_time")
                    .select("id"));
            if (targetFolder == null) {
                throw new LiteisleException("目标文件夹不存在或无权访问");
            }
            return targetFolder.getId();
        } else {
            //获取上传系统文件夹id
            Folders uploadFolder = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("user_id", userId)
                    .eq("folder_type", FolderTypeEnum.SYSTEM)
                    .eq("folder_name", "上传")
                    .eq("parent_id", 0)
                    .isNull("delete_time")
                    .select("id"));
            if (uploadFolder == null) {
                throw new LiteisleException("上传文件夹不存在");
            }
            //重新赋值folderId
            return uploadFolder.getId();
        }
    }

    /**
     * 【修正版】根据文件名后缀决定文件的主类型 (MUSIC 或 DOCUMENT)。
     *
     * @param fileName 完整的文件名，例如 "晴天.mp3" 或 "项目报告.pdf"。
     * @return 返回 FileTypeEnum.MUSIC 或 FileTypeEnum.DOCUMENT。
     * @throws LiteisleException 如果文件名无效。
     */
    private FileTypeEnum determineFileType(String suffix) {
        if (isMusicFile(suffix)) {
            return FileTypeEnum.MUSIC;
        } else if (isDocumentFile(suffix)) {
            return FileTypeEnum.DOCUMENT;
        }
        throw new LiteisleException("不支持的文件类型");
    }

    /**
     * [辅助方法] 根据文件后缀判断是否为支持的音乐文件。
     *
     * @param suffix 文件后缀名 (不包含点, 已转为小写)
     * @return 如果是音乐文件则返回 true，否则返回 false
     */
    private boolean isMusicFile(String suffix) {
        if (!StringUtils.hasText(suffix)) {
            return false;
        }
        return MUSIC_EXTENSIONS.contains(suffix.toLowerCase(Locale.ROOT));
    }

    private boolean isDocumentFile(String suffix) {
        if (!StringUtils.hasText(suffix)) {
            return false;
        }
        return DOCUMENT_EXTENSIONS.contains(suffix.toLowerCase(Locale.ROOT));
    }

    /**
     * [辅助方法] 根据文件后缀判断是否为 Markdown 文件。
     * 这个方法在你分离处理 MD 文件逻辑时仍然有用。
     *
     * @param suffix 文件后缀名 (不包含点, 已转为小写)
     * @return 如果是 Markdown 文件则返回 true，否则返回 false
     */
    private boolean isMarkdownFile(String suffix) {
        return "md".equalsIgnoreCase(suffix);
    }

    private String normalizeOriginalFilename(String originalFilename) {
        String normalized = FilenameUtils.getName(originalFilename);
        if (!StringUtils.hasText(normalized)) {
            throw new LiteisleException("文件名不能为空");
        }
        return normalized;
    }

    private Path stageUploadedFile(MultipartFile file, String suffix) throws IOException {
        Path tempFile = java.nio.file.Files.createTempFile("liteisle-upload-", "." + suffix);
        try (InputStream inputStream = file.getInputStream()) {
            java.nio.file.Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private String calculateFileHash(Path filePath) throws Exception {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(filePath)) {
            return HashUtil.generateSHA256(inputStream);
        }
    }

    private void validateFileSignature(Path filePath, String suffix) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(filePath)) {
            byte[] header = inputStream.readNBytes(16);
            boolean valid = switch (suffix.toLowerCase(Locale.ROOT)) {
                case "pdf" -> startsWith(header, "%PDF".getBytes(StandardCharsets.US_ASCII));
                case "doc", "xls", "ppt" -> startsWith(header, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0});
                case "docx" -> isOfficeOpenXml(filePath, "word/document.xml");
                case "xlsx" -> isOfficeOpenXml(filePath, "xl/workbook.xml");
                case "pptx" -> isOfficeOpenXml(filePath, "ppt/presentation.xml");
                case "mp3" -> startsWith(header, "ID3".getBytes(StandardCharsets.US_ASCII))
                        || (header.length >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0);
                case "flac" -> startsWith(header, "fLaC".getBytes(StandardCharsets.US_ASCII));
                case "wav" -> header.length >= 12
                        && startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                        && header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E';
                case "ogg" -> startsWith(header, "OggS".getBytes(StandardCharsets.US_ASCII));
                case "wma" -> startsWith(header, new byte[]{0x30, 0x26, (byte) 0xB2, 0x75});
                case "m4a" -> header.length >= 12
                        && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
                case "aac" -> header.length >= 2
                        && (header[0] & 0xFF) == 0xFF
                        && ((header[1] & 0xF6) == 0xF0);
                case "txt" -> true;
                default -> true;
            };
            if (!valid) {
                throw new LiteisleException("文件内容与后缀名不匹配");
            }
        } catch (LiteisleException e) {
            throw e;
        } catch (Exception e) {
            throw new LiteisleException("文件类型校验失败");
        }
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isOfficeOpenXml(Path filePath, String requiredEntry) {
        try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
            return zipFile.getEntry("[Content_Types].xml") != null
                    && zipFile.getEntry(requiredEntry) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private void deleteQuietly(Path filePath) {
        if (filePath == null) {
            return;
        }
        try {
            java.nio.file.Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除上传临时文件失败: {}", filePath, e);
        }
    }

}
