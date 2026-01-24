-- 创建文件中心数据库
CREATE DATABASE IF NOT EXISTS file_center_db;

use file_center_db;
CREATE TABLE file_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件内部唯一标识，系统主键',

    file_name VARCHAR(255) NOT NULL COMMENT '文件原始名称，仅用于展示与下载',
    file_size BIGINT NOT NULL COMMENT '文件逻辑大小（字节），用于完整性校验',

    hash VARCHAR(128) NOT NULL COMMENT '文件内容哈希，用于去重、秒传',

    status VARCHAR(32) NOT NULL COMMENT '文件生命周期状态（Uploaded / Active / Expired / Recycled / Destroyed）',

    storage_type VARCHAR(32) NOT NULL COMMENT '存储类型（LOCAL / COS / OSS 等）',
    file_key VARCHAR(512) NOT NULL COMMENT '文件在存储系统中的唯一定位符',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文件记录创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '文件元数据最近更新时间',

    UNIQUE KEY uk_file_hash (hash),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) COMMENT='文件主表';
