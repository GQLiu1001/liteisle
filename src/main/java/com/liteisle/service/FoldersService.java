package com.liteisle.service;

import com.liteisle.common.domain.Folders;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.domain.response.DocumentViewResp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
* @author 11965
* @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Service
* @createDate 2025-07-10 20:09:48
*/
public interface FoldersService extends IService<Folders> {
    /**
     * 系统创建用户默认文件夹
     * @param userId 用户di
     */
    void createUserDefaultFolder(Long userId);

    /**
     * 按需获取用户文档页面的文档分类信息
     * @param content 用户可能的搜索内容
     * @return
     */
    CompletableFuture<List<DocumentViewResp.Notebook>> getDocumentViewWithContent(String content);
}
