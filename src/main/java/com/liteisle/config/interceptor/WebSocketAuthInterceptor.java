package com.liteisle.config.interceptor;

import com.liteisle.util.JwtUtil; // 引入你之前写的JwtUtil
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component // 同样，让 Spring 管理
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 握手前
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // HTTP Headers 在 ServerHttpRequest 中
        // 前端在建立 WebSocket 连接时，需要在请求头中附带 Authorization: Bearer <token>
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 使用你的 JwtUtil 验证 token
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token); // 确保这个方法是安全的
                if (userId != null) {
                    // 关键点3：将验证通过的用户ID放入 attributes
                    // 后续在 WebSocketHandler 的 session.getAttributes().get("userId") 中可以获取
                    attributes.put("userId", userId);
                    log.info("WebSocket 握手认证成功, userId: {}", userId);
                    return true; // 认证成功，允许握手
                }
            }
        }

        log.warn("WebSocket 握手认证失败，拒绝连接。");
        return false; // 认证失败，拒绝握手
    }

    /**
     * 握手后
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // do nothing
    }
}