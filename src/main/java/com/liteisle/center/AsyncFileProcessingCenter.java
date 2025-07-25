package com.liteisle.center;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.MusicMetadata;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.common.dto.websocket.ShareSaveCompletedMessage;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.MusicMetadataService;
import com.liteisle.service.core.StoragesService;
import com.liteisle.service.core.TransferLogService;
import com.liteisle.util.FFmpegUtil;
import com.liteisle.util.MinioUtil;
import com.liteisle.service.business.WebSocketService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.liteisle.common.constant.RedisConstant.FILE_HASH_LOCK_PREFIX;
import static com.liteisle.common.constant.SystemConstant.DATA_BUCKET_PREFIX;

@Slf4j
@Service
public class AsyncFileProcessingCenter {

    @Resource
    private MinioUtil minioUtil;
    @Resource
    private FFmpegUtil ffmpegUtil;
    @Resource
    private StoragesService storagesService;
    @Resource
    private FilesService filesService;
    @Resource
    private MusicMetadataService musicMetadataService;
    @Resource
    private TransferLogService transferLogService;
    @Resource
    private WebSocketService webSocketService;
    @Resource
    private RedissonClient redissonClient;
    // 【关键修改 #1】注入自身的代理对象，使用 @Lazy 防止循环依赖
    @Resource
    @Lazy
    private AsyncFileProcessingCenter self;
    /**
     * 【新增】异步处理转存分享文件的任务。
     * 它的核心工作是更新引用计数和文件/日志状态。
     *
     * @param sharerId          分享者的用户ID
     * @param receiverId        接收者（当前登录用户）的用户ID
     * @param filesToSave       新创建的、状态为PROCESSING的Files记录列表
     * @param logsToUpdate      新创建的、状态为PROCESSING的TransferLog记录列表
     */
    @Async("virtualThreadPool")
    @Transactional(rollbackFor = Exception.class)
    public void processSharedFilesSave(
            Long sharerId,
            Long receiverId,
            List<Files> filesToSave,
            List<TransferLog> logsToUpdate) {

        try {
            for (int i = 0; i < filesToSave.size(); i++) {
                Files newFileRecord = filesToSave.get(i);
                TransferLog logRecord = logsToUpdate.get(i);

                // 1. 找到原始文件记录来获取 storage_id
                // (注意：这里假设分享的文件/文件夹在转存期间没有被分享者删除)
                Files originalFile = filesService.getById(logRecord.getFileId());
                if (originalFile == null || originalFile.getStorageId() == null) {
                    // 如果源文件没了，标记此文件失败
                    updateFileStatus(newFileRecord.getId(), FileStatusEnum.FAILED);
                    updateTransferLog(logRecord.getId(), TransferStatusEnum.FAILED, "源文件已不存在");
                    continue; // 继续处理下一个文件
                }

                // 2. 关联 Storage ID 并更新引用计数
                newFileRecord.setStorageId(originalFile.getStorageId());
                storagesService.update(new UpdateWrapper<Storages>()
                        .eq("id", originalFile.getStorageId())
                        .setSql("reference_count = reference_count + 1"));

                // 3. 如果是音乐文件，复制元数据
                if (Objects.equals(newFileRecord.getFileType(), FileTypeEnum.MUSIC)) {
                    MusicMetadata originalMetadata = musicMetadataService.getById(originalFile.getId());
                    if (originalMetadata != null) {
                        MusicMetadata newMetadata = new MusicMetadata();
                        newMetadata.setFileId(newFileRecord.getId());
                        newMetadata.setArtist(originalMetadata.getArtist());
                        newMetadata.setAlbum(originalMetadata.getAlbum());
                        newMetadata.setDuration(originalMetadata.getDuration());
                        musicMetadataService.save(newMetadata);
                    }
                }

                // 4. 更新文件和日志状态为成功
                updateFileStatus(newFileRecord.getId(), FileStatusEnum.AVAILABLE);
                updateTransferLog(logRecord.getId(), TransferStatusEnum.SUCCESS, null);

                // 5. 更新新文件记录本身（主要是为了保存storage_id）
                filesService.updateById(newFileRecord);
            }

            log.info("用户 {} 成功转存了 {} 个文件", receiverId, filesToSave.size());
            
            // 8. WebSocket 通知前端转存完成
            ShareSaveCompletedMessage shareMessage = new ShareSaveCompletedMessage(
                    filesToSave.size(),
                    filesToSave.size(),
                    0,
                    filesToSave.getFirst().getFolderId(),
                    sharerId,
                    new ArrayList<>()
            );
            webSocketService.sendShareSaveCompleted(receiverId, shareMessage);

        } catch (Exception e) {
            log.error("异步转存分享文件失败. ReceiverId: {}", receiverId, e);
            // 如果整个批次失败，将所有相关记录标记为失败
            filesToSave.forEach(file -> updateFileStatus(file.getId(), FileStatusEnum.FAILED));
            logsToUpdate.forEach(log -> updateTransferLog(log.getId(), TransferStatusEnum.FAILED, "内部服务器错误"));
            
            // WebSocket 通知前端转存失败
            List<String> failedFileNames = filesToSave.stream()
                    .map(Files::getFileName)
                    .toList();
            ShareSaveCompletedMessage shareMessage = new ShareSaveCompletedMessage(
                    filesToSave.size(),
                    0,
                    filesToSave.size(),
                    filesToSave.isEmpty() ? null : filesToSave.getFirst().getFolderId(),
                    sharerId,
                    failedFileNames
            );
            webSocketService.sendShareSaveFailed(receiverId, shareMessage);
        }
    }

