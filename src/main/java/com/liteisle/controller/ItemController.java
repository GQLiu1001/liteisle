package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.ItemsDeleteReq;
import com.liteisle.common.domain.request.ItemsOperationReq;
import com.liteisle.common.domain.request.ItemsRenameReq;
import com.liteisle.common.domain.request.SetOrderReq;
import com.liteisle.common.domain.response.ItemDetailResp;
import com.liteisle.service.business.ItemViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
@Tag(name = "项目操作接口")
public class ItemController {
    //TODO 待测试
    @Resource
    private ItemViewService itemViewService;

    /**
     * 重命名项目 (文件/文件夹)
     */
    @Operation(summary = "重命名项目 (文件/文件夹)", description = "重命名项目 (文件/文件夹)")
    @PutMapping("/rename")
    public Result<Void> renameItem(
            @RequestBody ItemsRenameReq req) {
        itemViewService.renameItem(req);
        return Result.success();
    }

    /**
     * 移动文件或文件夹
     */
    @Operation(summary = "移动文件或文件夹", description = "移动文件或文件夹")
    @PutMapping("/move")
    public Result<Void> moveItems(@RequestBody ItemsOperationReq req) {
        itemViewService.moveItems(req);
        return Result.success();
    }

    /**
     * 删除项目
     */
    @Operation(summary = "将一个或多个文件/文件夹移入回收站（软删除）。", description = "将一个或多个文件/文件夹移入回收站（软删除）。 (移入回收站)")
    @DeleteMapping
    public Result<Void> deleteItems(@RequestBody ItemsDeleteReq req) {
        itemViewService.deleteItems(req);
        return Result.success();
    }

    /**
     * 获取项目详情 (文件/文件夹)
     */
    @Operation(summary = "获取项目详情 (文件/文件夹)", description = "获取项目详情 (文件/文件夹)")
    @GetMapping("/{item_id}/detail")
    public Result<ItemDetailResp> getItemDetail(@PathVariable("item_id") Long itemId, @RequestParam String itemType) {
        ItemDetailResp resp = itemViewService.getItemDetail(itemId, itemType);
        return Result.success(resp);
    }


    /**
     * 复制项目 (文件/文件夹)
     */
    @Operation(summary = "复制项目 (文件/文件夹)", description = "复制项目 (文件/文件夹)")
    @PostMapping("/copy")
    public Result<Void> copyItems(@RequestBody ItemsOperationReq req) {
        itemViewService.copyItems(req);
        return Result.success();
    }

    /**
     * 自定义排序 (文件/文件夹)
     */
    @Operation(summary = "自定义排序 (文件/文件夹)", description = "自定义排序 (文件/文件夹)")
    @PutMapping("/{item_id}/set-order")
    public Result<Void> setItemOrder(
            @PathVariable("item_id") Long itemId,
            @RequestBody SetOrderReq req,
            @RequestParam String itemType) {
        itemViewService.setItemOrder(itemId, req, itemType);
        return Result.success();
    }
}
