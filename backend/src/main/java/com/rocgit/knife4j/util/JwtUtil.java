package com.rocgit.knife4j.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：签发 / 解析 / 校验
 *
 *  - secret：HS256 对称密钥（≥ 32 字节）
 *  - 载荷 claim 形如 { uid: 10001, openid: "..." }
 *  - 默认过期时间从配置文件 jwt.expiration 读取（秒）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800}")
    private Long expirationSeconds;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 token
     */
    public String sign(Long userId, String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("openid", openid == null ? "" : openid);

        Date now = new Date();
        Date expire = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token，返回 Claims。失败/过期/被篡改 抛异常由调用方处理。
     */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 仅取用户 ID（最常用）
     */
    public Long parseUserId(String token) {
        Object v = parse(token).get("uid");
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    public String parseOpenid(String token) {
        Object v = parse(token).get("openid");
        return v == null ? null : v.toString();
    }
}
