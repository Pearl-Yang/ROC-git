package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import com.rocgit.knife4j.dto.sms.PhoneLoginRequest;
import com.rocgit.knife4j.dto.sms.SendCodeRequest;
import com.rocgit.knife4j.entity.user.LoginVO;
import com.rocgit.knife4j.service.sms.SmsService;
import com.rocgit.knife4j.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 短信验证码 Controller（T-25 手机号+验证码登录）
 *
 * <p>两个公开接口（不需 token，参见 {@code WebConfig} 白名单）：
 * <ol>
 *   <li>POST /api/v1/sms/send-code       发验证码（限流 60s/次，10 次/日）</li>
 *   <li>POST /api/v1/sms/login-by-phone  手机号+验证码登录/注册</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@Tag(name = "15.短信验证码", description = "T-25 手机号+验证码登录")
public class SmsController {

    private final SmsService smsService;
    private final UserService userService;

    @Operation(summary = "发送登录验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest req) {
        String purpose = (req.getPurpose() == null || req.getPurpose().isEmpty())
                ? "login" : req.getPurpose();
        smsService.sendLoginCode(req.getPhone());
        return Result.success("验证码已发送（" + purpose + "）", null);
    }

    @Operation(summary = "手机号+验证码登录", description = "未注册则自动创建账号；与微信登录共用 JWT 颁发逻辑")
    @PostMapping("/login-by-phone")
    public Result<LoginVO> loginByPhone(@Valid @RequestBody PhoneLoginRequest req,
                                        HttpServletRequest httpReq) {
        String ip = clientIp(httpReq);
        String device = req.getPhone(); // 用手机号当 device 标识，写入 login_history
        LoginVO vo = userService.loginByPhone(
                req.getPhone(),
                req.getCode(),
                ip,
                device,
                req.getNickname(),
                req.getAvatarUrl());
        return Result.success(vo);
    }

    private String clientIp(HttpServletRequest req) {
        if (req == null) return "";
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = req.getRemoteAddr();
        return ip == null ? "" : ip;
    }
}
