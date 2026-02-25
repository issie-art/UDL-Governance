package cn.udl.governance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName file_metadata
 */
@TableName(value ="file_metadata")
@Data
public class FileMetadata {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String fileName;

    private Long fileSize;

    private String hash;

    private String status;

    private String storageType;

    private String fileKey;
    
    // 生命周期管理字段
    private Date expirationTime;
    private Date deletedAt;
    private String deletedBy;

    private Date createdAt;

    private Date updatedAt;
}