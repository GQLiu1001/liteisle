package com.liteisle.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteisle.common.Result;
import com.liteisle.common.dto.response.FocusCalendarResp;
import com.liteisle.common.dto.response.FocusStatsPageResp;
import com.liteisle.service.UserFocusRecordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/focus")
@Tag(name = "专注接口")
public class FocusController {

    @Resource
    private UserFocusRecordsService userFocusRecordsService;

    /**
     * 创建专注记录
     */
    @Operation(summary = "创建专注记录，有概率会返回新获得岛屿url", description = "创建专注记录")
    @PostMapping("/records")
    public Result<String> createFocusRecord(@RequestParam("focus_minutes") Integer min) {
        String url = userFocusRecordsService.createFocusRecord(min);
        return url==null?Result.success():Result.success(url);
    }

    /**
     * 获取专注总次数
     */
    @Operation(summary = "获取专注总次数", description = "获取专注总次数")
    @GetMapping("/stats/total-count")
    public Result<Long> getFocusTotalCount() {
        Long count = userFocusRecordsService.getFocusTotalCount();
        return Result.success(count);
    }

    /**
     * 获取专注记录列表
     */
    @Operation(summary = "获取专注记录列表", description = "获取专注记录列表")
    @GetMapping("/stats/records")
    public Result<FocusStatsPageResp> getFocusRecords(
            @RequestParam(defaultValue = "1", name = "page") Integer current,
            @RequestParam(defaultValue = "10",name = "size") Integer size) {
        IPage<FocusStatsPageResp.FocusRecord> page = new Page<>(current, size);
        IPage<FocusStatsPageResp.FocusRecord> pageData = userFocusRecordsService.getFocusRecords(page);
        FocusStatsPageResp focusStatsPageResp = new FocusStatsPageResp();
        focusStatsPageResp.setCurrentPage(pageData.getCurrent());
        focusStatsPageResp.setPageSize(pageData.getSize());
        focusStatsPageResp.setTotal(pageData.getTotal());
        focusStatsPageResp.setRecords(pageData.getRecords());
        return Result.success(focusStatsPageResp);
    }

    /**
     * 获取专注日历数据
     */
    @Operation(summary = "获取专注日历数据", description = "获取专注日历数据")
    @GetMapping("/stats/calendar")
    public Result<FocusCalendarResp> getFocusCalendar(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        FocusCalendarResp resp = userFocusRecordsService.getFocusCalendar(year, month);
        return Result.success(resp);
    }
}
