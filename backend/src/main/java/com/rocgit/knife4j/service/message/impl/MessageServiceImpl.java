package com.rocgit.knife4j.service.message.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.message.*;
import com.rocgit.knife4j.entity.message.Message;
import com.rocgit.knife4j.entity.message.MessageVO;
import com.rocgit.knife4j.entity.message.SubscriptionMessage;
import com.rocgit.knife4j.entity.message.UnreadCountVO;
import com.rocgit.knife4j.mapper.message.MessageMapper;
import com.rocgit.knife4j.mapper.message.SubscriptionMessageMapper;
import com.rocgit.knife4j.service.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired private MessageMapper messageMapper;
    @Autowired private SubscriptionMessageMapper subscriptionMessageMapper;

    @Override
    public UnreadCountVO unreadCount(Long userId) {
        Long c = messageMapper.selectCount(
                new QueryWrapper<Message>().eq("user_id", userId).eq("is_read", 0).eq("deleted", 0));
        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotal(c == null ? 0 : c.intValue());
        vo.setUnread(c == null ? 0 : c.intValue());
        return vo;
    }

    @Override
    public Map<String, Object> list(Long userId, int pageNum, int pageSize) {
        QueryWrapper<Message> qw = new QueryWrapper<Message>()
                .eq("user_id", userId).eq("deleted", 0).orderByDesc("created_at");
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        qw.last("LIMIT " + offset + "," + pageSize);
        List<Message> list = messageMapper.selectList(qw);
        Long total = messageMapper.selectCount(
                new QueryWrapper<Message>().eq("user_id", userId).eq("deleted", 0));
        List<MessageVO> vos = new ArrayList<>();
        for (Message m : list) vos.add(toVO(m));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", vos);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    @Transactional
    public void readAll(Long userId, ReadAllRequest request) {
        Message update = new Message();
        update.setIsRead(1);
        messageMapper.update(update,
                new UpdateWrapper<Message>().eq("user_id", userId).eq("deleted", 0).eq("is_read", 0));
    }

    @Override
    public List<MessageVO> announcementList(Long userId, int pageNum, int pageSize) {
        List<Message> list = messageMapper.selectList(
                new QueryWrapper<Message>()
                        .eq("type", "announcement")
                        .eq("deleted", 0)
                        .orderByDesc("created_at")
                        .last("LIMIT " + pageSize));
        List<MessageVO> result = new ArrayList<>();
        for (Message m : list) result.add(toVO(m));
        return result;
    }

    @Override
    @Transactional
    public void subscribe(SubscribeRequest request) {
        if (request == null || request.getTemplateId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "templateId 必填");
        }
        Long uid = request.getUserId();
        String openid = request.getOpenid();
        // 唯一性 (templateId, user_id, deleted=0)
        SubscriptionMessage exist = subscriptionMessageMapper.selectOne(
                new QueryWrapper<SubscriptionMessage>()
                        .eq("template_id", request.getTemplateId())
                        .eq("user_id", uid)
                        .eq("deleted", 0));
        if (exist != null) {
            // 已订阅：更新 subscribed=1, expires_at
            exist.setSubscribed(1);
            exist.setOpenid(openid);
            exist.setExpiresAt(LocalDateTime.now().plusDays(30));
            subscriptionMessageMapper.updateById(exist);
        } else {
            SubscriptionMessage sm = new SubscriptionMessage();
            sm.setTemplateId(request.getTemplateId());
            sm.setUserId(uid);
            sm.setOpenid(openid);
            sm.setSubscribed(1);
            sm.setExpiresAt(LocalDateTime.now().plusDays(30));
            subscriptionMessageMapper.insert(sm);
        }
    }

    @Override
    @Transactional
    public void unsubscribe(SubscribeRequest request) {
        if (request == null || request.getTemplateId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "templateId 必填");
        }
        Long uid = request.getUserId();
        SubscriptionMessage exist = subscriptionMessageMapper.selectOne(
                new QueryWrapper<SubscriptionMessage>()
                        .eq("template_id", request.getTemplateId())
                        .eq("user_id", uid)
                        .eq("deleted", 0));
        if (exist != null) {
            exist.setSubscribed(0);
            subscriptionMessageMapper.updateById(exist);
        }
    }

    /** P1-1：列出当前用户有效订阅 */
    @Override
    public List<SubscriptionMessage> listSubscriptions(Long userId) {
        return subscriptionMessageMapper.selectList(
                new QueryWrapper<SubscriptionMessage>()
                        .eq("user_id", userId)
                        .eq("deleted", 0)
                        .eq("subscribed", 1));
    }

    private MessageVO toVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(m.getId());
        vo.setTitle(m.getTitle());
        vo.setContent(m.getContent());
        vo.setType(m.getType());
        vo.setRead(m.getIsRead() != null && m.getIsRead() == 1);
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}
