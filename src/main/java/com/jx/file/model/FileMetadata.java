package com.jx.file.model;

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
    @TableId(type = IdType.ASSIGN_UUID)
    private Long id;

    private String file_name;

    private Long file_size;

    private String hash;

    private String status;

    private String storage_type;

    private String file_key;

    private Date created_at;

    private Date updated_at;
}