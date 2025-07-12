package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.TranslateReq;
import com.liteisle.common.domain.response.FileUploadAsyncResp;
import com.liteisle.common.domain.response.TranslateResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/translate")
@Tag(name = "划词翻译接口")
public class TranslateController {

    /**
     * 划词翻译
     */
    @Operation(summary = "划词翻译", description = "划词翻译")
    @PostMapping
    public Result<TranslateResp> uploadFile(@RequestBody TranslateReq req) {
        return Result.success();
    }
}
