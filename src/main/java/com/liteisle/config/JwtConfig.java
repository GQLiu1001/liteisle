package com.liteisle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 签名密钥
     */
    private String secret;

    /**
     * 过期时间配置
     */
    private Expiration expiration = new Expiration();

    @Data
    public static class Expiration {
        /**
         * 访问令牌过期时间（默认7天）
         */
        private Duration accessToken = Duration.ofDays(7);
        
        /**
         * 刷新令牌过期时间（默认30天）
         */
        private Duration refreshToken = Duration.ofDays(30);
        
        /**
         * 匿名用户令牌过期时间（默认1天）
         */
        private Duration anonymous = Duration.ofDays(1);
    }
}