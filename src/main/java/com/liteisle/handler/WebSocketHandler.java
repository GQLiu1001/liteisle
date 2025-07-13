package com.liteisle.handler;

import com.liteisle.util.UserContextHolder; // 假设你有类似工具
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component // 关键点1：让 Spring 管理这个 Handler
public class WebSocketHandler extends TextWebSocketHandler {

    // 用于存储所有已连接的客户端 <UserId, Session>
    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 session 的 attributes 中获取用户ID（这个ID是在握手拦截器中放入的）
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.put(userId, session);
            log.info("WebSocket 连接成功, userId: {}, 当前在线人数: {}", userId, SESSIONS.size());
        } else {
            // 如果没有 userId，说明握手拦截器验证失败，直接关闭连接
            session.close(CloseStatus.POLICY_VIOLATION.withReason("用户未认证"));
            log.warn("检测到未认证的 WebSocket 尝试连接，已关闭。");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.remove(userId);
            log.info("WebSocket 连接断开, userId: {}, 当前在线人数: {}", userId, SESSIONS.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 你的文档目前没有定义 C2S (客户端到服务端) 的消息
        // 这里可以处理心跳包等逻辑，例如客户端定时发送 "ping"
        log.info("收到来自 userId: {} 的消息: {}", session.getAttributes().get("userId"), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误, userId: {}, 错误信息: {}", session.getAttributes().get("userId"), exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
        // 从 map 中移除
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.remove(userId);
        }
    }

    /**
     * 关键点2：提供一个静态或Bean方法，供其他服务调用，向指定用户发送消息
     * @param userId 用户ID
     * @param payload JSON 格式的消息体字符串
     */
    public void sendMessageToUser(Long userId, String payload) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
                log.info("成功向 userId: {} 发送消息: {}", userId, payload);
            } catch (IOException e) {
                log.error("向 userId: {} 发送消息失败: {}", userId, e.getMessage());
            }
        } else {
            log.warn("尝试向 userId: {} 发送消息，但用户不在线或 session 已关闭。", userId);
        }
    }
}