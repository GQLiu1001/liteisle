package com.liteisle.util;

import java.security.SecureRandom;

public class CaptchaUtil {

    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * 生成指定长度的验证码，包含大小写字母和数字
     * @param length 验证码长度，建议6或10
     * @return 生成的验证码字符串
     */
    public static String generateCaptcha(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("验证码长度必须大于0");
        }

        StringBuilder captcha = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHAR_POOL.length());
            captcha.append(CHAR_POOL.charAt(index));
        }
        return captcha.toString();
    }

    /**
     * 生成6位验证码
     * @return 6位验证码字符串
     */
    public static String generate6DigitCaptcha() {
        return generateCaptcha(6);
    }

    /**
     * 生成10位验证码
     * @return 10位验证码字符串
     */
    public static String generate10DigitCaptcha() {
        return generateCaptcha(10);
    }

    // 测试main
    public static void main(String[] args) {
        System.out.println("6位验证码：" + generate6DigitCaptcha());
        System.out.println("10位验证码：" + generate10DigitCaptcha());
    }
}
