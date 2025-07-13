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


    // 清理 ThreadLocal，防止内存泄露
    public static void clear() {
        USER_ID.remove();
    }

}
