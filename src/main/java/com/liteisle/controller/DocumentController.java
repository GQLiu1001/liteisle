package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.dto.request.MarkdownCreateReq;
import com.liteisle.common.dto.request.MarkdownUpdateReq;
import com.liteisle.common.dto.response.DocumentViewResp;
import com.liteisle.common.dto.response.MarkdownContentResp;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.view.DocumentViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/documents")
@Tag(name = "文档接口")
public class DocumentController {

    @Resource
    private FilesService filesService;
    @Resource
    private DocumentViewService documentViewService;

    /**
     * 获取文档页面信息
     */
    @Operation(summary = "获取文档页面信息", description = "获取文档页面信息")
    @GetMapping
    public Result<DocumentViewResp> getDocumentView(@RequestParam(required = false) String content) {
        DocumentViewResp resp =  documentViewService.getDocumentView(content);
        return Result.success(resp);
    }

    /**
     * 获取非 MD 文档预览/下载链接
     */
    @Operation(summary = "获取非 MD 文档预览/下载链接", description = "获取非 MD 文档预览/下载链接")
    @GetMapping("/{file_id}/view")
    public Result<String> getDocumentViewUrl(@PathVariable("file_id") Long fileId) {
        String url = filesService.getDocumentViewUrl(fileId);
        return Result.success(url);
    }

    /**
     * 创建Markdown文档
     */
    @Operation(summary = "创建Markdown文档", description = "创建Markdown文档")
    @PostMapping("/md")
    public Result<Long> createMarkdown(@RequestBody MarkdownCreateReq req) {
        Long fileId = filesService.createMarkdown(req);
        return Result.success(fileId);
    }

    /**
     * 获取Markdown文档内容
     */
    @Operation(summary = "获取Markdown文档内容", description = "获取Markdown文档内容")
    @GetMapping("/md/{file_id}")
    public Result<MarkdownContentResp> getMarkdownContent(@PathVariable("file_id") Long fileId) {
        MarkdownContentResp resp = filesService.getMarkdownContent(fileId);
        return Result.success(resp);
    }

    /**
     * 更新并保存Markdown文档内容
     */
    @Operation(summary = "更新并保存Markdown文档内容", description = "更新Markdown文档内容")
    @PutMapping("/md/{file_id}")
    public Result<Void> updateMarkdown(
            @PathVariable("file_id") Long fileId,
            @RequestBody MarkdownUpdateReq req) {
        filesService.updateMarkdown(fileId, req);
        return Result.success();
    }
}
