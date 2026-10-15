package com.rocgit.knife4j.service.sms.impl;

import com.rocgit.knife4j.service.sms.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 腾讯云短信通道（T-25，预留骨架）
 *
 * <p>激活条件：{@code sms.provider=tencent}。
 *
 * <p>必填配置（{@code application.yml} → {@code sms.tencent.*}）：
 * <ul>
 *   <li>secret-id    腾讯云 API 密钥 ID</li>
 *   <li>secret-key   腾讯云 API 密钥 KEY</li>
 *   <li>app-id       短信应用 ID（SdkAppId）</li>
 *   <li>sign-name    已审核通过的短信签名</li>
 *   <li>template-id  已审核通过的短信模板 ID</li>
 * </ul>
 *
 * <p>credential 缺失时，本类不会启动（条件注解要求 {@code sms.provider=tencent}），
 * 由 LogSmsProvider 兜底。credential 错误时 SDK 抛 {@code TencentCloudSDKException}，
 * 本类会捕获并返回 false，让上层降级。
 *
 * <p><b>回学校拿到 SDK credential 后只需：</b>
 * <ol>
 *   <li>把环境变量 {@code TENCENT_SMS_SECRET_ID / TENCENT_SMS_SECRET_KEY / TENCENT_SMS_APP_ID /
 *       TENCENT_SMS_SIGN_NAME / TENCENT_SMS_TEMPLATE_ID} 填上</li>
 *   <li>设置 {@code SMS_PROVIDER=tencent}</li>
 *   <li>把下方 {@code doSend} 方法里被注释的代码放开即可</li>
 * </ol>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "tencent")
public class TencentSmsProvider implements SmsProvider {

    @Value("${sms.tencent.secret-id:}")
    private String secretId;

    @Value("${sms.tencent.secret-key:}")
    private String secretKey;

    @Value("${sms.tencent.app-id:}")
    private String appId;

    @Value("${sms.tencent.sign-name:ROC-git}")
    private String signName;

    @Value("${sms.tencent.template-id:}")
    private String templateId;

    @Override
    public String channel() {
        return "tencent";
    }

    @Override
    public boolean sendCode(String phone, String code, String purpose) {
        // 1) credential 自检：缺少关键字段直接抛业务异常，前端能看到
        if (secretId == null || secretId.isEmpty()
                || secretKey == null || secretKey.isEmpty()
                || appId == null || appId.isEmpty()
                || templateId == null || templateId.isEmpty()) {
            String msg = "腾讯云短信 credential 不完整：缺 " +
                    (secretId.isEmpty() ? "secret-id " : "") +
                    (secretKey.isEmpty() ? "secret-key " : "") +
                    (appId.isEmpty() ? "app-id " : "") +
                    (templateId.isEmpty() ? "template-id " : "") +
                    "（请设置 sms.tencent.* 或环境变量 TENCENT_SMS_*）";
            log.warn("[SMS][tencent] {}", msg);
            throw new com.rocgit.knife4j.common.BusinessException(
                    com.rocgit.knife4j.common.ResultCode.BAD_REQUEST, msg);
        }

        // 2) 真发（参考代码：回学校拿到 credential 后放开以下注释块即可）
        try {
            /*
            com.tencentcloudapi.common.Credential cred =
                    new com.tencentcloudapi.common.Credential(secretId, secretKey);
            com.tencentcloudapi.common.profile.ClientProfile clientProfile =
                    new com.tencentcloudapi.common.profile.ClientProfile();
            clientProfile.setSignMethod("HmacSHA256");
            com.tencentcloudapi.sms.v20210111.SmsClient client =
                    new com.tencentcloudapi.sms.v20210111.SmsClient(cred, "ap-guangzhou", clientProfile);
            com.tencentcloudapi.sms.v20210111.models.SendSmsRequest req =
                    new com.tencentcloudapi.sms.v20210111.models.SendSmsRequest();
            req.setSmsSdkAppId(appId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            req.setPhoneNumberSet(new String[]{ "+86" + phone });
            // 模板变量顺序需与腾讯云后台模板一致：{1}=验证码，{2}=有效期
            req.setTemplateParamSet(new String[]{ code, "5" });
            com.tencentcloudapi.sms.v20210111.models.SendSmsResponse resp =
                    client.SendSms(req);
            if (resp.getSendStatusSet() != null && resp.getSendStatusSet().length > 0) {
                String code0 = resp.getSendStatusSet()[0].getCode();
                return "Ok".equalsIgnoreCase(code0);
            }
            return false;
            */
            // 当前未放开真发，credential 已填但代码未开 ⇒ 返回 false，降级到 log
            log.warn("[SMS][tencent] 骨架未放开真发，请按注释启用 Tencent SDK 调用");
            return false;
        } catch (Exception e) {
            log.error("[SMS][tencent] 发送异常，降级为日志通道: {}", e.getMessage());
            return false;
        }
    }
}
