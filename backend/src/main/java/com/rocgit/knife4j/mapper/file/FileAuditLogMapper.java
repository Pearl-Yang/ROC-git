package com.rocgit.knife4j.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.file.FileAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileAuditLogMapper extends BaseMapper<FileAuditLog> {
}
