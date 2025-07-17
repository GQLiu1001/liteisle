package com.liteisle.service;

import com.liteisle.common.domain.Files;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.dto.request.MarkdownCreateReq;
import com.liteisle.common.dto.request.MarkdownUpdateReq;
import com.liteisle.common.dto.response.*;

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
     *
     * @param content 用户可能的搜索内容
     * @return DocumentFile
     */
    CompletableFuture<List<DocumentViewResp.DocumentFile>> getDocumentViewWithContent(String content);

    /**
     * 获取文档页面的访问地址
     *
     * @param fileId 文件id
     * @return 访问地址
     */
    String getDocumentViewUrl(Long fileId);

    /**
     * 创建Markdown文档
     *
     * @param req 创建参数
     * @return 文件id
     */
    Long createMarkdown(MarkdownCreateReq req);

    /**
     * 获取Markdown文档内容
     *
     * @param fileId 文件id
     * @return 文档内容
     */
    MarkdownContentResp getMarkdownContent(Long fileId);

    /**
     * 更新Markdown文档内容
     *
     * @param fileId 文件id
     * @param req    更新参数
     */
    void updateMarkdown(Long fileId, MarkdownUpdateReq req);

    /**
     * 获取音乐页面 信息
     *
     * @param content 可选搜索内容
     * @return 音乐页面信息
     */
    CompletableFuture<List<MusicViewResp.MusicFile>> getMusicViewWithContent(String content);

    /**
     * 获取音乐播放链接
     *
     * @param fileId 文件id
     * @return 音乐播放链接
     */
    String getMusicPlayUrl(Long fileId);

    /**
     * 获取回收站页面信息
     *
     * @param content 可选搜索内容
     * @return 回收站页面信息
     */
    CompletableFuture<List<RecycleBinContentResp.FileItem>> getRecycleBinViewWithContent(String content);
    /**
     * 获取文件夹内容
     *
     * @param folderId 文件夹id
     * @param sortBy  排序方式
     * @param sortOrder 排序方式
     * @return 文件夹内容
     */
    CompletableFuture<List<FolderContentResp.FileItem>> getFolderContentWithSort(
            Long folderId, String sortBy, Long userId ,String sortOrder, String content);

    /**
     * 获取父文件夹下所有文件的size大小
     *
     * @param folderId 文件夹id
     * @param userId   分享人id
     * @return 父文件夹下所有文件的size大小
     */
    Long getFileTotalSizeFromFolderId(Long folderId,Long userId);
}
