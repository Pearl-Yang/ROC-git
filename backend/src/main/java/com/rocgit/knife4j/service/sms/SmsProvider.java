package com.rocgit.knife4j.service.sms;

/**
 * 短信通道抽象（T-25）
 *
 * <p>实现类通过 {@code sms.provider} 配置项激活：
 * <ul>
 *   <li>log  —— {@code LogSmsProvider}，默认；把验证码写到 logs/sms-*.log，方便本地演示</li>
 *   <li>tencent —— {@code TencentSmsProvider}，预留骨架，credential 填齐后即可切真发</li>
 * </ul>
 */
public interface SmsProvider {

    /**
     * 通道标识
     * @return log / tencent
     */
    String channel();

    /**
     * 发送验证码。
     *
     * @param phone   手机号（已通过格式校验）
     * @param code    6 位验证码
     * @param purpose 用途：login / reset
     * @return true=成功；false=通道不可用（调用方应降级或上报）
     */
    boolean sendCode(String phone, String code, String purpose);
}
