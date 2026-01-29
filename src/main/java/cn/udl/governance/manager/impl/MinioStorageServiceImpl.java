package cn.udl.governance.manager.impl;

import cn.udl.governance.manager.FileStorageService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class MinioStorageServiceImpl implements FileStorageService {

    @Value("${storage.minio.endpoint}")
    private String endpoint;

    @Value("${storage.minio.access-key}")
    private String accessKey;

    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @Value("${storage.minio.bucket-name}")
    private String bucketName;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }


    @Override
    public void upload(String objectName, InputStream inputStream, String contentType) {
        log.info("MinIO store: {} {}", bucketName, objectName);
        try {
            // 检查桶是否存在，不存在则创建
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
            // 如果桶不存在，则创建新桶
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            // 记录桶创建日志
                log.info("Created bucket: {}", bucketName);
            }

        // 使用MinIO客户端上传对象到指定存储桶
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)      // 指定存储桶名称
                            .object(objectName)     // 指定对象名称
                            .stream(inputStream, inputStream.available(), -1)  // 设置输入流和大小
                            .contentType(contentType) // 设置内容类型
                            .build()                 // 构建参数
            );
        } catch (Exception e) {
        // 记录错误日志
            log.error("MinIO store failed", e);
        // 抛出运行时异常，封装原始异常信息
            throw new RuntimeException("MinIO storage error", e);
        }
    }

    @Override
    public String getUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(60 * 60 * 24) // 1 day
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO getUrl failed", e);
            return null;
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO delete failed", e);
        }
    }
}
