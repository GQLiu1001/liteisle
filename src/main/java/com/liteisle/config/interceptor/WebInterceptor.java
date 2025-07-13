package com.liteisle.config.interceptor;


import com.liteisle.util.JwtUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class WebInterceptor extends AbstractAuthInterceptor{

    private final JwtUtil jwtUtil;

    public WebInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    protected boolean doAuth(String token, HttpServletRequest request) {
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid token: {}", token);
            return false;
        }

        // 建议添加对令牌是否过期的检查
        if (jwtUtil.isTokenExpired(token)) {
            log.warn("Expired token: {}", token);
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            log.warn("User ID is missing in token");
            return false;
        }

        UserContextHolder.setUserId(userId);
        return true;
    }
}
