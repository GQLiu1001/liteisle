package com.liteisle.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteisle.config.FFmpegConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FFmpegUtil {

    private final FFmpegConfig ffmpegConfig;
    private final ObjectMapper objectMapper; // Spring Boot 自动配置了 ObjectMapper

    /**
     * 提取音乐文件的元数据。
     * 使用 ffprobe (FFmpeg套件的一部分) 来获取信息。
     *
     * @param mediaFilePath 媒体文件的本地临时路径
     * @return 包含元数据的 Map，例如 {"artist": "周杰伦", "album": "叶惠美", "duration": "269.096000"}
     * @throws Exception 如果执行或解析失败
     */
    public Map<String, String> getMusicMetadata(String mediaFilePath) throws Exception {
        // ffprobe 命令，用于以JSON格式输出文件的格式和流信息
        List<String> command = new ArrayList<>();
        // 使用 ffprobe 而不是 ffmpeg
        command.add(ffmpegConfig.getPath().replace("ffmpeg", "ffprobe"));
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("json");
        command.add("-show_format");
        command.add("-show_streams");
        command.add(mediaFilePath);

        log.info("Executing ffprobe command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // 将错误流合并到标准输出流

        Process process = processBuilder.start();

        // 读取命令的输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        // 等待进程执行完成，设置一个超时时间防止无限等待
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            log.error("ffprobe process did not finish successfully. Exit code: {}", process.exitValue());
            log.error("Command output: {}", output);
            throw new RuntimeException("执行 ffprobe 命令失败。");
        }

        String jsonOutput = output.toString();
        log.debug("ffprobe JSON output: {}", jsonOutput);

        return parseMetadataFromJson(jsonOutput);
    }

    /**
     * 从 ffprobe 的 JSON 输出中解析元数据。
     * @param jsonOutput ffprobe 返回的 JSON 字符串
     * @return 包含关键元数据的 Map
     */
    private Map<String, String> parseMetadataFromJson(String jsonOutput) {
        Map<String, String> metadata = new HashMap<>();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonOutput);
            JsonNode formatNode = rootNode.path("format");

            if (formatNode.isMissingNode()) {
                log.warn("ffprobe output does not contain 'format' node.");
                return metadata;
            }

            // 从 format.tags 中提取元数据
            JsonNode tagsNode = formatNode.path("tags");
            if (!tagsNode.isMissingNode()) {
                tagsNode.fields().forEachRemaining(entry -> {
                    // key 转为小写，便于统一处理
                    metadata.put(entry.getKey().toLowerCase(), entry.getValue().asText());
                });
            }

            // 获取时长
            if (formatNode.has("duration")) {
                metadata.put("duration", formatNode.get("duration").asText());
            }

        } catch (Exception e) {
            log.error("解析 ffprobe JSON 输出失败: {}", e.getMessage(), e);
        }
        return metadata;
    }
}