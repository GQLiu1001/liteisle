package com.liteisle.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteisle.common.dto.websocket.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * WebSocket消息工具类
 */
@Slf4j
@Component
public class WebSocketMessageUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建WebSocket消息
     * 
     * @param event 事件类型
     * @param data 消息数据
     * @return WebSocket消息对象
     */
    public static WebSocketMessage createMessage(String event, Object data) {
        WebSocketMessage message = new WebSocketMessage(event, data);
        message.setMessageId(UUID.randomUUID().toString());
        return message;
    }
    
    /**
     * 将WebSocket消息转换为JSON字符串
     * 
     * @param message WebSocket消息对象
     * @return JSON字符串
     */
    public static String toJson(WebSocketMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("WebSocket消息序列化失败", e);
            return "{\"event\":\"error\",\"data\":{\"message\":\"消息序列化失败\"}}";
        }
    }
    
    /**
     * 创建并序列化WebSocket消息
     * 
     * @param event 事件类型
     * @param data 消息数据
     * @return JSON字符串
     */
    public static String createAndSerialize(String event, Object data) {
        WebSocketMessage message = createMessage(event, data);
        return toJson(message);
    }
}