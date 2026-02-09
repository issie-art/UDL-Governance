package cn.udl.governance.service.impl;

import cn.udl.governance.enums.FileStatusEnum;
import cn.udl.governance.manager.FileStorageService;
import cn.udl.governance.model.FileMetadata;
import cn.udl.governance.service.FileLifecycleService;
import cn.udl.governance.service.FileMetadataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 文件生命周期管理服务实现类
 */
@Slf4j
@Service
public class FileLifecycleServiceImpl implements FileLifecycleService {
    
    @Resource
    private FileMetadataService fileMetadataService;
    
    @Resource
    private FileStorageService fileStorageService;
    
    @Override
    @Transactional
    public boolean activateFile(FileMetadata fileMetadata, Date expirationTime) {
        try {
            // 验证当前状态是否为 UPLOADED
            if (!FileStatusEnum.UPLOADED.getName().equals(fileMetadata.getStatus())) {
                log.warn("File {} is not in UPLOADED status, current status: {}", 
                        fileMetadata.getId(), fileMetadata.getStatus());
                return false;
            }
            
            // 更新状态为 ACTIVE
            fileMetadata.setStatus(FileStatusEnum.ACTIVE.getName());
            fileMetadata.setExpiration_time(expirationTime);
            fileMetadata.setUpdated_at(new Date());
            
            // 清除可能存在的删除相关信息
            fileMetadata.setDeleted_at(null);
            fileMetadata.setDeleted_by(null);

            // 保存数据至数据库
            boolean success = fileMetadataService.updateById(fileMetadata);
            if (success) {
                log.info("File {} activated successfully, expiration time: {}", 
                        fileMetadata.getId(), expirationTime);
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to activate file {}: {}", fileMetadata.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean expireFile(Long fileId) {
        try {
            FileMetadata fileMetadata = fileMetadataService.getById(fileId);
            if (fileMetadata == null) {
                log.warn("File {} not found", fileId);
                return false;
            }
            
            // 验证当前状态是否为 ACTIVE
            if (!FileStatusEnum.ACTIVE.getName().equals(fileMetadata.getStatus())) {
                log.warn("File {} is not in ACTIVE status, current status: {}", 
                        fileId, fileMetadata.getStatus());
                return false;
            }
            
            // 更新状态为 EXPIRED
            fileMetadata.setStatus(FileStatusEnum.EXPIRED.getName());
            fileMetadata.setUpdated_at(new Date());
            
            boolean success = fileMetadataService.updateById(fileMetadata);
            if (success) {
                log.info("File {} expired successfully", fileId);
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to expire file {}: {}", fileId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean recycleFile(Long fileId, String deletedBy) {
        try {
            FileMetadata fileMetadata = fileMetadataService.getById(fileId);
            if (fileMetadata == null) {
                log.warn("File {} not found", fileId);
                return false;
            }
            
            String currentStatus = fileMetadata.getStatus();
            // 验证当前状态是否为 ACTIVE 或 EXPIRED
            if (!FileStatusEnum.ACTIVE.getName().equals(currentStatus) && 
                !FileStatusEnum.EXPIRED.getName().equals(currentStatus)) {
                log.warn("File {} cannot be recycled, current status: {}", fileId, currentStatus);
                return false;
            }
            
            // 更新状态为 RECYCLED
            fileMetadata.setStatus(FileStatusEnum.RECYCLED.getName());
            fileMetadata.setDeleted_at(new Date());
            fileMetadata.setDeleted_by(deletedBy);
            fileMetadata.setUpdated_at(new Date());
            
            boolean success = fileMetadataService.updateById(fileMetadata);
            if (success) {
                log.info("File {} recycled successfully, deleted by: {}", fileId, deletedBy);
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to recycle file {}: {}", fileId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean destroyFile(Long fileId) {
        try {
            FileMetadata fileMetadata = fileMetadataService.getById(fileId);
            if (fileMetadata == null) {
                log.warn("File {} not found", fileId);
                return false;
            }
            
            // 验证当前状态是否为 RECYCLED
            if (!FileStatusEnum.RECYCLED.getName().equals(fileMetadata.getStatus())) {
                log.warn("File {} is not in RECYCLED status, current status: {}", 
                        fileId, fileMetadata.getStatus());
                return false;
            }
            
            // 从存储中物理删除文件
            try {
                fileStorageService.delete(fileMetadata.getFile_key());
                log.info("File {} physically deleted from storage", fileMetadata.getFile_key());
            } catch (Exception e) {
                log.error("Failed to delete file from storage: {}", fileMetadata.getFile_key(), e);
                return false;
            }
            
            // 更新状态为 DESTROYED
            fileMetadata.setStatus(FileStatusEnum.DESTROYED.getName());
            fileMetadata.setUpdated_at(new Date());
            
            boolean success = fileMetadataService.updateById(fileMetadata);
            if (success) {
                log.info("File {} destroyed successfully", fileId);
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to destroy file {}: {}", fileId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<FileMetadata> getFilesToExpire(Date checkTime) {
        try {
            LambdaQueryWrapper<FileMetadata> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileMetadata::getStatus, FileStatusEnum.ACTIVE.getName())
                    .le(FileMetadata::getExpiration_time, checkTime)
                    .isNotNull(FileMetadata::getExpiration_time);
            
            return fileMetadataService.list(queryWrapper);
        } catch (Exception e) {
            log.error("Failed to get files to expire: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<FileMetadata> getFilesToRecycle(Date checkTime, int waitDays) {
        try {
            // 计算等待期截止时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(checkTime);
            calendar.add(Calendar.DAY_OF_MONTH, -waitDays);
            Date cutoffTime = calendar.getTime();
            
            LambdaQueryWrapper<FileMetadata> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileMetadata::getStatus, FileStatusEnum.EXPIRED.getName())
                    .le(FileMetadata::getUpdated_at, cutoffTime);
            
            return fileMetadataService.list(queryWrapper);
        } catch (Exception e) {
            log.error("Failed to get files to recycle: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<FileMetadata> getFilesToDestroy(Date checkTime, int retentionDays) {
        try {
            // 计算保留期截止时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(checkTime);
            calendar.add(Calendar.DAY_OF_MONTH, -retentionDays);
            Date cutoffTime = calendar.getTime();
            
            LambdaQueryWrapper<FileMetadata> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileMetadata::getStatus, FileStatusEnum.RECYCLED.getName())
                    .le(FileMetadata::getDeleted_at, cutoffTime);
            
            return fileMetadataService.list(queryWrapper);
        } catch (Exception e) {
            log.error("Failed to get files to destroy: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
