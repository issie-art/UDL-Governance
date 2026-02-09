package cn.udl.governance.controller;

import cn.udl.governance.common.BaseResponse;
import cn.udl.governance.service.FileLifecycleService;
import cn.udl.governance.utils.ResultUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文件生命周期管理控制器
 * 提供用户主动删除文件的API接口
 */
@RequestMapping("/file/lifecycle")
@RestController
@Slf4j
public class FileLifecycleController {
    
    @Resource
    private FileLifecycleService fileLifecycleService;
    
    /**
     * 用户主动删除文件
     * Active/Expired → Recycled
     * @param fileId 文件ID
     * @param username 用户名（可选，默认为anonymous）
     * @return 删除结果
     */
    @DeleteMapping("/delete/{fileId}")
    public BaseResponse<String> deleteFile(
            @PathVariable Long fileId,
            @RequestParam(required = false, defaultValue = "anonymous") String username) {
        
        log.info("User {} requesting to delete file {}", username, fileId);
        
        // 用户主动删除，标记为 USER:username
        String deletedBy = "USER:" + username;
        boolean success = fileLifecycleService.recycleFile(fileId, deletedBy);
        
        if (success) {
            log.info("File {} deleted successfully by user {}", fileId, username);
            return ResultUtils.success("File deleted successfully");
        } else {
            log.warn("Failed to delete file {} by user {}", fileId, username);
            return ResultUtils.error(40000, "Failed to delete file");
        }
    }
}
