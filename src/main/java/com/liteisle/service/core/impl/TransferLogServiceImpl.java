package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.common.dto.request.TransferStatusUpdateReq;
import com.liteisle.common.dto.response.TransferLogPageResp;
import com.liteisle.common.dto.response.TransferSummaryResp;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.common.enums.TransferTypeEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.FoldersService;
import com.liteisle.service.core.TransferLogService;
import com.liteisle.mapper.TransferLogMapper;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .eq("delete_time", null));
        long downloadCount = this.count(new QueryWrapper<TransferLog>()
                .eq("user_id", userId)
                .eq("transfer_type", TransferTypeEnum.DOWNLOAD)
                .eq("delete_time", null));

        return new TransferSummaryResp(uploadCount, downloadCount);
    }

    @Override
    public void updateTransferStatus(Long logId, TransferStatusUpdateReq req) {
        //由客户端在下载任务结束（成功、失败、取消）后调用，更新其在后端的日志状态
        Long userId = UserContextHolder.getUserId();

        TransferLog one = this.getOne(new QueryWrapper<TransferLog>()
                .eq("id", logId)
                .eq("user_id", userId)
                .eq("delete_time", null));

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
                .select("id")
                .isNull("delete_time"));
        list.forEach(log -> {
            this.deleteOneTransferLog(log.getId(), deleteFile);
        });
    }

    @Override
    public void cancelUploadMission(Long logId) {
        //TODO 链接 transfer websocket链 取消上传任务 减少用户额度
    }

    private void dealOneLog(Long userId, Long logId, Boolean deleteFile) {
        // 1. 验证并软删除传输日志本身
        TransferLog transferLog = this.getOne(new QueryWrapper<TransferLog>()
                .eq("id", logId)
                .eq("user_id", userId)
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

            // 关键优化：使用 if-else if 结构，因为 file_id 和 folder_id 是互斥的
            if (transferLog.getFileId() != null) {
                // 2.1 情况一：关联的是文件
                filesService.update(new UpdateWrapper<Files>()
                        .eq("id", transferLog.getFileId())
                        .eq("user_id", userId) // 确保操作的是自己的文件
                        .set("delete_time", new Date())); // 软删除文件

            } else if (transferLog.getFolderId() != null) {
                // 2.2 情况二：关联的是文件夹
                Long folderIdToDelete = transferLog.getFolderId();

                try {
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
                } catch (Exception e) {
                    throw new LiteisleException(e.getMessage());
                }
            }
        }
    }
}




