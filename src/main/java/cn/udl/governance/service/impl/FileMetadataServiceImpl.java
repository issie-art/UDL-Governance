package cn.udl.governance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.udl.governance.model.FileMetadata;
import cn.udl.governance.service.FileMetadataService;
import cn.udl.governance.mapper.FileMetadataMapper;
import org.springframework.stereotype.Service;

@Service
public class FileMetadataServiceImpl extends ServiceImpl<FileMetadataMapper, FileMetadata>
    implements FileMetadataService{

}




