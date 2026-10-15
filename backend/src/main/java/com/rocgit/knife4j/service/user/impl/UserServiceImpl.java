package com.rocgit.knife4j.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.user.LoginRequest;
import com.rocgit.knife4j.dto.user.UpdateProfileRequest;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.entity.group.GroupMember;
import com.rocgit.knife4j.entity.user.LoginHistory;
import com.rocgit.knife4j.entity.user.LoginHistoryVO;
import com.rocgit.knife4j.entity.user.ProfileVO;
import com.rocgit.knife4j.entity.user.ContributionStatsVO;
import com.rocgit.knife4j.entity.user.LoginVO;
import com.rocgit.knife4j.mapper.UserMapper;
import com.rocgit.knife4j.mapper.group.GroupMemberMapper;
import com.rocgit.knife4j.mapper.user.LoginHistoryMapper;
import com.rocgit.knife4j.service.sms.SmsService;
import com.rocgit.knife4j.service.user.UserService;
import com.rocgit.knife4j.service.wechat.WechatMpService;
import com.rocgit.knife4j.service.wechat.WechatSessionVO;
import com.rocgit.knife4j.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;
    private final com.rocgit.knife4j.service.wechat.WechatMpService wechatMpService;

    @Override
    @Transactional
    public LoginVO login(LoginRequest request, String ip, String device) {
        if (request == null || request.getCode() == null || request.getCode().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "code不能为空");
        }
        String code = request.getCode();
        String openid;
        String unionid = null;

        // W3：wx_real_ 前缀或配置了 appid 时，调真接口 jscode2session
        if (wechatMpService != null && wechatMpService.enabled()
                && (code.startsWith("wx_real_") || (request.getCode().length() > 20 && !code.startsWith("wx_user_")))) {
            WechatSessionVO session = wechatMpService.code2Session(code.startsWith("wx_real_") ? code.substring(8) : code);
            if (!session.ok() || session.getOpenid() == null) {
                throw new BusinessException(ResultCode.FAIL,
                        "微信登录失败: " + (session.getErrmsg() == null ? "未知错误" : session.getErrmsg()));
            }
            openid = session.getOpenid();
            unionid = session.getUnionid();
        } else {
            // dev mock：把 code 直接当 openid 用
            openid = code;
        }

        User user = userMapper.selectOne(
                new QueryWrapper<User>()
                        .eq("openid", openid).eq("deleted", 0).last("LIMIT 1"));

        boolean isNew = false;
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(request.getNickname() == null ? "新用户" : request.getNickname());
            user.setAvatarUrl(request.getAvatarUrl() == null ? "" : request.getAvatarUrl());
            userMapper.insert(user);
            isNew = true;
        } else {
            if (request.getNickname() != null) user.setNickname(request.getNickname());
            if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
            userMapper.updateById(user);
        }

        try {
            LoginHistory h = new LoginHistory();
            h.setUserId(user.getId());
            h.setDevice("WeChat " + (device == null ? "" : device));
            h.setIp(ip == null ? "" : ip);
            h.setLoginTime(LocalDateTime.now());
            loginHistoryMapper.insert(h);
        } catch (Exception ignore) { }

        String token = jwtUtil.sign(user.getId(), user.getOpenid());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setOpenid(user.getOpenid());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setIsNewUser(isNew);
        return vo;
    }

    @Override
    @Transactional
    public LoginVO loginByPhone(String phone, String code, String ip, String device,
                                String nickname, String avatarUrl) {
        if (phone == null || phone.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        if (code == null || code.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码不能为空");
        }

        // 1) 验证码校验（命中即标记 used=1）
        if (!smsService.verifyCode(phone, code, "login")) {
            throw new BusinessException(ResultCode.SMS_CODE_INVALID);
        }

        // 2) 查 user；不存在则按 phone 自动注册
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("phone", phone).eq("deleted", 0).last("LIMIT 1"));
        boolean isNew = false;
        if (user == null) {
            user = new User();
            user.setOpenid("phone_" + phone);
            user.setNickname(nickname == null || nickname.isEmpty()
                    ? ("手机用户" + phone.substring(7)) : nickname);
            user.setAvatarUrl(avatarUrl == null ? "" : avatarUrl);
            user.setPhone(phone);
            userMapper.insert(user);
            isNew = true;
        } else {
            // 已存在：保留原 nickname / avatar，仅补 phone（如历史账号没填）
            if (user.getPhone() == null || user.getPhone().isEmpty()) {
                user.setPhone(phone);
            }
            if (nickname != null && !nickname.isEmpty() && (user.getNickname() == null || user.getNickname().isEmpty())) {
                user.setNickname(nickname);
            }
            if (avatarUrl != null && !avatarUrl.isEmpty() && (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty())) {
                user.setAvatarUrl(avatarUrl);
            }
            userMapper.updateById(user);
        }

        // 3) 写登录历史
        try {
            LoginHistory h = new LoginHistory();
            h.setUserId(user.getId());
            h.setDevice("Phone " + maskPhone(phone));
            h.setIp(ip == null ? "" : ip);
            h.setLoginTime(LocalDateTime.now());
            loginHistoryMapper.insert(h);
        } catch (Exception ignore) { }

        // 4) 签 token
        String token = jwtUtil.sign(user.getId(), user.getOpenid());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setOpenid(user.getOpenid());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setIsNewUser(isNew);
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    @Override
    public ProfileVO getProfile(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");

        Long groupCount = groupMemberMapper.selectCount(
                new QueryWrapper<GroupMember>().eq("user_id", userId).eq("deleted", 0));
        Long projectCount = 0L;

        ProfileVO vo = new ProfileVO();
        vo.setUserId(u.getId());
        vo.setOpenid(u.getOpenid());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarUrl());
        vo.setPhone(u.getPhone());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setGroupCount(groupCount == null ? 0 : groupCount.intValue());
        vo.setProjectCount(projectCount == null ? 0 : projectCount.intValue());
        return vo;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        if (request.getNickname() != null) u.setNickname(request.getNickname());
        if (request.getAvatarUrl() != null) u.setAvatarUrl(request.getAvatarUrl());
        // 真实姓名 / 学号 / 手机 / 邮箱 等扩展信息已迁出 user 表，
        // 此处不写库（如需保存，可走 user_profile / wx_user_ext 等扩展表）。
        userMapper.updateById(u);
    }

    @Override
    public List<LoginHistoryVO> loginHistory(Long userId, int limit) {
        List<LoginHistory> list = loginHistoryMapper.selectList(
                new QueryWrapper<LoginHistory>()
                        .eq("user_id", userId)
                        .eq("deleted", 0)
                        .orderByDesc("login_time")
                        .last(limit > 0 ? "LIMIT " + limit : "LIMIT 30"));
        List<LoginHistoryVO> result = new ArrayList<>();
        for (LoginHistory h : list) {
            LoginHistoryVO vo = new LoginHistoryVO();
            vo.setLoginTime(h.getLoginTime());
            vo.setDevice(h.getDevice());
            vo.setIp(h.getIp());
            result.add(vo);
        }
        return result;
    }

    @Override
    public ContributionStatsVO contributionStats(Long userId) {
        // T-06：真接 SQL，不再写 0
        ContributionStatsVO vo = new ContributionStatsVO();
        vo.setUploadedFiles(safeInt(userMapper.countFilesUploaded(userId)));
        vo.setApprovedFiles(safeInt(userMapper.countFilesApproved(userId)));
        vo.setCompletedTasks(safeInt(userMapper.countCompletedTasks(userId)));
        vo.setDiscussions(safeInt(userMapper.countDiscussions(userId)));
        // 加入天数：基于 user.created_at
        User u = userMapper.selectById(userId);
        long days = 1;
        if (u != null && u.getCreatedAt() != null) {
            days = java.time.temporal.ChronoUnit.DAYS.between(
                    u.getCreatedAt().toLocalDate(),
                    LocalDateTime.now().toLocalDate()) + 1;
        }
        vo.setJoinDays((int) Math.max(1, days));
        vo.setLastActiveTime(LocalDateTime.now());
        return vo;
    }

    private int safeInt(Long v) { return v == null ? 0 : v.intValue(); }

    // P1-1：用 JwtUtil 真签 refresh token
    @Override
    public Map<String, Object> refreshToken(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        String token = jwtUtil.sign(u.getId(), u.getOpenid());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresIn", 604800);
        data.put("userId", u.getId());
        data.put("openid", u.getOpenid());
        return data;
    }

    // P1-1：真绑 openid，去重
    @Override
    @Transactional
    public void bindWechat(Long userId, String openid, String unionid, String nickname, String avatarUrl) {
        if (openid == null || openid.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "openid 必填");
        }
        // 去重：如果 openid 已经绑过其它用户，先解绑
        User exist = userMapper.selectOne(
                new QueryWrapper<User>().eq("openid", openid).eq("deleted", 0).last("LIMIT 1"));
        if (exist != null && !exist.getId().equals(userId)) {
            // 把旧用户的 openid 改成占位（"unbound_" + id），避免唯一冲突
            exist.setOpenid("unbound_" + exist.getId() + "_" + System.currentTimeMillis());
            userMapper.updateById(exist);
        }
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        u.setOpenid(openid);
        if (nickname != null && !nickname.isEmpty() && (u.getNickname() == null || u.getNickname().isEmpty())) {
            u.setNickname(nickname);
        }
        if (avatarUrl != null && !avatarUrl.isEmpty() && (u.getAvatarUrl() == null || u.getAvatarUrl().isEmpty())) {
            u.setAvatarUrl(avatarUrl);
        }
        userMapper.updateById(u);
    }

    // W3：openid 绑定状态
    @Override
    public Map<String, Object> openidStatus(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        Map<String, Object> data = new LinkedHashMap<>();
        String oid = u.getOpenid();
        boolean bound = oid != null && !oid.isEmpty() && !oid.startsWith("canceled_") && !oid.startsWith("unbound_");
        data.put("bound", bound);
        // 不返回完整 openid，只返前缀 + 长度，避免泄露
        if (oid != null && oid.length() > 4) {
            data.put("openidHint", oid.substring(0, 4) + "***" + oid.substring(oid.length() - 2));
        } else {
            data.put("openidHint", "");
        }
        data.put("wechatMpEnabled", wechatMpService != null && wechatMpService.enabled());
        return data;
    }

    // P3-5：注销账号（软删 + 30 天物理删除）
    @Override
    @Transactional
    public Map<String, Object> cancelAccount(Long userId, String reason) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        if (u.getDeleted() != null && u.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号已注销");
        }
        Date now = new Date();
        Date purge = new Date(now.getTime() + 30L * 24 * 3600 * 1000);
        u.setDeleted(1);
        u.setCanceledAt(now);
        u.setPurgeAt(purge);
        if (reason != null && !reason.isEmpty()) {
            u.setCancelReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        }
        // 解绑 openid 释放唯一键
        u.setOpenid("canceled_" + u.getId() + "_" + System.currentTimeMillis());
        u.setPhone(null);
        userMapper.updateById(u);

        Map<String, Object> data = new HashMap<>();
        data.put("canceledAt", now);
        data.put("purgeAt", purge);
        data.put("retentionDays", 30);
        return data;
    }

    // P3-5：导出用户数据（按《隐私政策》第 4 条）
    @Override
    public Map<String, Object> exportUserData(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", u.getId());
        profile.put("nickname", u.getNickname());
        profile.put("avatarUrl", u.getAvatarUrl());
        profile.put("phone", u.getPhone());
        profile.put("createdAt", u.getCreatedAt());
        data.put("profile", profile);

        // 登录历史（最多 100 条）
        List<LoginHistory> histories = loginHistoryMapper.selectList(
                new QueryWrapper<LoginHistory>().eq("user_id", userId)
                        .orderByDesc("login_at").last("LIMIT 100"));
        data.put("loginHistory", histories);

        // 小组
        List<GroupMember> groups = groupMemberMapper.selectList(
                new QueryWrapper<GroupMember>().eq("user_id", userId));
        data.put("groups", groups);

        data.put("exportedAt", new Date());
        return data;
    }

    // B3 stub: UserService 接口要求 openidStatus(Long) 已实现。
    // 之前缺此方法导致主代码编译失败；W3 真实逻辑后续接入。
    @Override
    public Map<String, Object> openidStatus(Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("bound", false);
        data.put("userId", userId);
        return data;
    }
}
