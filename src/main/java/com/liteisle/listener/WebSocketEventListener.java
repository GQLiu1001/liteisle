package com.liteisle.listener;

import com.liteisle.handler.WebSocketHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket事件监听器
 * 处理WebSocket连接和断开事件的业务逻辑
 */
@Slf4j
@Component
public class WebSocketEventListener {
    
    @Resource
    private WebSocketHandler webSocketHandler;
    
    /**
     * 处理WebSocket连接建立事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            // 获取当前在线用户数
            int onlineCount = webSocketHandler.getOnlineUserCount();
            log.info("WebSocket连接建立，当前在线用户数: {}", onlineCount);
            
            // 这里可以添加更多连接建立时的业务逻辑
            // 例如：记录用户上线时间、发送欢迎消息等
            
        } catch (Exception e) {
            log.error("处理WebSocket连接事件时发生错误", e);
        }
    }
    
    /**
     * 处理WebSocket连接断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            // 获取当前在线用户数
            int onlineCount = webSocketHandler.getOnlineUserCount();
            log.info("WebSocket连接断开，当前在线用户数: {}", onlineCount);
            
            // 这里可以添加更多连接断开时的业务逻辑
            // 例如：记录用户下线时间、清理相关资源等
            
        } catch (Exception e) {
            log.error("处理WebSocket断开事件时发生错误", e);
        }
    }
}