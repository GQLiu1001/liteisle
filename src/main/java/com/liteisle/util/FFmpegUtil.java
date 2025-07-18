package com.liteisle.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteisle.config.FFmpegConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
     *
     * @param mediaFilePath 媒体文件的本地临时路径
     * @return 包含元数据的 Map，例如 {"artist": "周杰伦", "album": "叶惠美", "duration": "269.096"}
     * @throws Exception 如果执行或解析失败
     */
    public Map<String, String> getMusicMetadata(String mediaFilePath) throws Exception {
        // ffprobe 命令，用于以JSON格式输出文件的格式和流信息
        List<String> command = new ArrayList<>();
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
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String jsonOutput = readProcessOutput(process);

        // 等待进程执行完成，设置一个超时时间防止无限等待
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            log.error("ffprobe process did not finish successfully. Exit code: {}", process.exitValue());
            log.error("Command output: {}", jsonOutput);
            throw new RuntimeException("执行 ffprobe 命令失败。");
        }

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

            // 从 format.tags 中提取元数据 (artist, album)
            JsonNode tagsNode = formatNode.path("tags");
            if (!tagsNode.isMissingNode()) {
                tagsNode.fields().forEachRemaining(entry -> {
                    // key 转为小写，便于统一处理
                    metadata.put(entry.getKey().toLowerCase(), entry.getValue().asText());
                });
            }

            // 获取时长 (duration)
            if (formatNode.has("duration")) {
                metadata.put("duration", formatNode.get("duration").asText());
            }

        } catch (Exception e) {
            log.error("解析 ffprobe JSON 输出失败: {}", e.getMessage(), e);
        }
        return metadata;
    }

    /**
     * =========================================================================
     * 提取专辑封面 (这是一个独立且更复杂的操作)
     * =========================================================================
     * ffprobe 只能检测到封面的存在，而 ffmpeg 负责将其提取为图片文件。
     *
     * @param mediaFilePath 媒体文件的本地临时路径
     * @param outputImagePath 提取出的封面图片要保存的路径 (例如: "/tmp/cover.jpg")
     * @return 如果成功提取封面，返回 true，否则返回 false。
     */
    public boolean extractCoverArt(String mediaFilePath, String outputImagePath) {
        // 构建 ffmpeg 命令来提取封面
        List<String> command = new ArrayList<>();
        command.add(ffmpegConfig.getPath()); // 使用 ffmpeg 而不是 ffprobe
        command.add("-i");
        command.add(mediaFilePath);
        command.add("-an"); // 禁用音频流
        command.add("-vcodec"); // 指定视频编解码器
        command.add("copy"); // 直接复制流，不进行重新编码，效率最高
        command.add("-y"); // 如果输出文件已存在，则覆盖
        command.add(outputImagePath);

        log.info("Executing ffmpeg cover extraction command: {}", String.join(" ", command));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // 合并错误和标准输出流
            Process process = processBuilder.start();

            // 读取并丢弃输出，防止进程阻塞
            readProcessOutput(process);

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                File outputFile = new File(outputImagePath);
                // 检查文件是否存在且大小大于0，确保提取成功
                if (outputFile.exists() && outputFile.length() > 0) {
                    log.info("成功提取专辑封面到: {}", outputImagePath);
                    return true;
                }
            }
            log.warn("未能从 {} 提取专辑封面。进程退出码: {}", mediaFilePath, process.exitValue());
            return false;
        } catch (Exception e) {
            log.error("提取专辑封面时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 读取进程的输出流并返回字符串。
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        return output.toString();
    }


}