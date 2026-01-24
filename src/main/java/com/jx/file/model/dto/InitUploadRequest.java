package com.jx.file.model.dto;

import lombok.Data;

/**
 * 初始上传请求体
 */
@Data
public class InitUploadRequest {
    private String fileName;
    private Long fileSize;
}