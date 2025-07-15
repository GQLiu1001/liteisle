package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.TranslateReq;
import com.liteisle.common.domain.response.TranslateResp;
import com.liteisle.service.business.TranslateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/translate")
@Tag(name = "划词翻译接口")
public class TranslateController {
    @Resource
    private TranslateService translateService;

    @Operation(summary = "划词翻译", description = "接收文本和可选的文件名上下文，返回翻译结果")
    @PostMapping
    public Result<TranslateResp> translate(@RequestBody TranslateReq req) {
        TranslateResp resp = translateService.translate(req);
        return Result.success(resp);
    }
}
