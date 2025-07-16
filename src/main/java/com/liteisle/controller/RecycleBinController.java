package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.RecycleBinReq;
import com.liteisle.common.domain.response.RecycleBinContentResp;
import com.liteisle.service.business.RecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recycle-bin")
@Tag(name = "回收站接口")
public class RecycleBinController {
    //TODO 待测试
    @Resource
    private RecycleBinService recycleBinService;

    /**
     * 获取回收站内容
     */
    @Operation(summary = "获取回收站内容", description = "获取回收站内容")
    @GetMapping
    public Result<RecycleBinContentResp> getRecycleBinContent(@RequestParam(required = false) String content) {
        RecycleBinContentResp resp = recycleBinService.getRecycleBinContent(content);
        return Result.success(resp);
    }

    /**
     * 恢复回收站项目
     */
    @Operation(summary = "恢复回收站项目", description = "恢复回收站项目")
    @PostMapping("/restore")
    public Result<Void> restoreItems(@RequestBody RecycleBinReq req) {
        recycleBinService.restoreItems(req);
        return Result.success();
    }

    /**
     * 彻底删除回收站项目
     */
    @Operation(summary = "彻底删除回收站项目", description = "彻底删除回收站项目")
    @DeleteMapping("/items")
    public Result<Void> purgeItems(@RequestBody RecycleBinReq req) {
        recycleBinService.purgeItems(req);
        return Result.success();
    }

    /**
     * 清空回收站
     */
    @Operation(summary = "清空回收站", description = "清空回收站")
    @DeleteMapping("/all")
    public Result<Void> clearRecycleBin() {
        recycleBinService.clearRecycleBin();
        return Result.success();
    }
}
