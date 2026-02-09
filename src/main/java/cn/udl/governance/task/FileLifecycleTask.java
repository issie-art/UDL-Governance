package cn.udl.governance.task;

import cn.udl.governance.config.FileLifecycleConfig;
import cn.udl.governance.model.FileMetadata;
import cn.udl.governance.service.FileLifecycleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 文件生命周期定时任务
 * 负责检查和执行状态转换
 */
@Slf4j
@Component
public class FileLifecycleTask {
    
    @Resource
    private FileLifecycleService fileLifecycleService;
    
    @Resource
    private FileLifecycleConfig fileLifecycleConfig;
    
    /**
     * Active → Expired 状态转换任务
     * 检查已过期的Active文件，转换为Expired状态
     * 默认每小时执行一次
     */
    @Scheduled(fixedRateString = "${file.lifecycle.active-to-expired-check-interval}")
    public void checkActiveToExpired() {
        if (!fileLifecycleConfig.isEnableAutoExpiration()) {
            log.debug("Auto expiration check is disabled");
            return;
        }
        
        try {
            log.info("Starting Active→Expired check task...");
            Date currentTime = new Date();
            
            // 获取需要过期的文件列表
            List<FileMetadata> filesToExpire = fileLifecycleService.getFilesToExpire(currentTime);
            
            if (filesToExpire.isEmpty()) {
                log.info("No files need to be expired");
                return;
            }
            
            log.info("Found {} files that need to be expired", filesToExpire.size());
            
            int successCount = 0;
            for (FileMetadata file : filesToExpire) {
                try {
                    boolean success = fileLifecycleService.expireFile(file.getId());
                    if (success) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Error expiring file {}: {}", file.getId(), e.getMessage());
                }
            }
            
            log.info("Active→Expired task completed. Success: {}/{}", successCount, filesToExpire.size());
            
        } catch (Exception e) {
            log.error("Error in Active→Expired check task: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Expired → Recycled 状态转换任务
     * 检查已过等待期的Expired文件，转换为Recycled状态
     * 默认每天执行一次
     */
    @Scheduled(fixedRateString = "${file.lifecycle.expired-to-recycled-check-interval}")
    public void checkExpiredToRecycled() {
        if (!fileLifecycleConfig.isEnableAutoRecycle()) {
            log.debug("Auto recycle check is disabled");
            return;
        }
        
        try {
            log.info("Starting Expired→Recycled check task...");
            Date currentTime = new Date();
            int waitDays = fileLifecycleConfig.getExpiredToRecycledDays();
            
            // 获取需要回收的文件列表
            List<FileMetadata> filesToRecycle = fileLifecycleService.getFilesToRecycle(currentTime, waitDays);
            
            if (filesToRecycle.isEmpty()) {
                log.info("No files need to be recycled");
                return;
            }
            
            log.info("Found {} files that need to be recycled", filesToRecycle.size());
            
            int successCount = 0;
            for (FileMetadata file : filesToRecycle) {
                try {
                    // 系统自动回收，标记为 SYSTEM
                    boolean success = fileLifecycleService.recycleFile(file.getId(), "SYSTEM");
                    if (success) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Error recycling file {}: {}", file.getId(), e.getMessage());
                }
            }
            
            log.info("Expired→Recycled task completed. Success: {}/{}", successCount, filesToRecycle.size());
            
        } catch (Exception e) {
            log.error("Error in Expired→Recycled check task: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Recycled → Destroyed 状态转换任务
     * 检查已过保留期的Recycled文件，执行物理删除并转换为Destroyed状态
     * 默认每6小时执行一次
     */
    @Scheduled(fixedRateString = "${file.lifecycle.recycled-to-destroyed-check-interval}")
    public void checkRecycledToDestroyed() {
        if (!fileLifecycleConfig.isEnableAutoDestroy()) {
            log.debug("Auto destroy check is disabled");
            return;
        }
        
        try {
            log.info("Starting Recycled→Destroyed check task...");
            Date currentTime = new Date();
            int retentionDays = fileLifecycleConfig.getRecycledToDestroyedDays();
            
            // 获取需要销毁的文件列表
            List<FileMetadata> filesToDestroy = fileLifecycleService.getFilesToDestroy(currentTime, retentionDays);
            
            if (filesToDestroy.isEmpty()) {
                log.info("No files need to be destroyed");
                return;
            }
            
            log.info("Found {} files that need to be destroyed", filesToDestroy.size());
            
            int successCount = 0;
            for (FileMetadata file : filesToDestroy) {
                try {
                    boolean success = fileLifecycleService.destroyFile(file.getId());
                    if (success) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Error destroying file {}: {}", file.getId(), e.getMessage());
                }
            }
            
            log.info("Recycled→Destroyed task completed. Success: {}/{}", successCount, filesToDestroy.size());
            
        } catch (Exception e) {
            log.error("Error in Recycled→Destroyed check task: {}", e.getMessage(), e);
        }
    }
}
