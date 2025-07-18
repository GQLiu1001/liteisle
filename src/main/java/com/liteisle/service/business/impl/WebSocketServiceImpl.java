package com.liteisle.service.business.impl;

import com.liteisle.common.constant.WebSocketMessageType;
import com.liteisle.common.dto.websocket.FileStatusUpdateMessage;
import com.liteisle.common.dto.websocket.ShareSaveCompletedMessage;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.handler.WebSocketHandler;
import com.liteisle.service.business.WebSocketService;
import com.liteisle.util.WebSocketMessageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket业务服务实现类
 */
@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {
    
    @Resource
    private WebSocketHandler webSocketHandler;
    
    @Override
    public void sendFileStatusUpdate(Long userId, Long fileId, Long logId,
                                   FileStatusEnum fileStatus, TransferStatusEnum transferStatus,
                                   String fileName, String errorMessage, Integer progress) {
        try {
            FileStatusUpdateMessage message = new FileStatusUpdateMessage(
                    fileId, logId, fileStatus, transferStatus, fileName, errorMessage, progress
            );
            String messageJson = WebSocketMessageUtil.createAndSerialize(WebSocketMessageType.FILE_STATUS_UPDATED, message);
            webSocketHandler.sendMessageToUser(userId, messageJson);
            
            log.debug("发送文件状态更新消息给用户 {}: 文件ID={}, 状态={}", userId, fileId, fileStatus);
        } catch (Exception e) {
            log.error("发送文件状态更新消息失败: userId={}, fileId={}", userId, fileId, e);
        }
    }
    
    @Override
    public void sendShareSaveCompleted(Long userId, ShareSaveCompletedMessage message) {
        try {
            String messageJson = WebSocketMessageUtil.createAndSerialize(WebSocketMessageType.SHARE_SAVE_COMPLETED, message);
            webSocketHandler.sendMessageToUser(userId, messageJson);
            
            log.debug("发送分享转存完成消息给用户 {}: 成功={}, 失败={}", 
                    userId, message.getSuccessCount(), message.getFailedCount());
        } catch (Exception e) {
            log.error("发送分享转存完成消息失败: userId={}", userId, e);
        }
    }
    
    @Override
    public void sendShareSaveFailed(Long userId, ShareSaveCompletedMessage message) {
        try {
            String messageJson = WebSocketMessageUtil.createAndSerialize(WebSocketMessageType.SHARE_SAVE_FAILED, message);
            webSocketHandler.sendMessageToUser(userId, messageJson);
            
            log.debug("发送分享转存失败消息给用户 {}: 失败文件数={}", userId, message.getFailedCount());
        } catch (Exception e) {
            log.error("发送分享转存失败消息失败: userId={}", userId, e);
        }
    }
    

}