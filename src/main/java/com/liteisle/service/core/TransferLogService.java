package com.liteisle.service.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.liteisle.common.domain.TransferLog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.dto.request.TransferStatusUpdateReq;
import com.liteisle.common.dto.response.TransferLogPageResp;
import com.liteisle.common.dto.response.TransferSummaryResp;

/**
* @author 11965
* @description 针对表【transfer_log(文件上传下载行为日志表)】的数据库操作Service
* @createDate 2025-07-10 20:09:48
*/
public interface TransferLogService extends IService<TransferLog> {
    /**
     * 获取传输历史记录
     * @param page 分页参数
     * @param status 可选文件状态
     * @return 分页结果
     */
    IPage<TransferLogPageResp.TransferRecord> getTransferLogs(IPage<TransferLogPageResp.TransferRecord> page, String status);
    /**
     * 获取传输统计摘要
     * @return 摘要结果
     */
    TransferSummaryResp getTransferSummary();
    /**
     * 更新下载任务传输状态
     * @param logId 日志ID
     * @param req 更新参数
     */
    void updateTransferStatus(Long logId, TransferStatusUpdateReq req);
    /**
     * 删除单条传输记录,从传输列表中删除一条记录
     * @param logId 日志ID
     * @param deleteFile 是否删除文件
     */
    void deleteOneTransferLog(Long logId, Boolean deleteFile);
    /**
     * 清空已完成的传输记录
     * @param deleteFile 是否删除文件
     */
    void completedCleanTransferLog(Boolean deleteFile);
}
