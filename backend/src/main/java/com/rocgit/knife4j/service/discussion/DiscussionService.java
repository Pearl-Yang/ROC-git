package com.rocgit.knife4j.service.discussion;

import com.rocgit.knife4j.dto.discussion.*;
import com.rocgit.knife4j.entity.discussion.DiscussionVO;

import java.util.Map;

public interface DiscussionService {

    Map<String, Object> list(Long projectId, int pageNum, int pageSize);

    DiscussionVO detail(Long discussionId);

    DiscussionVO create(Long userId, CreateDiscussionRequest request);

    void update(Long discussionId, UpdateDiscussionRequest request);

    void delete(Long discussionId);
}