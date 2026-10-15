package com.rocgit.knife4j.common;

import lombok.Getter;

/**
 * 业务错误码枚举
 *
 * 对应 openapi.yaml 中的错误码说明：
 *   0    成功
 *   400  请求参数错误
 *   401  未认证
 *   403  无权限
 *   404  资源不存在
 *   409  资源冲突
 *   429  请求频繁
 *   500  服务器错误
 *
 * 国际化（T-E-2）：
 *   - 构造时除 message 外，多带一个 i18nKey
 *   - 通过 {@link #getLocalizedMessage()} 按当前 Locale 取真实文案
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success", "common.success"),
    FAIL(500, "服务器内部错误", "common.fail"),
    BAD_REQUEST(400, "请求参数错误", "common.badRequest"),
    UNAUTHORIZED(401, "未认证", "common.unauthorized"),
    FORBIDDEN(403, "无权限访问", "common.forbidden"),
    NOT_FOUND(404, "资源不存在", "common.notFound"),
    CONFLICT(409, "资源冲突", "common.conflict"),
    TOO_MANY_REQUESTS(429, "请求过于频繁", null),

    // 业务级错误码（10xxx）
    USER_NOT_FOUND(10001, "用户不存在", "user.notFound"),
    INVALID_TOKEN(10002, "无效的 Token", "user.invalidToken"),
    TOKEN_EXPIRED(10003, "Token 已过期", "user.tokenExpired"),
    GROUP_NOT_FOUND(10101, "小组不存在", "group.notFound"),
    NOT_GROUP_MEMBER(10102, "您不是该小组成员", "group.notMember"),
    INVITE_CODE_INVALID(10103, "邀请码无效", "group.inviteCodeInvalid"),
    PROJECT_NOT_FOUND(10201, "项目不存在", "project.notFound"),
    FILE_NOT_FOUND(10301, "文件不存在", "file.notFound"),
    FILE_UPLOAD_FAIL(10302, "文件上传失败", "file.uploadFail"),
    FILE_LOCKED(10303, "文件已被他人锁定，请稍后再试", "file.locked"),
    TASK_NOT_FOUND(10401, "任务不存在", "task.notFound"),
    SHARE_LINK_EXPIRED(10501, "分享链接已过期", "share.expired"),

    // T-25：手机号验证码登录
    PHONE_INVALID(10601, "手机号格式不正确", "phone.invalid"),
    SMS_CODE_INVALID(10602, "验证码错误或已过期", "sms.codeInvalid"),
    SMS_SEND_TOO_FREQ(10603, "请求过于频繁，请稍后再试", "sms.tooFreq"),
    SMS_DAILY_LIMIT(10604, "当日验证码获取次数已达上限", "sms.dailyLimit"),
    SMS_PROVIDER_DOWN(10605, "短信通道暂时不可用", "sms.providerDown"),
    PHONE_ALREADY_REGISTERED(10606, "该手机号已绑定其他账号", "phone.alreadyRegistered"),

    // 阶段/任务
    STAGE_NOT_FOUND(11101, "阶段不存在", "stage.notFound"),
    TASK_ALREADY_COMPLETED(11201, "任务已完成", "task.alreadyCompleted"),
    TASK_OVERDUE(11202, "任务已逾期", "task.overdue"),

    // 协作
    DISCUSSION_NOT_FOUND(12101, "研讨记录不存在", "discussion.notFound"),
    DECISION_NOT_FOUND(12201, "决策记录不存在", "decision.notFound"),
    FILE_AUDIT_FORBIDDEN(12301, "您没有审核权限", "fileAudit.forbidden"),
    SHARE_TOKEN_INVALID(12401, "分享口令无效", "share.tokenInvalid"),

    // 数据库/集成
    DB_CONFLICT(50001, "数据冲突，请刷新后重试", "db.conflict"),
    DB_CONSTRAINT(50002, "数据约束冲突", "db.constraint");

    private final Integer code;
    private final String message;
    private final String i18nKey;

    ResultCode(Integer code, String message, String i18nKey) {
        this.code = code;
        this.message = message;
        this.i18nKey = i18nKey;
    }

    /**
     * 按当前线程 Locale 取本地化消息（找不到时回退内置中文 message）
     */
    public String getLocalizedMessage() {
        if (i18nKey == null) return message;
        try {
            String s = I18nUtil.get(i18nKey, I18nUtil.getLocale());
            if (s != null && !s.isEmpty() && !s.equals(i18nKey)) return s;
        } catch (Exception ignore) {}
        return message;
    }
}
