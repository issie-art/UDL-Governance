package com.jx.file.model.dto;

import lombok.Data;

/**
 * 请求合并分片请求
 */
@Data
public class CompleteUploadRequest {
    private String uploadId;
    private String fileName;
}