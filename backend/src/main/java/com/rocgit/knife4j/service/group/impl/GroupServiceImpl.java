package com.rocgit.knife4j.service.group.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.PageRequest;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.group.*;
import com.rocgit.knife4j.entity.Group;
import com.rocgit.knife4j.entity.User;
import com.rocgit.knife4j.entity.group.*;
import com.rocgit.knife4j.entity.invite.InviteCode;
import com.rocgit.knife4j.entity.task.Task;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.mapper.GroupMapper;
import com.rocgit.knife4j.mapper.UserMapper;
import com.rocgit.knife4j.mapper.ProjectMapper;
import com.rocgit.knife4j.mapper.group.GroupMemberMapper;
import com.rocgit.knife4j.mapper.task.TaskMapper;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.mapper.invite.InviteCodeMapper;
import com.rocgit.knife4j.service.group.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired private GroupMapper groupMapper;
    @Autowired private GroupMemberMapper groupMemberMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ProjectMapper projectMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private FileMapper fileMapper;
    @Autowired private InviteCodeMapper inviteCodeMapper;

    @Override
    @Transactional
    public GroupVO create(Long creatorUserId, CreateGroupRequest request) {
        Group g = new Group();
        g.setName(request.getName() == null ? "未命名小组" : request.getName());
        g.setDescription(request.getDescription());
        g.setLeaderId(creatorUserId);
        g.setStatus("active");
        g.setInviteCode(genInviteCode());
        groupMapper.insert(g);

        // 创建者自动作为 leader 加入
        GroupMember m = new GroupMember();
        m.setGroupId(g.getId());
        m.setUserId(creatorUserId);
        m.setRole("leader");
        m.setStatus("active");
        m.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(m);

        // 同步 invite_code
        InviteCode ic = new InviteCode();
        ic.setCode(g.getInviteCode());
        ic.setGroupId(g.getId());
        ic.setExpiresAt(LocalDateTime.now().plusYears(1));
        inviteCodeMapper.insert(ic);

        GroupVO vo = new GroupVO();
        vo.setGroupId(g.getId());
        vo.setName(g.getName());
        vo.setDescription(g.getDescription());
        vo.setLeaderId(g.getLeaderId());

        User leader = userMapper.selectById(creatorUserId);
        vo.setLeaderName(leader == null ? "" : leader.getNickname());
        vo.setInviteCode(g.getInviteCode());
        vo.setMemberCount(1);
        vo.setProjectCount(0);
        vo.setCreatedAt(g.getCreatedAt());
        return vo;
    }

    private String genInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    @Override
    @Transactional
    public Map<String, Object> join(Long userId, JoinGroupRequest request) {
        if (request.getInviteCode() == null) {
            throw new BusinessException(ResultCode.INVITE_CODE_INVALID, "邀请码不能为空");
        }
        Group group = groupMapper.selectOne(
                new QueryWrapper<Group>().eq("invite_code", request.getInviteCode()).eq("deleted", 0).last("LIMIT 1"));
        if (group == null) {
            throw new BusinessException(ResultCode.INVITE_CODE_INVALID, "邀请码无效");
        }
        // 已加入检查
        GroupMember exist = groupMemberMapper.selectOne(
                new QueryWrapper<GroupMember>()
                        .eq("group_id", group.getId())
                        .eq("user_id", userId)
                        .eq("deleted", 0)
                        .last("LIMIT 1"));
        if (exist == null) {
            GroupMember m = new GroupMember();
            m.setGroupId(group.getId());
            m.setUserId(userId);
            m.setRole("member");
            m.setStatus("active");
            m.setJoinedAt(LocalDateTime.now());
            groupMemberMapper.insert(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", group.getId());
        data.put("groupName", group.getName());
        data.put("joinedAt", System.currentTimeMillis());
        return data;
    }

    @Override
    public ValidateInviteCodeVO validateInviteCode(String inviteCode) {
        ValidateInviteCodeVO vo = new ValidateInviteCodeVO();
        if (inviteCode == null || inviteCode.isEmpty()) {
            vo.setValid(false);
            return vo;
        }
        Group g = groupMapper.selectOne(
                new QueryWrapper<Group>().eq("invite_code", inviteCode).eq("deleted", 0).last("LIMIT 1"));
        if (g == null) {
            vo.setValid(false);
            return vo;
        }
        vo.setValid(true);
        vo.setGroupName(g.getName());
        Long memberCount = groupMemberMapper.selectCount(
                new QueryWrapper<GroupMember>().eq("group_id", g.getId()).eq("deleted", 0));
        vo.setMemberCount(memberCount == null ? 0 : memberCount.intValue());
        User leader = userMapper.selectById(g.getLeaderId());
        vo.setLeaderName(leader == null ? "" : leader.getNickname());
        return vo;
    }

    @Override
    public List<GroupVO> myList(Long userId) {
        List<GroupMember> memberships = groupMemberMapper.selectList(
                new QueryWrapper<GroupMember>().eq("user_id", userId).eq("deleted", 0));
        if (memberships.isEmpty()) return Collections.emptyList();
        List<Long> gids = new ArrayList<>();
        for (GroupMember m : memberships) gids.add(m.getGroupId());
        List<Group> groups = groupMapper.selectBatchIds(gids);
        List<GroupVO> result = new ArrayList<>();
        for (Group g : groups) {
            if (g.getDeleted() != null && g.getDeleted() == 1) continue;
            GroupVO vo = toVO(g);
            result.add(vo);
        }
        return result;
    }

    @Override
    public GroupVO detail(Long groupId) {
        Group g = groupMapper.selectById(groupId);
        if (g == null || (g.getDeleted() != null && g.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.GROUP_NOT_FOUND, "小组不存在");
        }
        return toVO(g);
    }

    private GroupVO toVO(Group g) {
        GroupVO vo = new GroupVO();
        vo.setGroupId(g.getId());
        vo.setName(g.getName());
        vo.setDescription(g.getDescription());
        vo.setLeaderId(g.getLeaderId());
        vo.setInviteCode(g.getInviteCode());
        vo.setStatus(g.getStatus());
        vo.setCreatedAt(g.getCreatedAt());
        Long mc = groupMemberMapper.selectCount(
                new QueryWrapper<GroupMember>().eq("group_id", g.getId()).eq("deleted", 0));
        vo.setMemberCount(mc == null ? 0 : mc.intValue());
        Long pc = projectMapper.selectCount(
                new QueryWrapper<com.rocgit.knife4j.entity.Project>().eq("group_id", g.getId()).eq("deleted", 0));
        vo.setProjectCount(pc == null ? 0 : pc.intValue());
        User leader = userMapper.selectById(g.getLeaderId());
        vo.setLeaderName(leader == null ? "" : leader.getNickname());
        return vo;
    }

    @Override
    public void update(Long groupId, UpdateGroupRequest request) {
        Group g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(ResultCode.GROUP_NOT_FOUND.getCode());
        if (request.getName() != null) g.setName(request.getName());
        if (request.getDescription() != null) g.setDescription(request.getDescription());
        groupMapper.updateById(g);
    }

    @Override
    @Transactional
    public void leave(Long userId, Long groupId) {
        GroupMember m = groupMemberMapper.selectOne(
                new QueryWrapper<GroupMember>()
                        .eq("group_id", groupId)
                        .eq("user_id", userId)
                        .eq("deleted", 0)
                        .last("LIMIT 1"));
        if (m == null) throw new BusinessException(ResultCode.NOT_GROUP_MEMBER, "不是小组成员");
        if ("leader".equals(m.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "组长不能直接退出，请先转让");
        }
        m.setStatus("removed");
        groupMemberMapper.updateById(m);
    }

    @Override
    public Map<String, Object> members(Long groupId, int pageNum, int pageSize) {
        List<GroupMember> list = groupMemberMapper.selectList(
                new QueryWrapper<GroupMember>().eq("group_id", groupId).eq("deleted", 0));
        List<Map<String, Object>> memberList = new ArrayList<>();
        for (GroupMember m : list) {
            User u = userMapper.selectById(m.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", m.getUserId());
            item.put("nickname", u == null ? "" : u.getNickname());
            item.put("avatarUrl", u == null ? "" : u.getAvatarUrl());
            item.put("role", m.getRole());
            item.put("joinedAt", m.getJoinedAt());
            item.put("status", m.getStatus());
            memberList.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", (long) memberList.size());
        result.put("list", memberList);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public void updateRemark(Long groupId, Long userId, String remark) {
        // 简化：remark 暂不持久化到独立字段（group_member 表无 remark 列）
    }

    @Override
    @Transactional
    public void transferLeader(Long groupId, TransferLeaderRequest request) {
        Group g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(ResultCode.GROUP_NOT_FOUND.getCode());
        Long newLeader = request.getNewLeaderId();
        GroupMember oldM = groupMemberMapper.selectOne(
                new QueryWrapper<GroupMember>().eq("group_id", groupId).eq("user_id", g.getLeaderId()).last("LIMIT 1"));
        if (oldM != null) { oldM.setRole("member"); groupMemberMapper.updateById(oldM); }
        GroupMember newM = groupMemberMapper.selectOne(
                new QueryWrapper<GroupMember>().eq("group_id", groupId).eq("user_id", newLeader).last("LIMIT 1"));
        if (newM == null) {
            newM = new GroupMember();
            newM.setGroupId(groupId);
            newM.setUserId(newLeader);
            newM.setRole("leader");
            newM.setStatus("active");
            newM.setJoinedAt(LocalDateTime.now());
            groupMemberMapper.insert(newM);
        } else {
            newM.setRole("leader");
            groupMemberMapper.updateById(newM);
        }
        g.setLeaderId(newLeader);
        groupMapper.updateById(g);
    }

    @Override
    @Transactional
    public RegenerateInviteCodeVO regenerateInviteCode(Long groupId) {
        Group g = groupMapper.selectById(groupId);
        if (g == null) throw new BusinessException(ResultCode.GROUP_NOT_FOUND.getCode());
        String newCode = genInviteCode();
        g.setInviteCode(newCode);
        groupMapper.updateById(g);

        // 同步 invite_code 表
        inviteCodeMapper.delete(
                new QueryWrapper<InviteCode>().eq("group_id", groupId));
        InviteCode ic = new InviteCode();
        ic.setCode(newCode);
        ic.setGroupId(groupId);
        ic.setExpiresAt(LocalDateTime.now().plusYears(1));
        inviteCodeMapper.insert(ic);

        RegenerateInviteCodeVO vo = new RegenerateInviteCodeVO();
        vo.setInviteCode(newCode);
        vo.setExpiresAt(ic.getExpiresAt());
        return vo;
    }

    @Override
    public GroupStatsVO stats(Long groupId) {
        GroupStatsVO vo = new GroupStatsVO();
        Long mc = groupMemberMapper.selectCount(
                new QueryWrapper<GroupMember>().eq("group_id", groupId).eq("deleted", 0));
        vo.setMemberCount(mc == null ? 0 : mc.intValue());
        Long pc = projectMapper.selectCount(
                new QueryWrapper<com.rocgit.knife4j.entity.Project>().eq("group_id", groupId).eq("deleted", 0));
        vo.setProjectCount(pc == null ? 0 : pc.intValue());
        Long apc = projectMapper.selectCount(
                new QueryWrapper<com.rocgit.knife4j.entity.Project>().eq("group_id", groupId).eq("deleted", 0).eq("status", "active"));
        vo.setActiveProjectCount(apc == null ? 0 : apc.intValue());
        Long cpc = projectMapper.selectCount(
                new QueryWrapper<com.rocgit.knife4j.entity.Project>().eq("group_id", groupId).eq("deleted", 0).eq("status", "completed"));
        vo.setCompletedProjectCount(cpc == null ? 0 : cpc.intValue());
        // 文件/任务统计（跨项目汇总，简化处理）
        vo.setTotalFileCount(0);
        vo.setTotalTaskCount(0);
        return vo;
    }

    @Override
    public List<MemberStatsVO> memberStats(Long groupId) {
        // T-06：真接 SQL，按已 approved 的 file 数降序
        List<GroupMember> members = groupMemberMapper.selectList(
                new QueryWrapper<GroupMember>().eq("group_id", groupId).eq("deleted", 0));
        List<MemberStatsVO> result = new ArrayList<>();
        // 先查所有 projectId
        List<com.rocgit.knife4j.entity.Project> projects = projectMapper.selectList(
                new QueryWrapper<com.rocgit.knife4j.entity.Project>().eq("group_id", groupId).eq("deleted", 0));
        List<Long> projectIds = new ArrayList<>();
        for (com.rocgit.knife4j.entity.Project p : projects) projectIds.add(p.getId());

        for (GroupMember m : members) {
            User u = userMapper.selectById(m.getUserId());
            MemberStatsVO vo = new MemberStatsVO();
            vo.setUserId(m.getUserId());
            vo.setNickname(u == null ? "" : u.getNickname());

            // 在本组项目里的上传文件数 + 已审核通过文件数 + 完成 task 数
            long fileCount = 0L, approvedCount = 0L, taskCount = 0L;
            if (!projectIds.isEmpty()) {
                fileCount = fileMapper.selectCount(new QueryWrapper<FileEntity>()
                        .in("project_id", projectIds).eq("uploader_id", m.getUserId()).eq("deleted", 0));
                approvedCount = fileMapper.selectCount(new QueryWrapper<FileEntity>()
                        .in("project_id", projectIds).eq("uploader_id", m.getUserId())
                        .eq("status", "approved").eq("deleted", 0));
                taskCount = taskMapper.selectCount(new QueryWrapper<Task>()
                        .in("project_id", projectIds).eq("assignee_id", m.getUserId())
                        .eq("status", "completed").eq("deleted", 0));
            }
            vo.setFileCount((int) Math.min(Integer.MAX_VALUE, fileCount));
            vo.setTaskCount((int) Math.min(Integer.MAX_VALUE, taskCount));
            // 贡献分：审核通过 3 分 + 上传 1 分 + 完成 task 2 分
            int contrib = (int) (approvedCount * 3L + fileCount + taskCount * 2L);
            vo.setContributions(contrib);
            result.add(vo);
        }
        // 按贡献分降序，rank 重置
        result.sort((a, b) -> Integer.compare(
                b.getContributions() == null ? 0 : b.getContributions(),
                a.getContributions() == null ? 0 : a.getContributions()));
        for (int i = 0; i < result.size(); i++) result.get(i).setRank(i + 1);
        return result;
    }
}