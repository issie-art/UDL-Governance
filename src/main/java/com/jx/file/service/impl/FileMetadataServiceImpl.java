package com.jx.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jx.file.model.FileMetadata;
import com.jx.file.service.FileMetadataService;
import com.jx.file.mapper.FileMetadataMapper;
import org.springframework.stereotype.Service;

@Service
public class FileMetadataServiceImpl extends ServiceImpl<FileMetadataMapper, FileMetadata>
    implements FileMetadataService{

}




