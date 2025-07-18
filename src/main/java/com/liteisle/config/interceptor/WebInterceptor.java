package com.liteisle.config.interceptor;

import com.liteisle.util.JwtUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebInterceptor extends AbstractAuthInterceptor {

    private final JwtUtil jwtUtil;

    public WebInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean doAuth(String token, HttpServletRequest request) {
        if (!jwtUtil.validateToken(token)) {
            log.warn("令牌无效或已在黑名单中: {}", token);
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            log.warn("无法从有效令牌中解析出User ID");
            return false;
        }

        UserContextHolder.setUserId(userId);
        UserContextHolder.setUserToken(token);
        return true;
    }
}
