<template>
  <view class="page">
    <!-- 顶部 -->
    <view class="topbar">
      <view class="icon-btn" @tap="goBack">
        <Icon name="lucide:chevron-left" :width="20" :height="20" color="#0F172A" />
      </view>
      <text class="topbar-title">小组详情</text>
      <view class="icon-btn accent" @tap="onCreateProject">
        <Icon name="lucide:plus" :width="18" :height="18" color="#2563EB" />
      </view>
    </view>

    <view v-if="!group.id && !group.groupId" class="state">加载中…</view>

    <view v-else>
      <!-- 渐变 banner -->
      <view class="banner">
        <view class="banner-row">
          <view class="banner-avatar" :style="{ background: avatarColor(group.name) }">
            <text>{{ firstChar(group.name) }}</text>
          </view>
          <view class="banner-info">
            <text class="banner-name">{{ group.name || '未命名小组' }}</text>
            <text class="banner-desc">{{ group.description || '小组简介待补充' }}</text>
            <view class="banner-meta">
              <view class="meta-chip">
                <Icon name="lucide:users" :width="12" :height="12" color="#475569" />
                <text>{{ members.length }} 人</text>
              </view>
              <view class="meta-chip">
                <Icon name="lucide:folder" :width="12" :height="12" color="#475569" />
                <text>{{ projects.length }} 项目</text>
              </view>
              <view class="meta-chip">
                <Icon name="lucide:calendar" :width="12" :height="12" color="#475569" />
                <text>{{ formatDate(group.createdAt) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 项目 -->
      <view class="section">
        <view class="section-head">
          <text class="section-title">小组项目</text>
          <text class="section-count">{{ projects.length }} 个</text>
        </view>
        <view v-if="!projects.length" class="empty-card">
          <view class="empty-art">
            <Icon name="lucide:folder-plus" :width="28" :height="28" color="#94A3B8" />
          </view>
          <text class="empty-text">暂无项目，点右上角创建</text>
        </view>
        <view v-for="p in projects" :key="p.id || p.projectId" class="project-card" @tap="openProject(p)">
          <view class="project-row">
            <view class="project-icon" :style="{ background: colorOf(p.name) }">
              <text>{{ firstChar(p.name) }}</text>
            </view>
            <view class="project-body">
              <text class="project-name">{{ p.name }}</text>
              <text class="project-meta">{{ p.status === 'archived' ? '已归档' : (p.status || '进行中') }} · 进度 {{ p.progress || 0 }}%</text>
              <view class="progress-bar">
                <view class="progress-fill" :style="{ width: (p.progress || 0) + '%' }"></view>
              </view>
            </view>
            <Icon name="lucide:chevron-right" :width="16" :height="16" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <!-- 成员 -->
      <view class="section">
        <view class="section-head">
          <text class="section-title">小组成员</text>
          <text class="section-count">{{ members.length }} 人</text>
        </view>
        <view v-if="!members.length" class="empty-card">
          <text class="empty-text">暂无成员</text>
        </view>
        <view class="card">
          <view v-for="(m, idx) in members" :key="m.userId || m.id" class="member-row">
            <view class="member-avatar" :style="{ background: avatarColor(m.nickname || m.name) }">
              <text>{{ firstChar(m.nickname || m.name) }}</text>
            </view>
            <view class="member-info">
              <text class="member-name">{{ m.nickname || m.name }}</text>
              <text class="member-role">{{ m.role === 'leader' ? '组长' : '组员' }}</text>
            </view>
            <view v-if="m.role === 'leader'" class="role-badge leader">
              <Icon name="lucide:crown" :width="12" :height="12" color="#D97706" />
              <text>组长</text>
            </view>
            <view v-else class="role-badge">
              <text>组员</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '@/utils/api.js'
import Icon from '@/components/Icon.vue'
import { textAvatar } from '@/utils/avatar.js'

const group = ref({})
const members = ref([])
const projects = ref([])

const palette = ['#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#06B6D4', '#EF4444', '#6366F1']

function firstChar(name) {
  if (!name) return 'G'
  return String(name).slice(0, 1)
}
function avatarColor(name) {
  return textAvatar(name).color
}
function colorOf(name) {
  const idx = Math.abs(hashCode(name || '')) % palette.length
  return palette[idx]
}
function hashCode(s) {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return h
}

onLoad((options) => {
  if (options && options.groupId) {
    loadGroup(options.groupId)
    loadMembers(options.groupId)
    loadProjects(options.groupId)
  }
})

async function loadGroup(gid) {
  try {
    const r = await api.groupDetail(gid)
    group.value = r || {}
  } catch (e) { group.value = {} }
}

async function loadMembers(gid) {
  try {
    const r = await api.groupMembers(gid)
    members.value = Array.isArray(r) ? r : (r && r.list) || []
  } catch (e) { members.value = [] }
}

async function loadProjects(gid) {
  try {
    const r = await api.groupProjects(gid)
    projects.value = Array.isArray(r) ? r : (r && r.list) || []
  } catch (e) { projects.value = [] }
}

function openProject(p) {
  const id = p.id || p.projectId
  if (!id) return
  uni.navigateTo({ url: '/pages/project/project?projectId=' + id })
}

function onCreateProject() {
  const gid = group.value.id || group.value.groupId
  if (!gid) return
  uni.navigateTo({ url: '/pages/create/create?groupId=' + gid })
}

function formatDate(t) { if (!t) return '—'; return String(t).slice(0, 10) }

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/home/home' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F8FAFC;
  padding-bottom: env(safe-area-inset-bottom);
}

/* 顶部 */
.topbar {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  gap: 12px;
}
.topbar-title {
  flex: 1;
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
}
.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #F1F5F9;
}
.icon-btn.accent {
  background: #DBEAFE;
}

