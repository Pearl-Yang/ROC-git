package com.rocgit.knife4j.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.user.LoginHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginHistoryMapper extends BaseMapper<LoginHistory> {
}
