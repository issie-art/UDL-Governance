package cn.udl.governance.model.vo;

import lombok.Data;

/**
 * 初始请求响应体
 */
@Data
public class InitUploadResponse {
    private String uploadId;
    private Integer totalChunks;
}