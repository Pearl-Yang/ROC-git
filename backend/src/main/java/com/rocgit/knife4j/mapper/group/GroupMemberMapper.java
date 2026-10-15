package com.rocgit.knife4j.mapper.group;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.group.GroupMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
}
