package com.rocgit.knife4j.service.project;

import com.rocgit.knife4j.dto.project.*;
import com.rocgit.knife4j.entity.project.*;

import java.util.List;
import java.util.Map;

public interface ProjectService {

    ProjectDetailVO create(Long userId, CreateProjectRequest request);

    List<ProjectVO> list(Long groupId);

    ProjectDetailVO detail(Long projectId);

    void update(Long projectId, UpdateProjectRequest request);

    void delete(Long projectId);

    ProjectVO copy(Long userId, Long projectId, CopyProjectRequest request);

    void complete(Long projectId);

    Map<String, Object> progress(Long projectId);

    List<ActivityVO> activities(Long projectId, String type, int pageNum, int pageSize);

    Map<String, Object> taskStats(Long projectId);

    StageVO addStage(Long projectId, AddStageRequest request);

    List<StageVO> stageList(Long projectId);

    void stageOrder(Long projectId, StageOrderRequest request);

    StageVO stageDetail(Long projectId, Long stageId);

    void updateStage(Long projectId, Long stageId, UpdateStageRequest request);

    void deleteStage(Long projectId, Long stageId);

    List<ProjectTemplateVO> templateList();

    ProjectTemplateVO templateDetail(Long templateId);

    ProjectTemplateVO createTemplate(Long userId, CreateTemplateRequest request);

    /**
     * T-06：项目成员贡献度统计（按 projectId 聚合）
     * @return 按 score 降序排列的 ContributionStatsVO 列表
     */
    java.util.List<ContributionStatsVO> contributionStats(Long projectId);
}