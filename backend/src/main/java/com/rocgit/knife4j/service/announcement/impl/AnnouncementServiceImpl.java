package com.rocgit.knife4j.service.announcement.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.entity.announcement.Announcement;
import com.rocgit.knife4j.entity.announcement.AnnouncementVO;
import com.rocgit.knife4j.mapper.announcement.AnnouncementMapper;
import com.rocgit.knife4j.service.announcement.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired private AnnouncementMapper announcementMapper;

    @Override
    public Map<String, Object> list(Long groupId, Long projectId, int pageNum, int pageSize) {
        QueryWrapper<Announcement> qw = new QueryWrapper<Announcement>()
                .eq("deleted", 0).orderByDesc("created_at");
        // 绠€鍖栵細鍏憡琛ㄧ敤 message 鎵胯浇锛岃繖閲?type='announcement'
        qw.eq("type", "announcement");
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        qw.last("LIMIT " + offset + "," + pageSize);
        List<Announcement> list = announcementMapper.selectList(qw);
        List<AnnouncementVO> vos = new ArrayList<>();
        for (Announcement a : list) vos.add(toVO(a));
        Long total = announcementMapper.selectCount(
                new QueryWrapper<Announcement>().eq("deleted", 0).eq("type", "announcement"));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", vos);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    @Transactional
    public AnnouncementVO create(Long userId, Long groupId, Long projectId, String title, String content) {
        Announcement a = new Announcement();
        a.setUserId(userId);
        a.setTitle(title);
        a.setContent(content);
        a.setType("announcement");
        announcementMapper.insert(a);
        return toVO(a);
    }

    @Override
    @Transactional
    public void delete(Long announcementId) {
        Announcement a = announcementMapper.selectById(announcementId);
        if (a == null) return;
        a.setDeleted(1);
        announcementMapper.updateById(a);
    }

    private AnnouncementVO toVO(Announcement a) {
        AnnouncementVO vo = new AnnouncementVO();
        vo.setAnnouncementId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setContent(a.getContent());
        vo.setIsRead(a.getIsRead() != null && a.getIsRead() == 1);
        vo.setLevel("normal");
        vo.setCreatedAt(a.getCreatedAt());
        return vo;
    }
}
