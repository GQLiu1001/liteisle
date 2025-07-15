package com.liteisle.service;

import com.liteisle.common.domain.Files;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.domain.response.DocumentViewResp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
* @author 11965
* @description 针对表【files(文件通用基础信息表)】的数据库操作Service
* @createDate 2025-07-10 20:09:48
*/
public interface FilesService extends IService<Files> {
    /**
     * 按需获取文档页面具体的文件信息
     * @param content 用户可能的搜索内容
     * @return
     */
    CompletableFuture<List<DocumentViewResp.DocumentFile>> getDocumentViewWithContent(String content);
}
