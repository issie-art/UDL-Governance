package cn.udl.governance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件生命周期配置类
 * 所有时长参数从配置文件读取，禁止硬编码
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.lifecycle")
public class FileLifecycleConfig {
    
    /**
     * Active状态到过期的时长（天）
     * 文件激活后多少天过期
     */
    private int activeToExpiredDays;
    
    /**
     * Expired状态等待回收时长（天）
     * 文件过期后多少天自动进入回收站
     */
    private int expiredToRecycledDays;
    
    /**
     * Recycled状态保留时长（天）
     * 文件进入回收站后多少天执行物理删除
     */
    private int recycledToDestroyedDays;
    
    /**
     * Active→Expired 检查任务执行间隔（毫秒）
     * 默认每小时执行一次
     */
    private long activeToExpiredCheckInterval;
    
    /**
     * Expired→Recycled 检查任务执行间隔（毫秒）
     * 默认每天执行一次
     */
    private long expiredToRecycledCheckInterval;
    
    /**
     * Recycled→Destroyed 检查任务执行间隔（毫秒）
     * 默认每6小时执行一次
     */
    private long recycledToDestroyedCheckInterval;
    
    /**
     * 是否启用自动过期检查
     */
    private boolean enableAutoExpiration;
    
    /**
     * 是否启用自动回收检查
     */
    private boolean enableAutoRecycle;
    
    /**
     * 是否启用自动销毁检查
     */
    private boolean enableAutoDestroy;
}
