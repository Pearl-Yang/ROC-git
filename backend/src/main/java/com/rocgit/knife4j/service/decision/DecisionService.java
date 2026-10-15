package com.rocgit.knife4j.service.decision;

import com.rocgit.knife4j.dto.decision.*;
import com.rocgit.knife4j.entity.decision.DecisionVO;

import java.util.Map;

public interface DecisionService {

    Map<String, Object> list(Long projectId, int pageNum, int pageSize);

    DecisionVO detail(Long decisionId);

    DecisionVO create(Long userId, CreateDecisionRequest request);

    void update(Long decisionId, UpdateDecisionRequest request);

    void delete(Long decisionId);
}