    /**
     * 【最终正确版】处理新上传文件的异步任务。
     * - 此方法只负责加锁和解锁，不应有 @Transactional 注解。
     */
    @Async("virtualThreadPool")
    // 【关键修改 #2】移除这里的 @Transactional 注解！！！
    public void processNewFile(byte[] fileBytes, String originalFilename, long fileSize,
                               String mimeType, String fileHash, Long fileId, Long logId) {

        String lockKey = FILE_HASH_LOCK_PREFIX + fileHash;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (isLocked) {
                try {
                    // 【关键修改 #3】使用 self (代理对象) 来调用事务方法
                    self.processNewFileInTransaction(fileBytes, originalFilename, fileSize, mimeType, fileHash, fileId, logId);
                } catch (Exception e) {
                    log.error("文件处理事务执行失败. FileId: {}, LogId: {}", fileId, logId, e);
                    handleFailure(fileId, logId, e);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.warn("获取 Redisson 锁失败，任务终止。FileId: {}, Hash: {}", fileId, fileHash);
                handleFailure(fileId, logId, new Exception("系统繁忙，无法处理文件，请稍后重试"));
            }
        } catch (InterruptedException e) {
            log.error("获取 Redisson 锁时被中断", e);
            handleFailure(fileId, logId, e);
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 【关键修改 #4】真正在事务中执行的核心逻辑。
     * - 此方法必须是 public，以便代理能够拦截。
     * - 此方法必须有 @Transactional 注解。
     */
    @Transactional(rollbackFor = Exception.class)
    public void processNewFileInTransaction(byte[] fileBytes, String originalFilename, long fileSize,
                                            String mimeType, String fileHash, Long fileId, Long logId) throws Exception {
        // 这里的内部逻辑是完全正确的，无需修改
        File tempFile = null;
        try {
            Files fileRecord = filesService.getById(fileId);
            if (fileRecord == null) throw new IllegalStateException("文件记录丢失: " + fileId);
            if (Objects.equals(fileRecord.getFileType(), FileTypeEnum.MUSIC)) {
                tempFile = File.createTempFile("upload-", "-" + originalFilename);
                try (FileOutputStream fos = new FileOutputStream(tempFile)) { fos.write(fileBytes); }
            }

            String storagePath = DATA_BUCKET_PREFIX + fileHash.substring(0, 2) + "/" + fileHash.substring(2, 4) + "/" + fileHash;
            if (!minioUtil.objectExists(storagePath)) {
                minioUtil.uploadFile(new ByteArrayInputStream(fileBytes), fileSize, storagePath, mimeType);
            }

            Storages existingStorage = storagesService.getOne(new QueryWrapper<Storages>().eq("file_hash", fileHash));
            Storages storageToUse;
            if (existingStorage != null) {
                storagesService.update(new UpdateWrapper<Storages>().eq("id", existingStorage.getId()).setSql("reference_count = reference_count + 1"));
                storageToUse = existingStorage;
            } else {
                Storages newStorage = new Storages();
                newStorage.setFileHash(fileHash);
                newStorage.setFileSize(fileSize);
                newStorage.setMimeType(mimeType);
                newStorage.setStoragePath(storagePath);
                newStorage.setReferenceCount(1);
                storagesService.save(newStorage);
                storageToUse = newStorage;
            }

            fileRecord.setStorageId(storageToUse.getId());
            if (Objects.equals(fileRecord.getFileType(), FileTypeEnum.MUSIC) && tempFile != null) {
                handleMusicMetadata(tempFile.getAbsolutePath(), fileId);
            }

            fileRecord.setFileStatus(FileStatusEnum.AVAILABLE);
            filesService.updateById(fileRecord);
            updateTransferLog(logId, TransferStatusEnum.SUCCESS, null);

            log.info("文件处理成功. FileId: {}, LogId: {}", fileId, logId);
            webSocketService.sendFileStatusUpdate(
                    fileRecord.getUserId(), fileId, logId, FileStatusEnum.AVAILABLE, TransferStatusEnum.SUCCESS, fileRecord.getFileName(), null, 100
            );
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
            }
        }
    }

    /**
     * 统一的失败处理逻辑
     */
    private void handleFailure(Long fileId, Long logId, Exception e) {
        updateFileStatus(fileId, FileStatusEnum.FAILED);
        updateTransferLog(logId, TransferStatusEnum.FAILED, e.getMessage());
        Files fileRecord = filesService.getById(fileId);
        if (fileRecord != null) {
            webSocketService.sendFileStatusUpdate(
                    fileRecord.getUserId(), fileId, logId, FileStatusEnum.FAILED, TransferStatusEnum.FAILED, fileRecord.getFileName(), e.getMessage(), 0
            );
        }
    }

    private void handleMusicMetadata(String filePath, Long fileId) throws Exception {
        Map<String, String> metadataMap = ffmpegUtil.getMusicMetadata(filePath);
        MusicMetadata musicMetadata = new MusicMetadata();
        musicMetadata.setFileId(fileId);
        musicMetadata.setArtist(metadataMap.getOrDefault("artist", "未知艺术家"));
        musicMetadata.setAlbum(metadataMap.getOrDefault("album", "未知专辑"));
        int duration = (int) Double.parseDouble(metadataMap.getOrDefault("duration", "0.0"));
        musicMetadata.setDuration(duration);
        boolean save = musicMetadataService.save(musicMetadata);
        if (!save) {
            log.warn("保存音乐元数据失败: {}", musicMetadata);
        }
    }

    private void updateFileStatus(Long fileId, FileStatusEnum status) {
        Files file = new Files();
        file.setId(fileId);
        file.setFileStatus(status);
        filesService.updateById(file);
    }

    private void updateTransferLog(Long logId, TransferStatusEnum status, String errorMessage) {
        TransferLog log = new TransferLog();
        log.setId(logId);
        log.setLogStatus(status);
        if (errorMessage != null) {
            log.setErrorMessage(errorMessage);
        }
        transferLogService.updateById(log);
    }
}
