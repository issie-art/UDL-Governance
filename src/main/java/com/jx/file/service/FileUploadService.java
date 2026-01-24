package com.jx.file.service;

import com.jx.file.common.BaseResponse;
import com.jx.file.model.dto.CompleteUploadRequest;
import com.jx.file.model.dto.InitUploadRequest;
import com.jx.file.model.dto.UploadChunkRequest;
import com.jx.file.model.vo.CompleteUploadResponse;
import com.jx.file.model.vo.InitUploadResponse;
import com.jx.file.model.vo.UploadChunkResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileUploadService {
    /**
     * 初始化上传方法的接口声明
     */
    BaseResponse<InitUploadResponse> initUpload(InitUploadRequest request);

    /**
     * 分片上传
     */

    BaseResponse<UploadChunkResponse> uploadChunk(UploadChunkRequest request, MultipartFile file) throws IOException;

    /**
     * 完成上传确认方法
     */
    BaseResponse<CompleteUploadResponse> completeUpload(CompleteUploadRequest request);

    /**
     * 断点续传
     */
    BaseResponse<List<Integer>> checkUploadedChunks(String uploadId);
}
