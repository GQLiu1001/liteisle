package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.ItemsSelectionReq;
import com.liteisle.common.domain.response.DownloadSessionResp;
import com.liteisle.service.business.DownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/download")
@Tag(name = "下载接口")
public class DownloadController {

    @Resource
    private DownloadService downloadService;
    /**
     * 下载文件
     * 后端负责：
     * 1. 递归解析所有文件。
     * 2. 为每个下载任务自动创建传输日志记录。
     * 3. 为每个文件生成带签名的下载URL。
     * 4. 将包含所有必要信息的完整清单一次性返回给客户端。
     */
    @Operation(summary = "下载文件", description = "所有下载到本地操作的唯一入口。")
    @PostMapping("/create")
    public Result<DownloadSessionResp> registerDownload(@RequestBody ItemsSelectionReq req) {
        //TODO 待测试
        DownloadSessionResp resp = downloadService.registerDownload(req);
        return Result.success(resp);
    }


}
