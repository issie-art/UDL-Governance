package com.jx.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file.chunk")
public class ChunkConfig {

    /**
     * 默认分片大小（字节），默认为10MB
     */
    private Long size;

    /**
     * 最大分片数量限制
     */
    private Integer maxCount;

    /**
     * 最小分片大小（字节），默认为1KB
     */
    private Long minSize;

    /**
     * 最大文件大小限制（字节），默认为10GB
     */
    private Long maxFileSize;
}