package com.liteisle.config;

import com.liteisle.config.interceptor.WebSocketAuthInterceptor;
import com.liteisle.handler.WebSocketHandler; // 名字和你的可能不同
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private WebSocketHandler webSocketHandler;

    @Resource
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws") // 使用注入的 Handler
                .addInterceptors(webSocketAuthInterceptor) // 添加握手拦截器
                .setAllowedOrigins("*"); // TODO: 在生产环境中替换为具体的域名
    }
}