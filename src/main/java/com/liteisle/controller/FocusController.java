package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.FocusRecordReq;
import com.liteisle.common.domain.response.FocusCalendarResp;
import com.liteisle.common.domain.response.FocusStatsPageResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/focus")
@Tag(name = "专注接口")
public class FocusController {

    /**
     * 创建专注记录
     */
    @Operation(summary = "创建专注记录，有概率会返回新获得岛屿url", description = "创建专注记录")
    @PostMapping("/records")
    public Result<String> createFocusRecord(@RequestBody FocusRecordReq req) {
        return Result.success();
    }

    /**
     * 获取专注总次数
     */
    @Operation(summary = "获取专注总次数", description = "获取专注总次数")
    @GetMapping("/stats/total-count")
    public Result<Integer> getFocusTotalCount() {
        return Result.success();
    }

    /**
     * 获取专注记录列表
     */
    @Operation(summary = "获取专注记录列表", description = "获取专注记录列表")
    @GetMapping("/stats/records")
    public Result<FocusStatsPageResp> getFocusRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success();
    }

    /**
     * 获取专注日历数据
     */
    @Operation(summary = "获取专注日历数据", description = "获取专注日历数据")
    @GetMapping("/stats/calendar")
    public Result<FocusCalendarResp> getFocusCalendar(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success();
    }
}
