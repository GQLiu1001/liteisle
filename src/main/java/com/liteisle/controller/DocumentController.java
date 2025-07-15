package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.MarkdownCreateReq;
import com.liteisle.common.domain.request.MarkdownUpdateReq;
import com.liteisle.common.domain.response.DocumentViewResp;
import com.liteisle.common.domain.response.MarkdownContentResp;
import com.liteisle.service.FilesService;
import com.liteisle.service.FoldersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
@Slf4j
@RestController
@RequestMapping("/documents")
@Tag(name = "文档接口")
public class DocumentController {

    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;

    /**
     * 获取文档页面信息
     */
    @Operation(summary = "获取文档页面信息", description = "获取文档页面信息")
    @GetMapping
    public Result<DocumentViewResp> getDocumentView(@RequestParam(required = false) String content) {
        // 并行执行两个异步查询
        CompletableFuture<List<DocumentViewResp.DocumentFile>> fileFuture = filesService.getDocumentViewWithContent(content);
        CompletableFuture<List<DocumentViewResp.Notebook>> folderFuture = foldersService.getDocumentViewWithContent(content);

        // 合并两个异步结果
        DocumentViewResp resp = CompletableFuture.allOf(fileFuture, folderFuture)
                .thenApply(v -> {
                    try {
                        return new DocumentViewResp(
                                folderFuture.get(),  // 获取文件夹列表结果
                                fileFuture.get()     // 获取文件列表结果
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("获取文档信息失败", e);
                    }
                })
                .exceptionally(ex -> {
                    log.error("获取文档页面信息失败", ex);
                    return new DocumentViewResp(Collections.emptyList(), Collections.emptyList());
                })
                .join();  // 等待所有任务完成

        return Result.success(resp);
    }

    /**
     * 获取非 MD 文档预览/下载链接
     */
    @Operation(summary = "获取非 MD 文档预览/下载链接", description = "获取非 MD 文档预览/下载链接")
    @GetMapping("/{file_id}/view")
    public Result<String> getDocumentViewUrl(@PathVariable("file_id") Long fileId) {
        return Result.success();
    }

    /**
     * 创建Markdown文档
     */
    @Operation(summary = "创建Markdown文档", description = "创建Markdown文档")
    @PostMapping("/md")
    public Result<Void> createMarkdown(@RequestBody MarkdownCreateReq req) {
        return Result.success();
    }

    /**
     * 获取Markdown文档内容
     */
    @Operation(summary = "获取Markdown文档内容", description = "获取Markdown文档内容")
    @GetMapping("/md/{file_id}")
    public Result<MarkdownContentResp> getMarkdownContent(@PathVariable("file_id") Long fileId) {
        return Result.success();
    }

    /**
     * 更新并保存Markdown文档内容
     */
    @Operation(summary = "更新并保存Markdown文档内容", description = "更新Markdown文档内容")
    @PutMapping("/md/{file_id}")
    public Result<Void> updateMarkdown(
            @PathVariable("file_id") Long fileId,
            @RequestBody MarkdownUpdateReq req) {
        return Result.success();
    }
}
