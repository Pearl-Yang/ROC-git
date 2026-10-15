package com.rocgit.knife4j.service.user;

import com.rocgit.knife4j.dto.user.LoginRequest;
import com.rocgit.knife4j.dto.user.UpdateProfileRequest;
import com.rocgit.knife4j.entity.user.ContributionStatsVO;
import com.rocgit.knife4j.entity.user.LoginHistoryVO;
import com.rocgit.knife4j.entity.user.LoginVO;
import com.rocgit.knife4j.entity.user.ProfileVO;

import java.util.List;
import java.util.Map;

/**
 * 用户 Service 接口
 *
 * <p>所有方法签名严格匹配 {@code UserController} 的调用。
 */
public interface UserService {

    LoginVO login(LoginRequest request, String ip, String device);

    /**
     * T-25：手机号+验证码登录（公开接口）
     *
     * @param phone  已校验格式的 11 位手机号
     * @param code   6 位验证码（先经过 SmsService.verifyCode 校验）
     * @param ip     客户端 IP（写 login_history）
     * @param device 设备标识（写 login_history）
     * @param nickname 可选：首次登录自动注册时使用的昵称
     * @param avatarUrl 可选：首次登录自动注册时使用的头像
     */
    LoginVO loginByPhone(String phone, String code, String ip, String device,
                         String nickname, String avatarUrl);

    ProfileVO getProfile(Long userId);

    void updateProfile(Long userId, UpdateProfileRequest request);

    List<LoginHistoryVO> loginHistory(Long userId, int limit);

    ContributionStatsVO contributionStats(Long userId);

    // P1-1：真用 JwtUtil 重签
    Map<String, Object> refreshToken(Long userId);

    // P1-1：真绑 openid，去重
    void bindWechat(Long userId, String openid, String unionid, String nickname, String avatarUrl);

    // W3：openid 绑定状态
    Map<String, Object> openidStatus(Long userId);

    // P3-5：注销账号（软删）
    Map<String, Object> cancelAccount(Long userId, String reason);

    // P3-5：导出用户数据
    Map<String, Object> exportUserData(Long userId);
}