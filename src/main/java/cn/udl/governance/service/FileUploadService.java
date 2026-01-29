package cn.udl.governance.service;

import cn.udl.governance.common.BaseResponse;
import cn.udl.governance.model.dto.CompleteUploadRequest;
import cn.udl.governance.model.dto.InitUploadRequest;
import cn.udl.governance.model.dto.UploadChunkRequest;
import cn.udl.governance.model.vo.CompleteUploadResponse;
import cn.udl.governance.model.vo.InitUploadResponse;
import cn.udl.governance.model.vo.UploadChunkResponse;
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
