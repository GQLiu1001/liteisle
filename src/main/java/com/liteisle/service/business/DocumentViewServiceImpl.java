package com.liteisle.service.business;

import com.liteisle.common.domain.response.DocumentViewResp;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.impl.FilesServiceImpl;
import com.liteisle.service.impl.FoldersServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
@Slf4j
@Service
public class DocumentViewServiceImpl implements DocumentViewService {
    @Resource
    private FilesServiceImpl filesService;
    @Resource
    private FoldersServiceImpl foldersService;
    @Override
    public DocumentViewResp getDocumentView(String content) {
        // 并行执行两个异步查询
        CompletableFuture<List<DocumentViewResp.DocumentFile>> fileFuture = filesService.getDocumentViewWithContent(content);
        CompletableFuture<List<DocumentViewResp.Booklist>> folderFuture = foldersService.getDocumentViewWithContent(content);

        return CompletableFuture.allOf(fileFuture, folderFuture)
                .thenApply(v -> {
                    try {
                        return new DocumentViewResp(
                                folderFuture.get(),  // 获取文件夹列表结果
                                fileFuture.get()     // 获取文件列表结果
                        );
                    } catch (Exception e) {
                        throw new LiteisleException("获取文档信息失败"+e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("获取文档页面信息失败", ex);
                    return new DocumentViewResp(Collections.emptyList(), Collections.emptyList());
                })
                .join();
    }
}
