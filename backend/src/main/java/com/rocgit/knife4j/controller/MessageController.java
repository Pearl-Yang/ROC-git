package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.message.*;
import com.rocgit.knife4j.entity.message.*;
import com.rocgit.knife4j.service.message.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/message")
@Tag(name = "10.消息", description = "系统消息 / 订阅 / 公告")
@SecurityRequirement(name = "Bearer")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Operation(summary = "未读数")
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> unreadCount() {
        Long uid = UserContext.requireUserId();
        return Result.success(messageService.unreadCount(uid));
    }

    @Operation(summary = "消息列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long uid = UserContext.requireUserId();
        return Result.success(messageService.list(uid, pageNum, pageSize));
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public Result<Void> readAll(@RequestBody(required = false) @Valid ReadAllRequest request) {
        Long uid = UserContext.requireUserId();
        messageService.readAll(uid, request);
        return Result.success("已全部已读", null);
    }

    @Operation(summary = "公告列表")
    @GetMapping("/announcement-list")
    public Result<List<MessageVO>> announcementList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        Long uid = UserContext.requireUserId();
        return Result.success(messageService.announcementList(uid, pageNum, pageSize));
    }

    @Operation(summary = "订阅模板")
    @PostMapping("/subscribe")
    public Result<Void> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Long uid = UserContext.requireUserId();
        request.setUserId(uid);
        messageService.subscribe(request);
        return Result.success("订阅成功", null);
    }

    @Operation(summary = "取消订阅")
    @PostMapping("/unsubscribe")
    public Result<Void> unsubscribe(@Valid @RequestBody SubscribeRequest request) {
        Long uid = UserContext.requireUserId();
        request.setUserId(uid);
        messageService.unsubscribe(request);
        return Result.success("已取消订阅", null);
    }

    @Operation(summary = "我的订阅")
    @GetMapping("/subscriptions")
    public Result<List<SubscriptionMessage>> subscriptions() {
        Long uid = UserContext.requireUserId();
        return Result.success(messageService.listSubscriptions(uid));
    }

    @Operation(summary = "更新订阅")
    @PutMapping("/subscription")
    public Result<Void> updateSubscription(@Valid @RequestBody UpdateSubscriptionRequest request) {
        Long uid = UserContext.requireUserId();
        // 将更新动作复用 subscribe / unsubscribe：先按 type 查一条再改 subscribed 状态
        SubscribeRequest sr = new SubscribeRequest();
        sr.setUserId(uid);
        sr.setType(request.getType());
        sr.setTemplateId(request.getType());
        if (Boolean.TRUE.equals(request.getEnabled())) {
            messageService.subscribe(sr);
        } else {
            messageService.unsubscribe(sr);
        }
        return Result.success("更新成功", null);
    }
}
