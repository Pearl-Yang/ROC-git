package com.rocgit.knife4j.service.achievement.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.achievement.*;
import com.rocgit.knife4j.entity.achievement.Achievement;
import com.rocgit.knife4j.entity.achievement.AchievementVO;
import com.rocgit.knife4j.mapper.achievement.AchievementMapper;
import com.rocgit.knife4j.service.achievement.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementMapper achievementMapper;

    @Override
    public Map<String, Object> list(Long projectId, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;
        QueryWrapper<Achievement> qw = new QueryWrapper<>();
        if (projectId != null) qw.eq("project_id", projectId);
        qw.eq("deleted", 0).orderByDesc("created_at");
        List<Achievement> all = achievementMapper.selectList(qw);
        int from = Math.min((pageNum - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        List<AchievementVO> vos = new ArrayList<>();
        if (from < to) {
            for (Achievement a : all.subList(from, to)) vos.add(toVO(a));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", all.size());
        data.put("records", vos);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return data;
    }

    @Override
    public AchievementVO detail(Long achievementId) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "成绩不存在");
        return toVO(a);
    }

    @Override
    @Transactional
    public AchievementVO create(Long userId, CreateAchievementRequest req) {
        Achievement a = new Achievement();
        a.setProjectId(req.getProjectId());
        a.setStageId(req.getStageId() == null ? 0L : req.getStageId());
        a.setTitle(req.getTitle() == null ? "" : req.getTitle());
        a.setDescription(req.getDescription());
        achievementMapper.insert(a);
        return toVO(a);
    }

    @Override
    @Transactional
    public void update(Long achievementId, UpdateAchievementRequest req) {
        Achievement a = achievementMapper.selectById(achievementId);
        if (a == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "成绩不存在");
        if (req.getTitle() != null) a.setTitle(req.getTitle());
        if (req.getDescription() != null) a.setDescription(req.getDescription());
        achievementMapper.updateById(a);
    }

    @Override
    @Transactional
    public void delete(Long achievementId) {
        achievementMapper.deleteById(achievementId);
    }

    private AchievementVO toVO(Achievement a) {
        AchievementVO vo = new AchievementVO();
        vo.setAchievementId(a.getId());
        vo.setProjectId(a.getProjectId());
        vo.setStageId(a.getStageId());
        vo.setTitle(a.getTitle());
        vo.setDescription(a.getDescription());
        vo.setCreatedAt(a.getCreatedAt());
        return vo;
    }
}
