package cn.udl.governance.controller;

import cn.udl.governance.common.BaseResponse;
import cn.udl.governance.model.dto.CompleteUploadRequest;
import cn.udl.governance.model.dto.InitUploadRequest;
import cn.udl.governance.model.dto.UploadChunkRequest;
import cn.udl.governance.model.vo.CompleteUploadResponse;
import cn.udl.governance.model.vo.InitUploadResponse;
import cn.udl.governance.model.vo.UploadChunkResponse;
import cn.udl.governance.service.FileUploadService;
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
            @RequestParam("file") MultipartFile file) throws IOException {

        UploadChunkRequest request = new UploadChunkRequest();
        request.setUploadId(uploadId);
        request.setChunkIndex(chunkIndex);

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