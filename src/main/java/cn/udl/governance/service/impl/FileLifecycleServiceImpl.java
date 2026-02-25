package cn.udl.governance.service.impl;

import cn.udl.governance.common.ErrorCode;
import cn.udl.governance.enums.FileStatusEnum;
import cn.udl.governance.exception.BusinessException;
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
    @Transactional(rollbackFor = Exception.class)
    public boolean activateFile(FileMetadata fileMetadata, Date expirationTime) {
        // 验证当前状态是否为 UPLOADED
        if (!FileStatusEnum.UPLOADED.getName().equals(fileMetadata.getStatus())) {
            log.warn("File {} is not in UPLOADED status, current status: {}", 
                    fileMetadata.getId(), fileMetadata.getStatus());
            return false;
        }
        
        // 更新状态为 ACTIVE
        fileMetadata.setStatus(FileStatusEnum.ACTIVE.getName());
        fileMetadata.setExpirationTime(expirationTime);
        fileMetadata.setUpdatedAt(new Date());
        
        // 清除可能存在的删除相关信息
        fileMetadata.setDeletedAt(null);
        fileMetadata.setDeletedBy(null);

        // 保存数据至数据库
        boolean success = fileMetadataService.updateById(fileMetadata);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to activate file: " + fileMetadata.getId());
        }
        log.info("File {} activated successfully, expiration time: {}", 
                fileMetadata.getId(), expirationTime);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean expireFile(Long fileId) {
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
        fileMetadata.setUpdatedAt(new Date());
        
        boolean success = fileMetadataService.updateById(fileMetadata);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to expire file: " + fileId);
        }
        log.info("File {} expired successfully", fileId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recycleFile(Long fileId, String deletedBy) {
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
        fileMetadata.setDeletedAt(new Date());
        fileMetadata.setDeletedBy(deletedBy);
        fileMetadata.setUpdatedAt(new Date());
        
        boolean success = fileMetadataService.updateById(fileMetadata);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to recycle file: " + fileId);
        }
        log.info("File {} recycled successfully, deleted by: {}", fileId, deletedBy);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean destroyFile(Long fileId) {
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
        String fileKey = fileMetadata.getFileKey();
        if (fileKey == null || fileKey.trim().isEmpty()) {
            log.error("File key is null or empty for file id: {}", fileId);
            return false;
        }
        
        // 先更新数据库状态为 DESTROYED，确保数据库操作在事务内
        fileMetadata.setStatus(FileStatusEnum.DESTROYED.getName());
        fileMetadata.setUpdatedAt(new Date());
        
        boolean success = fileMetadataService.updateById(fileMetadata);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to destroy file: " + fileId);
        }
        
        // 数据库更新成功后，再物理删除文件（不可回滚操作放最后）
        fileStorageService.delete(fileKey);
        log.info("File {} destroyed successfully, physically deleted from storage", fileId);
        return true;
    }
    
    @Override
    public List<FileMetadata> getFilesToExpire(Date checkTime) {
        try {
            LambdaQueryWrapper<FileMetadata> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileMetadata::getStatus, FileStatusEnum.ACTIVE.getName())
                    .le(FileMetadata::getExpirationTime, checkTime)
                    .isNotNull(FileMetadata::getExpirationTime);
            
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
                    .le(FileMetadata::getUpdatedAt, cutoffTime);
            
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
                    .le(FileMetadata::getDeletedAt, cutoffTime);
            
            return fileMetadataService.list(queryWrapper);
        } catch (Exception e) {
            log.error("Failed to get files to destroy: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
