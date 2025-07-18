package com.liteisle.common.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * WebSocket消息基础类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    /**
     * 消息类型
     */
    private String event;
    
    /**
     * 消息数据
     */
    private Object data;
    
    /**
     * 消息时间戳
     */
    private Date timestamp;
    
    /**
     * 消息ID（可选，用于客户端确认）
     */
    private String messageId;
    
    public WebSocketMessage(String event, Object data) {
        this.event = event;
        this.data = data;
        this.timestamp = new Date();
    }
}