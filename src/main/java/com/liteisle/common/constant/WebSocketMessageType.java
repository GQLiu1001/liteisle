package com.liteisle.common.constant;

/**
 * WebSocket消息类型常量
 */
public class WebSocketMessageType {
    
    // 文件相关消息
    public static final String FILE_STATUS_UPDATED = "file.status.updated";
    public static final String FILE_UPLOAD_PROGRESS = "file.upload.progress";
    public static final String FILE_UPLOAD_COMPLETED = "file.upload.completed";
    public static final String FILE_UPLOAD_FAILED = "file.upload.failed";
    
    // 分享相关消息
    public static final String SHARE_SAVE_COMPLETED = "share.save.completed";
    public static final String SHARE_SAVE_FAILED = "share.save.failed";
    public static final String SHARE_SAVE_PROGRESS = "share.save.progress";
    
    // 系统通知消息
    public static final String SYSTEM_NOTIFICATION = "system.notification";
    public static final String SYSTEM_MAINTENANCE = "system.maintenance";
    public static final String SYSTEM_ANNOUNCEMENT = "system.announcement";
    
    // 用户相关消息
    public static final String USER_LOGIN = "user.login";
    public static final String USER_LOGOUT = "user.logout";
    public static final String USER_PROFILE_UPDATED = "user.profile.updated";
    
    // 存储相关消息
    public static final String STORAGE_QUOTA_WARNING = "storage.quota.warning";
    public static final String STORAGE_QUOTA_EXCEEDED = "storage.quota.exceeded";
    public static final String STORAGE_CLEANUP_COMPLETED = "storage.cleanup.completed";
    
    // 回收站相关消息
    public static final String RECYCLE_AUTO_CLEANUP = "recycle.auto.cleanup";
    public static final String RECYCLE_RESTORE_COMPLETED = "recycle.restore.completed";
    
    // 心跳消息
    public static final String HEARTBEAT_PING = "ping";
    public static final String HEARTBEAT_PONG = "pong";
    
    // 错误消息
    public static final String ERROR_MESSAGE = "error";
    public static final String CONNECTION_ERROR = "connection.error";
    
    private WebSocketMessageType() {
        // 私有构造函数，防止实例化
    }
}