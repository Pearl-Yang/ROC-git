package com.rocgit.knife4j.common;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 当前登录用户上下文
 *
 *  通过 {@link com.rocgit.knife4j.interceptor.JwtInterceptor} 把
 *  当前用户的 userId（Long）与 openid（String）写入 request attribute，
 *  Controller / Service 中通过静态方法取出使用。
 */
public final class UserContext {

    public static final String ATTR_USER_ID  = "userId";
    public static final String ATTR_OPENID   = "openid";

    private UserContext() { }

    public static Long currentUserId() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        Object uid = req.getAttribute(ATTR_USER_ID);
        if (uid == null) return null;
        if (uid instanceof Long)  return (Long) uid;
        if (uid instanceof Number) return ((Number) uid).longValue();
        try { return Long.parseLong(uid.toString()); } catch (NumberFormatException e) { return null; }
    }

    public static String currentOpenid() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        Object v = req.getAttribute(ATTR_OPENID);
        return v == null ? null : v.toString();
    }

    /** 必须登录才能拿到，否则抛 401 */
    public static Long requireUserId() {
        Long uid = currentUserId();
        if (uid == null) {
            throw new com.rocgit.knife4j.common.BusinessException(401, "未登录");
        }
        return uid;
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
