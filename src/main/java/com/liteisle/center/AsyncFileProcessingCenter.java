package com.liteisle.center;

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
import com.liteisle.util.MimeTypeUtil;
import com.liteisle.util.MinioUtil;
import com.liteisle.service.business.WebSocketService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * 【修正版】处理新上传文件的异步任务
     */
    @Async("virtualThreadPool") // 使用你的虚拟线程池执行异步任务
    @Transactional(rollbackFor = Exception.class)
    // 【关键修改】更新方法签名，接收字节数组和元数据
    public void processNewFile(byte[] fileBytes, String originalFilename, long fileSize,
                               String mimeType, String fileHash, Long fileId, Long logId) {
        File tempFile = null;
        try {
            // 1. 【修改】将传入的 byte[] 转为临时 File，以便 FFmpeg 等工具处理
            tempFile = File.createTempFile("upload-", "-" + originalFilename);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileBytes);
            }

            // 2. 上传文件到 MinIO
            String storagePath = DATA_BUCKET_PREFIX + fileHash.substring(0, 2) + "/" + fileHash.substring(2, 4) + "/" + fileHash;
            // 【修改】调用 MinioUtil 的重载方法，传入字节流和元数据
            minioUtil.uploadFile(new ByteArrayInputStream(fileBytes), fileSize, storagePath, mimeType);

            // 3. 创建并保存 Storages 记录
            Storages newStorage = new Storages();
            newStorage.setFileHash(fileHash);
            newStorage.setFileSize(fileSize); // 使用传入的 fileSize
            newStorage.setMimeType(mimeType); // 使用传入的 mimeType
            newStorage.setStoragePath(storagePath);
            newStorage.setReferenceCount(1);
            storagesService.save(newStorage);

            // 4. 更新 Files 记录，关联 storage_id (逻辑不变)
            Files fileRecord = filesService.getById(fileId);
            if (fileRecord == null) {
                throw new IllegalStateException("文件记录丢失: " + fileId);
            }
            fileRecord.setStorageId(newStorage.getId());

            // 5. 如果是音乐文件，提取元数据 (逻辑不变, 使用 tempFile)
            if (Objects.equals(fileRecord.getFileType(), FileTypeEnum.MUSIC)) {
                handleMusicMetadata(tempFile.getAbsolutePath(), fileId);
            }

            // 6. 更新文件状态为可用 (逻辑不变)
            fileRecord.setFileStatus(FileStatusEnum.AVAILABLE);
            filesService.updateById(fileRecord);

            // 7. 更新传输日志为成功 (逻辑不变)
            updateTransferLog(logId, TransferStatusEnum.SUCCESS, null);

            log.info("文件处理成功. FileId: {}, LogId: {}", fileId, logId);

            // 8. WebSocket 通知 (逻辑不变)
            webSocketService.sendFileStatusUpdate(
                    fileRecord.getUserId(), fileId, logId, FileStatusEnum.AVAILABLE, TransferStatusEnum.SUCCESS, fileRecord.getFileName(), null, 100
            );


        } catch (Exception e) {
            log.error("异步文件处理失败. FileId: {}, LogId: {}", fileId, logId, e);
            // 发生任何异常，都将状态更新为失败
            updateFileStatus(fileId, FileStatusEnum.FAILED);
            updateTransferLog(logId, TransferStatusEnum.FAILED, e.getMessage());
            
            // WebSocket 通知前端文件处理失败
            Files fileRecord = filesService.getById(fileId);
            if (fileRecord != null) {
                webSocketService.sendFileStatusUpdate(
                        fileRecord.getUserId(), fileId, logId, FileStatusEnum.FAILED, TransferStatusEnum.FAILED, fileRecord.getFileName(), e.getMessage(), 0
                );
            }
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
