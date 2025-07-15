package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.response.FileUploadAsyncResp;
import com.liteisle.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "上传接口")
public class UploadController {

    @Resource
    private FileUploadService fileUploadService;

    /**
     * 上传文件 (异步处理)
     */
    @Operation(summary = "上传文件", description = "上传文件 (异步处理)")
    @PostMapping("/upload")
    public Result<FileUploadAsyncResp> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long folderId) {
        FileUploadAsyncResp resp = fileUploadService.uploadFile(file, folderId);
        return Result.success(resp);
    }

    /**
     * 图片上传接口（专门处理md文档图片）
     */
    @Operation(summary = "图片上传接口（专门处理md文档图片）", description = "图片上传接口（专门处理md文档图片）")
    @PostMapping("/upload/image")
    public Result<String> uploadMdImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long file_id) {
        String url = fileUploadService.uploadMdImage(file, file_id);
        return Result.success(url);
    }
}
