package com.liteisle.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket连接建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket连接关闭: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到WebSocket消息: {}", message.getPayload());
        // 这里可以处理客户端发送的消息
    }

    /**
     * 广播消息给所有连接的客户端
     */
    public static void broadcast(String event, Object payload) {
        WebSocketMessage message = new WebSocketMessage(event, payload);
        ObjectMapper mapper = new ObjectMapper();
        
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    String jsonMessage = mapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException e) {
                log.error("发送WebSocket消息失败", e);
            }
        });
    }

    /**
     * 发送消息给指定会话
     */
    public static void sendToSession(String sessionId, String event, Object payload) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            WebSocketMessage message = new WebSocketMessage(event, payload);
            ObjectMapper mapper = new ObjectMapper();
            
            try {
                String jsonMessage = mapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                log.error("发送WebSocket消息失败", e);
            }
        }
    }

    /**
     * WebSocket消息格式
     */
    public static class WebSocketMessage {
        private String event;
        private Object payload;

        public WebSocketMessage(String event, Object payload) {
            this.event = event;
            this.payload = payload;
        }

        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }
    }
}