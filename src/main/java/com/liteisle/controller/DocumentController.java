package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.MarkdownCreateReq;
import com.liteisle.common.domain.request.MarkdownUpdateReq;
import com.liteisle.common.domain.response.DocumentViewResp;
import com.liteisle.common.domain.response.MarkdownContentResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
@Tag(name = "文档接口")
public class DocumentController {


    /**
     * 获取文档页面信息
     */
    @Operation(summary = "获取文档页面信息", description = "获取文档页面信息")
    @GetMapping
    public Result<DocumentViewResp> getDocumentView(@RequestParam(required = false) String content) {
        return Result.success();
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
