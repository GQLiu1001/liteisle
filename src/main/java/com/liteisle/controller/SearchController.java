package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.response.SearchContentResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "搜索接口")
public class SearchController {

    /**
     * 云盘页面全局搜索（递归所有文件文件夹）
     */
    @Operation(summary = "云盘页面全局搜索", description = "云盘页面全局搜索（递归所有文件文件夹）")
    @GetMapping("/search")
    public Result<List<SearchContentResp>> searchContent(@RequestParam String content) {
        return Result.success();
    }
}
