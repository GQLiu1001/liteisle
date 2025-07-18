package com.liteisle.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 哈希生成与校验工具类
 */
public final class HashUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    /**
     * 私有构造函数，防止实例化
     */
    private HashUtil() {
    }

    /**
     * 为一个输入流生成 SHA-256 哈希值。
     *
     * @param inputStream 要计算哈希的输入流。该方法在处理后不会关闭流。
     * @return SHA-256 哈希值的十六进制字符串表示形式。
     * @throws IOException 如果读取输入流时发生错误。
     * @throws NoSuchAlgorithmException 如果环境中不支持 SHA-256 算法。
     */
    public static String generateSHA256(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = digest.digest();
        return bytesToHex(hashBytes);
    }

    /**
     * 为一个字符串生成 SHA-256 哈希值。
     *
     * @param originalString 要计算哈希的原始字符串。
     * @return SHA-256 哈希值的十六进制字符串表示形式。
     */
    public static String generateSHA256(String originalString) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // 在现代 Java 环境中，SHA-256 总是可用的，因此这更像是一个理论上的异常
            throw new RuntimeException("无法找到 SHA-256 算法", e);
        }
    }


    /**
     * 校验一个输入流的 SHA-256 哈希值是否与预期值匹配。
     *
     * @param inputStream 要校验的输入流。该方法在处理后不会关闭流。
     * @param expectedHash 预期的 SHA-256 哈希值（十六进制字符串）。
     * @return 如果生成的哈希与预期值（不区分大小写）匹配，则返回 true，否则返回 false。
     * @throws IOException 如果读取输入流时发生错误。
     * @throws NoSuchAlgorithmException 如果环境中不支持 SHA-256 算法。
     */
    public static boolean validateSHA256(InputStream inputStream, String expectedHash) throws IOException, NoSuchAlgorithmException {
        if (expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }
        String actualHash = generateSHA256(inputStream);
        return expectedHash.equalsIgnoreCase(actualHash);
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param hashBytes 字节数组。
     * @return 十六进制字符串。
     */
    private static String bytesToHex(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}