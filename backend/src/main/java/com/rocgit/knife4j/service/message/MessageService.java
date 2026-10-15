package com.rocgit.knife4j.service.message;

import com.rocgit.knife4j.dto.message.*;
import com.rocgit.knife4j.entity.message.MessageVO;
import com.rocgit.knife4j.entity.message.SubscriptionMessage;
import com.rocgit.knife4j.entity.message.UnreadCountVO;

import java.util.List;
import java.util.Map;

public interface MessageService {

    UnreadCountVO unreadCount(Long userId);

    Map<String, Object> list(Long userId, int pageNum, int pageSize);

    void readAll(Long userId, ReadAllRequest request);

    List<MessageVO> announcementList(Long userId, int pageNum, int pageSize);

    void subscribe(SubscribeRequest request);

    void unsubscribe(SubscribeRequest request);

    List<SubscriptionMessage> listSubscriptions(Long userId);
}