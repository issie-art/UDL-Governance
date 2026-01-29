-- 创建文件中心数据库
CREATE DATABASE IF NOT EXISTS udl_governance;

use udl_governance;
CREATE TABLE file_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件唯一ID，主键',

    file_name VARCHAR(255) COMMENT '文件名',
    file_size BIGINT COMMENT '大小（字节）',

    hash VARCHAR(128) COMMENT '内容哈希',

    status VARCHAR(32) COMMENT '生命周期状态',

    storage_type VARCHAR(32) COMMENT '存储类型',
    file_key VARCHAR(512) COMMENT '资源定位符',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    KEY idx_created_at (created_at),
    KEY idx_updated_at (updated_at)
) COMMENT='文件元信息';
