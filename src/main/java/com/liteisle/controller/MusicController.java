package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.response.MusicViewResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/music")
@Tag(name = "音乐接口")
public class MusicController {

    /**
     * 获取音乐页面信息
     */
    @Operation(summary = "获取音乐页面信息", description = "获取音乐页面信息")
    @GetMapping
    public Result<MusicViewResp> getMusicView(
            @RequestParam(required = false) String content) {
        return Result.success();
    }

    /**
     *  获取音乐播放链接
     */
    @Operation(summary = " 获取音乐播放链接", description = " 获取音乐播放链接")
    @GetMapping("/{file_id}/play")
    public Result<String> getMusicPlayUrl(@PathVariable("file_id") Long fileId) {
        return Result.success();
    }

}
