package cn.udl.governance;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@SpringBootTest
class FileApplicationTests {

     @Test
    void contextLoads() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://110.41.48.52:9000") // 默认9000端口 [4]
                .credentials("minioadmin", "minioadmin123") // 默认用户名和密码 [4]
                .build();
        // 定义桶名称
        String bucketName = "my-bucket";
        // 检查桶是否存在，如果不存在则创建
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            // 创建桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            System.out.println("桶创建成功: " + bucketName);
        } else {
            System.out.println("桶已存在: " + bucketName);
        }
        // 模拟文件上传
        try {
            // 1. 创建一个临时文件用于测试
            File tempFile = File.createTempFile("test", ".txt");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("这是测试文件内容");
            }

            // 2. 设置上传参数
            String objectName = "test-file.txt"; // 文件在MinIO中的名称
            String contentType = "text/plain"; // 文件类型

            // 3. 执行上传
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("my-bucket") // 桶名称
                            .object(objectName) // 对象名称
                            .filename(tempFile.getAbsolutePath()) // 本地文件路径
                            .contentType(contentType) // 文件类型
                            .build()
            );

            System.out.println("文件上传成功: " + objectName);

            // 4. 清理临时文件
            tempFile.delete();

        } catch (Exception e) {
            System.err.println("文件上传失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
