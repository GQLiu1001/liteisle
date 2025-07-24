package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.dto.response.FileUploadAsyncResp;
import com.liteisle.service.business.FileUploadService;
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
            @RequestParam("folder_id") Long folderId) {
        FileUploadAsyncResp resp = fileUploadService.uploadFile(file, folderId);
        return Result.success(resp);
    }
}
