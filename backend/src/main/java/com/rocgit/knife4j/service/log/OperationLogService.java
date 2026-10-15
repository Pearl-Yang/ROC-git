package com.rocgit.knife4j.service.log;

import com.rocgit.knife4j.entity.log.LogStatsVO;
import com.rocgit.knife4j.entity.log.OperationLogVO;

import java.util.Map;

public interface OperationLogService {

    Map<String, Object> list(Long projectId, String type, int pageNum, int pageSize);

    LogStatsVO stats(Long projectId);

    void record(Long userId, Long projectId, String action, String targetType, Long targetId, String description);
}