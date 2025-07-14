package com.liteisle.util;

import java.util.regex.Pattern;

public final class PathValidator {

    // 只允许字母、数字、中文字符、下划线、连字符和点
    // 注意：根据你的需求调整正则表达式，比如是否支持空格等
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5.-]+$");

    /**
     * 校验文件夹或文件名是否安全。
     *
     * @param name 要校验的名称
     * @return 如果安全则为 true，否则为 false
     */
    public static boolean isSafeName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // 1. 检查是否包含路径遍历符 ".."
        if (name.contains("..")) {
            return false;
        }
        // 2. 检查是否包含路径分隔符 "/" 或 "\", 文件名和文件夹名本身不应包含
        if (name.contains("/") || name.contains("\\")) {
            return false;
        }
        // 3. 使用正则表达式检查是否只包含允许的字符
        return SAFE_NAME_PATTERN.matcher(name).matches();
    }

    /**
     * 清理和规范化路径或文件名。
     * 例如，移除所有非法字符。
     * @param name 原始名称
     * @return 清理后的安全名称
     */
    public static String sanitize(String name) {
        if (name == null) return "";
        // 移除所有不符合白名单的字符
        return name.replaceAll("[^\\w\\u4e00-\\u9fa5.-]", "");
    }
}