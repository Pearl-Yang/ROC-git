package com.rocgit.knife4j.service.discussion.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.discussion.CreateDiscussionRequest;
import com.rocgit.knife4j.dto.discussion.UpdateDiscussionRequest;
import com.rocgit.knife4j.entity.discussion.Discussion;
import com.rocgit.knife4j.entity.discussion.DiscussionVO;
import com.rocgit.knife4j.mapper.discussion.DiscussionMapper;
import com.rocgit.knife4j.service.discussion.DiscussionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionMapper discussionMapper;

    @Override
    public Map<String, Object> list(Long projectId, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;
        QueryWrapper<Discussion> qw = new QueryWrapper<>();
        if (projectId != null) qw.eq("project_id", projectId);
        qw.eq("deleted", 0).orderByDesc("meeting_time");
        List<Discussion> all = discussionMapper.selectList(qw);
        int from = Math.min((pageNum - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        List<DiscussionVO> vos = new ArrayList<>();
        for (Discussion d : all.subList(from, to)) vos.add(toVO(d));
        Map<String, Object> data = new HashMap<>();
        data.put("total", all.size());
        data.put("records", vos);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return data;
    }

    @Override
    public DiscussionVO detail(Long discussionId) {
        Discussion d = discussionMapper.selectById(discussionId);
        if (d == null) throw new BusinessException(ResultCode.NOT_FOUND, "研讨不存在");
        return toVO(d);
    }

    @Override
    @Transactional
    public DiscussionVO create(Long userId, CreateDiscussionRequest req) {
        Discussion d = new Discussion();
        d.setProjectId(req.getProjectId());
        d.setTopic(req.getTopic() == null ? "" : req.getTopic());
        d.setContent(req.getContent() == null ? "" : req.getContent());
        d.setCreatorId(userId);
        if (req.getMeetingTime() != null) d.setMeetingTime(req.getMeetingTime());
        discussionMapper.insert(d);
        return toVO(d);
    }

    @Override
    @Transactional
    public void update(Long discussionId, UpdateDiscussionRequest req) {
        Discussion d = discussionMapper.selectById(discussionId);
        if (d == null) throw new BusinessException(ResultCode.NOT_FOUND, "研讨不存在");
        if (req.getTopic() != null) d.setTopic(req.getTopic());
        if (req.getContent() != null) d.setContent(req.getContent());
        if (req.getMeetingTime() != null) d.setMeetingTime(req.getMeetingTime());
        discussionMapper.updateById(d);
    }

    @Override
    @Transactional
    public void delete(Long discussionId) {
        discussionMapper.deleteById(discussionId);
    }

    private DiscussionVO toVO(Discussion d) {
        DiscussionVO vo = new DiscussionVO();
        vo.setDiscussionId(d.getId());
        vo.setProjectId(d.getProjectId());
        vo.setTopic(d.getTopic());
        vo.setContent(d.getContent());
        vo.setMeetingTime(d.getMeetingTime());
        vo.setCreatorId(d.getCreatorId());
        vo.setCreatedAt(d.getCreatedAt());
        return vo;
    }
}
