<template>
  <view class="page messages-page">
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:chevron-left" :size="20" color="#475569" />
        </view>
        <view class="topbar-center">
          <text class="topbar-title">信息中心</text>
          <text class="topbar-sub">
            <text class="text-blue fw-700">{{ unreadCount }} 条未读</text> · {{ urgentCount }} 项紧急
          </text>
        </view>
        <view class="btn btn-ghost" style="height:30px; line-height:30px; padding:0 10px; font-size:11px;" @tap="markRead">
          <RocIcon name="lucide:check-check" :size="12" color="#475569" />
          <text>全部已读</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <!-- Search -->
      <view class="search-wrap">
        <view class="search-box">
          <RocIcon name="lucide:search" :size="14" color="#94A3B8" />
          <input class="search-input" placeholder="搜索通知、人员或项目" v-model="keyword" @input="keyword = $event.detail.value" />
          <view v-if="keyword" class="search-clear" @tap="keyword = ''">
            <RocIcon name="lucide:x" :size="10" color="#FFFFFF" />
          </view>
        </view>
      </view>

      <!-- Filter chips -->
      <scroll-view scroll-x class="h-scroll" :show-scrollbar="false">
        <view class="chip-row">
          <view :class="['msg-chip', filter === 'all' ? 'msg-chip-on' : '']" @tap="filter = 'all'">
            <text>全部</text>
            <text class="msg-chip-num">{{ messages.length }}</text>
          </view>
          <view :class="['msg-chip', filter === 'urgent' ? 'msg-chip-on' : '']" @tap="filter = 'urgent'">
            <RocIcon name="lucide:alarm-clock" :size="10" color="#D97706" />
            <text>紧急</text>
            <text class="msg-chip-num">{{ urgentCount }}</text>
          </view>
          <view :class="['msg-chip', filter === 'review' ? 'msg-chip-on' : '']" @tap="filter = 'review'">
            <RocIcon name="lucide:file-check" :size="10" color="#D97706" />
            <text>待审核</text>
          </view>
          <view :class="['msg-chip', filter === 'version' ? 'msg-chip-on' : '']" @tap="filter = 'version'">
            <RocIcon name="lucide:git-branch" :size="10" color="#7C3AED" />
            <text>版本</text>
          </view>
          <view :class="['msg-chip', filter === 'meeting' ? 'msg-chip-on' : '']" @tap="filter = 'meeting'">
            <RocIcon name="lucide:users" :size="10" color="#059669" />
            <text>研讨</text>
          </view>
          <view :class="['msg-chip', filter === 'deadline' ? 'msg-chip-on' : '']" @tap="filter = 'deadline'">
            <RocIcon name="lucide:bell" :size="10" color="#D97706" />
            <text>提醒</text>
          </view>
        </view>
      </scroll-view>

      <!-- Urgent banner -->
      <view v-if="(filter === 'all' || filter === 'urgent') && urgentItem" class="urgent-card">
        <view class="row-icon icon-bg-amber" style="background:#FEF3C7;">
          <RocIcon name="lucide:alarm-clock" :size="16" color="#D97706" />
        </view>
        <view class="row-body">
          <view class="row" style="padding:0; gap:6px;">
            <view class="status-pill status-pill-amber"><text>紧急</text></view>
            <text class="fs-10 text-amber">还剩 {{ urgentItem.daysLeft || 4 }} 天</text>
          </view>
          <text class="row-title fw-700" style="margin-top:4px;">{{ urgentItem.title }}</text>
          <text class="row-meta">{{ urgentItem.projectName }} · 截止 {{ urgentItem.deadline }}</text>
          <view class="row" style="padding:0; gap:6px; margin-top:8px;">
            <view class="btn btn-amber-mini" @tap="onHandleUrgent"><text>立即处理</text></view>
            <view class="btn btn-ghost" style="height:26px; line-height:26px; padding:0 10px; font-size:11px;" @tap="showToast('已设置稍后提醒')"><text>稍后提醒</text></view>
          </view>
        </view>
      </view>

      <!-- Today -->
      <view v-if="groupedMessages.today.length" class="group">
        <view class="group-head">
          <text class="group-name">今天</text>
          <text class="group-date">{{ todayDateText }}</text>
          <view class="hairline" style="flex:1; background:#F1F5F9;"></view>
        </view>
        <view v-for="m in groupedMessages.today" :key="m.id" class="notice-card" :class="m.unread ? 'notice-unread' : ''" @tap="onTapMessage(m)">
          <view class="row" style="padding:0; align-items:flex-start;">
            <view v-if="m.unread" class="unread-dot"></view>
            <view v-else class="unread-dot-spacer"></view>
            <view :class="['row-icon', iconBgFor(m.type)]">
              <RocIcon :name="iconFor(m.type)" :size="15" :color="iconColorFor(m.type)" />
            </view>
            <view class="row-body">
              <view class="row" style="padding:0; justify-content:space-between; align-items:flex-start;">
                <text class="row-title fw-700">{{ m.title }}</text>
                <text class="fs-10 text-faint">{{ m.time }}</text>
              </view>
              <text class="row-meta">{{ m.description }}</text>
              <view v-if="m.actionLabel" class="row" style="padding:0; gap:6px; margin-top:6px;">
                <view class="btn btn-blue-mini" @tap.stop="onMessageAction(m)">
                  <text>{{ m.actionLabel }}</text>
                </view>
                <text v-if="m.projectName" class="fs-10 text-faint">{{ m.projectName }}</text>
              </view>
              <text v-else-if="m.projectName" class="fs-10 text-faint" style="display:block; margin-top:4px;">{{ m.projectName }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- Yesterday -->
      <view v-if="groupedMessages.yesterday.length" class="group">
        <view class="group-head">
          <text class="group-name">昨天</text>
          <text class="group-date">9月09日</text>
          <view class="hairline" style="flex:1; background:#F1F5F9;"></view>
        </view>
        <view v-for="m in groupedMessages.yesterday" :key="m.id" class="notice-card" @tap="onTapMessage(m)">
          <view class="row" style="padding:0; align-items:flex-start;">
            <view v-if="m.unread" class="unread-dot"></view>
            <view v-else class="unread-dot-spacer"></view>
            <view :class="['row-icon', iconBgFor(m.type)]">
              <RocIcon :name="iconFor(m.type)" :size="15" :color="iconColorFor(m.type)" />
            </view>
            <view class="row-body">
              <view class="row" style="padding:0; justify-content:space-between; align-items:flex-start;">
                <text class="row-title fw-700">{{ m.title }}</text>
                <text class="fs-10 text-faint">{{ m.time }}</text>
              </view>
              <text class="row-meta">{{ m.description }}</text>
              <text v-if="m.projectName" class="fs-10 text-faint" style="display:block; margin-top:4px;">{{ m.projectName }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- Earlier -->
      <view v-if="groupedMessages.earlier.length" class="group">
        <view class="group-head">
          <text class="group-name text-muted">更早</text>
          <view class="hairline" style="flex:1; background:#F1F5F9;"></view>
        </view>
        <view v-for="m in groupedMessages.earlier" :key="m.id" class="notice-card notice-archive" @tap="onTapMessage(m)">
          <view class="row" style="padding:0; align-items:flex-start;">
            <RocIcon name="lucide:check-circle-2" :size="14" color="#10B981" cls="mt-1" />
            <view class="row-body" style="padding-left:6px;">
              <text class="row-title text-muted">{{ m.title }}</text>
              <text class="fs-10 text-faint" style="display:block; margin-top:2px;">{{ m.time }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="!filteredMessages.length" class="empty-block">
        <view class="icon-btn" style="width:56px; height:56px; background:#F1F5F9;">
          <RocIcon name="lucide:search-x" :size="22" color="#94A3B8" />
        </view>
        <text class="empty-title">没有找到匹配的通知</text>
        <text class="empty-text">换个关键词或切换筛选试试</text>
      </view>

      <!-- Bottom tools -->
      <view class="bottom-tools">
        <view class="bottom-tool" @tap="goSettings">
          <RocIcon name="lucide:settings" :size="11" color="#94A3B8" />
          <text>通知设置</text>
        </view>
        <text class="bottom-divider">|</text>
        <view class="bottom-tool">
          <RocIcon name="lucide:archive" :size="11" color="#94A3B8" />
          <text>归档历史</text>
        </view>
      </view>

      <view class="safe-bottom" style="height: 24px;"></view>
    </scroll-view>

    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import RocIcon from '../../components/Icon.vue'

const safeTop = ref(20)
const messages = ref([])
const unreadCount = ref(0)
const filter = ref('all')
const keyword = ref('')
const toast = ref({ show: false, text: '' })

const todayDateText = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}月${d.getDate()}日`
})

const urgentItem = computed(() => messages.value.find(m => m.type === 'urgent'))
const urgentCount = computed(() => messages.value.filter(m => m.type === 'urgent').length)

const filteredMessages = computed(() => {
  let list = messages.value
  if (filter.value !== 'all') list = list.filter(m => m.type === filter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(m => (m.title + m.description + (m.projectName || '')).toLowerCase().includes(kw))
  }
  return list
})

const groupedMessages = computed(() => {
  const today = []
  const yesterday = []
  const earlier = []
  filteredMessages.value.forEach(m => {
    if (m.group === 'today') today.push(m)
    else if (m.group === 'yesterday') yesterday.push(m)
    else earlier.push(m)
  })
  return { today, yesterday, earlier }
})

onLoad(() => { refreshSafeTop() })
onShow(() => { load() })

function refreshSafeTop() {
  try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {}
}

async function load() {
  try {
    const list = await api.messageList({ pageSize: 20 })
    const items = (list && list.list) || list || []
    messages.value = enrichMessages(items)
    const unread = await api.unreadCount().catch(() => null)
    if (unread) unreadCount.value = unread.total || 0
  } catch (e) {
    messages.value = demoMessages()
    unreadCount.value = 5
  }
}

function enrichMessages(items) {
  if (items.length) {
    return items.map((m, i) => ({
      ...m,
      type: m.type || ['urgent', 'review', 'version', 'meeting', 'deadline'][i % 5],
      group: m.group || (i < 2 ? 'today' : i < 4 ? 'yesterday' : 'earlier'),
      unread: m.unread !== false
    }))
  }
  return demoMessages()
}

function demoMessages() {
  return [
    { id: 1, type: 'urgent', title: '资料归集即将截止', description: '截止 9月14日', projectName: '社区调研报告', daysLeft: 4, deadline: '9月14日', group: 'today', unread: true, time: '08:00' },
    { id: 2, type: 'review', title: '许萌提交了 访谈录音转写稿', description: '请尽快审核入库', projectName: '社区调研报告', actionLabel: '立即审核', group: 'today', unread: true, time: '10:18' },
    { id: 3, type: 'version', title: '王宁更新了 调研报告大纲', description: 'v3 → v4 · 新增问题分析章节 + 1200 字', projectName: '统计学小组作业', group: 'today', unread: true, time: '10:36' },
    { id: 4, type: 'meeting', title: '第 3 次线上研讨已记录', description: '访谈提纲修订 · 5 条纪要', projectName: '社区调研报告', group: 'yesterday', unread: true, time: '19:30' },
    { id: 5, type: 'deadline', title: '许萌的访谈整理即将到期', description: '将于 09/12 截止', projectName: '社区调研报告', group: 'yesterday', unread: false, time: '08:00' },
    { id: 6, type: 'review', title: '你确认了「选题与分工」成果', description: '记录已归档', projectName: '社区调研报告', group: 'earlier', unread: false, time: '9月03日' }
  ]
}

function goBack() { uni.navigateBack() }
function goSettings() { uni.navigateTo({ url: '/pages/settings/settings' }) }
function markRead() {
  api.readAll().catch(() => {})
  messages.value = messages.value.map(m => ({ ...m, unread: false }))
  unreadCount.value = 0
  showToast('已全部已读')
}

function onHandleUrgent() {
  if (urgentItem.value && urgentItem.value.projectId) {
    uni.navigateTo({ url: '/pages/project/project?id=' + urgentItem.value.projectId })
  }
}
function onMessageAction(m) {
  if (m.projectId) uni.navigateTo({ url: '/pages/project/project?id=' + m.projectId })
}
function onTapMessage(m) { m.unread = false; if (m.projectId) uni.navigateTo({ url: '/pages/project/project?id=' + m.projectId }) }

function iconFor(type) {
  const m = { urgent: 'lucide:alarm-clock', review: 'lucide:file-check', version: 'lucide:git-branch', meeting: 'lucide:users', deadline: 'lucide:bell' }
  return m[type] || 'lucide:bell'
}
function iconBgFor(type) {
  const m = { urgent: 'icon-bg-amber', review: 'icon-bg-amber', version: 'icon-bg-violet', meeting: 'icon-bg-emerald', deadline: 'icon-bg-amber' }
  return m[type] || 'icon-bg-slate'
}
function iconColorFor(type) {
  const m = { urgent: '#D97706', review: '#D97706', version: '#7C3AED', meeting: '#059669', deadline: '#D97706' }
  return m[type] || '#475569'
}

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.messages-page { background: var(--bg-system); }
.content { flex: 1; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; display: flex; align-items: center; justify-content: space-between; }
.topbar-center { flex: 1; text-align: center; }
.topbar-title { font-size: 16px; font-weight: 600; color: #0F172A; display: block; }
.topbar-sub { font-size: 11px; color: #64748B; margin-top: 2px; display: block; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; position: relative; }

.search-wrap { padding: 12px 20px 0; }
.search-box { background: #F1F5F9; border: 1px solid transparent; border-radius: 12px; padding: 0 12px; height: 36px; display: flex; align-items: center; gap: 6px; transition: border-color 0.15s ease, background 0.15s ease; }
.search-box:focus-within { background: #FFFFFF; border-color: #2563EB; }
.search-input { flex: 1; height: 36px; font-size: 12px; color: #0F172A; background: transparent; border: none; outline: none; }
.search-clear { width: 18px; height: 18px; border-radius: 999px; background: #94A3B8; display: flex; align-items: center; justify-content: center; }

.chip-row { display: flex; gap: 6px; padding: 12px 20px; }
.msg-chip { display: inline-flex; align-items: center; gap: 4px; padding: 6px 12px; border-radius: 999px; background: #FFFFFF; border: 1px solid #E2E8F0; font-size: 11px; font-weight: 500; color: #475569; white-space: nowrap; }
.msg-chip-on { background: #2563EB; color: #FFFFFF; border-color: #2563EB; }
.msg-chip-on .msg-chip-num { color: #FFFFFF; }
.msg-chip-num { color: #94A3B8; font-weight: 700; }

.urgent-card { margin: 12px 20px 0; background: #FFFBEB; border: 1px solid #FDE68A; border-radius: 12px; padding: 12px; display: flex; gap: 10px; }
.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.row-title { font-size: 12.5px; color: #334155; line-height: 1.4; display: block; }
.row-meta { font-size: 11px; color: #64748B; margin-top: 2px; line-height: 1.4; display: block; }
.row-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-bg-blue { background: #EFF6FF; }
.icon-bg-sky { background: #F0F9FF; }
.icon-bg-violet { background: #F5F3FF; }
.icon-bg-emerald { background: #ECFDF5; }
.icon-bg-amber { background: #FFFBEB; }
.icon-bg-rose { background: #FFF1F2; }
.icon-bg-slate { background: #F1F5F9; }

.btn-amber-mini { background: #D97706; color: #FFFFFF; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; }
.btn-blue-mini { background: #EFF6FF; color: #2563EB; padding: 4px 10px; border-radius: 6px; font-size: 10.5px; font-weight: 600; }

.group { padding: 16px 20px 0; }
.group-head { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.group-name { font-size: 11px; font-weight: 600; color: #64748B; }
.group-date { font-size: 10px; color: #94A3B8; }
.hairline { height: 1px; background: #E2E8F0; flex: 1; }

.notice-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; padding: 12px; margin-bottom: 8px; transition: transform 0.15s ease, box-shadow 0.15s ease; position: relative; }
.notice-card:active { transform: translateX(2px); box-shadow: 0 4px 12px rgba(15,23,42,0.06); }
.notice-unread { background: #FFFFFF; border-left: 3px solid #2563EB; padding-left: 9px; }
.notice-archive { background: #F8FAFC; opacity: 0.85; border-color: #E2E8F0; }
.unread-dot { width: 6px; height: 6px; border-radius: 999px; background: #2563EB; margin-top: 8px; flex-shrink: 0; }
.unread-dot-spacer { width: 6px; height: 6px; flex-shrink: 0; }

.status-pill { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
.status-pill-amber { background: #FEF3C7; color: #92400E; }
.status-pill-emerald { background: #D1FAE5; color: #047857; }
.status-pill-blue { background: #DBEAFE; color: #1D4ED8; }

.empty-block { display: flex; flex-direction: column; align-items: center; padding: 48px 0 24px; }
.empty-title { font-size: 13px; font-weight: 600; color: #334155; margin-top: 12px; }
.empty-text { font-size: 11px; color: #94A3B8; margin-top: 4px; }

.bottom-tools { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 16px 0 8px; font-size: 11px; color: #94A3B8; }
.bottom-tool { display: flex; align-items: center; gap: 4px; }
.bottom-divider { color: #E2E8F0; }

.text-faint { color: #94A3B8; }
.text-muted { color: #64748B; }
.text-blue { color: #2563EB; }
.text-amber { color: #D97706; }
.fw-700 { font-weight: 700; }
.fs-10 { font-size: 10px; }
.fs-11 { font-size: 11px; }
.fs-12 { font-size: 12px; }
.mt-1 { margin-top: 4px; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
</style>
