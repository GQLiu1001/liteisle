package com.liteisle.config.interceptor;

import com.liteisle.util.JwtUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 握手前
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        // 方案1：从Authorization头获取token（保持现有逻辑）
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (validateAndSetUser(token, attributes, "Header")) {
                return true;
            }
        }

        // 方案2：从URL参数获取token（新增支持）
        String query = request.getURI().getQuery();
        if (query != null) {
            String token = extractTokenFromQuery(query);
            if (token != null && validateAndSetUser(token, attributes, "URL参数")) {
                return true;
            }
        }

        log.warn("WebSocket 握手认证失败，拒绝连接。请求URI: {}", request.getURI());
        return false;
    }

    /**
     * 验证token并设置用户信息
     */
    private boolean validateAndSetUser(String token, Map<String, Object> attributes, String authMethod) {
        try {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    attributes.put("userId", userId);
                    log.info("WebSocket 握手认证成功 ({}), userId: {}", authMethod, userId);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket token验证失败 ({}): {}", authMethod, e.getMessage());
        }
        return false;
    }

    /**
     * 从查询字符串中提取token
     */
    private String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        try {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    String encodedToken = param.substring(6);
                    // URL解码token
                    return URLDecoder.decode(encodedToken, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.warn("解析URL参数中的token失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 握手后
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // do nothing
    }
}