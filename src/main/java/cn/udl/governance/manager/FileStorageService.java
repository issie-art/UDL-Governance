package cn.udl.governance.manager;

import java.io.InputStream;

/**
 * 统一文件存储接口
 */
public interface FileStorageService {
    
    /**
     * 存储文件
     * @param objectName 对象名称（路径+文件名）
     * @param inputStream 文件流
     * @param contentType 内容类型
     */
    void upload(String objectName, InputStream inputStream, String contentType);

    /**
     * 获取文件访问地址
     * @param objectName 对象名称
     * @return 访问 URL
     */
    String getUrl(String objectName);

    /**
     * 删除文件
     * @param objectName 对象名称
     */
    void delete(String objectName);
}
