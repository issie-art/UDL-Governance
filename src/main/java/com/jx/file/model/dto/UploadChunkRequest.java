package com.jx.file.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上次请求
 */
@Data
public class UploadChunkRequest {
    private String uploadId;
    private Integer chunkIndex; // 当前分片的索引号，用于标识分片在上传顺序中的位置
}