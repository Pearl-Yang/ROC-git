package com.rocgit.knife4j.service.common;

import com.rocgit.knife4j.dto.common.FeedbackRequest;
import com.rocgit.knife4j.dto.common.UploadRequest;

import java.util.Map;

public interface CommonService {

    Map<String, Object> config();

    Map<String, Object> upload(UploadRequest request);

    void feedback(Long userId, FeedbackRequest request);
}