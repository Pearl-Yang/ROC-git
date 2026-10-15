package com.rocgit.knife4j.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.message.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
