package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.FolderCreateReq;
import com.liteisle.common.domain.request.SetOrderReq;
import com.liteisle.common.domain.response.FolderContentResp;
import com.liteisle.common.domain.response.FolderHierarchyResp;
import com.liteisle.common.domain.response.FolderInfoResp;
import com.liteisle.common.domain.response.ItemDetailResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folders")
@Tag(name = "文件夹接口")
public class FolderController {

    /**
     * 获取指定文件夹内容
     */
    @Operation(summary = "获取指定文件夹内容", description = "获取指定文件夹内容")
    @GetMapping("/{folder_id}")
    public Result<FolderContentResp> getFolderContent(
            @PathVariable("folder_id") Long folderId,
            @RequestParam(required = false) String sortBy) {
        return Result.success();
    }

    /**
     * 创建文件夹
     */
    @Operation(summary = "创建文件夹", description = "创建文件夹")
    @PostMapping
    public Result<FolderInfoResp> createFolder(@RequestBody FolderCreateReq req) {
        return Result.success();
    }


    /**
     * 获取所有文件夹层级 (用于移动对话框)
     */
    @Operation(summary = "获取所有文件夹层级", description = "获取所有文件夹层级 (用于移动对话框)")
    @GetMapping("/hierarchy")
    public Result<List<FolderHierarchyResp>> getFolderHierarchy() {
        return Result.success();
    }

}
