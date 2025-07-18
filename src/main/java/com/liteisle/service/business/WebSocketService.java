package com.liteisle.service.business;

import com.liteisle.common.dto.websocket.FileStatusUpdateMessage;
import com.liteisle.common.dto.websocket.ShareSaveCompletedMessage;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.TransferStatusEnum;

/**
 * WebSocket业务服务接口
 */
public interface WebSocketService {
    
    /**
     * 发送文件状态更新消息
     * 
     * @param userId 用户ID
     * @param fileId 文件ID
     * @param logId 传输日志ID
     * @param fileStatus 文件状态
     * @param transferStatus 传输状态
     * @param fileName 文件名
     * @param errorMessage 错误信息
     * @param progress 进度百分比
     */
    void sendFileStatusUpdate(Long userId, Long fileId, Long logId, 
                             FileStatusEnum fileStatus, TransferStatusEnum transferStatus,
                             String fileName, String errorMessage, Integer progress);
    
    /**
     * 发送分享转存完成消息
     * 
     * @param userId 用户ID
     * @param message 分享转存完成消息
     */
    void sendShareSaveCompleted(Long userId, ShareSaveCompletedMessage message);
    
    /**
     * 发送分享转存失败消息
     * 
     * @param userId 用户ID
     * @param message 分享转存失败消息
     */
    void sendShareSaveFailed(Long userId, ShareSaveCompletedMessage message);

    

}