package com.jx.file.controller;

import com.jx.file.common.BaseResponse;
import com.jx.file.model.dto.CompleteUploadRequest;
import com.jx.file.model.dto.InitUploadRequest;
import com.jx.file.model.dto.UploadChunkRequest;
import com.jx.file.model.vo.CompleteUploadResponse;
import com.jx.file.model.vo.InitUploadResponse;
import com.jx.file.model.vo.UploadChunkResponse;
import com.jx.file.service.FileUploadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Resource
    private FileUploadService fileUploadService;

    @PostMapping("/init")
    public BaseResponse<InitUploadResponse> initUpload(@RequestBody InitUploadRequest request) {
        return fileUploadService.initUpload(request);
    }

    @PostMapping("/chunk")
    public BaseResponse<UploadChunkResponse> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunkSize") Long chunkSize,
            @RequestParam("file") MultipartFile file) throws IOException {

        UploadChunkRequest request = new UploadChunkRequest();
        request.setUploadId(uploadId);
        request.setChunkIndex(chunkIndex);
        request.setChunkSize(chunkSize);

        return fileUploadService.uploadChunk(request, file);
    }

    @PostMapping("/complete")
    public BaseResponse<CompleteUploadResponse> completeUpload(@RequestBody CompleteUploadRequest request) {
        return fileUploadService.completeUpload(request);
    }

    @GetMapping("/check/{uploadId}")
    public BaseResponse<List<Integer>> checkUploadedChunks(@PathVariable String uploadId) {
        return fileUploadService.checkUploadedChunks(uploadId);
    }
}