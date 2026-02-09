package cn.udl.governance.service;

import cn.udl.governance.model.FileMetadata;
import java.util.Date;
import java.util.List;

/**
 * 文件生命周期管理服务接口
 */
public interface FileLifecycleService {
    
    /**
     * 激活文件 - Uploaded → Active
     * @param fileMetadata 文件元数据
     * @param expirationTime 过期时间
     * @return 是否激活成功
     */
    boolean activateFile(FileMetadata fileMetadata, Date expirationTime);
    
    /**
     * 过期文件 - Active → Expired
     * @param fileId 文件ID
     * @return 是否过期成功
     */
    boolean expireFile(Long fileId);
    
    /**
     * 回收文件 - Active/Expired → Recycled
     * @param fileId 文件ID
     * @param deletedBy 删除人标识（SYSTEM 或 USER:username）
     * @return 是否回收成功
     */
    boolean recycleFile(Long fileId, String deletedBy);
    
    /**
     * 销毁文件 - Recycled → Destroyed
     * @param fileId 文件ID
     * @return 是否销毁成功
     */
    boolean destroyFile(Long fileId);
    
    /**
     * 获取需要过期的文件列表（Active且已超过过期时间）
     * @param checkTime 检查时间点
     * @return 需要过期的文件列表
     */
    List<FileMetadata> getFilesToExpire(Date checkTime);
    
    /**
     * 获取需要回收的文件列表（Expired且超过等待期）
     * @param checkTime 检查时间点
     * @param waitDays 等待天数
     * @return 需要回收的文件列表
     */
    List<FileMetadata> getFilesToRecycle(Date checkTime, int waitDays);
    
    /**
     * 获取需要销毁的文件列表（Recycled且超过保留期）
     * @param checkTime 检查时间点
     * @param retentionDays 保留天数
     * @return 需要销毁的文件列表
     */
    List<FileMetadata> getFilesToDestroy(Date checkTime, int retentionDays);
}
