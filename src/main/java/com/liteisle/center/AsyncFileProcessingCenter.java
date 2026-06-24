package com.liteisle.center;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.MusicMetadata;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.common.domain.Users;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.MusicMetadataService;
import com.liteisle.service.core.StoragesService;
import com.liteisle.service.core.TransferLogService;
import com.liteisle.service.core.UsersService;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
    private UsersService usersService;
    @Resource
    private WebSocketService webSocketService;
    @Resource
    private RedissonClient redissonClient;
    // 【关键修改 #1】注入自身的代理对象，使用 @Lazy 防止循环依赖
    @Resource
    @Lazy
    private AsyncFileProcessingCenter self;

    /**
     * 【最终正确版】处理新上传文件的异步任务。
     * - 此方法只负责加锁和解锁，不应有 @Transactional 注解。
     */
    @Async("virtualThreadPool")
    public void processNewFile(Path stagedFile, String originalFilename, long fileSize,
                               String mimeType, String fileHash, Long fileId, Long logId) {

        String lockKey = FILE_HASH_LOCK_PREFIX + fileHash;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (isLocked) {
                try {
                    // 【关键修改 #3】使用 self (代理对象) 来调用事务方法
                    self.processNewFileInTransaction(stagedFile, originalFilename, fileSize, mimeType, fileHash, fileId, logId);
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
        } finally {
            deleteStagedFile(stagedFile);
        }
    }


    /**
     * 【关键修改 #4】真正在事务中执行的核心逻辑。
     * - 此方法必须是 public，以便代理能够拦截。
     * - 此方法必须有 @Transactional 注解。
     */
    @Transactional(rollbackFor = Exception.class)
    public void processNewFileInTransaction(Path stagedFile, String originalFilename, long fileSize,
                                            String mimeType, String fileHash, Long fileId, Long logId) throws Exception {
        Files fileRecord = filesService.getOne(new LambdaQueryWrapper<Files>().eq(Files::getId, fileId));
        if (fileRecord == null) throw new IllegalStateException("文件记录丢失: " + fileId);
        if (stagedFile == null || !java.nio.file.Files.exists(stagedFile)) {
            throw new IllegalStateException("上传临时文件丢失: " + stagedFile);
        }

        String storagePath = DATA_BUCKET_PREFIX + fileHash.substring(0, 2) + "/" + fileHash.substring(2, 4) + "/" + fileHash;
        if (!minioUtil.objectExists(storagePath)) {
            try (InputStream inputStream = java.nio.file.Files.newInputStream(stagedFile)) {
                minioUtil.uploadFile(inputStream, fileSize, storagePath, mimeType);
            }
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
        if (Objects.equals(fileRecord.getFileType(), FileTypeEnum.MUSIC)) {
            handleMusicMetadata(stagedFile.toAbsolutePath().toString(), fileId);
        }

        fileRecord.setFileStatus(FileStatusEnum.AVAILABLE);
        filesService.updateById(fileRecord);
        updateTransferLog(logId, TransferStatusEnum.SUCCESS, null);

        log.info("文件处理成功. FileId: {}, LogId: {}", fileId, logId);
        webSocketService.sendFileStatusUpdate(
                fileRecord.getUserId(), fileId, logId, FileStatusEnum.AVAILABLE, TransferStatusEnum.SUCCESS, fileRecord.getFileName(), null, 100
        );
    }

    /**
     * 统一的失败处理逻辑
     */
    private void handleFailure(Long fileId, Long logId, Exception e) {
        updateFileStatus(fileId, FileStatusEnum.FAILED);
        updateTransferLog(logId, TransferStatusEnum.FAILED, e.getMessage());
        Files fileRecord = filesService.getById(fileId);
        restoreUserStorageForFailedUpload(fileRecord, logId);
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

    private void restoreUserStorageForFailedUpload(Files fileRecord, Long logId) {
        if (fileRecord == null || fileRecord.getUserId() == null || fileRecord.getStorageId() != null) {
            return;
        }
        TransferLog transferLog = transferLogService.getById(logId);
        if (transferLog == null || transferLog.getItemSize() == null || transferLog.getItemSize() <= 0) {
            return;
        }
        usersService.update(new UpdateWrapper<Users>()
                .eq("id", fileRecord.getUserId())
                .setSql("storage_used = GREATEST(0, storage_used - " + transferLog.getItemSize() + ")"));
    }

    private void deleteStagedFile(Path stagedFile) {
        if (stagedFile == null) {
            return;
        }
        try {
            java.nio.file.Files.deleteIfExists(stagedFile);
        } catch (IOException e) {
            log.warn("删除上传临时文件失败: {}", stagedFile, e);
        }
    }
}
