package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.DocumentMetadata;
import com.liteisle.service.DocumentMetadataService;
import com.liteisle.mapper.DocumentMetadataMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【document_metadata(文档文件专属元数据表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class DocumentMetadataServiceImpl extends ServiceImpl<DocumentMetadataMapper, DocumentMetadata>
    implements DocumentMetadataService{

}




