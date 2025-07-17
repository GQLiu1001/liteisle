package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.common.dto.request.TransferStatusUpdateReq;
import com.liteisle.common.dto.response.TransferLogPageResp;
import com.liteisle.common.dto.response.TransferSummaryResp;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.common.enums.TransferTypeEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.TransferLogService;
import com.liteisle.mapper.TransferLogMapper;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author 11965
* @description 针对表【transfer_log(文件上传下载行为日志表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class TransferLogServiceImpl extends ServiceImpl<TransferLogMapper, TransferLog>
    implements TransferLogService{

    @Resource
    private TransferLogMapper transferLogMapper;
    @Resource
    private FilesService filesService;

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
        return transferLogMapper.getTransferLogs(page, status,userId);
    }

    @Override
    public TransferSummaryResp getTransferSummary() {
        Long userId = UserContextHolder.getUserId();

        long uploadCount = this.count(new QueryWrapper<TransferLog>()
                .eq("user_id", userId)
                .eq("transfer_type", TransferTypeEnum.UPLOAD));
        long downloadCount = this.count(new QueryWrapper<TransferLog>()
                .eq("user_id", userId)
                .eq("transfer_type", TransferTypeEnum.DOWNLOAD));

        return new TransferSummaryResp(uploadCount, downloadCount);
    }

    @Override
    public void updateTransferStatus(Long logId, TransferStatusUpdateReq req) {
        //由客户端在下载任务结束（成功、失败、取消）后调用，更新其在后端的日志状态
        Long userId = UserContextHolder.getUserId();

        TransferLog one = this.getOne(new QueryWrapper<TransferLog>()
                .eq("id", logId)
                .eq("user_id", userId));

        if (one == null || !one.getLogStatus().equals(TransferStatusEnum.PROCESSING)) {
            throw new LiteisleException("所选log不存在");
        }

        one.setLogStatus(req.getLogStatus());
        one.setErrorMessage(req.getErrorMessage());
        one.setTransferDurationMs(req.getTransferDurationMs());
        one.setUpdateTime(new Date());
        boolean update = this.updateById(one);
        if (!update){
            throw new LiteisleException("更新失败");
        }
    }
}




