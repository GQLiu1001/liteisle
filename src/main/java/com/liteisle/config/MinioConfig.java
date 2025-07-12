package com.liteisle.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * MinIO 服务端端点
     */
    private String endpoint;

    /**
     * Access key
     */
    private String accessKey;

    /**
     * Secret key
     */
    private String secretKey;

    /**
     * 默认的存储桶名称
     */
    private String bucket;

    /**
     * 将 MinioClient 注册为 Spring Bean
     * @return MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        log.info("开始初始化 MinIO Client...");
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            log.info("MinIO Client 初始化成功！Endpoint: {}", endpoint);
            return minioClient;
        } catch (Exception e) {
            log.error("初始化 MinIO Client 失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法初始化 MinIO Client", e);
        }
    }
}