package com.liteisle.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.liteisle.common.domain.TransferLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteisle.common.dto.response.TransferLogPageResp;

/**
* @author 11965
* @description 针对表【transfer_log(文件上传下载行为日志表)】的数据库操作Mapper
* @createDate 2025-07-10 20:09:48
* @Entity com.liteisle.common.domain.TransferLog
*/
public interface TransferLogMapper extends BaseMapper<TransferLog> {

    IPage<TransferLogPageResp.TransferRecord> getTransferLogs(IPage<TransferLogPageResp.TransferRecord> page, String status,Long userId);
}




