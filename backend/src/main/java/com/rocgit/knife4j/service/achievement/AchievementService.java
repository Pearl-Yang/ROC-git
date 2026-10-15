package com.rocgit.knife4j.service.achievement;

import com.rocgit.knife4j.dto.achievement.*;
import com.rocgit.knife4j.entity.achievement.AchievementVO;

import java.util.Map;

public interface AchievementService {

    Map<String, Object> list(Long projectId, int pageNum, int pageSize);

    AchievementVO detail(Long achievementId);

    AchievementVO create(Long userId, CreateAchievementRequest request);

    void update(Long achievementId, UpdateAchievementRequest request);

    void delete(Long achievementId);
}