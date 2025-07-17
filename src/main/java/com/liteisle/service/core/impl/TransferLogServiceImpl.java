package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.service.core.TransferLogService;
import com.liteisle.mapper.TransferLogMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【transfer_log(文件上传下载行为日志表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class TransferLogServiceImpl extends ServiceImpl<TransferLogMapper, TransferLog>
    implements TransferLogService{

}




