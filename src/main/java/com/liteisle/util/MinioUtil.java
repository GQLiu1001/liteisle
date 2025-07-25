package com.liteisle.util;

import com.liteisle.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private String defaultBucketName;

    /**
     * 在服务启动时初始化，确保默认存储桶存在。
     */
    @PostConstruct
    public void init() {
        this.defaultBucketName = minioConfig.getBucket();
        try {
            createBucketIfNotExists(this.defaultBucketName);
        } catch (Exception e) {
            log.error("创建或检查默认存储桶 '{}' 失败: {}", this.defaultBucketName, e.getMessage(), e);
            // 在生产环境中，这可能是个严重问题，可以考虑让应用启动失败
            // throw new IllegalStateException("MinIO a默认存储桶初始化失败", e);
        }
    }

    /**
     * 上传文件到 MinIO。
     * 强烈推荐在业务层构建好 objectName，使其包含用户路径以实现隔离。
     * 例如: "user_123/avatars/profile.jpg"
     *
     * @param file           Spring 的 MultipartFile 对象
     * @param fullObjectName 在 MinIO 中的完整对象名称（包含路径）
     * @return 存储在 MinIO 中的完整对象名，这个值应该被存入数据库。
     * @throws Exception 上传过程中发生的任何错误
     */
    public String uploadFile(MultipartFile file, String fullObjectName) throws Exception {
        // 使用 try-with-resources 确保 InputStream 被正确关闭
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(this.defaultBucketName)
                            .object(fullObjectName)
                            .stream(inputStream, file.getSize(), -1) // 使用 file.getSize() 获取准确的文件大小
                            .contentType(file.getContentType())
                            .build()
            );

        }
        log.info("文件上传成功！存储桶: {}, 对象名: {}", this.defaultBucketName, fullObjectName);
        // 返回完整的对象名，以便存入数据库
        return fullObjectName;
    }

    /**
     * 【新增方法】上传文件流到 MinIO。
     * 这是为异步处理设计的重载版本，接收内存中的字节流，解决了临时文件被删除的问题。
     *
     * @param inputStream    文件内容的输入流
     * @param objectSize     文件大小
     * @param fullObjectName 在 MinIO 中的完整对象名称
     * @param contentType    文件的 MIME 类型
     * @return 存储在 MinIO 中的完整对象名
     * @throws Exception 上传过程中发生的任何错误
     */
    public String uploadFile(InputStream inputStream, long objectSize, String fullObjectName, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(this.defaultBucketName)
                        .object(fullObjectName)
                        .stream(inputStream, objectSize, -1) // 使用传入的参数
                        .contentType(contentType)           // 使用传入的参数
                        .build()
        );
        log.info("文件流上传成功！存储桶: {}, 对象名: {}", this.defaultBucketName, fullObjectName);
        return fullObjectName;
    }

    /**
     * 获取一个临时的、带签名授权的 URL，用于客户端下载或预览（播放）。
     * 这是访问私有文件的标准、安全方式。
     *
     * @param fullObjectName 文件的完整对象名 (e.g., "user_123/music/song.mp3")
     * @param expiry         过期时间数值
     * @param unit           过期时间单位 (e.g., TimeUnit.MINUTES)
     * @return 一个有时限的、可公开访问的 URL
     * @throws Exception 生成 URL 过程中发生的任何错误
     */
    public String getPresignedObjectUrl(String fullObjectName, int expiry, TimeUnit unit) throws Exception {
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET) // 用于下载/播放，所以是 GET
                        .bucket(this.defaultBucketName)
                        .object(fullObjectName)
                        .expiry(expiry, unit)
                        .build()
        );
        log.info("成功为对象 '{}' 生成预签名URL", fullObjectName);
        return url;
    }

    /**
     * 删除 MinIO 中的一个文件。
     *
     * @param fullObjectName 要删除的文件的完整对象名
     * @throws Exception 删除过程中发生的任何错误
     */
    public void removeFile(String fullObjectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(this.defaultBucketName)
                        .object(fullObjectName)
                        .build()
        );
        log.info("成功删除文件: {}", fullObjectName);
    }

    /**
     * 在 MinIO 中创建一个“文件夹”。
     * MinIO (S3) 中没有真正的文件夹概念，它是通过创建一个零字节的对象并以 '/' 结尾来模拟的。
     *
     * @param folderPath 文件夹的完整路径，必须以 '/' 结尾 (e.g., "user_123/documents/")
     * @throws Exception 创建过程中发生的任何错误
     */
    public void createFolder(String folderPath) throws Exception {
        if (!folderPath.endsWith("/")) {
            throw new IllegalArgumentException("文件夹路径必须以 '/' 结尾");
        }
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(this.defaultBucketName)
                        .object(folderPath)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1) // 空文件流
                        .build()
        );
        log.info("成功创建文件夹: {}", folderPath);
    }

    /**
     * 检查存储桶是否存在，如果不存在则创建。设为私有方法，由 init 调用。
     */
    private void createBucketIfNotExists(String bucketName) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("存储桶 '{}' 不存在，已成功创建。", bucketName);
        } else {
            log.info("存储桶 '{}' 已存在。", bucketName);
        }
    }

    /**
     * 检查文件在 MinIO 中是否存在。
     *
     * @param fullObjectName 文件的完整对象名
     * @return 如果文件存在则返回 true，否则返回 false。
     */
    public boolean objectExists(String fullObjectName) {
        try {
            // statObject 会在对象不存在时抛出异常，存在则返回对象信息
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(this.defaultBucketName)
                            .object(fullObjectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            // MinIO Java SDK 在找不到对象时会抛出 ErrorResponseException
            // 我们可以根据需要判断异常类型，但通常任何异常都意味着“我们无法确认它存在”
            // 为简单起见，这里捕获通用异常并返回 false
            return false;
        }
    }
}