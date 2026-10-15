package com.rocgit.knife4j.mapper.discussion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.discussion.Discussion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiscussionMapper extends BaseMapper<Discussion> {
}
