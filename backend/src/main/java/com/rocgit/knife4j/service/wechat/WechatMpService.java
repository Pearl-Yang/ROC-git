package com.rocgit.knife4j.service.wechat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * W3 微信小程序 jscode2session 真接。
 *
 * <p>激活条件：{@code wechat.mp.app-id != '' && wechat.mp.app-secret != ''}。
 * <p>未配置时（dev 默认）走 noop，不接真接口，与 P1-1 兼容。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "wechat.mp.app-id")
public class WechatMpService {

    @Value("${wechat.mp.app-id:}")
    private String appId;

    @Value("${wechat.mp.app-secret:}")
    private String appSecret;

    @Value("${wechat.mp.jscode2session-url:https://api.weixin.qq.com/sns/jscode2session}")
    private String jscode2sessionUrl;

    private final RestTemplate http = new RestTemplate();

    @PostConstruct
    public void init() {
        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            log.warn("[WechatMp] app-id / app-secret 未配置，小程序登录将不可用");
        } else {
            log.info("[WechatMp] 已激活 appId={}", appId);
        }
    }

    /**
     * 调 jscode2session 拿 openid / session_key。
     * 出错时返非 null 但 ok=false 的对象；不会抛异常。
     */
    public WechatSessionVO code2Session(String code) {
        WechatSessionVO vo = new WechatSessionVO();
        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            vo.setErrcode("MISSING_CREDENTIAL");
            vo.setErrmsg("wechat.mp.app-id/app-secret 未配置");
            return vo;
        }
        if (code == null || code.isEmpty()) {
            vo.setErrcode("INVALID_CODE");
            vo.setErrmsg("code 不能为空");
            return vo;
        }
        String url = jscode2sessionUrl
                + "?appid=" + safe(appId)
                + "&secret=" + safe(appSecret)
                + "&js_code=" + safe(code)
                + "&grant_type=authorization_code";
        try {
            WechatSessionVO body = http.getForObject(url, WechatSessionVO.class);
            if (body == null) {
                vo.setErrcode("EMPTY_RESPONSE");
                vo.setErrmsg("微信返回空响应");
            } else {
                vo.setOpenid(body.getOpenid());
                vo.setSessionKey(body.getSessionKey());
                vo.setUnionid(body.getUnionid());
                vo.setErrcode(body.getErrcode());
                vo.setErrmsg(body.getErrmsg());
            }
        } catch (Exception e) {
            log.error("[WechatMp] code2session 失败：{}", e.getMessage());
            vo.setErrcode("HTTP_ERROR");
            vo.setErrmsg(e.getMessage());
        }
        return vo;
    }

    public boolean enabled() {
        return appId != null && !appId.isEmpty() && appSecret != null && !appSecret.isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "" : java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
