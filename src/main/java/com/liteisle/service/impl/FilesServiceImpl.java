package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Files;
import com.liteisle.service.FilesService;
import com.liteisle.mapper.FilesMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【files(文件元数据及处理状态)】的数据库操作Service实现
* @createDate 2025-07-10 09:31:27
*/
@Service
public class FilesServiceImpl extends ServiceImpl<FilesMapper, Files>
    implements FilesService{

}




