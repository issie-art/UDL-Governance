package cn.udl.governance.controller;

import cn.udl.governance.common.BaseResponse;
import cn.udl.governance.enums.FileStatusEnum;
import cn.udl.governance.model.FileMetadata;
import cn.udl.governance.model.MinioEvent;
import cn.udl.governance.service.FileMetadataService;
import cn.udl.governance.utils.ResultUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Minio 回调接口
 */
@RequestMapping("/minio")
@RestController
@Slf4j
public class MinioCallbackController {

    @Value("${storage.minio.callback-token}")
    private String callbackToken;

    @Resource
    private FileMetadataService fileMetadataService;

    @PostMapping("/callback")
    public BaseResponse<String> handleMinioCallback(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MinioEvent event) {
        
        // 1. Token 校验：判断两侧 Token 是否一致 (MinIO 通常以 'Bearer <token>' 格式发送)
        if (authHeader == null || !authHeader.contains(callbackToken)) {
            log.warn("Invalid callback token: {}", authHeader);
            return ResultUtils.success("Invalid callback token");
        }

        // 2. 解析文件 Key (MinIO 传回的 Key 包含 Bucket 前缀，如 'files/xxx.jpg')
        if (event.getRecords() == null || event.getRecords().isEmpty()) {
            log.warn("No records found in event");
            return ResultUtils.success("Invalid event: no records found");
        }

        String fileKey = event.getRecords().get(0).getS3().getObject().getKey();

        //URL 进行解码
        fileKey= URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
        // 3. 幂等检查：根据 file_key 查询元数据
        FileMetadata metadata = fileMetadataService.getOne(
                new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getFile_key, fileKey)
        );

        if (metadata == null) {
            log.warn("File metadata not found: {}", fileKey);
            return ResultUtils.success("File metadata not found");
        }

        // 如果状态已经是 Active，直接返回 (幂等逻辑)
        if (FileStatusEnum.ACTIVE.getName().equals(metadata.getStatus())) {
            log.warn("File {} is already Active, skipping.", fileKey);
            return ResultUtils.success("File is already Active");
        }

        // 4. 状态更新：将状态修改为 Active
        metadata.setStatus(FileStatusEnum.fromCode(2).getName());
        fileMetadataService.updateById(metadata);

        log.info("File {} status updated to Active via callback", fileKey);
        return ResultUtils.success("File status updated to Active");
    }
}