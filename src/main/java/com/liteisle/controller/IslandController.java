package com.liteisle.controller;

import com.liteisle.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/islands")
@Tag(name = "岛屿接口")
public class IslandController {

    /**
     * 获取岛屿收藏页面数据
     */
    @Operation(summary = "获取岛屿收藏页面数据，返回用户所获得的岛屿url的String列表", description = "获取岛屿收藏页面数据")
    @GetMapping("/me")
    public Result<List<String>> getIslandCollection() {
        return Result.success();
    }

}
