package com.liteisle.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteisle.common.Result;
import com.liteisle.common.dto.request.TransferStatusUpdateReq;
import com.liteisle.common.dto.response.TransferLogPageResp;
import com.liteisle.common.dto.response.TransferSummaryResp;
import com.liteisle.service.core.TransferLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
@Tag(name = "传输接口")
public class TransferController {

    @Resource
    private TransferLogService transferLogService;

    /**
     * 获取传输历史记录
     */
    @Operation(summary = "获取传输历史记录", description = "获取传输历史记录")
    @GetMapping
    public Result<TransferLogPageResp> getTransferLogs(
            @RequestParam(defaultValue = "1", name = "current_page") Integer currentPage,
            @RequestParam(defaultValue = "10", name = "page_size") Integer pageSize,
            @RequestParam(name = "status") String status) {
        IPage<TransferLogPageResp.TransferRecord> page = new Page<>(currentPage, pageSize);
        IPage<TransferLogPageResp.TransferRecord> records = transferLogService.getTransferLogs(page, status);
        TransferLogPageResp transferLogPageResp = new TransferLogPageResp();
        transferLogPageResp.setCurrentPage(currentPage);
        transferLogPageResp.setPageSize(pageSize);
        transferLogPageResp.setTotal(records.getTotal());
        transferLogPageResp.setRecords(records.getRecords());
        return Result.success(transferLogPageResp);
    }

    /**
     * 获取传输统计摘要
     */
    @Operation(summary = "获取传输统计摘要", description = "获取传输统计摘要")
    @GetMapping("/summary")
    public Result<TransferSummaryResp> getTransferSummary() {
        TransferSummaryResp resp = transferLogService.getTransferSummary();
        return Result.success(resp);
    }

    /**
     * 更新传输状态
     */
    @Operation(summary = "更新传输状态",
            description = "由客户端在下载任务结束（成功、失败、取消）后，" +
                    "或任何由客户端发起的取消操作后调用，以更新其在后端的日志状态。")
    @PutMapping("/{log_id}/status")
    public Result<Void> updateTransferStatus(
            @PathVariable("log_id") Long logId,
            @RequestBody TransferStatusUpdateReq req) {
        transferLogService.updateTransferStatus(logId, req);
        return Result.success();
    }

    /**
     * 删除单条传输记录
     */
    @Operation(summary = "删除单条传输记录,从传输列表中删除一条记录。", description = "取消传输任务")
    @DeleteMapping("/{log_id}")
    public Result<Void> deleteOneTransferLog(
            @PathVariable("log_id") Long logId,
            @RequestParam(defaultValue = "false") Boolean deleteFile) {
        transferLogService.deleteOneTransferLog(logId, deleteFile);
        return Result.success();
    }

    /**
     * 清空已完成的传输记录
     */
    @Operation(summary = "清空已完成的传输记录。", description = "清空已完成的传输记录")
    @DeleteMapping("/completed")
    public Result<Void> completedCleanTransferLog(
            @RequestParam(defaultValue = "false") Boolean deleteFile) {
        transferLogService.completedCleanTransferLog(deleteFile);
        return Result.success();
    }

    /**
     * 取消上传任务
     */
    @Operation(summary = "取消上传任务",
            description = "取消一个正在进行的上传任务。此操作会中断文件传输，并清理服务器上已接收的该文件的临时数据（恢复用户额度）。")
    @PostMapping("/upload/{log_id}/cancel")
    public Result<String> cancelUploadMission(
            @Parameter(description = "要取消的上传任务的传输日志ID")
            @PathVariable("log_id") Long logId) {
        transferLogService.cancelUploadMission(logId);
        return Result.success("上传任务已取消");
    }
}