.state {
  text-align: center;
  padding: 60px 16px;
  color: #94A3B8;
  font-size: 13px;
}

/* banner */
.banner {
  padding: 16px;
  background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
  color: #FFFFFF;
}
.banner-row {
  display: flex;
  gap: 14px;
  align-items: center;
}
.banner-avatar {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  color: #FFFFFF;
  background: rgba(255, 255, 255, 0.25);
  border: 2px solid rgba(255, 255, 255, 0.30);
  flex-shrink: 0;
}
.banner-info { flex: 1; min-width: 0; }
.banner-name {
  font-size: 18px;
  font-weight: 800;
  display: block;
  color: #FFFFFF;
}
.banner-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  display: block;
  margin-top: 4px;
}
.banner-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.meta-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #475569;
  background: rgba(255, 255, 255, 0.95);
  padding: 3px 8px;
  border-radius: 9999px;
  font-weight: 600;
}

/* section */
.section { padding: 16px; }
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}
.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}
.section-count {
  font-size: 11px;
  color: #94A3B8;
}
.empty-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 32px 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.empty-art {
  width: 56px;
  height: 56px;
  background: #F1F5F9;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-text { color: #94A3B8; font-size: 13px; }

/* project */
.project-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  padding: 12px 14px;
  margin-bottom: 8px;
}
.project-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.project-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  color: #FFFFFF;
  flex-shrink: 0;
}
.project-body { flex: 1; min-width: 0; }
.project-name {
  font-size: 14px;
  color: #0F172A;
  font-weight: 700;
  display: block;
}
.project-meta {
  font-size: 11px;
  color: #94A3B8;
  display: block;
  margin-top: 2px;
}
.progress-bar {
  height: 4px;
  background: #F1F5F9;
  border-radius: 4px;
  margin-top: 6px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3B82F6, #6366F1);
  border-radius: 4px;
}

/* 成员 */
.card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  overflow: hidden;
}
.member-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #F1F5F9;
}
.member-row:last-child { border-bottom: none; }
.member-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #FFFFFF;
  flex-shrink: 0;
}
.member-info { flex: 1; min-width: 0; }
.member-name {
  display: block;
  font-size: 14px;
  color: #0F172A;
  font-weight: 600;
}
.member-role {
  display: block;
  font-size: 11px;
  color: #94A3B8;
  margin-top: 2px;
}
.role-badge {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  background: #F1F5F9;
  border-radius: 9999px;
  font-size: 11px;
  color: #475569;
  font-weight: 600;
}
.role-badge.leader {
  background: linear-gradient(135deg, #FEF3C7, #FDE68A);
  color: #D97706;
}
</style>
