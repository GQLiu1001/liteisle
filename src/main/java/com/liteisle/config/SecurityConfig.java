package com.liteisle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    /**
     * 配置密码编码器，使用BCrypt算法
     * BCrypt会自动生成盐值并包含在加密后的密码中
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 强度因子10-31，数值越高安全性越高但性能越低，默认是10
        return new BCryptPasswordEncoder(12);
    }
}