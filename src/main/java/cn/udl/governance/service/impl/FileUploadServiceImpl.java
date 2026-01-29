package cn.udl.governance.service.impl;

import cn.udl.governance.common.BaseResponse;
import cn.udl.governance.common.ErrorCode;
import cn.udl.governance.config.ChunkConfig;
import cn.udl.governance.exception.BusinessException;
import cn.udl.governance.model.dto.CompleteUploadRequest;
import cn.udl.governance.model.dto.InitUploadRequest;
import cn.udl.governance.model.dto.UploadChunkRequest;
import cn.udl.governance.model.vo.CompleteUploadResponse;
import cn.udl.governance.model.vo.InitUploadResponse;
import cn.udl.governance.model.vo.UploadChunkResponse;
import cn.udl.governance.service.FileUploadService;
import cn.udl.governance.utils.FileMergeExecutor;
import cn.udl.governance.utils.RedisUtil;
import cn.udl.governance.utils.ResultUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ChunkConfig chunkConfig;
    @Resource
    private FileMergeExecutor fileMergeExecutor;

    private static final String UPLOAD_INFO_KEY = "upload_info:";
    private static final String CHUNK_RECORD_KEY = "chunk_record:";
    // 通用临时目录
    private static final String BASE_TEMP_DIR = System.getProperty("java.io.tmpdir") + "/file_chunks";

    @Override
    public BaseResponse<InitUploadResponse> initUpload(InitUploadRequest request) {
        log.info("initUpload request: {}", request);

        Long fileSize = request.getFileSize();

        // 1. 基础合法性
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid file size");
        }

        //最大文件限制
        validateFileSize(request.getFileSize());

        // 3. 计算分片数量
        int totalChunks = calculateTotalChunks(fileSize);
        if (totalChunks <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Invalid chunk calculation");
        }
        // 生成唯一的上传 uploadId
        String uploadId = generateUploadId();

        // 存储信息到 Redis
        Map<String, Object> uploadInfo = new HashMap<>();
        uploadInfo.put("fileSize", request.getFileSize());
        uploadInfo.put("totalChunks", totalChunks);
        uploadInfo.put("createdAt", System.currentTimeMillis());
        uploadInfo.put("fileName", request.getFileName());

        redisUtil.setByString(UPLOAD_INFO_KEY + uploadId, uploadInfo, 48, TimeUnit.HOURS);

        InitUploadResponse response = new InitUploadResponse();
        response.setUploadId(uploadId);
        response.setTotalChunks(totalChunks);
        return ResultUtils.success(response);
    }

    @Override
    public BaseResponse<UploadChunkResponse> uploadChunk(UploadChunkRequest request, MultipartFile file) throws IOException {

        String uploadId = request.getUploadId();
        Integer chunkIndex = request.getChunkIndex();
        long maxChunkSize = chunkConfig.getSize();

        // 1. 校验 uploadId 是否存在
        Map<String, Object> uploadInfo = (Map<String, Object>) redisUtil.getByString(UPLOAD_INFO_KEY + uploadId);
        if (uploadInfo == null) {
            log.warn("Invalid or expired uploadId: {}", uploadId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid or expired uploadId");
        }

        int totalChunks = (Integer) uploadInfo.get("totalChunks");

        // 2. 校验 chunkIndex 合法性
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            log.warn("Invalid chunkIndex: {}", chunkIndex);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid chunkIndex");
        }

        // 3. 校验 chunkSize 合法性
        if (file.getSize() > maxChunkSize) {
            log.warn("Invalid chunk size: {}", file.getSize());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid chunk size");
        }

        // 4. 创建目录
        File uploadTempDir = new File(BASE_TEMP_DIR, uploadId);
        if (!uploadTempDir.exists() && !uploadTempDir.mkdirs()) {
            log.warn("Failed to create upload directory: {}", BASE_TEMP_DIR);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create upload directory");
        }

        File chunkFile = new File(uploadTempDir, chunkIndex + ".chunk");

        // 5. 防止重复覆盖
        if (chunkFile.exists()) {
            UploadChunkResponse response = new UploadChunkResponse();
            response.setChunkNumber(chunkIndex);
            response.setStatus("already_uploaded");
            return ResultUtils.success(response);
        }

        // 6. 写文件（先写临时）
        File tempFile = new File(uploadTempDir, chunkIndex + ".chunk.tmp");
        file.transferTo(tempFile);

        // 7. 原子替换
        if (!tempFile.renameTo(chunkFile)) {
            tempFile.delete();
            log.error("Failed to finalize chunk file: {}", chunkFile.getAbsolutePath());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create upload directory");
        }

        // 8. 写 Redis 记录
        redisUtil.setBySet(
                CHUNK_RECORD_KEY + uploadId,
                String.valueOf(chunkIndex),
                24,
                TimeUnit.HOURS
        );

        UploadChunkResponse response = new UploadChunkResponse();
        response.setChunkNumber(chunkIndex);
        response.setStatus("success");
        return ResultUtils.success(response);
    }


    @Override
    public BaseResponse<CompleteUploadResponse> completeUpload(CompleteUploadRequest request) {
        log.info("completeUpload request: {}", request);

        // 校验上传 ID
        String uploadId = request.getUploadId();

        // 获取上传信息
        String uploadInfoKey = UPLOAD_INFO_KEY + uploadId;
        Object uploadInfoObj = redisUtil.getByString(uploadInfoKey);

        if (uploadInfoObj == null) {
            CompleteUploadResponse response = new CompleteUploadResponse();
            response.setStatus("FAILED");
            response.setMissingChunks(Collections.emptyList());
            return ResultUtils.success(response);
        }

        Map<String, Object> uploadInfo = (Map<String, Object>) uploadInfoObj;
        // 获取分片总数
        Integer totalChunks = (Integer) uploadInfo.get("totalChunks");
        // 获取文件名
        String fileName = (String) uploadInfo.get("fileName");

        if (totalChunks == null) {
            CompleteUploadResponse response = new CompleteUploadResponse();
            response.setStatus("FAILED");
            response.setMissingChunks(Collections.emptyList());
            return ResultUtils.success(response);
        }

        // 获取已上传的分块记录
        String chunkRecordKey = CHUNK_RECORD_KEY + uploadId;
        Set<Object> uploadedChunks = redisUtil.getBySet(chunkRecordKey);

        if (uploadedChunks == null) {
            uploadedChunks = new HashSet<>();
        }

        Set<Integer> uploadedChunkSet = uploadedChunks.stream()
                .map(obj -> Integer.parseInt(obj.toString()))
                .collect(Collectors.toSet());

        // 计算缺失的分块
        List<Integer> missingChunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!uploadedChunkSet.contains(i)) {
                missingChunks.add(i);
            }
        }


        CompleteUploadResponse response = new CompleteUploadResponse();
        if (!missingChunks.isEmpty()) {
            response.setStatus("INCOMPLETE");
            response.setMissingChunks(missingChunks);
            return ResultUtils.success(response);
        }
        // 所有分片齐全，提交异步合并
        response.setStatus("COMPLETED");
        fileMergeExecutor.submit(() -> mergeFile(uploadId, totalChunks, fileName));

        return ResultUtils.success(response);
    }

    @Override
    public BaseResponse<List<Integer>> checkUploadedChunks(String uploadId) {
        // 1. 检查上传信息是否存在
        Map<String, Object> uploadInfo = (Map<String, Object>) redisUtil.getByString(UPLOAD_INFO_KEY + uploadId);
        if (uploadInfo == null) {
            return ResultUtils.success(Collections.emptyList());
        }

        // 2. 获取已上传的分片记录
        Set<Object> uploadedChunks = redisUtil.getBySet(CHUNK_RECORD_KEY + uploadId);
        if (uploadedChunks == null) {
            return ResultUtils.success(Collections.emptyList());
        }

        // 3. 转换并返回已上传的分片列表
        List<Integer> uploadedChunkList = uploadedChunks.stream()
                .map(obj -> Integer.parseInt(obj.toString()))
                .sorted()
                .collect(Collectors.toList());

        return ResultUtils.success(uploadedChunkList);
    }


    /**
     * 合并文件的方法
     */
    private void mergeFile(String uploadId, int totalChunks, String fileName) {

        // 构建上传文件的临时目录路径
        Path uploadDir = Paths.get(BASE_TEMP_DIR, uploadId);

        // 检查上传临时目录是否存在，如果不存在则记录警告日志并返回
        if (!Files.exists(uploadDir)) {
            log.warn("Upload temp dir not exists: {}", uploadDir);
            return;
        }


        String namePart;
        String extensionPart;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            namePart = fileName.substring(0, lastDotIndex);
            extensionPart = fileName.substring(lastDotIndex + 1);
        } else {
            // 没有后缀的情况
            namePart = fileName;
            extensionPart = "txt"; // 或者给默认值
        }

        // 创建临时合并文件路径和最终合并文件的路径
        Path tempMergedFile = Paths.get(BASE_TEMP_DIR, uploadId + "." + extensionPart);
        Path finalMergedFile = Paths.get(BASE_TEMP_DIR, namePart + "." + extensionPart);

        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(tempMergedFile))) {

            // 遍历所有分片文件
            for (int i = 0; i < totalChunks; i++) {
                // 构建当前分片的文件路径
                Path chunkPath = uploadDir.resolve(i + ".chunk");

                // 检查分片文件是否存在，如果不存在则记录错误日志并返回
                if (!Files.exists(chunkPath)) {
                    log.error("Missing chunk file: {}", chunkPath);
                    return;
                }

                // 将分片文件复制到输出流中
                Files.copy(chunkPath, out);
            }

        } catch (IOException e) {
            // 合并过程中发生异常时记录错误日志并返回
            log.error("Merge failed for uploadId: {}", uploadId, e);
            return;
        }

        try {
            // 原子性提交
            Files.move(
                    tempMergedFile,
                    finalMergedFile,
                    StandardCopyOption.REPLACE_EXISTING,//如果目标文件已存在，会自动替换
                    StandardCopyOption.ATOMIC_MOVE//原子性移动，保证文件移动操作的原子性
            );
        } catch (IOException e) {
            log.error("Failed to finalize merged file", e);
            return;
        }

        // 清理
        cleanup(uploadId);
        log.info("Merge success, uploadId={}", uploadId);
    }

    /**
     * 清理临时文件
     */
    private void cleanup(String uploadId) {

        // 删除临时分片目录
        Path uploadDir = Paths.get(BASE_TEMP_DIR, uploadId);
        deleteRecursively(uploadDir.toFile());

        // 删除 Redis 记录
        redisUtil.delete(UPLOAD_INFO_KEY + uploadId);
        redisUtil.delete(CHUNK_RECORD_KEY + uploadId);
    }

    /**
     * 清除Redis 记录
     */
    private void deleteRecursively(File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * 根据文件大小和配置的分片大小计算总分片数
     */
    private int calculateTotalChunks(Long fileSize) {
        // 确保分片大小不小于最小值
        Long effectiveChunkSize = Math.max(chunkConfig.getMinSize(), chunkConfig.getSize());

        // 计算总分片数
        int totalChunks = (int) Math.ceil((double) fileSize / effectiveChunkSize);

        // 检查是否超出最大分片数量限制
        if (totalChunks > chunkConfig.getMaxCount()) {
            log.warn("File chunks exceed maximum limit: {}", chunkConfig.getMaxCount());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "File chunks exceed maximum limit: " + chunkConfig.getMaxCount(
            ));
        }

        return totalChunks;
    }

    /**
     * 验证文件大小是否超出限制
     */
    private void validateFileSize(Long fileSize) {
        if (fileSize > chunkConfig.getMaxFileSize()) {
            log.warn("File size exceeds maximum limit: {}", chunkConfig.getMaxFileSize());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "File size exceeds maximum limit: " + chunkConfig.getMaxFileSize() + " bytes");
        }
    }

    /**
     * 生成唯一的文件 ID
     */
    private String generateUploadId() {
        return UUID.randomUUID().toString();
    }

}