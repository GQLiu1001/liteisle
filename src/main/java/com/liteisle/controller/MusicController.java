package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.dto.response.MusicViewResp;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.view.MusicViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/music")
@Tag(name = "音乐接口")
public class MusicController {

    @Resource
    private MusicViewService musicViewService;
    @Resource
    private FilesService filesService;

    /**
     * 获取音乐页面信息
     */
    @Operation(summary = "获取音乐页面信息", description = "获取音乐页面信息")
    @GetMapping
    public Result<MusicViewResp> getMusicView(@RequestParam(required = false) String content) {
        MusicViewResp musicViewResp = musicViewService.getMusicView(content);
        return Result.success(musicViewResp);
    }

    /**
     * 获取音乐播放链接
     */
    @Operation(summary = " 获取音乐播放链接", description = " 获取音乐播放链接")
    @GetMapping("/{file_id}/play")
    public Result<String> getMusicPlayUrl(@PathVariable("file_id") Long fileId) {
        String url = filesService.getMusicPlayUrl(fileId);
        return Result.success(url);
    }

}
