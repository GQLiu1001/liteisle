package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.FolderCreateReq;
import com.liteisle.common.domain.response.FolderContentResp;
import com.liteisle.common.domain.response.FolderHierarchyResp;
import com.liteisle.service.FoldersService;
import com.liteisle.service.business.FolderViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folders")
@Tag(name = "文件夹接口")
public class FolderController {

    @Resource
    private FolderViewService folderViewService ;
    @Resource
    private FoldersService foldersService ;

    /**
     * 获取指定文件夹内容
     */
    @Operation(summary = "获取指定文件夹内容", description = "获取指定文件夹内容")
    @GetMapping("/{folder_id}")
    public Result<FolderContentResp> getFolderContent(
            @PathVariable("folder_id") Long folderId,
            @RequestParam(required = false) String sortBy) {
        FolderContentResp resp = folderViewService.getFolderContent(folderId, sortBy);
        return Result.success(resp);
    }

    /**
     * 创建文件夹
     */
    @Operation(summary = "创建文件夹", description = "创建文件夹")
    @PostMapping
    public Result<Void> createFolder(@RequestBody FolderCreateReq req) {
        foldersService.createFolder(req);
        return Result.success();
    }


    /**
     * 获取所有文件夹层级 (用于移动对话框)
     */
    @Operation(summary = "获取所有文件夹层级", description = "获取所有文件夹层级 (用于移动对话框)")
    @GetMapping("/hierarchy")
    public Result<List<FolderHierarchyResp>> getFolderHierarchy() {
        List<FolderHierarchyResp> resp = foldersService.getFolderHierarchy();
        return Result.success(resp);
    }

}
