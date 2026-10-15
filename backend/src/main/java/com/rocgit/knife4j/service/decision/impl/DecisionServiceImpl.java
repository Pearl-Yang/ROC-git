package com.rocgit.knife4j.service.decision.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.decision.*;
import com.rocgit.knife4j.entity.decision.Decision;
import com.rocgit.knife4j.entity.decision.DecisionVO;
import com.rocgit.knife4j.mapper.decision.DecisionMapper;
import com.rocgit.knife4j.service.decision.DecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DecisionServiceImpl implements DecisionService {

    private final DecisionMapper decisionMapper;

    @Override
    public Map<String, Object> list(Long projectId, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;
        QueryWrapper<Decision> qw = new QueryWrapper<>();
        if (projectId != null) qw.eq("project_id", projectId);
        qw.eq("deleted", 0).orderByDesc("created_at");
        List<Decision> all = decisionMapper.selectList(qw);
        int from = Math.min((pageNum - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        List<DecisionVO> vos = new ArrayList<>();
        for (Decision d : all.subList(from, to)) vos.add(toVO(d));
        Map<String, Object> data = new HashMap<>();
        data.put("total", all.size());
        data.put("records", vos);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return data;
    }

    @Override
    public DecisionVO detail(Long decisionId) {
        Decision d = decisionMapper.selectById(decisionId);
        if (d == null) throw new BusinessException(ResultCode.NOT_FOUND, "决策不存在");
        return toVO(d);
    }

    @Override
    @Transactional
    public DecisionVO create(Long userId, CreateDecisionRequest req) {
        Decision d = new Decision();
        d.setProjectId(req.getProjectId());
        d.setTitle(req.getTitle() == null ? "" : req.getTitle());
        d.setContent(req.getContent() == null ? "" : req.getContent());
        d.setCreatorId(userId);
        d.setDiscussionId(req.getDiscussionId() == null ? 0L : req.getDiscussionId());
        decisionMapper.insert(d);
        return toVO(d);
    }

    @Override
    @Transactional
    public void update(Long decisionId, UpdateDecisionRequest req) {
        Decision d = decisionMapper.selectById(decisionId);
        if (d == null) throw new BusinessException(ResultCode.NOT_FOUND, "决策不存在");
        if (req.getTitle() != null) d.setTitle(req.getTitle());
        if (req.getContent() != null) d.setContent(req.getContent());
        decisionMapper.updateById(d);
    }

    @Override
    @Transactional
    public void delete(Long decisionId) {
        decisionMapper.deleteById(decisionId);
    }

    private DecisionVO toVO(Decision d) {
        DecisionVO vo = new DecisionVO();
        vo.setDecisionId(d.getId());
        vo.setProjectId(d.getProjectId());
        vo.setTitle(d.getTitle());
        vo.setContent(d.getContent());
        vo.setCreatorId(d.getCreatorId());
        vo.setDiscussionId(d.getDiscussionId());
        vo.setCreatedAt(d.getCreatedAt());
        return vo;
    }
}
