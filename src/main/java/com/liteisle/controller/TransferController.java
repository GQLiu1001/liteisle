package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.TransferStatusUpdateReq;
import com.liteisle.common.domain.response.TransferLogPageResp;
import com.liteisle.common.domain.response.TransferSummaryResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
@Tag(name = "传输接口")
public class TransferController {

    /**
     * 获取传输历史记录
     */
    @Operation(summary = "获取传输历史记录", description = "获取传输历史记录")
    @GetMapping
    public Result<TransferLogPageResp> getTransferLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        return Result.success();
    }

    /**
     * 获取传输统计摘要
     */
    @Operation(summary = "获取传输统计摘要", description = "获取传输统计摘要")
    @GetMapping("/summary")
    public Result<TransferSummaryResp> getTransferSummary() {
        return Result.success();
    }

    /**
     * 更新传输状态
     */
    @Operation(summary = "更新传输状态", description = "由客户端在下载任务结束（成功、失败、取消）后，或任何由客户端发起的取消操作后调用，以更新其在后端的日志状态。")
    @PutMapping("/{log_id}/status")
    public Result<Void> updateTransferStatus(@PathVariable("log_id") Long logId, @RequestBody TransferStatusUpdateReq req) {
        return Result.success();
    }

    /**
     * 取消传输任务
     */
    @Operation(summary = "取消传输任务,从传输列表中删除一条记录。", description = "取消传输任务")
    @DeleteMapping("/{log_id}")
    public Result<Void> cancelTransfer(@PathVariable("log_id") Long logId,@RequestParam(defaultValue = "false") Boolean deleteFile) {
        return Result.success();
    }

    /**
     * 清空已完成的传输记录
     */
    @Operation(summary = "清空已完成的传输记录。", description = "清空已完成的传输记录")
    @DeleteMapping("/completed")
    public Result<Void> completedCleanTransferLog(@RequestParam(defaultValue = "false") Boolean deleteFile) {
        return Result.success();
    }

    /**
     * 取消上传任务
     */
    @Operation(summary = "取消上传任务。", description = "取消上传任务")
    @PostMapping("/upload/{log_id}/cancel")
    public Result<Void> cancelUploadMission(@PathVariable("log_id") Long logId) {
        return Result.success();
    }
}
