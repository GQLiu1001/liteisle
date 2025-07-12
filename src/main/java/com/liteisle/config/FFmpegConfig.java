package com.liteisle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ffmpeg")
public class FFmpegConfig {
    /**
     * FFmpeg 可执行文件的路径。
     */
    private String path;
}