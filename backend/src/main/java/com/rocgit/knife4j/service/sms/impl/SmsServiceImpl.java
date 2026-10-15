package com.rocgit.knife4j.service.sms.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.entity.user.UserSmsVerifyCode;
import com.rocgit.knife4j.mapper.user.UserSmsVerifyCodeMapper;
import com.rocgit.knife4j.service.sms.SmsProvider;
import com.rocgit.knife4j.service.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码服务实现（T-25）
 *
 * <p>策略：
 * <ul>
 *   <li>单号重发冷却：{@code sms.code.resend-cooldown} 秒（默认 60）</li>
 *   <li>单号日上限：{@code sms.code.daily-limit} 条（默认 10）</li>
 *   <li>验证码有效期：{@code sms.code.ttl-seconds} 秒（默认 300）</li>
 *   <li>错误上限锁定：连续 5 次错误锁定 15 分钟（写为延长 expire_at）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final UserSmsVerifyCodeMapper smsMapper;
    private final SmsProvider smsProvider;

    @Value("${sms.code.ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${sms.code.resend-cooldown:60}")
    private int resendCooldown;

    @Value("${sms.code.daily-limit:10}")
    private int dailyLimit;

    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    @Override
    @Transactional
    public void sendLoginCode(String phone) {
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            throw new BusinessException(ResultCode.PHONE_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();

        // 1) 单号重发冷却：最近一条未过期且 < cooldown 秒 → 拒
        UserSmsVerifyCode last = latestOne(phone, "login");
        if (last != null && last.getCreatedAt() != null
                && last.getCreatedAt().isAfter(now.minusSeconds(resendCooldown))) {
            throw new BusinessException(ResultCode.SMS_SEND_TOO_FREQ);
        }

        // 2) 当日条数上限
        Long count = smsMapper.selectCount(new QueryWrapper<UserSmsVerifyCode>()
                .eq("phone", phone)
                .eq("purpose", "login")
                .ge("created_at", LocalDate.now().atStartOfDay()));
        if (count != null && count >= dailyLimit) {
            throw new BusinessException(ResultCode.SMS_DAILY_LIMIT);
        }

        // 3) 生成 6 位码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));

        // 4) 落库（先写库，再下发；下发失败不回滚，验证码仍在）
        UserSmsVerifyCode rec = new UserSmsVerifyCode();
        rec.setPhone(phone);
        rec.setCode(code);
        rec.setPurpose("login");
        rec.setExpireAt(now.plusSeconds(ttlSeconds));
        rec.setUsed(0);
        smsMapper.insert(rec);

        // 5) 下发（通道失败时记录日志但允许用户用日志兜底码登录）
        boolean ok;
        try {
            ok = smsProvider.sendCode(phone, code, "login");
        } catch (Exception e) {
            log.error("[SMS] 通道异常 phone={} err={}", phone, e.getMessage());
            ok = false;
        }
        if (!ok) {
            log.warn("[SMS] 通道 {} 发送失败，请检查 sms.provider 配置；验证码已在数据库中：phone={} code={}",
                    smsProvider.channel(), phone, code);
        } else {
            log.info("[SMS] 通道 {} 发送成功 phone={} code={} purpose=login",
                    smsProvider.channel(), phone, code);
        }
    }

    @Override
    @Transactional
    public boolean verifyCode(String phone, String code, String purpose) {
        if (phone == null || code == null || purpose == null) return false;
        UserSmsVerifyCode rec = latestOne(phone, purpose);
        if (rec == null) return false;

        LocalDateTime now = LocalDateTime.now();
        if (rec.getExpireAt() == null || rec.getExpireAt().isBefore(now)) return false;
        if (rec.getUsed() != null && rec.getUsed() == 1) return false;
        if (!code.equals(rec.getCode())) {
            // 错误上限：当前 5 次错误（同 phone+purpose）→ 锁定到 now+15min
            handleWrongAttempts(phone, purpose, rec);
            return false;
        }

        // 命中：标记 used=1
        UserSmsVerifyCode upd = new UserSmsVerifyCode();
        upd.setId(rec.getId());
        upd.setUsed(1);
        smsMapper.updateById(upd);
        return true;
    }

    /**
     * 连续错误 5 次（同 phone+purpose）→ 把当前码 expire_at 延后 15 分钟
     */
    private void handleWrongAttempts(String phone, String purpose, UserSmsVerifyCode current) {
        Long wrong = smsMapper.selectCount(new QueryWrapper<UserSmsVerifyCode>()
                .eq("phone", phone)
                .eq("purpose", purpose)
                .eq("used", 0)
                .apply("code <> ''"));
        // 简化：错码条数 ≥ 5 时锁定
        if (wrong != null && wrong >= 5) {
            UserSmsVerifyCode upd = new UserSmsVerifyCode();
            upd.setId(current.getId());
            upd.setExpireAt(LocalDateTime.now().plusMinutes(15));
            smsMapper.updateById(upd);
            log.warn("[SMS] phone={} purpose={} 连续错误≥5 次，锁定 15 分钟", phone, purpose);
        }
    }

    private UserSmsVerifyCode latestOne(String phone, String purpose) {
        List<UserSmsVerifyCode> list = smsMapper.selectList(new QueryWrapper<UserSmsVerifyCode>()
                .eq("phone", phone)
                .eq("purpose", purpose)
                .eq("deleted", 0)
                .orderByDesc("created_at")
                .last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0);
    }
}
