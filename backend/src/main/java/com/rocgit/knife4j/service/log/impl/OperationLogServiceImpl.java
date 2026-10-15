package com.rocgit.knife4j.service.log.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.entity.log.LogStatsVO;
import com.rocgit.knife4j.entity.log.OperationLog;
import com.rocgit.knife4j.entity.log.OperationLogVO;
import com.rocgit.knife4j.mapper.log.OperationLogMapper;
import com.rocgit.knife4j.mapper.UserMapper;
import com.rocgit.knife4j.service.log.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private UserMapper userMapper;

    @Override
    public Map<String, Object> list(Long projectId, String type, int pageNum, int pageSize) {
        QueryWrapper<OperationLog> qw = new QueryWrapper<OperationLog>()
                .eq("project_id", projectId).eq("deleted", 0).orderByDesc("created_at");
        if (type != null && !type.isEmpty()) qw.eq("target_type", type);
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        qw.last("LIMIT " + offset + "," + pageSize);
        List<OperationLog> list = operationLogMapper.selectList(qw);
        Long total = operationLogMapper.selectCount(
                new QueryWrapper<OperationLog>().eq("project_id", projectId).eq("deleted", 0));
        List<OperationLogVO> vos = new ArrayList<>();
        for (OperationLog o : list) vos.add(toVO(o));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", vos);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public LogStatsVO stats(Long projectId) {
        LogStatsVO vo = new LogStatsVO();
        vo.setTotalCount(operationLogMapper.selectCount(
                new QueryWrapper<OperationLog>().eq("project_id", projectId).eq("deleted", 0)));
        Long fileCount = operationLogMapper.selectCount(
                new QueryWrapper<OperationLog>().eq("project_id", projectId).eq("deleted", 0).eq("target_type", "file"));
        Long taskCount = operationLogMapper.selectCount(
                new QueryWrapper<OperationLog>().eq("project_id", projectId).eq("deleted", 0).eq("target_type", "task"));
        vo.setFileCount(fileCount == null ? 0 : fileCount.intValue());
        vo.setTaskCount(taskCount == null ? 0 : taskCount.intValue());
        return vo;
    }

    @Override
    @Transactional
    public void record(Long userId, Long projectId, String action, String targetType, Long targetId, String description) {
        OperationLog o = new OperationLog();
        o.setUserId(userId);
        o.setProjectId(projectId);
        o.setAction(action == null ? "" : action);
        o.setTargetType(targetType == null ? "" : targetType);
        o.setTargetId(targetId == null ? 0L : targetId);
        o.setDescription(description);
        operationLogMapper.insert(o);
    }

    private OperationLogVO toVO(OperationLog o) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(o.getId());
        vo.setUserId(o.getUserId());
        User u = userMapper.selectById(o.getUserId());
        vo.setUserName(u == null ? "" : u.getNickname());
        vo.setAction(o.getAction());
        vo.setTargetType(o.getTargetType());
        vo.setTargetId(o.getTargetId());
        vo.setDescription(o.getDescription());
        vo.setCreatedAt(o.getCreatedAt());
        return vo;
    }
}