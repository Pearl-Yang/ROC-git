package com.rocgit.knife4j.service.wechat;

import lombok.Data;

/**
 * W3 微信小程序登录响应
 */
@Data
public class WechatSessionVO {
    private String openid;
    private String sessionKey;
    private String unionid;
    private String errcode;
    private String errmsg;
    public boolean ok() { return errcode == null || errcode.isEmpty() || "0".equals(errcode); }
}
