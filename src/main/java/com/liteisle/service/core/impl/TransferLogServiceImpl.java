package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.common.domain.Users;
import com.liteisle.common.dto.request.TransferStatusUpdateReq;
import com.liteisle.common.dto.response.TransferLogPageResp;
import com.liteisle.common.dto.response.TransferSummaryResp;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.common.enums.TransferTypeEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.business.WebSocketService;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.FoldersService;
import com.liteisle.service.core.StoragesService;
import com.liteisle.service.core.TransferLogService;
import com.liteisle.service.core.UsersService;
import com.liteisle.mapper.TransferLogMapper;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 11965
 * @description 针对表【transfer_log(文件上传下载行为日志表)】的数据库操作Service实现
 * @createDate 2025-07-10 20:09:48
 */
@Slf4j
@Service
public class TransferLogServiceImpl extends ServiceImpl<TransferLogMapper, TransferLog>
        implements TransferLogService {

    @Resource
    private TransferLogMapper transferLogMapper;
    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;
    @Resource
    private UsersService usersService;
    @Resource
    private StoragesService storagesService;
    @Resource
    private WebSocketService webSocketService;

    @Override
    public IPage<TransferLogPageResp.TransferRecord> getTransferLogs(IPage<TransferLogPageResp.TransferRecord> page, String status) {
        //校验合理性
        try {
            TransferStatusEnum transferStatusEnum = TransferStatusEnum.fromValue(status);
            // 校验通过，可继续后续业务逻辑，比如判断状态等
        } catch (IllegalArgumentException e) {
            throw new LiteisleException("参数错误，传入的传输状态不合法");
        }
        Long userId = UserContextHolder.getUserId();
        return transferLogMapper.getTransferLogs(page, status, userId);
    }

    @Override
    public TransferSummaryResp getTransferSummary() {
        Long userId = UserContextHolder.getUserId();

        long uploadCount = this.count(new QueryWrapper<TransferLog>()
                .eq("user_id", userId)
                .eq("transfer_type", TransferTypeEnum.UPLOAD)
                .isNull("delete_time"));
        long downloadCount = this.count(new QueryWrapper<TransferLog>()
                .eq("user_id", userId)
                .eq("transfer_type", TransferTypeEnum.DOWNLOAD)
                .isNull("delete_time"));

        return new TransferSummaryResp(uploadCount, downloadCount);
    }

    @Override
    public void updateTransferStatus(Long logId, TransferStatusUpdateReq req) {
        //由客户端在下载任务结束（成功、失败、取消）后调用，更新其在后端的日志状态
        Long userId = UserContextHolder.getUserId();

        TransferLog one = this.getOne(new QueryWrapper<TransferLog>()
                .eq("id", logId)
                .eq("user_id", userId)
                .isNull("delete_time"));

        if (one == null || !one.getLogStatus().equals(TransferStatusEnum.PROCESSING)) {
            throw new LiteisleException("所选log不存在");
        }

        one.setLogStatus(req.getLogStatus());
        one.setErrorMessage(req.getErrorMessage());
        one.setTransferDurationMs(req.getTransferDurationMs());
        one.setUpdateTime(new Date());
        boolean update = this.updateById(one);
        if (!update) {
            throw new LiteisleException("更新失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteOneTransferLog(Long logId, Boolean deleteFile) {
        Long userId = UserContextHolder.getUserId();
        dealOneLog(userId, logId, deleteFile);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void completedCleanTransferLog(Boolean deleteFile) {
        Long userId = UserContextHolder.getUserId();
        List<TransferLog> list = this.list(new QueryWrapper<TransferLog>()
                .eq("user_id", userId)
                .eq("log_status", "success")
                .select("id")
                .isNull("delete_time"));
        list.forEach(log -> {
            this.deleteOneTransferLog(log.getId(), deleteFile);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelUploadMission(Long logId) {
        Long userId = UserContextHolder.getUserId();

        // 1. 验证传输日志是否存在且属于当前用户
        TransferLog transferLog = this.getOne(new QueryWrapper<TransferLog>()
                .eq("id", logId)
                .eq("user_id", userId)
                .eq("transfer_type", TransferTypeEnum.UPLOAD)
                .eq("log_status", TransferStatusEnum.PROCESSING)
                .isNull("delete_time"));

        if (transferLog == null) {
            throw new LiteisleException("上传任务不存在或已完成，无法取消");
        }

        try {
            // 2. 更新传输日志状态为取消
            transferLog.setLogStatus(TransferStatusEnum.CANCELED);
            transferLog.setErrorMessage("用户主动取消上传");
            transferLog.setUpdateTime(new Date());
            this.updateById(transferLog);

            // 3. 处理关联的文件或文件夹
            String fileName = "";
            Long fileSize = 0L;

            if (transferLog.getFileId() != null) {
                // 处理单个文件
                Files file = filesService.getById(transferLog.getFileId());
                if (file != null) {
                    fileName = file.getFileName();
                    // 通过storageId获取文件大小
                    if (file.getStorageId() != null) {
                        Storages storage = storagesService.getById(file.getStorageId());
                        if (storage != null) {
                            fileSize = storage.getFileSize();
                        }
                    }

                    // 物理删除文件记录（直接删除，不放入回收站）
                    filesService.removeById(transferLog.getFileId());

                    // 如果文件已经有storage_id，需要减少引用计数
                    if (file.getStorageId() != null) {
                        storagesService.update(new UpdateWrapper<Storages>()
                                .eq("id", file.getStorageId())
                                .setSql("reference_count = reference_count - 1"));
                    }
                }
            } else if (transferLog.getFolderId() != null) {
                // 处理文件夹
                Folders folder = foldersService.getById(transferLog.getFolderId());
                if (folder != null) {
                    fileName = folder.getFolderName();

                    // 获取文件夹下所有处理中的文件
                    List<Files> folderFiles = filesService.list(new QueryWrapper<Files>()
                            .eq("folder_id", transferLog.getFolderId())
                            .eq("user_id", userId)
                            .eq("file_status", FileStatusEnum.PROCESSING));

                    // 计算总文件大小并物理删除文件
                    if (!folderFiles.isEmpty()) {
                        List<Long> fileIdsToDelete = new ArrayList<>();
                        for (Files file : folderFiles) {
                            fileIdsToDelete.add(file.getId());

                            // 通过storageId获取文件大小
                            if (file.getStorageId() != null) {
                                Storages storage = storagesService.getById(file.getStorageId());
                                if (storage != null && storage.getFileSize() != null) {
                                    fileSize += storage.getFileSize();
                                }

                                // 如果文件已经有storage_id，需要减少引用计数
                                storagesService.update(new UpdateWrapper<Storages>()
                                        .eq("id", file.getStorageId())
                                        .setSql("reference_count = reference_count - 1"));
                            }
                        }

                        // 批量物理删除文件记录
                        filesService.removeByIds(fileIdsToDelete);
                    }

                    // 物理删除文件夹
                    foldersService.removeById(transferLog.getFolderId());
                }
            }

            // 4. 恢复用户存储配额
            if (fileSize > 0) {
                usersService.update(new UpdateWrapper<Users>()
                        .eq("id", userId)
                        .setSql("storage_used = storage_used - " + fileSize));
            }

            // 5. 发送WebSocket通知
            webSocketService.sendFileStatusUpdate(
                    userId,
                    transferLog.getFileId(),
                    logId,
                    null, // 文件已被物理删除，不需要状态
                    TransferStatusEnum.CANCELED,
                    fileName,
                    "上传任务已被用户取消",
                    0
            );

            log.info("用户 {} 成功取消上传任务，logId: {}, 恢复存储空间: {} bytes", userId, logId, fileSize);

        } catch (Exception e) {
            log.error("取消上传任务失败，logId: {}, userId: {}", logId, userId, e);
            throw new LiteisleException("取消上传任务失败: " + e.getMessage());
        }
    }

    private void dealOneLog(Long userId, Long logId, Boolean deleteFile) {
        // 1. 验证并软删除传输日志本身 (这部分逻辑保持不变，是正确的)
        TransferLog transferLog = this.getOne(new QueryWrapper<TransferLog>()
                .eq("id", logId)
                .eq("user_id", userId)
                .eq("log_status", "success")
                .isNull("delete_time"));

        if (transferLog == null) {
            throw new LiteisleException("所选的传输记录不存在或已被删除");
        }

        boolean logUpdated = this.update(new UpdateWrapper<TransferLog>()
                .eq("id", logId)
                .set("delete_time", new Date()));

        if (!logUpdated) {
            throw new LiteisleException("删除传输记录失败");
        }

        // 2. 如果用户选择同时“删除”关联项目，并且是上传记录
        if (deleteFile && transferLog.getTransferType() == TransferTypeEnum.UPLOAD) {

            // 情况一：如果 file_id 存在，说明这是一条【单文件上传】的记录
            if (transferLog.getFileId() != null) {

                // 只需要软删除这个文件即可
                filesService.update(new UpdateWrapper<Files>()
                        .eq("id", transferLog.getFileId())
                        .eq("user_id", userId)
                        .set("delete_time", new Date()));

                // 情况二：如果 file_id 为 null，说明这是一条【文件夹转存】的记录
            } else if (transferLog.getFolderId() != null) {
                // （为严谨起见，加上 getFolderId() != null 的判断）

                Long folderIdToDelete = transferLog.getFolderId();

                // a. 软删除文件夹本身
                foldersService.update(new UpdateWrapper<Folders>()
                        .eq("id", folderIdToDelete)
                        .eq("user_id", userId)
                        .set("delete_time", new Date()));

                // b. 软删除该文件夹下的所有文件
                filesService.update(new UpdateWrapper<Files>()
                        .eq("folder_id", folderIdToDelete)
                        .eq("user_id", userId)
                        .set("delete_time", new Date()));
            }
        }
    }
}



