package com.rocgit.knife4j.service.version;

import com.rocgit.knife4j.dto.version.VersionRollbackRequest;
import com.rocgit.knife4j.entity.version.FileVersionVO;
import com.rocgit.knife4j.entity.version.VersionCompareVO;

import java.util.List;

public interface FileVersionService {

    List<FileVersionVO> list(Long fileId);

    VersionCompareVO compare(Long fileId, String from, String to);

    void rollback(Long fileId, VersionRollbackRequest request);
}
