package com.rocgit.knife4j.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订阅设置请求
 */
@Data
@Schema(description = "订阅设置请求")
public class SubscribeRequest {

    @Schema(description = "类型", example = "audit",
            allowableValues = {"audit", "task", "member", "stage", "mention"})
    private String type;

    @Schema(description = "是否订阅")
    private Boolean enabled;

    @Schema(description = "订阅渠道")
    private String channel;

    // P1-1：补充字段以支持真实订阅存储
    @Schema(description = "模板ID（用于映射到 subscription_message.template_id）")
    private String templateId;

    @Schema(description = "用户ID（来自当前登录态时不必传）")
    private Long userId;

    @Schema(description = "微信 openid")
    private String openid;
}
