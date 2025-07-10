package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Storages;
import com.liteisle.service.StoragesService;
import com.liteisle.mapper.StoragesMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【storages(存储唯一文件实体，避免重复上传)】的数据库操作Service实现
* @createDate 2025-07-10 09:31:28
*/
@Service
public class StoragesServiceImpl extends ServiceImpl<StoragesMapper, Storages>
    implements StoragesService{

}




