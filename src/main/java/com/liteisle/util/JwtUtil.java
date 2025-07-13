package com.liteisle.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.liteisle.config.JwtConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * JWT 工具类
 * 用于生成、验证和解析JWT令牌
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;
    private Algorithm algorithm;
    private JWTVerifier verifier;

    private static final String ISSUER = "liteisle";
    private static final String USERNAME_CLAIM = "username";
    private static final String USER_ID_CLAIM = "userId";

    // 初始化 Algorithm 和 JWTVerifier
    @PostConstruct
    public void init() {
        algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
    }

    /**
     * 生成JWT令牌
     *
     */
    public String generateToken(String username, Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtConfig.getExpiration().getAccessToken());

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withClaim(USERNAME_CLAIM, username)
                .withClaim(USER_ID_CLAIM, userId)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiration))
                .sign(algorithm);
    }

    /**
     * 验证JWT令牌
     */
    public boolean validateToken(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            // 提升日志级别为 WARN
            log.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从JWT令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            //对 JWT 的 payload 部分进行 Base64 解码并读取其中的声明（claims）
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getClaim(USERNAME_CLAIM).asString();
        } catch (Exception e) {
            log.error("从JWT令牌中获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从JWT令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getClaim(USER_ID_CLAIM).asLong();
        } catch (Exception e) {
            log.error("从JWT令牌中获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取JWT令牌的过期时间
     *
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getExpiresAt();
        } catch (Exception e) {
            log.error("从JWT令牌中获取过期时间失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查JWT令牌是否过期
     *
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }
}