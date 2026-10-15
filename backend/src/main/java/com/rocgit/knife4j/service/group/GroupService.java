package com.rocgit.knife4j.service.group;

import com.rocgit.knife4j.dto.group.*;
import com.rocgit.knife4j.entity.group.*;

import java.util.List;
import java.util.Map;

public interface GroupService {

    GroupVO create(Long creatorUserId, CreateGroupRequest request);

    Map<String, Object> join(Long userId, JoinGroupRequest request);

    ValidateInviteCodeVO validateInviteCode(String inviteCode);

    List<GroupVO> myList(Long userId);

    GroupVO detail(Long groupId);

    void update(Long groupId, UpdateGroupRequest request);

    void leave(Long userId, Long groupId);

    Map<String, Object> members(Long groupId, int pageNum, int pageSize);

    void updateRemark(Long groupId, Long userId, String remark);

    void transferLeader(Long groupId, TransferLeaderRequest request);

    RegenerateInviteCodeVO regenerateInviteCode(Long groupId);

    GroupStatsVO stats(Long groupId);

    List<MemberStatsVO> memberStats(Long groupId);
}