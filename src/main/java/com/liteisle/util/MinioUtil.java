package com.liteisle.util;

import com.liteisle.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
     * PostConstruct 注解确保在 Spring 容器完成依赖注入后，
     * 立即执行此方法，用于检查并创建默认的存储桶。
     */
    @PostConstruct
    public void init() {
        this.defaultBucketName = minioConfig.getBucket();
        try {
            createBucketIfNotExists(this.defaultBucketName);
        } catch (Exception e) {
            log.error("创建默认存储桶'{}'失败: {}", this.defaultBucketName, e.getMessage(), e);
            // 这里可以根据业务决定是否抛出异常中断应用启动
        }
    }

    /**
     * 检查存储桶是否存在，如果不存在则创建。
     * @param bucketName 存储桶名称
     * @throws Exception 如果操作失败
     */
    public void createBucketIfNotExists(String bucketName) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("存储桶 '{}' 不存在，已成功创建。", bucketName);
        } else {
            log.info("存储桶 '{}' 已存在。", bucketName);
        }
    }

    /**
     * 上传文件到默认存储桶。
     * @param multipartFile Spring 的 MultipartFile 对象
     * @param objectName 在 MinIO 中存储的对象名称（可以包含路径，如 "avatars/user123.jpg"）
     * @return 文件的访问 URL
     * @throws Exception 如果上传失败
     */
    public String uploadFile(MultipartFile multipartFile, String objectName) throws Exception {
        return uploadFile(this.defaultBucketName, multipartFile.getInputStream(), objectName, multipartFile.getContentType());
    }

    /**
     * 通用文件上传方法。
     * @param bucketName 存储桶名称
     * @param stream 文件输入流
     * @param objectName 对象名称
     * @param contentType 文件类型 (e.g., "image/jpeg")
     * @return 文件的访问 URL
     * @throws Exception 如果上传失败
     */
    public String uploadFile(String bucketName, InputStream stream, String objectName, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(stream, stream.available(), -1)
                        .contentType(contentType)
                        .build()
        );
        String url = minioConfig.getEndpoint() + "/" + bucketName + "/" + objectName;
        log.info("文件上传成功！对象名: {}, URL: {}", objectName, url);
        return url;
    }

    /**
     * 生成一个带签名的、有过期时间的临时访问URL（用于下载或预览）。
     * 这是安全分享私有文件的推荐方式。
     * @param objectName 对象名称
     * @param expiry     过期时间
     * @param unit       时间单位
     * @return 临时的文件访问 URL
     * @throws Exception 如果生成失败
     */
    public String getPresignedObjectUrl(String objectName, int expiry, TimeUnit unit) throws Exception {
        return getPresignedObjectUrl(this.defaultBucketName, objectName, expiry, unit);
    }

    public String getPresignedObjectUrl(String bucketName, String objectName, int expiry, TimeUnit unit) throws Exception {
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiry, unit)
                        .build()
        );
        log.info("成功生成预签名URL for object '{}': {}", objectName, url);
        return url;
    }

    /**
     * 删除文件。
     * @param objectName 对象名称
     * @throws Exception 如果删除失败
     */
    public void removeFile(String objectName) throws Exception {
        removeFile(this.defaultBucketName, objectName);
    }

    public void removeFile(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("成功删除文件: {}", objectName);
    }
}