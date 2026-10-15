package com.rocgit.knife4j.service.sms;

/**
 * 短信验证码服务（T-25 手机号+验证码登录）
 */
public interface SmsService {

    /**
     * 发送登录验证码（含限流：单号 60s 冷却、每日 10 条上限）
     *
     * @param phone 已校验格式的 11 位手机号
     */
    void sendLoginCode(String phone);

    /**
     * 校验验证码；命中即标记 used=1（一次性）
     *
     * @param phone   手机号
     * @param code    6 位验证码
     * @param purpose 用途：login / reset
     * @return true=验证通过
     */
    boolean verifyCode(String phone, String code, String purpose);
}
