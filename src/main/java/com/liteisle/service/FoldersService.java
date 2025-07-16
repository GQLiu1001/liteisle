package com.liteisle.service;

import com.liteisle.common.domain.Folders;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.domain.request.FolderCreateReq;
import com.liteisle.common.domain.response.DocumentViewResp;
import com.liteisle.common.domain.response.FolderHierarchyResp;
import com.liteisle.common.domain.response.MusicViewResp;

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
     * @return 文档分类信息
     */
    CompletableFuture<List<DocumentViewResp.Booklist>> getDocumentViewWithContent(String content);

    /**
     * 创建文件夹
     *
     * @param req 文件夹创建请求
     */
    void createFolder(FolderCreateReq req);

    /**
     * 获取所有文件夹层级
     * @return 文件夹层级信息
     */
    List<FolderHierarchyResp> getFolderHierarchy();
    /**
     * 获取音乐分类信息
     *
     * @param content 搜索内容
     * @return 音乐分类信息
     */
    CompletableFuture<List<MusicViewResp.Playlist>> getMusicViewWithContent(String content);
}
