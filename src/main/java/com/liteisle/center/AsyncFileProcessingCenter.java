package com.liteisle.center;

import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.MusicMetadata;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.handler.WebSocketHandler; // 假设你的WebSocket处理器
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.MusicMetadataService;
import com.liteisle.service.core.StoragesService;
import com.liteisle.service.core.TransferLogService;
import com.liteisle.util.FFmpegUtil;
import com.liteisle.util.MimeTypeUtil;
import com.liteisle.util.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

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
    private WebSocketHandler webSocketHandler; // 用于发送实时通知
    @Async("virtualThreadPool") // 使用你的虚拟线程池执行异步任务
    @Transactional(rollbackFor = Exception.class)
    public void processNewFile(MultipartFile multipartFile, String fileHash, Long fileId, Long logId) {
        File tempFile = null;
        try {
            // 1. 将 MultipartFile 转为临时 File，以便 FFmpeg 处理
            tempFile = File.createTempFile("upload-", "-" + multipartFile.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(multipartFile.getBytes());
            }

            // 2. 上传文件到 MinIO
            // 使用哈希值构造存储路径，实现去重 e.g., data/e3/b0/e3b0c44298fc1c14...
            String storagePath =
                    DATA_BUCKET_PREFIX + fileHash.substring(0, 2) + "/" + fileHash.substring(2, 4) + "/" + fileHash;
            minioUtil.uploadFile(multipartFile, storagePath);

            // 3. 创建并保存 Storages 记录
            Storages newStorage = new Storages();
            newStorage.setFileHash(fileHash);
            newStorage.setFileSize(multipartFile.getSize());
            newStorage.setMimeType(MimeTypeUtil.getMimeType(multipartFile.getOriginalFilename()));
            newStorage.setStoragePath(storagePath);
            newStorage.setReferenceCount(1);
            storagesService.save(newStorage);

            // 4. 更新 Files 记录，关联 storage_id
            Files fileRecord = filesService.getById(fileId);
            if (fileRecord == null) {
                throw new IllegalStateException("文件记录丢失: " + fileId);
            }
            fileRecord.setStorageId(newStorage.getId());

            // 5. 如果是音乐文件，提取元数据
            if (Objects.equals(fileRecord.getFileType(), FileTypeEnum.MUSIC)) {
                handleMusicMetadata(tempFile.getAbsolutePath(), fileId);
            }

            // 6. 更新文件状态为可用
            fileRecord.setFileStatus(FileStatusEnum.AVAILABLE);
            filesService.updateById(fileRecord);

            // 7. 更新传输日志为成功
            updateTransferLog(logId, TransferStatusEnum.SUCCESS, null);

            log.info("文件处理成功. FileId: {}, LogId: {}", fileId, logId);
            // 8. WebSocket 通知前端
            // webSocketHandler.sendMessageToUser(
            // fileRecord.getUserId(), "{\"event\": \"file.status.updated\", ...}");


        } catch (Exception e) {
            log.error("异步文件处理失败. FileId: {}, LogId: {}", fileId, logId, e);
            // 发生任何异常，都将状态更新为失败
            updateFileStatus(fileId, FileStatusEnum.FAILED);
            updateTransferLog(logId, TransferStatusEnum.FAILED, e.getMessage());
            // webSocketHandler.sendMessageToUser(...);
        } finally {
            // 9. 确保临时文件被删除
            if (tempFile != null && tempFile.exists()) {
                boolean delete = tempFile.delete();
                if (!delete) {
                    log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                }
            }
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
