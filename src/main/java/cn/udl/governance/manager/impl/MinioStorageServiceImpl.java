package cn.udl.governance.manager.impl;

import cn.udl.governance.common.ErrorCode;
import cn.udl.governance.exception.BusinessException;
import cn.udl.governance.manager.FileStorageService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

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
                .httpClient(new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(300, TimeUnit.SECONDS) // 增大写超时
                        .readTimeout(300, TimeUnit.SECONDS)
                        .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES)) // 复用连接
                        .build())
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
            log.info("MinIO store success {}", objectName);
        } catch (Exception e) {
            // 记录错误日志
            log.error("MinIO store failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
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
        // 参数校验
        if (objectName == null || objectName.trim().isEmpty()) {
            log.warn("Object name is null or empty, cannot delete");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对象名称为空");
        }
        
        try {
            // 先检查对象是否存在
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            // 执行删除
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            
            // 验证删除是否成功
            try {
                minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
                );
                // 如果还能获取到对象信息，说明删除失败
                log.error("File still exists after deletion: {}", objectName);
                throw new RuntimeException("File deletion verification failed");
            } catch (Exception e) {
                // 期望的异常，说明文件已被成功删除
                log.info("Successfully deleted and verified file from MinIO: {}", objectName);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", objectName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件删除失败: " + e.getMessage());
        }
    }
}
