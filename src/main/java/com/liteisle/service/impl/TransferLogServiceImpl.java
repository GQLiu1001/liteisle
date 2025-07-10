package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.service.TransferLogService;
import com.liteisle.mapper.TransferLogMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【transfer_log(文件上传下载行为日志)】的数据库操作Service实现
* @createDate 2025-07-10 09:31:28
*/
@Service
public class TransferLogServiceImpl extends ServiceImpl<TransferLogMapper, TransferLog>
    implements TransferLogService{

}




