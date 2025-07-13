package com.liteisle.util;

public class UserContextHolder {
    //用户ID
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }
    public static Long getUserId() {
        return USER_ID.get();
    }

    //用户token
    private static final ThreadLocal<String> USER_TOKEN = new ThreadLocal<>();
    public static void setUserToken(String token) {
        USER_TOKEN.set(token);
    }
    public static String getUserToken() {
        return USER_TOKEN.get();
    }


    // 清理 ThreadLocal，防止内存泄露
    public static void clear() {
        USER_ID.remove();
        USER_TOKEN.remove();
    }

}
