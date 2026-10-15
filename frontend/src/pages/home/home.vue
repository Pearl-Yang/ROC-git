<template>
  <view class="page home-page">
    <!-- Top bar -->
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="topbar-user" @tap="goProfile">
          <view class="topbar-avatar" :style="{ background: avatarColor }">
            <text class="topbar-avatar-text">{{ avatarChar }}</text>
          </view>
          <view class="topbar-greet">
            <text class="topbar-sub">{{ todayText }}</text>
            <text class="topbar-title">早安~</text>
          </view>
        </view>
        <view class="topbar-actions">
          <view class="icon-btn" @tap="goMessages">
            <RocIcon name="lucide:bell" :size="18" color="#475569" />
            <view v-if="unreadTotal > 0" class="badge-num">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</view>
          </view>
          <view class="btn-fab" @tap="goCreate">
            <RocIcon name="lucide:plus" :size="20" color="#FFFFFF" />
          </view>
        </view>
      </view>
    </view>

    <scroll-view
      scroll-y class="content"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      :style="{ paddingTop: (safeTop + 56) + 'px' }"
    >
      <!-- Welcome banner -->
      <view class="banner-wrap">
        <view class="banner-dark">
          <view class="banner-ring"></view>
          <view class="banner-content">
            <view class="banner-head">
              <RocIcon name="lucide:zap" :size="14" color="#FBBF24" />
              <text class="banner-eyebrow">本周要点</text>
            </view>
            <text class="banner-title">{{ activeProjectsCount }} 个项目进行中</text>
            <text class="banner-tip">{{ bannerTip }}</text>
            <view class="banner-cta-row">
              <view class="banner-cta-light" @tap="goProject">
                <text class="banner-cta-text-dark">查看项目</text>
              </view>
              <view class="banner-cta-ghost" @tap="goFiles">
                <text class="banner-cta-text-light">上传资料</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- Stats overview -->
      <view class="banner-wrap">
        <view class="stat-card">
          <view class="stat-cell" @tap="goProject">
            <text class="stat-num stat-num-dark">{{ activeProjectsCount }}</text>
            <text class="stat-lbl">进行中</text>
          </view>
          <view class="divider-y"></view>
          <view class="stat-cell" @tap="goMessages">
            <text class="stat-num stat-num-amber">{{ pendingCount }}</text>
            <text class="stat-lbl">待审核</text>
          </view>
          <view class="divider-y"></view>
          <view class="stat-cell">
            <text class="stat-num stat-num-rose">{{ dueCount }}</text>
            <text class="stat-lbl">临期</text>
          </view>
          <view class="divider-y"></view>
          <view class="stat-cell" @tap="goFiles">
            <text class="stat-num stat-num-emerald">{{ fileCount }}</text>
            <text class="stat-lbl">已入库</text>
          </view>
        </view>
      </view>

      <!-- Quick actions -->
      <view class="quick-section">
        <text class="section-title">快捷功能</text>
        <view class="quick-grid">
          <view class="quick-cell" @tap="onUpload">
            <view class="quick-icon quick-icon-blue">
              <RocIcon name="lucide:upload-cloud" :size="20" color="#2563EB" />
            </view>
            <text class="quick-label">上传</text>
          </view>
          <view class="quick-cell" @tap="goFiles">
            <view class="quick-icon quick-icon-violet">
              <RocIcon name="lucide:file-search" :size="20" color="#7C3AED" />
            </view>
            <text class="quick-label">检索</text>
          </view>
          <view class="quick-cell" @tap="goMeeting">
            <view class="quick-icon quick-icon-emerald">
              <RocIcon name="lucide:users" :size="20" color="#059669" />
            </view>
            <text class="quick-label">研讨</text>
          </view>
          <view class="quick-cell" @tap="goCreate">
            <view class="quick-icon quick-icon-amber">
              <RocIcon name="lucide:plus-circle" :size="20" color="#D97706" />
            </view>
            <text class="quick-label">建项目</text>
          </view>
        </view>
      </view>

      <!-- Current projects -->
      <view class="section-head">
        <text class="section-title">当前项目</text>
        <text class="more" @tap="goProject">查看全部</text>
      </view>
      <scroll-view scroll-x class="h-scroll">
        <view class="proj-scroll-row">
          <view
            v-for="(p, idx) in projects"
            :key="p.id || idx"
            class="proj-card"
            :style="`border-top: 3px solid ${projectColor(idx)};`"
            @tap="openProject(p)"
          >
            <text class="proj-stage" :style="`color: ${projectColor(idx)};`">第 {{ p.currentStageNo || 1 }} 阶段 · 还剩 {{ p.daysLeft || 0 }} 天</text>
            <text class="proj-name">{{ p.name }}</text>
            <text class="proj-course">{{ p.course || '软件工程实践' }}</text>
            <view class="proj-progress-row">
              <text class="proj-progress-lbl">进度</text>
              <text class="proj-progress-val">{{ p.progress || 0 }}%</text>
            </view>
            <view class="progress progress-thin">
              <view class="progress-fill" :style="{ width: (p.progress || 0) + '%' }"></view>
            </view>
            <view class="proj-foot">
              <view class="avatar-stack">
                <view v-for="(m, mi) in (p.members || []).slice(0, 3)" :key="mi" class="avatar avatar-sm stack-item" :style="{ background: avatarColorFor(m) }">{{ avatarCharOf(m) }}</view>
                <view v-if="(p.members || []).length > 3" class="avatar avatar-sm stack-more">+{{ (p.members || []).length - 3 }}</view>
              </view>
              <text class="proj-files">{{ p.fileCount || 0 }}份资料</text>
            </view>
          </view>
          <view v-if="!projects.length" class="empty" style="width: 270px;">
            <text class="empty-emoji">📁</text>
            <text class="empty-text">还没有项目</text>
          </view>
        </view>
      </scroll-view>
      <view v-if="projects.length > 1" class="dot-row">
        <view
          v-for="(_, idx) in projects"
          :key="idx"
          :class="['dot', idx === 0 ? 'dot-on' : '']"
        ></view>
      </view>

      <!-- Discussions -->
      <view class="section-head">
        <text class="section-title">研讨记录</text>
        <text class="more" @tap="goMeeting">查看全部</text>
      </view>
      <view class="block-pad">
        <view
          v-for="(d, idx) in discussions"
          :key="d.id || idx"
          class="list-card"
          @tap="goMeeting"
        >
          <view :class="['row-icon', discussionIconBg(d.mode)]">
            <RocIcon :name="discussionIcon(d.mode)" :size="18" :color="discussionIconColor(d.mode)" />
          </view>
          <view class="row-body">
            <text class="row-title">第 {{ d.no || (idx + 1) }} 次 · {{ d.mode || '线上研讨' }}</text>
            <text class="row-meta">{{ d.topic || '访谈提纲修订' }} · {{ d.attendeeCount || 3 }} 人 · {{ d.date || '9月08日' }}</text>
          </view>
          <view class="row-action" style="display:flex; flex-direction:column; align-items:flex-end; gap:2px;">
            <text :class="['fs-11 fw-600', d.pendingCount ? 'text-amber' : 'text-emerald']">
              {{ d.pendingCount ? d.pendingCount + ' 待落地' : '已全部落地' }}
            </text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
        <view v-if="!discussions.length" class="empty">
          <text class="empty-text">暂无研讨记录</text>
        </view>
      </view>

      <!-- Due tasks -->
      <view class="section-head">
        <text class="section-title">临期任务</text>
        <text class="more text-rose">{{ dueTasks.length }} 项</text>
      </view>
      <view class="block-pad">
        <view
          v-for="(tk, idx) in dueTasks"
          :key="tk.id || idx"
          class="list-card"
          @tap="openTask(tk)"
        >
          <view :class="['row-icon', tk.daysLeft <= 2 ? 'icon-bg-rose' : 'icon-bg-amber']">
            <RocIcon :name="tk.daysLeft <= 2 ? 'lucide:file-check' : 'lucide:clock'" :size="18" :color="tk.daysLeft <= 2 ? '#F43F5E' : '#F59E0B'" />
          </view>
          <view class="row-body">
            <text class="row-title">{{ tk.name }}</text>
            <text class="row-meta">{{ tk.projectName || '社区调研报告' }} · 还剩 {{ tk.daysLeft }} 天</text>
          </view>
          <view :class="['status-pill', tk.daysLeft <= 2 ? 'status-pill-rose' : 'status-pill-amber']">
            <text>{{ tk.daysLeft }}天</text>
          </view>
        </view>
        <view v-if="!dueTasks.length" class="empty">
          <text class="empty-text">无临期任务 🎉</text>
        </view>
      </view>

      <!-- Pending audit -->
      <view class="section-head">
        <text class="section-title">待审核</text>
        <text class="more" @tap="goMessages">全部</text>
      </view>
      <view class="block-pad">
        <view
          v-for="(f, idx) in pendingFiles"
          :key="f.id || idx"
          class="list-card"
        >
          <view class="row-icon icon-bg-sky">
            <RocIcon name="lucide:file-up" :size="18" color="#0EA5E9" />
          </view>
          <view class="row-body">
            <text class="row-title">{{ f.uploader || '许萌' }} · {{ f.name }}</text>
            <text class="row-meta">{{ f.time || '10:18' }}</text>
          </view>
          <view class="btn btn-ghost" style="height:28px; line-height:28px; padding:0 10px; font-size:11px;" @tap="approveFile(f)">
            <text>通过</text>
          </view>
        </view>
        <view v-if="!pendingFiles.length" class="empty">
          <text class="empty-text">无待审核文件</text>
        </view>
      </view>

      <!-- Recent files -->
      <view class="section-head">
        <view class="row" style="padding:0; gap:6px;">
          <text class="section-title">最近文件</text>
          <text class="fs-11 text-faint">{{ recentFiles.length }}个</text>
        </view>
        <text class="more" @tap="goFiles">查看全部</text>
      </view>
      <view class="block-pad">
        <view
          v-for="(f, idx) in recentFiles"
          :key="f.id || idx"
          class="list-card"
          @tap="openFile(f)"
        >
          <view :class="['row-icon', fileIconBg(f.ext)]">
            <RocIcon :name="fileIconName(f.ext)" :size="18" :color="fileIconColor(f.ext)" />
          </view>
          <view class="row-body">
            <text class="row-title">{{ f.name }}</text>
            <text class="row-meta">{{ f.time }}<text class="text-faint"> · </text>{{ f.uploader }}</text>
          </view>
          <view :class="['type-chip', fileTypeChip(f.ext)]">
            <text>{{ (f.ext || 'FILE').toUpperCase() }}</text>
          </view>
          <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
        </view>
        <view v-if="!recentFiles.length" class="empty">
          <text class="empty-text">暂无文件</text>
        </view>
      </view>

      <!-- Team activity -->
      <view class="section-head">
        <view class="row" style="padding:0; gap:6px;">
          <text class="section-title">团队动态</text>
          <view v-if="activityFeed.length" class="status-pill status-pill-rose"><text>3条新</text></view>
        </view>
        <text class="more">查看全部</text>
      </view>
      <view class="block-pad">
        <view class="card card-padded">
          <view class="timeline">
            <view class="timeline-line"></view>
            <view
              v-for="(a, idx) in activityFeed"
              :key="a.id || idx"
              class="timeline-item"
            >
              <view class="timeline-dot" :style="{ background: a.color || '#0EA5E9' }">
                <text class="timeline-dot-text">{{ avatarCharOf(a.user) }}</text>
              </view>
              <view class="timeline-body">
                <text class="timeline-text">
                  <text class="fw-700">{{ a.user }}</text> {{ a.verb }} <text :class="a.emphasisClass">{{ a.target }}</text>
                </text>
                <view class="row" style="padding:0; gap:4px;">
                  <RocIcon name="lucide:clock" :size="11" color="#94A3B8" />
                  <text class="fs-11 text-faint">{{ a.time }}</text>
                  <text v-if="a.detail" class="fs-11 text-faint">· {{ a.detail }}</text>
                </view>
              </view>
            </view>
            <view v-if="!activityFeed.length" class="empty">
              <text class="empty-text">暂无动态</text>
            </view>
          </view>
        </view>
      </view>

      <view class="safe-bottom" style="height: 24px;"></view>
    </scroll-view>

    <!-- Toast -->
    <view v-if="toast.show" class="toast">
      <text class="toast-text">{{ toast.text }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { useUserStore } from '../../utils/user-store.js'
import { useI18n } from '../../utils/i18n.js'
import { textAvatar } from '../../utils/avatar.js'
import RocIcon from '../../components/Icon.vue'

const { t } = useI18n()
const store = useUserStore()

const safeTop = ref(20)
const refreshing = ref(false)

const groups = ref([])
const activeGroupId = ref(null)
const projects = ref([])
const discussions = ref([])
const dueTasks = ref([])
const pendingFiles = ref([])
const recentFiles = ref([])
const activityFeed = ref([])
const unreadTotal = ref(0)
const pendingCount = ref(0)
const fileCount = ref(0)

const toast = ref({ show: false, text: '' })

const user = computed(() => store.user.value || {})
const nickname = computed(() => user.value.nickname || '林知行')
const avatarChar = computed(() => textAvatar(nickname.value).char)
const avatarColor = computed(() => textAvatar(nickname.value).color)

const todayText = computed(() => {
  const d = new Date()
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${days[d.getDay()]}，${d.getMonth() + 1}月${d.getDate()}日`
})
const activeProjectsCount = computed(() => projects.value.length)
const dueCount = computed(() => dueTasks.value.length)
const bannerTip = computed(() => {
  if (dueTasks.value.length) return `资料归集还剩 ${dueTasks.value[0].daysLeft || 4} 天，记得及时推进`
  if (projects.value.length) return `${projects.value.length} 个项目持续推进中`
  return '从创建项目开始你的协作之旅'
})

onLoad(() => { refreshSafeTop() })
onShow(() => { loadAll() })

function refreshSafeTop() {
  try {
    const info = uni.getSystemInfoSync()
    safeTop.value = info.statusBarHeight || 20
  } catch (e) { /* ignore */ }
}

async function loadAll() {
  await Promise.all([
    loadGroups(),
    loadUnread()
  ])
  await Promise.all([
    loadProjects(),
    loadDiscussions(),
    loadDueTasks(),
    loadRecentFiles(),
    loadActivity()
  ])
}

async function loadGroups() {
  try {
    const list = await api.myGroups()
    groups.value = list || []
    if (groups.value.length) {
      activeGroupId.value = groups.value[0].id || groups.value[0].groupId
    }
  } catch (e) { groups.value = [] }
}

async function loadUnread() {
  try {
    const unread = await api.unreadCount()
    if (unread && typeof unread.total === 'number') {
      unreadTotal.value = unread.total
      pendingCount.value = unread.audit || 0
    }
  } catch (e) { /* keep demo */ }
}

async function loadProjects() {
  if (!activeGroupId.value) {
    projects.value = demoProjects()
    return
  }
  try {
    const list = await api.projectList(activeGroupId.value)
    if (list && list.length) {
      // load each project to enrich members
      const enriched = await Promise.all(list.map(async (p, idx) => {
        let members = []
        try {
          if (p.groupId) {
            const m = await api.groupMembers(p.groupId, { pageSize: 100 })
            const memberList = (m && (m.list || m.records || m.members)) || []
            members = memberList.map(x => x.nickname)
          }
        } catch (e) { /* ignore */ }
        return {
          ...p,
          members,
          currentStageNo: p.currentStageNo || 2,
          daysLeft: p.daysLeft || (4 - idx),
          fileCount: p.fileCount || (12 - idx * 3)
        }
      }))
      projects.value = enriched
    } else {
      projects.value = demoProjects()
    }
  } catch (e) {
    projects.value = demoProjects()
  }
}

function demoProjects() {
  return [
    {
      id: 1, name: '社区调研报告', course: '软件工程实践',
      progress: 68, currentStageNo: 2, daysLeft: 4, fileCount: 12,
      members: ['林知行', '许萌', '王宁', '陈安']
    },
    {
      id: 2, name: '统计学小组作业', course: '统计学原理',
      progress: 22, currentStageNo: 1, daysLeft: 8, fileCount: 5,
      members: ['陈安', '刘欣']
    },
    {
      id: 3, name: '创新创业项目', course: '创业基础',
      progress: 45, currentStageNo: 1, daysLeft: 12, fileCount: 8,
      members: ['李想', '赵雨', '周明']
    }
  ]
}

async function loadDiscussions() {
  try {
    const list = await api.discussionList(activeGroupId.value || 1, { pageSize: 5 })
    if (list && list.length) {
      discussions.value = list.map((d, i) => ({
        ...d,
        no: list.length - i,
        mode: d.mode || (i % 2 ? '线上研讨' : '线下研讨'),
        topic: d.topic || d.title,
        attendeeCount: d.attendeeCount || 3,
        pendingCount: d.pendingCount || 0,
        date: d.date || '9月08日'
      }))
    } else {
      discussions.value = demoDiscussions()
    }
  } catch (e) {
    discussions.value = demoDiscussions()
  }
}
function demoDiscussions() {
  return [
    { id: 1, no: 3, mode: '线上研讨', topic: '访谈提纲修订', attendeeCount: 3, date: '9月08日', pendingCount: 2 },
    { id: 2, no: 2, mode: '线下研讨', topic: '图书馆研讨室', attendeeCount: 3, date: '9月04日', pendingCount: 0 }
  ]
}

async function loadDueTasks() {
  try {
    const list = await api.dueSoonTasks(7)
    if (list && list.length) {
      dueTasks.value = list.map(t => ({ ...t, daysLeft: t.daysLeft || t.remainingDays || 4 }))
    } else {
      dueTasks.value = demoDue()
    }
  } catch (e) {
    dueTasks.value = demoDue()
  }
}
function demoDue() {
  return [
    { id: 1, name: '资料归集 · 访谈整理', projectName: '社区调研报告', daysLeft: 4 },
    { id: 2, name: '成果审核 · 调研大纲 v4', projectName: '统计学小组', daysLeft: 2 }
  ]
}

async function loadRecentFiles() {
  // pendingFiles: 从 messageList 过滤类型为 audit_pending
  // recentFiles: 从每个项目的 fileList 各取最新 1 个，合并排序
  try {
    const msgs = await api.messageList({ pageSize: 20 }).catch(() => null)
    const list = (msgs && (msgs.records || msgs.list || msgs.items || msgs)) || []
    if (list.length) {
      pendingFiles.value = list
        .filter(m => m.type === 'audit_pending' || m.bizType === 'audit_pending' || m.category === 'audit')
        .slice(0, 5)
        .map(m => ({
          id: m.id || m.messageId,
          name: m.title || m.content || '待审核文件',
          uploader: (m.sender && m.sender.nickname) || m.senderNickname || '成员',
          time: m.time || (m.createdAt ? new Date(m.createdAt).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '')
        }))
      if (!pendingFiles.value.length) pendingFiles.value = demoPending()
    } else {
      pendingFiles.value = demoPending()
    }
  } catch (e) {
    pendingFiles.value = demoPending()
  }

  // recent files: 遍历 active group 的项目，每个项目拉 fileList 取最新 1
  try {
    const allFiles = []
    if (activeGroupId.value) {
      const projects = await api.projectList(activeGroupId.value).catch(() => [])
      for (const p of (projects || []).slice(0, 5)) {
        try {
          const res = await api.request({ url: '/v1/file/list', params: { projectId: p.projectId || p.id, pageSize: 5 } })
          const rows = (res && (res.records || res.list || res.items)) || []
          rows.forEach(vo => {
            const u = vo.uploader || {}
            allFiles.push({
              id: vo.fileId,
              name: vo.name,
              ext: (vo.type || '').toLowerCase() || (vo.name && vo.name.split('.').pop().toLowerCase()) || 'file',
              uploader: u.nickname || '',
              time: vo.uploadedAt ? formatTimeShort(vo.uploadedAt) : ''
            })
          })
        } catch (e) { /* ignore per project */ }
      }
    }
    if (allFiles.length) {
      allFiles.sort((a, b) => (b.time || '').localeCompare(a.time || ''))
      recentFiles.value = allFiles.slice(0, 5)
      fileCount.value = allFiles.length
    } else {
      recentFiles.value = demoRecent()
      fileCount.value = recentFiles.value.length + 12
    }
  } catch (e) {
    recentFiles.value = demoRecent()
    fileCount.value = recentFiles.value.length + 12
  }
}

function formatTimeShort(s) {
  if (!s) return ''
  try {
    const d = new Date(s)
    if (isNaN(d.getTime())) return s
    const now = new Date()
    const sameDay = d.toDateString() === now.toDateString()
    if (sameDay) return '今天 ' + (d.getHours() < 10 ? '0' + d.getHours() : d.getHours()) + ':' + (d.getMinutes() < 10 ? '0' + d.getMinutes() : d.getMinutes())
    const yesterday = new Date(now); yesterday.setDate(now.getDate() - 1)
    if (d.toDateString() === yesterday.toDateString()) return '昨天 ' + (d.getHours() < 10 ? '0' + d.getHours() : d.getHours()) + ':' + (d.getMinutes() < 10 ? '0' + d.getMinutes() : d.getMinutes())
    return (d.getMonth() + 1) + '月' + (d.getDate() < 10 ? '0' + d.getDate() : d.getDate()) + '日'
  } catch (e) { return s }
}
function demoPending() {
  return [
    { id: 1, name: '访谈记录 v2', uploader: '许萌', time: '10:18' }
  ]
}
function demoRecent() {
  return [
    { id: 1, name: '社区访谈整理.docx', ext: 'docx', uploader: '许萌', time: '今天 10:18' },
    { id: 2, name: '调研报告大纲.docx', ext: 'docx', uploader: '王宁', time: '昨天 20:36' },
    { id: 3, name: '访谈数据统计.xlsx', ext: 'xlsx', uploader: '林知行', time: '9月05日' }
  ]
}

async function loadActivity() {
  try {
    // 拉所有项目的活动，合并取最近 6 条
    if (!activeGroupId.value) {
      activityFeed.value = demoActivity()
      return
    }
    const projects = await api.projectList(activeGroupId.value).catch(() => [])
    const allActs = []
    for (const p of (projects || []).slice(0, 4)) {
      try {
        const res = await api.projectActivities(p.projectId || p.id, { pageSize: 6 })
        const rows = (res && (res.records || res.list || res.items || res)) || []
        rows.forEach(a => {
          const u = a.user || a.actor || a.userInfo || {}
          allActs.push({
            id: a.id || a.activityId,
            user: u.nickname || a.userNickname || a.user || '成员',
            verb: a.action || a.title || a.description || '操作',
            target: a.target || a.objectName || '',
            time: a.time || (a.createdAt ? formatTimeShort(a.createdAt) : ''),
            detail: p.name || a.projectName || '',
            color: a.color || '#0EA5E9',
            emphasisClass: a.emphasisClass || 'text-blue'
          })
        })
      } catch (e) { /* ignore per project */ }
    }
    if (allActs.length) {
      allActs.sort((a, b) => (b.time || '').localeCompare(a.time || ''))
      activityFeed.value = allActs.slice(0, 6)
    } else {
      activityFeed.value = demoActivity()
    }
  } catch (e) {
    activityFeed.value = demoActivity()
  }
}
function demoActivity() {
  return [
    { id: 1, user: '许萌', verb: '上传了', target: '访谈录音转写稿.docx', time: '10:18', detail: '社区调研报告', color: '#0EA5E9', emphasisClass: 'text-blue' },
    { id: 2, user: '王宁', verb: '更新了成果大纲', target: 'v3→v4', time: '昨天 20:36', detail: '新增第 4 章', color: '#F43F5E', emphasisClass: 'text-rose' },
    { id: 3, user: '林知行', verb: '确认', target: '「选题与分工」完成', time: '9月03日', detail: '阶段归档', color: '#F59E0B', emphasisClass: 'text-emerald' }
  ]
}

async function onRefresh() {
  refreshing.value = true
  await loadAll()
  refreshing.value = false
}

function projectColor(i) {
  const palette = ['#2563EB', '#7C3AED', '#10B981', '#F59E0B', '#F43F5E']
  return palette[i % palette.length]
}
function avatarCharOf(n) { return textAvatar(n || '?').char }
function avatarColorFor(n) { return textAvatar(n || '?').color }

function discussionIcon(mode) { return mode && mode.includes('线下') ? 'lucide:building-2' : 'lucide:monitor' }
function discussionIconBg(mode) { return mode && mode.includes('线下') ? 'icon-bg-emerald' : 'icon-bg-violet' }
function discussionIconColor(mode) { return mode && mode.includes('线下') ? '#059669' : '#7C3AED' }

function fileIconName(ext) {
  const m = { docx: 'lucide:file-text', xlsx: 'lucide:file-spreadsheet', pdf: 'lucide:file-text', img: 'lucide:image', pptx: 'lucide:monitor', zip: 'lucide:package' }
  return m[ext] || 'lucide:file'
}
function fileIconBg(ext) {
  const m = { docx: 'icon-bg-sky', xlsx: 'icon-bg-emerald', pdf: 'icon-bg-rose', img: 'icon-bg-violet', pptx: 'icon-bg-amber', zip: 'icon-bg-slate' }
  return m[ext] || 'icon-bg-sky'
}
function fileIconColor(ext) {
  const m = { docx: '#0EA5E9', xlsx: '#059669', pdf: '#E11D48', img: '#7C3AED', pptx: '#D97706', zip: '#475569' }
  return m[ext] || '#0EA5E9'
}
function fileTypeChip(ext) {
  const m = { docx: 'type-docx', xlsx: 'type-xlsx', pdf: 'type-pdf', img: 'type-img', pptx: 'type-pptx', zip: 'type-zip' }
  return m[ext] || 'type-other'
}

function goProfile() { uni.navigateTo({ url: '/pages/profile/profile' }) }
function goMessages() { uni.switchTab({ url: '/pages/messages/messages' }) }
function goCreate() { uni.navigateTo({ url: '/pages/create/create' }) }
function goProject() { uni.navigateTo({ url: '/pages/project/project?id=' + (projects.value[0] && projects.value[0].id || 1) }) }
function goFiles() { uni.switchTab({ url: '/pages/files/files' }) }
function goMeeting() { uni.navigateTo({ url: '/pages/meeting/meeting' }) }
function openProject(p) { uni.navigateTo({ url: '/pages/project/project?id=' + p.id }) }
function openTask(tk) { uni.navigateTo({ url: '/pages/project/project?id=' + (tk.projectId || 1) }) }
function openFile(f) { uni.navigateTo({ url: '/pages/view/view?fileId=' + f.id }) }

function onUpload() {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      if (res.tempFilePaths && res.tempFilePaths[0]) {
        uploadFile(res.tempFilePaths[0])
      }
    }
  })
}
async function uploadFile(filePath) {
  try {
    const pid = (projects.value[0] && projects.value[0].id) || 1
    const r = await api.uploadFile(filePath, { projectId: pid })
    showToast('已上传' + (r.fileName || '文件'))
    loadAll()
  } catch (e) { showToast((e && e.message) || '上传失败') }
}

function approveFile(f) {
  showToast('已通过')
  pendingFiles.value = pendingFiles.value.filter(x => x !== f)
}

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.home-page {
  background: var(--bg-system);
}
.content {
  flex: 1;
  height: calc(100vh - 0px);
}
.banner-wrap { padding: 0 20px; margin-top: 8px; }
.section-head { display: flex; align-items: center; justify-content: space-between; padding: 0 20px; margin: 20px 0 8px; }
.section-head .more { font-size: 12px; color: var(--blue-600); font-weight: 600; }
.section-title { font-size: 15px; font-weight: 600; color: #0F172A; }

.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; }
.topbar-user { display: flex; align-items: center; gap: 12px; }
.topbar-avatar {
  width: 44px; height: 44px; border-radius: 999px;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #FCD34D, #F59E0B);
  box-shadow: 0 1px 2px rgba(15,23,42,0.06);
}
.topbar-avatar-text { color: #78350F; font-size: 16px; font-weight: 800; }
.topbar-greet { display: flex; flex-direction: column; }
.topbar-sub { font-size: 11px; color: #94A3B8; }
.topbar-title { font-size: 20px; font-weight: 700; color: #0F172A; margin-top: 2px; }
.topbar-actions { display: flex; gap: 8px; }
.icon-btn { width: 40px; height: 40px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }
.badge-num {
  position: absolute; top: -2px; right: -2px;
  min-width: 16px; height: 16px; padding: 0 4px;
  border-radius: 999px; background: #F43F5E; color: #FFFFFF;
  font-size: 10px; font-weight: 700; line-height: 16px; text-align: center;
  border: 2px solid #FFFFFF;
}

.banner-dark {
  background: linear-gradient(135deg, #0F172A, #1E293B);
  border-radius: 16px;
  padding: 16px;
  position: relative;
  overflow: hidden;
}
.banner-ring {
  position: absolute; top: -40px; right: -40px;
  width: 160px; height: 160px; border-radius: 999px;
  background: radial-gradient(circle, rgba(59,130,246,0.5) 0%, rgba(59,130,246,0) 70%);
}
.banner-content { position: relative; }
.banner-head { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.banner-eyebrow { font-size: 11px; color: #CBD5E1; font-weight: 500; }
.banner-title { display: block; font-size: 15px; font-weight: 600; color: #FFFFFF; }
.banner-tip { display: block; font-size: 12px; color: #CBD5E1; margin-top: 4px; line-height: 1.4; }
.banner-cta-row { display: flex; gap: 8px; margin-top: 12px; }
.banner-cta-light {
  padding: 6px 14px; background: #FFFFFF; border-radius: 8px;
  display: flex; align-items: center;
}
.banner-cta-text-dark { font-size: 12px; font-weight: 600; color: #0F172A; }
.banner-cta-ghost {
  padding: 6px 14px; background: rgba(255,255,255,0.10); border-radius: 8px;
  display: flex; align-items: center;
}
.banner-cta-text-light { font-size: 12px; font-weight: 600; color: #FFFFFF; }

.stat-card {
  background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px;
  display: flex; align-items: center; padding: 4px 0; box-shadow: 0 1px 2px rgba(15,23,42,0.04);
}
.stat-cell {
  flex: 1; padding: 12px 4px; text-align: center;
  transition: background 0.15s ease;
  position: relative;
  overflow: hidden;
}
.stat-cell:active { background: #F8FAFC; }
.stat-cell:active::after {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(circle, rgba(37,99,235,0.08) 0%, transparent 70%);
  animation: ripple 0.4s ease-out;
}
@keyframes ripple {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.4); opacity: 0; }
}
.stat-num { display: block; font-size: 20px; font-weight: 800; line-height: 1.1; }
.stat-num-dark { color: #0F172A; }
.stat-num-amber { color: #D97706; }
.stat-num-rose { color: #E11D48; }
.stat-num-emerald { color: #059669; }
.stat-lbl { display: block; font-size: 11px; color: #64748B; margin-top: 4px; }
.divider-y { width: 1px; background: #F1F5F9; align-self: stretch; }

.quick-section { padding: 0 20px; margin-top: 20px; }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 12px; }
.quick-cell { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.quick-icon { width: 48px; height: 48px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.quick-icon-blue { background: #EFF6FF; }
.quick-icon-violet { background: #F5F3FF; }
.quick-icon-emerald { background: #ECFDF5; }
.quick-icon-amber { background: #FFFBEB; }
.quick-label { font-size: 12px; color: #334155; font-weight: 500; }

.proj-scroll-row { display: flex; flex-direction: row; flex-wrap: nowrap; padding: 0 20px 4px; gap: 12px; }
.proj-card {
  width: 270px; flex-shrink: 0; background: #FFFFFF;
  border-radius: 16px; border: 1px solid #E2E8F0;
  padding: 14px; box-shadow: 0 1px 2px rgba(15,23,42,0.04);
  border-top-width: 3px !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.proj-card:active {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15,23,42,0.08);
}
.proj-stage { font-size: 11px; font-weight: 600; }
.proj-name { display: block; font-size: 15px; font-weight: 600; color: #0F172A; margin-top: 4px; }
.proj-course { display: block; font-size: 12px; color: #64748B; margin-top: 2px; }
.proj-progress-row { display: flex; justify-content: space-between; margin-top: 12px; }
.proj-progress-lbl { font-size: 11px; color: #64748B; }
.proj-progress-val { font-size: 11px; color: #0F172A; font-weight: 700; }
.proj-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.proj-files { font-size: 11px; color: #64748B; }

.avatar-stack { display: flex; align-items: center; }
.stack-item { border: 2px solid #FFFFFF; }
.stack-item + .stack-item { margin-left: -8px; }
.stack-more { background: #F1F5F9; color: #64748B; border: 2px solid #FFFFFF; }

.dot-row { display: flex; justify-content: center; gap: 6px; margin-top: 12px; }
.dot { width: 6px; height: 6px; border-radius: 999px; background: #E2E8F0; }
.dot-on { width: 16px; background: #2563EB; }

.block-pad { padding: 0 20px; }
.list-card {
  display: flex; align-items: center; gap: 12px;
  background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px;
  padding: 14px; margin-bottom: 8px;
  box-shadow: 0 1px 2px rgba(15,23,42,0.04);
}
.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.row-title { font-size: 13px; font-weight: 600; color: #0F172A; line-height: 1.3; }
.row-meta { font-size: 11px; color: #64748B; margin-top: 2px; line-height: 1.3; }
.row-action { flex-shrink: 0; }
.row-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.icon-bg-blue { background: #EFF6FF; }
.icon-bg-sky { background: #F0F9FF; }
.icon-bg-violet { background: #F5F3FF; }
.icon-bg-emerald { background: #ECFDF5; }
.icon-bg-amber { background: #FFFBEB; }
.icon-bg-rose { background: #FFF1F2; }
.icon-bg-slate { background: #F1F5F9; }

.status-pill { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
.status-pill-rose { background: #FFF1F2; color: #BE123C; }
.status-pill-amber { background: #FFFBEB; color: #92400E; }
.status-pill-emerald { background: #ECFDF5; color: #047857; }
.status-pill-blue { background: #EFF6FF; color: #1D4ED8; }

.timeline { position: relative; }
.timeline-line { position: absolute; left: 15px; top: 12px; bottom: 12px; width: 1px; background: #E2E8F0; }
.timeline-item { display: flex; align-items: flex-start; gap: 12px; padding: 8px 0; position: relative; }
.timeline-dot {
  width: 30px; height: 30px; border-radius: 999px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; box-shadow: 0 0 0 2px #FFFFFF; position: relative; z-index: 1;
}
.timeline-dot-text { color: #FFFFFF; font-size: 11px; font-weight: 700; }
.timeline-body { flex: 1; min-width: 0; padding-top: 2px; }
.timeline-text { font-size: 12.5px; color: #334155; line-height: 1.5; }

.empty { padding: 24px 0; text-align: center; }
.empty-emoji { font-size: 32px; }
.empty-text { display: block; font-size: 13px; color: #94A3B8; margin-top: 6px; }

.toast {
  position: fixed; left: 50%; bottom: 100px;
  transform: translateX(-50%);
  background: rgba(15,23,42,0.85); color: #FFFFFF;
  padding: 8px 16px; border-radius: 999px;
  font-size: 12px; font-weight: 500;
  z-index: 99;
}
.toast-text { color: #FFFFFF; }
</style>
