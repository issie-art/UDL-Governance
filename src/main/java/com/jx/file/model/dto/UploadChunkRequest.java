package com.jx.file.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上次请求
 */
@Data
public class UploadChunkRequest {
    private String uploadId;
    private Integer chunkIndex;
    private Long chunkSize;
}