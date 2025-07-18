package com.liteisle.util;

import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * MIME 类型工具类
 * 根据文件后缀名提供标准的 MIME 类型。
 */
public final class MimeTypeUtil {

    /**
     * 私有构造函数，防止实例化。
     */
    private MimeTypeUtil() {}

    /**
     * 根据文件名获取其对应的 MIME 类型。
     *
     * @param fileName 完整的文件名，例如 "晴天.mp3" 或 "项目报告.pdf"。
     * @return 对应的 MIME 类型字符串。如果没有匹配到，则返回通用的 "application/octet-stream"。
     */
    public static String getMimeType(String fileName) {
        // 1. 安全地提取文件后缀
        if (!StringUtils.hasText(fileName)) {
            return "application/octet-stream"; // 默认的二进制流类型
        }

        String suffix = "";
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            suffix = fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        if (suffix.isEmpty()) {
            return "application/octet-stream";
        }

        // 2. 使用 switch 语句进行高效匹配
        return switch (suffix) {
            // --- 音乐文件 ---
            case "mp3" -> "audio/mpeg";
            case "flac" -> "audio/flac";
            case "wav" -> "audio/wav";
            case "aac" -> "audio/aac";
            case "ogg" -> "audio/ogg";
            case "wma" -> "audio/x-ms-wma";
            case "m4a" -> "audio/mp4";

            // --- 文档文件 ---
            case "md" -> "text/markdown";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            // --- 图片文件 (例如头像) ---
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";

            // --- 默认/未知类型 ---
            default -> "application/octet-stream";
        };
    }
}