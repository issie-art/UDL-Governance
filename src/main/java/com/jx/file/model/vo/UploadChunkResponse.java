package com.jx.file.model.vo;

import lombok.Data;
/**
 * 响应体
 */
@Data
public class UploadChunkResponse {
    private String status;
    private int chunkNumber;
}