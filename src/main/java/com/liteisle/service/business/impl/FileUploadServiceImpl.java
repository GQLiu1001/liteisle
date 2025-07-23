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
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

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
        String originalFilename = file.getOriginalFilename();

        // 2. 使用 FilenameUtils.getExtension() 来获取后缀
        String suffix = FilenameUtils.getExtension(originalFilename);
        if (!StringUtils.hasText(suffix)) {
            // 处理没有后缀名的情况
            throw new LiteisleException("文件缺少后缀名");
        }
        FileTypeEnum fileType = determineFileType(originalFilename);
        if (fileType == null) {
            throw new LiteisleException("不支持的文件类型");
        }

        //新增：减少用户存储额度 storage_used 注意先检查 storage_quota
//        Long storage = usersService.getById(userId).getStorageUsed();
//        if (storage + file.getSize() > usersService.getById(userId).getStorageQuota()) {
//            throw new LiteisleException("存储空间不足");
//        }
//        boolean updateStorage = usersService.update(new UpdateWrapper<Users>()
//                .setSql("storage_used = storage_used + " + file.getSize())
//                .eq("id", userId));
        //优化 防止读取-修改-写入
        // 将检查和更新合并到一个SQL语句中
        boolean updateSuccess = usersService.update(new UpdateWrapper<Users>()
                .setSql("storage_used = storage_used + " + file.getSize())
                .eq("id", userId)
                // 关键：在WHERE子句中直接检查空间是否足够
                .le("storage_used + " + file.getSize(), usersService.getById(userId).getStorageQuota())
        );

        if (!updateSuccess) {
            throw new LiteisleException("存储空间不足或更新失败");
        }

        // 步骤 3: 处理 Markdown 文件 (特殊路径)
        if (isMarkdownFile(suffix)) {
            return handleMarkdownUpload(file, targetFolderId, userId, suffix);
        }
        // --- 通用文件处理路径 ---
        // 步骤 4: 计算哈希
        String fileHash;
        try (InputStream inputStream = file.getInputStream()) {
            fileHash = HashUtil.generateSHA256(inputStream);
        } catch (Exception e) {
            log.error("计算文件哈希失败", e);
            throw new LiteisleException("无法处理上传的文件");
        }

        // 步骤 5: 尝试秒传
        FileUploadAsyncResp fastUploadResponse =
                handleFastUpload(fileHash, file, targetFolderId, userId, fileType, suffix);
        if (fastUploadResponse != null) {
            return fastUploadResponse; // 秒传成功，直接返回
        }

        // 步骤 6: 处理新文件上传
        return handleNewFileUpload(file, fileHash, targetFolderId, userId, fileType, suffix);
    }

    /**
     * 【修正版】处理新文件上传
     * 创建初始记录，并启动异步任务处理实际的文件上传和元数据提取。
     */
    private FileUploadAsyncResp handleNewFileUpload(
            MultipartFile file, String fileHash, Long targetFolderId,
            Long userId, FileTypeEnum fileType, String suffix) {

        // 1. 创建初始 Files 记录 (状态: PROCESSING)
        Files fileRecord = new Files();
        fileRecord.setUserId(userId);
        fileRecord.setFolderId(targetFolderId);
        fileRecord.setFileName(file.getOriginalFilename());
        fileRecord.setFileExtension(suffix);
        fileRecord.setFileType(fileType);
        fileRecord.setFileStatus(FileStatusEnum.PROCESSING);
        fileRecord.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()*100000));
        fileRecord.setCreateTime(new Date());
        fileRecord.setUpdateTime(new Date());
        boolean saveFile = filesService.save(fileRecord);
        if (!saveFile){
            throw new LiteisleException("创建初始文件记录失败");
        }
        // 2. 创建初始传输日志 (状态: PROCESSING)
        TransferLog logRecord = new TransferLog();
        logRecord.setUserId(userId);
        logRecord.setTransferType(TransferTypeEnum.UPLOAD);
        logRecord.setFileId(fileRecord.getId());
        logRecord.setFolderId(targetFolderId);
        logRecord.setItemName(file.getOriginalFilename());
        logRecord.setItemSize(file.getSize());
        logRecord.setLogStatus(TransferStatusEnum.PROCESSING);
        logRecord.setErrorMessage(null);
        logRecord.setTransferDurationMs(null);
        logRecord.setClientIp(null);
        logRecord.setCreateTime(new Date());
        logRecord.setUpdateTime(new Date());
        boolean saveLog = transferLogService.save(logRecord);
        if (!saveLog){
            throw new LiteisleException("创建初始传输日志失败");
        }

        // **3. 触发异步任务，把所有繁重的工作交给它**
        asyncFileProcessingCenter.processNewFile(file, fileHash, fileRecord.getId(), logRecord.getId());

        log.info("新文件上传任务已启动, fileId: {}, logId: {}. 立即返回响应。", fileRecord.getId(), logRecord.getId());

        // 4. 立即返回“处理中”的响应给前端
        return creatUploadResponse(fileRecord, logRecord);
    }


    private FileUploadAsyncResp handleFastUpload(
            String fileHash, MultipartFile file, Long targetFolderId,
            Long userId, FileTypeEnum fileType, String suffix) {
        //获取原本的storageId以及music meta数据
        Storages storages = storagesService.getOne(
                new QueryWrapper<Storages>().eq("file_hash", fileHash));
        if (storages == null){
            return null;
        }
        Long storagesId = storages.getId();
        //判断是否是music
        if (fileType.equals(FileTypeEnum.MUSIC)) {
            //是music
            //存files 复用原本的file_hash 也就是 storage_id
            Files originFile = filesService.getOne(
                    new QueryWrapper<Files>().eq("storage_id", storagesId));
            MusicMetadata originMusicMetadata = musicMetadataService.getOne(
                    new QueryWrapper<MusicMetadata>().eq("file_id", originFile.getId()));

            Files file2Save = new Files();
            file2Save.setUserId(userId);
            file2Save.setFolderId(targetFolderId);
            file2Save.setFileType(FileTypeEnum.MUSIC);
            file2Save.setFileStatus(FileStatusEnum.AVAILABLE);
            file2Save.setFileExtension(suffix);
            file2Save.setStorageId(storagesId);
            file2Save.setFileName(file.getOriginalFilename());
            file2Save.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()*100000));
            file2Save.setCreateTime(new Date());
            file2Save.setUpdateTime(new Date());
            boolean saveFile = filesService.save(file2Save);
            if (!saveFile) {
                throw new LiteisleException("创建文件失败");
            }
            //复制一份music meta存入数据库
            MusicMetadata musicMetadata = new MusicMetadata();
            musicMetadata.setFileId(file2Save.getId());
            musicMetadata.setArtist(originMusicMetadata.getArtist());
            musicMetadata.setAlbum(originMusicMetadata.getAlbum());
            musicMetadata.setDuration(originMusicMetadata.getDuration());
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
            transferLog.setItemName(file.getOriginalFilename());
            transferLog.setItemSize(file.getSize());
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
            if (!update){
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
            file2Save.setFileName(file.getOriginalFilename());
            file2Save.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()*100000));
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
            transferLog.setItemName(file.getOriginalFilename());
            transferLog.setItemSize(file.getSize());
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
            if (!update){
                throw new LiteisleException("更新文件引用计数失败");
            }
            return creatUploadResponse(file2Save, transferLog);
        }
    }

    private FileUploadAsyncResp handleMarkdownUpload(
            MultipartFile file, Long targetFolderId, Long userId, String suffix) {
        String content = "";
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
        file2Save.setFileName(file.getOriginalFilename());
        file2Save.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()*100000));
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
        transferLog.setItemName(file.getOriginalFilename());
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
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private Long resolveTargetFolderId(Long folderId, Long userId) {
        if (folderId != 0 && folderId > 0) {
            return folderId;
        } else {
            //获取上传系统文件夹id
            Folders uploadFolder = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("user_id", userId)
                    .eq("folder_type", FolderTypeEnum.SYSTEM)
                    .eq("folder_name", "上传")
                    .eq("parent_id", 0)
                    .eq("delete_time", null)
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
    private FileTypeEnum determineFileType(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new LiteisleException("文件名不能为空");
        }

        // 使用 Apache Commons IO 提供的工具类，更健壮
        String suffix = FilenameUtils.getExtension(fileName);

        if (isMusicFile(suffix)) {
            return FileTypeEnum.MUSIC;
        } else {
            // 所有非音乐类型的文件，在此业务中都归类为文档
            return FileTypeEnum.DOCUMENT;
        }
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
        // 定义支持的音乐格式列表
        final List<String> musicExtensions = Arrays.asList(
                "mp3", "flac", "wav", "aac", "ogg", "wma", "m4a"
        );
        return musicExtensions.contains(suffix.toLowerCase());
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

}
