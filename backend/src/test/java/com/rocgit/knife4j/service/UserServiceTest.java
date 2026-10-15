package com.rocgit.knife4j.service;

import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.dto.user.LoginRequest;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.mapper.UserMapper;
import com.rocgit.knife4j.mapper.group.GroupMemberMapper;
import com.rocgit.knife4j.mapper.user.LoginHistoryMapper;
import com.rocgit.knife4j.service.sms.SmsService;
import com.rocgit.knife4j.service.user.impl.UserServiceImpl;
import com.rocgit.knife4j.service.wechat.WechatMpService;
import com.rocgit.knife4j.util.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * T-Q-3：UserService 登录 / 验证码登录 / 刷新 token / 绑微信
 */
class UserServiceTest {

    @Test
    void login_nullRequest_shouldThrow() {
        UserServiceImpl svc = newSvc();
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.login(null, "127.0.0.1", "dev"));
        assertNotNull(ex);
    }

    @Test
    void login_blankCode_shouldThrow() {
        UserServiceImpl svc = newSvc();
        LoginRequest req = new LoginRequest();
        req.setCode("");
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.login(req, "127.0.0.1", "dev"));
        assertNotNull(ex);
    }

    @Test
    void login_newUser_shouldInsertAndReturnToken() {
        UserMapper userMapper = mock(UserMapper.class);
        GroupMemberMapper gm = mock(GroupMemberMapper.class);
        LoginHistoryMapper lh = mock(LoginHistoryMapper.class);
        JwtUtil jwt = mock(JwtUtil.class);
        SmsService sms = mock(SmsService.class);
        WechatMpService wx = mock(WechatMpService.class);
        when(wx.enabled()).thenReturn(false);

        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(123L);
            return 1;
        });
        when(jwt.sign(anyLong(), any())).thenReturn("jwt-xxx");

        UserServiceImpl svc = new UserServiceImpl(userMapper, gm, lh, jwt, sms, wx);

        LoginRequest req = new LoginRequest();
        req.setCode("wx_abc");
        req.setNickname("Alice");
        req.setAvatarUrl("https://x/avatar.png");

        var vo = svc.login(req, "127.0.0.1", "dev");
        assertNotNull(vo);
        assertEquals("jwt-xxx", vo.getToken());
        assertEquals(123L, vo.getUserId());
        assertTrue(vo.getIsNewUser());
    }

    @Test
    void login_existingUser_shouldUpdateAndReturnToken() {
        UserMapper userMapper = mock(UserMapper.class);
        GroupMemberMapper gm = mock(GroupMemberMapper.class);
        LoginHistoryMapper lh = mock(LoginHistoryMapper.class);
        JwtUtil jwt = mock(JwtUtil.class);
        SmsService sms = mock(SmsService.class);
        WechatMpService wx = mock(WechatMpService.class);
        when(wx.enabled()).thenReturn(false);

        User exist = new User();
        exist.setId(7L);
        exist.setOpenid("wx_abc");
        exist.setNickname("Alice");
        when(userMapper.selectOne(any())).thenReturn(exist);
        when(userMapper.updateById(any())).thenReturn(1);
        when(jwt.sign(anyLong(), any())).thenReturn("jwt-yyy");

        UserServiceImpl svc = new UserServiceImpl(userMapper, gm, lh, jwt, sms, wx);
        LoginRequest req = new LoginRequest();
        req.setCode("wx_abc");
        var vo = svc.login(req, "127.0.0.1", "dev");
        assertEquals("jwt-yyy", vo.getToken());
        assertFalse(vo.getIsNewUser());
    }

    @Test
    void loginByPhone_nullPhone_shouldThrow() {
        UserServiceImpl svc = newSvc();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.loginByPhone(null, "000000", "127.0.0.1", "dev", null, null));
        assertNotNull(ex);
    }

    @Test
    void loginByPhone_invalidCode_shouldThrow() {
        UserServiceImpl svc = newSvc();
        when(svcSms(svc).verifyCode(any(), any(), any())).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.loginByPhone("13800000000", "000000", "127.0.0.1", "dev", null, null));
        assertNotNull(ex);
    }

    @Test
    void refreshToken_userNotFound_shouldThrow() {
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectById(99L)).thenReturn(null);
        UserServiceImpl svc = new UserServiceImpl(userMapper,
                mock(GroupMemberMapper.class), mock(LoginHistoryMapper.class),
                mock(JwtUtil.class), mock(SmsService.class), mock(WechatMpService.class));
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.refreshToken(99L));
        assertNotNull(ex);
    }

    @Test
    void refreshToken_existing_shouldReturnNewJwt() {
        UserMapper userMapper = mock(UserMapper.class);
        User u = new User();
        u.setId(1L);
        u.setOpenid("wx_x");
        when(userMapper.selectById(1L)).thenReturn(u);
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.sign(1L, "wx_x")).thenReturn("new-jwt");

        UserServiceImpl svc = new UserServiceImpl(userMapper,
                mock(GroupMemberMapper.class), mock(LoginHistoryMapper.class),
                jwt, mock(SmsService.class), mock(WechatMpService.class));
        var data = svc.refreshToken(1L);
        assertNotNull(data);
        assertEquals("new-jwt", data.get("token"));
    }

    @Test
    void bindWechat_blankOpenid_shouldThrow() {
        UserServiceImpl svc = newSvc();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> svc.bindWechat(1L, null, null, "n", "a"));
        assertNotNull(ex);
    }

    @Test
    void bindWechat_conflict_shouldRebind() {
        UserMapper userMapper = mock(UserMapper.class);
        // 旧 user 已经占用了 openid
        User old = new User();
        old.setId(2L);
        old.setOpenid("wx_shared");
        when(userMapper.selectOne(any())).thenReturn(old);
        when(userMapper.updateById(any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenAnswer(inv -> {
            User me = new User();
            me.setId(1L);
            me.setOpenid("old_openid");
            return me;
        });

        UserServiceImpl svc = new UserServiceImpl(userMapper,
                mock(GroupMemberMapper.class), mock(LoginHistoryMapper.class),
                mock(JwtUtil.class), mock(SmsService.class), mock(WechatMpService.class));
        // 不应抛异常
        assertDoesNotThrow(() -> svc.bindWechat(1L, "wx_shared", null, "我", "https://a"));
        verify(userMapper, atLeast(2)).updateById(any());
    }

    private UserServiceImpl newSvc() {
        return new UserServiceImpl(
                mock(UserMapper.class),
                mock(GroupMemberMapper.class),
                mock(LoginHistoryMapper.class),
                mock(JwtUtil.class),
                mock(SmsService.class),
                mock(WechatMpService.class));
    }

    private SmsService svcSms(UserServiceImpl svc) {
        try {
            java.lang.reflect.Field f = UserServiceImpl.class.getDeclaredField("smsService");
            f.setAccessible(true);
            return (SmsService) f.get(svc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
