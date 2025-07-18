package com.liteisle.handler;

import com.liteisle.util.UserContextHolder; // 假设你有类似工具
import com.liteisle.common.constant.WebSocketMessageType;
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
        String payload = message.getPayload();
        Long userId = (Long) session.getAttributes().get("userId");
        
        // 处理心跳包
        if (WebSocketMessageType.HEARTBEAT_PING.equals(payload)) {
            session.sendMessage(new TextMessage(WebSocketMessageType.HEARTBEAT_PONG));
            log.debug("收到来自 userId: {} 的心跳包，已回复pong", userId);
            return;
        }
        
        // 处理其他消息类型
        log.info("收到来自 userId: {} 的消息: {}", userId, payload);
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
                log.debug("成功向 userId: {} 发送消息", userId);
            } catch (IOException e) {
                log.error("向 userId: {} 发送消息失败: {}", userId, e.getMessage());
                // 如果发送失败，移除无效的session
                SESSIONS.remove(userId);
            }
        } else {
            log.debug("尝试向 userId: {} 发送消息，但用户不在线或 session 已关闭。", userId);
        }
    }
    
    /**
     * 向所有在线用户广播消息
     * @param payload JSON 格式的消息体字符串
     */
    public void broadcastMessage(String payload) {
        SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.error("向 userId: {} 广播消息失败: {}", userId, e.getMessage());
                    SESSIONS.remove(userId);
                }
            } else {
                SESSIONS.remove(userId);
            }
        });
        log.info("广播消息给 {} 个在线用户", SESSIONS.size());
    }
    
    /**
     * 获取当前在线用户数量
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return SESSIONS.size();
    }
    
    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = SESSIONS.get(userId);
        return session != null && session.isOpen();
    }
}