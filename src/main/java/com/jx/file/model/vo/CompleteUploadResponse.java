package com.jx.file.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 响应体
 */
@Data
public class CompleteUploadResponse {
    private String status; // COMPLETED or INCOMPLETE
    private List<Integer> missingChunks;
}