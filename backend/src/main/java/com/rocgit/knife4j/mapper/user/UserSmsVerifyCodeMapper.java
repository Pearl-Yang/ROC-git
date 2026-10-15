package com.rocgit.knife4j.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.user.UserSmsVerifyCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信验证码 Mapper（T-25）
 */
@Mapper
public interface UserSmsVerifyCodeMapper extends BaseMapper<UserSmsVerifyCode> {
}
