package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.ShareCreateReq;
import com.liteisle.common.domain.request.ShareSaveReq;
import com.liteisle.common.domain.request.ShareVerifyReq;
import com.liteisle.common.domain.response.ShareCreateResp;
import com.liteisle.common.domain.response.ShareInfoResp;
import com.liteisle.common.domain.response.ShareRecordPageResp;
import com.liteisle.common.domain.response.ShareSaveAsyncResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shares")
@Tag(name = "分享接口")
public class ShareController {

    /**
     * 创建分享链接
     */
    @Operation(summary = "创建分享链接", description = "创建分享链接")
    @PostMapping
    public Result<ShareCreateResp> createShare(@RequestBody ShareCreateReq req) {
        return Result.success();
    }

    /**
     * 验证分享链接
     */
    @Operation(summary = "验证分享链接", description = "验证分享链接")
    @PostMapping("/verify")
    public Result<ShareInfoResp> verifyShare(@RequestBody ShareVerifyReq req) {
        return Result.success();
    }

    /**
     * 保存分享内容到自己的云盘
     */
    @Operation(summary = "保存分享内容到自己的云盘", description = "保存分享内容到自己的云盘")
    @PostMapping("/save")
    public Result<ShareSaveAsyncResp> saveShare(@RequestBody ShareSaveReq req) {
        return Result.success();
    }

    /**
     * 获取我的分享记录
     */
    @Operation(summary = "获取我的分享记录", description = "获取我的分享记录")
    @GetMapping("/me")
    public Result<ShareRecordPageResp> getShareRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success();
    }

    /**
     * 删除分享链接
     */
    @Operation(summary = "删除/取消分享链接，使其链接和提取码失效。", description = "删除分享链接")
    @DeleteMapping("/{share_id}")
    public Result<Void> deleteShare(@PathVariable("share_id") Long shareId) {
        return Result.success();
    }
}
