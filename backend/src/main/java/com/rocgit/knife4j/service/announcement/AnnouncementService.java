package com.rocgit.knife4j.service.announcement;

import com.rocgit.knife4j.entity.announcement.AnnouncementVO;

import java.util.Map;

public interface AnnouncementService {

    Map<String, Object> list(Long groupId, Long projectId, int pageNum, int pageSize);

    AnnouncementVO create(Long userId, Long groupId, Long projectId, String title, String content);

    void delete(Long announcementId);
}