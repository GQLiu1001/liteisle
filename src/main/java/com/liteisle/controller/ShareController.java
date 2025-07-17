package com.liteisle.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteisle.common.Result;
import com.liteisle.common.dto.request.ShareCreateReq;
import com.liteisle.common.dto.request.ShareSaveReq;
import com.liteisle.common.dto.request.ShareVerifyReq;
import com.liteisle.common.dto.response.ShareCreateResp;
import com.liteisle.common.dto.response.ShareInfoResp;
import com.liteisle.common.dto.response.ShareRecordPageResp;
import com.liteisle.common.dto.response.ShareSaveAsyncResp;
import com.liteisle.service.core.ShareLinksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shares")
@Tag(name = "分享接口")
public class ShareController {

    @Resource
    private ShareLinksService shareLinksService;

    /**
     * 创建分享链接
     */
    @Operation(summary = "创建分享链接", description = "创建分享链接")
    @PostMapping
    public Result<ShareCreateResp> createShare(@RequestBody ShareCreateReq req) {
        ShareCreateResp resp = shareLinksService.createShare(req);
        return Result.success(resp);
    }

    /**
     * 验证分享链接
     */
    @Operation(summary = "验证分享链接", description = "验证分享链接")
    @PostMapping("/verify")
    public Result<ShareInfoResp> verifyShare(@RequestBody ShareVerifyReq req) {
        ShareInfoResp resp = shareLinksService.verifyShare(req);
        return Result.success(resp);
    }

    /**
     * 保存分享内容到自己的云盘
     */
    @Operation(summary = "保存分享内容到自己的云盘", description = "保存分享内容到自己的云盘")
    @PostMapping("/save")
    public Result<ShareSaveAsyncResp> saveShare(@RequestBody ShareSaveReq req) {
        ShareSaveAsyncResp resp = shareLinksService.saveShare(req);
        return Result.success(resp);
    }

    /**
     * 获取我的分享记录
     */
    @Operation(summary = "获取我的分享记录", description = "获取我的分享记录")
    @GetMapping("/me")
    public Result<ShareRecordPageResp> getShareRecords(
            @RequestParam(defaultValue = "1",name = "page") Integer current,
            @RequestParam(defaultValue = "10",name = "size") Integer size) {
        IPage<ShareRecordPageResp.ShareRecord> page = new Page<>(current, size);
        IPage<ShareRecordPageResp.ShareRecord> pageData = shareLinksService.getShareRecords(page);
        ShareRecordPageResp shareRecordPageResp = new ShareRecordPageResp();
        shareRecordPageResp.setCurrentPage(pageData.getCurrent());
        shareRecordPageResp.setPageSize(pageData.getSize());
        shareRecordPageResp.setTotal(pageData.getTotal());
        shareRecordPageResp.setRecords(pageData.getRecords());
        return Result.success(shareRecordPageResp);
    }

    /**
     * 删除分享链接
     */
    @Operation(summary = "删除/取消分享链接，使其链接和提取码失效。", description = "删除分享链接")
    @DeleteMapping("/{share_id}")
    public Result<Void> deleteShare(@PathVariable("share_id") Long shareId) {
        shareLinksService.deleteShare(shareId);
        return Result.success();
    }
}
