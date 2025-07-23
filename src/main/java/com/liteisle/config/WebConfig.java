package com.liteisle.config;

import com.liteisle.config.interceptor.WebInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final WebInterceptor webInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器并指定拦截路径
        registry.addInterceptor(webInterceptor)
                .addPathPatterns(
                        "/**"
                )
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/reset",
                        "/auth/send-vcode",
                        "/auth/forgot-password",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-resources/**",
                        "/webjars/**");
    }
}
