package com.rocgit.knife4j.config;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * T-Q-1：API 访问日志过滤器
 *  - 记录 method / path / status / 耗时 / 客户端 IP
 *  - 注入 traceId（MDC），供全局异常处理关联
 *  - 排除 /files/** / /flowcharts/** / 静态资源
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("API_ACCESS");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/files/") || p.startsWith("/flowcharts/")
                || p.startsWith("/doc.html") || p.startsWith("/swagger-ui")
                || p.startsWith("/webjars/") || p.startsWith("/v3/api-docs")
                || p.endsWith(".png") || p.endsWith(".js") || p.endsWith(".css");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long t0 = System.currentTimeMillis();
        String traceId = req.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        res.setHeader("X-Trace-Id", traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            long cost = System.currentTimeMillis() - t0;
            int code = res.getStatus();
            String marker = (code >= 500) ? "[ERR]" : (code >= 400 ? "[WARN]" : "[OK]");
            log.info("{} {} {} {} {}ms ip={}",
                    marker, req.getMethod(), req.getRequestURI(), code, cost, clientIp(req));
            MDC.remove("traceId");
        }
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
