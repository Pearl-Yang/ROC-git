package com.rocgit.knife4j.interceptor;

import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 鉴权拦截器
 *
 *  - 解析 Authorization: Bearer xxx
 *  - 把 uid / openid 写入 request attribute，供 Controller 取出
 *  - 401 时直接返回 JSON：{ code: 401, message: "未登录" }
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS（预检请求不需要 token）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(response, 401, "未登录或缺少 Authorization 头");
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            return reject(response, 401, "Token 不能为空");
        }

        try {
            Claims claims = jwtUtil.parse(token);
            Object uidObj = claims.get("uid");
            if (uidObj == null) {
                return reject(response, 401, "Token 缺少 uid 声明");
            }
            Long uid = uidObj instanceof Number ? ((Number) uidObj).longValue() : Long.parseLong(uidObj.toString());
            String openid = (String) claims.get("openid");

            request.setAttribute(UserContext.ATTR_USER_ID, uid);
            request.setAttribute(UserContext.ATTR_OPENID, openid);
            return true;
        } catch (Exception e) {
            return reject(response, 401, "Token 无效或已过期：" + e.getMessage());
        }
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"timestamp\":%d}",
                code, msg, System.currentTimeMillis());
        response.getWriter().write(body);
        return false;
    }
}
