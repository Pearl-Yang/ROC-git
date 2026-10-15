<template>
  <view class="page meeting-page">
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:chevron-left" :size="20" color="#475569" />
        </view>
        <view class="topbar-center">
          <text class="topbar-title">研讨记录</text>
          <text class="topbar-sub">{{ projectName }} · {{ discussions.length }} 次</text>
        </view>
        <view class="btn-fab" @tap="showNew = true">
          <RocIcon name="lucide:plus" :size="18" color="#FFFFFF" />
        </view>
      </view>
    </view>

    <view class="tabbar-row">
      <view :class="['tab-item', { 'tab-on': activeTab === 'all' }]" @tap="activeTab = 'all'">
        <text>全部记录</text>
      </view>
      <view :class="['tab-item', { 'tab-on': activeTab === 'pending' }]" @tap="activeTab = 'pending'">
        <text>结论待落地</text>
        <view v-if="pendingCount" class="badge-amber">{{ pendingCount }}</view>
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <view v-if="activeTab === 'all'" class="tab-pane">
        <view v-for="(d, di) in discussions" :key="d.id || di" class="card card-padded mb-12" :style="{ background: d.bg || '#FFFFFF' }">
          <view :class="['meeting-head', `meeting-head-${d.tone || 'violet'}`]">
            <view class="meeting-head-row">
              <text class="meeting-no">第 {{ d.no || (discussions.length - di) }} 次 · {{ d.mode || '线上研讨' }}</text>
              <RocIcon :name="d.mode && d.mode.includes('线下') ? 'lucide:building-2' : 'lucide:monitor'" :size="14" color="#FFFFFF" />
            </view>
            <text class="meeting-title">{{ d.title || '访谈提纲修订' }}</text>
            <view class="meeting-meta-row">
              <text class="meeting-meta">{{ d.date || '9月08日' }}</text>
              <text class="meeting-meta-dot">·</text>
              <text class="meeting-meta">{{ d.attendeeCount || 3 }} 人参与</text>
            </view>
          </view>
          <view class="meeting-body">
            <text class="meeting-section-title">研讨议题</text>
            <text class="meeting-text">{{ d.topic || '围绕社区调研访谈提纲展开讨论' }}</text>
            <text class="meeting-section-title">最终决策</text>
            <view v-if="d.decisions && d.decisions.length">
              <view v-for="(item, ii) in d.decisions" :key="ii" class="decision-item">
                <view class="bullet"></view>
                <text class="meeting-text">{{ item }}</text>
              </view>
            </view>
            <text v-else class="meeting-text" style="color:#94A3B8;">暂无决策</text>
            <view class="participants">
              <text class="meeting-section-title">参与人员</text>
              <view v-if="d.participants && d.participants.length" class="avatar-stack" style="margin-top:6px;">
                <view v-for="(p, pi) in d.participants" :key="pi" class="avatar avatar-sm stack-item" :style="{ background: p.color }">{{ p.char }}</view>
              </view>
              <text v-else class="meeting-text" style="color:#94A3B8;">—</text>
            </view>
            <text class="meeting-section-title">已落地更新</text>
            <view v-if="d.updates && d.updates.length">
              <view v-for="(u, ui) in d.updates" :key="ui" :class="['update-row-new', u.status === 'done' ? 'update-done' : 'update-pending']">
                <RocIcon :name="u.status === 'done' ? 'lucide:check-circle-2' : 'lucide:clock'" :size="13" :color="u.status === 'done' ? '#059669' : '#D97706'" />
                <text class="update-text-new" :style="{ color: u.status === 'done' ? '#065F46' : '#92400E' }">{{ u.text }}</text>
              </view>
            </view>
            <text v-else class="meeting-text" style="color:#94A3B8;">暂无更新</text>
          </view>
        </view>
        <view v-if="!discussions.length" class="empty">
          <text class="empty-emoji">💬</text>
          <text class="empty-text">暂无研讨记录</text>
        </view>
      </view>

      <view v-else class="tab-pane">
        <view v-for="(p, pi) in pendingItems" :key="p.id || pi" class="card card-padded mb-8 pending-card" @tap="openItem(p)">
          <view class="row" style="padding:0; gap:10px;">
            <view class="row-icon icon-bg-amber" style="background:#FEF3C7;">
              <RocIcon name="lucide:clock" :size="16" color="#D97706" />
            </view>
            <view class="row-body">
              <text class="row-title fw-700">{{ p.title }}</text>
              <text class="row-meta">第 {{ p.no || 3 }} 次研讨 · 待落地</text>
            </view>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
        <view v-if="!pendingItems.length" class="empty">
          <text class="empty-emoji">✅</text>
          <text class="empty-text">所有结论已落地</text>
        </view>
      </view>

      <view class="safe-bottom" style="height: 24px;"></view>
    </scroll-view>

    <!-- New meeting sheet -->
    <RocSheet :visible="showNew" @update:visible="v => showNew = v">
      <view class="sheet-pad">
        <text class="sheet-title">新建研讨记录</text>
        <view class="form-group">
          <text class="form-label">研讨主题</text>
          <view class="input-wrap">
            <input class="text-input" v-model="newForm.title" placeholder="如：访谈提纲修订" />
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">研讨日期</text>
          <view class="input-wrap" @tap="onPickDate">
            <input class="text-input" v-model="newForm.date" placeholder="选择日期" disabled />
            <RocIcon name="lucide:calendar" :size="14" color="#94A3B8" />
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">参与人员</text>
          <view class="chip-row">
            <view v-for="(p, pi) in newForm.participants" :key="pi" class="chip chip-blue">
              <view class="avatar avatar-sm" :style="{ width: '16px', height: '16px', fontSize: '8px', background: p.color }">{{ p.char }}</view>
              <text>{{ p.name }}</text>
            </view>
            <view class="chip chip-outline" @tap="onAddMember"><RocIcon name="lucide:plus" :size="10" color="#475569" /><text>添加</text></view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">议题</text>
          <view class="input-wrap" style="height: 60px; align-items:flex-start; padding: 8px 12px;">
            <textarea class="text-input" v-model="newForm.topic" placeholder="简要描述本次研讨的议题" style="height: 44px;" />
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">最终决策</text>
          <view class="input-wrap" style="height: 80px; align-items:flex-start; padding: 8px 12px;">
            <textarea class="text-input" v-model="newForm.decision" placeholder="每行一条决策" style="height: 64px;" />
          </view>
        </view>
        <view class="row" style="padding:0; gap:8px; margin-top:16px;">
          <view class="btn btn-ghost flex-1" style="height:44px; line-height:44px; padding:0;" @tap="showNew = false"><text>取消</text></view>
          <view class="btn btn-accent flex-1" style="height:44px; line-height:44px; padding:0; width:auto;" @tap="onCreateMeeting"><text>保存记录</text></view>
        </view>
      </view>
    </RocSheet>

    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { textAvatar } from '../../utils/avatar.js'
import RocIcon from '../../components/Icon.vue'
import RocSheet from '../../components/Sheet.vue'

const safeTop = ref(20)
const activeTab = ref('all')
const discussions = ref([])
const projectName = ref('社区调研报告')
const projectId = ref(1)
const showNew = ref(false)
const newForm = ref({ title: '', date: '', participants: [], topic: '', decision: '' })
const toast = ref({ show: false, text: '' })

const pendingCount = computed(() => discussions.value.reduce((s, d) => s + (d.pendingCount || 0), 0))
const pendingItems = computed(() => {
  const list = []
  discussions.value.forEach(d => {
    if (d.pendingItems) list.push(...d.pendingItems)
  })
  return list
})

onLoad((q) => {
  if (q && q.id) projectId.value = Number(q.id)
  refreshSafeTop()
})
onShow(() => { if (api.isLoggedIn()) load() })

function refreshSafeTop() { try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {} }

function avatarFor(nickname) {
  const a = textAvatar(nickname || '?')
  return { char: a.char, color: a.color, name: nickname || '?' }
}

function formatDate(s) {
  if (!s) return ''
  try {
    const d = new Date(s)
    if (isNaN(d.getTime())) return s
    return (d.getMonth() + 1) + '月' + (d.getDate() < 10 ? '0' + d.getDate() : d.getDate()) + '日'
  } catch (e) { return s }
}

async function load() {
  try {
    // 1. 研讨列表
    const listRes = await api.discussionList(projectId.value, { pageSize: 20 })
    const list = (listRes && (listRes.records || listRes.list || listRes.items)) || []
    // 2. 并行拉决策、活动、成员
    const [decisionsRes, activitiesRes, groupMembers] = await Promise.all([
      api.decisionList(projectId.value, { pageSize: 50 }).catch(() => null),
      api.projectActivities(projectId.value, { pageSize: 30 }).catch(() => null),
      // 拉项目详情以获取 groupId，再拉成员
      api.projectDetail(projectId.value).then(p => p && p.groupId ? api.groupMembers(p.groupId, { pageSize: 100 }).catch(() => null) : null).catch(() => null)
    ])
    const decisions = (decisionsRes && (decisionsRes.records || decisionsRes.list || decisionsRes.items)) || []
    const activities = (activitiesRes && (activitiesRes.records || activitiesRes.list || activitiesRes.items)) || []
    const members = (groupMembers && (groupMembers.list || groupMembers.records || groupMembers.members)) || []

    const memberMap = {}
    members.forEach(m => { memberMap[m.userId] = avatarFor(m.nickname) })

    if (list.length) {
      discussions.value = list.map((d, i) => {
        // 关联到当前研讨的决策
        const ownDecisions = decisions.filter(x => x.discussionId === d.discussionId || x.discussionId === d.id).map(x => x.content || x.title || '')
        // 已落地更新：用 activities 过滤 type=task_done
        const ownUpdates = activities.filter(a => a.type === 'task_done' || a.actionType === 'task_done').slice(0, 3).map(a => ({
          icon: 'lucide:check',
          text: (a.user || '') + ' ' + (a.action || a.title || a.description || ''),
          color: '#10B981',
          textColor: '#047857',
          status: 'done'
        }))
        // 如果没有任何已落地，给一条 pending 提示
        if (!ownUpdates.length) {
          ownUpdates.push({
            icon: 'lucide:clock',
            text: '暂无已落地更新',
            color: '#D97706',
            textColor: '#92400E',
            status: 'pending'
          })
        }
        // 参与人员：取 memberMap 头 3 个
        const ownParticipants = members.slice(0, 4).map(m => avatarFor(m.nickname))
        return {
          ...d,
          id: d.discussionId || d.id,
          no: list.length - i,
          tone: ['violet', 'emerald', 'slate'][i % 3],
          bg: i === 0 ? 'linear-gradient(135deg, #F5F3FF, #FFFFFF)' : '#FFFFFF',
          date: formatDate(d.date || d.createTime || d.createdAt),
          attendeeCount: members.length || d.attendeeCount || 0,
          mode: d.mode || '线上研讨',
          topic: d.topic || d.content || '',
          decisions: ownDecisions,
          participants: ownParticipants,
          updates: ownUpdates,
          pendingCount: ownUpdates.length ? 0 : 0
        }
      })
    } else {
      discussions.value = []
    }
  } catch (e) {
    discussions.value = []
  }
}

function goBack() { uni.navigateBack() }
function openItem(p) { showToast('跳转到任务') }

function onPickDate() {
  // #ifdef MP-WEIXIN
  uni.showToast({ title: '请使用原生日期选择', icon: 'none' })
  // #endif
}
function onAddMember() {
  const a = textAvatar('新成员')
  newForm.value.participants.push({ name: '新成员', char: a.char, color: a.color })
}
function onCreateMeeting() {
  if (!newForm.value.title) return showToast('请填写主题')
  // 真接口：api.discussionCreate
  const decisions = (newForm.value.decision || '').split('\n').map(s => s.trim()).filter(Boolean)
  api.discussionCreate({
    projectId: projectId.value,
    title: newForm.value.title,
    content: newForm.value.topic || '',
    mode: '线上研讨',
    decisions: decisions
  }).then(() => {
    showToast('已保存')
    showNew.value = false
    newForm.value = { title: '', date: '', participants: [], topic: '', decision: '' }
    load()
  }).catch(err => showToast((err && err.message) || '保存失败'))
}

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.meeting-page { background: var(--bg-system); }
.content { flex: 1; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; display: flex; align-items: center; justify-content: space-between; }
.topbar-center { flex: 1; text-align: center; }
.topbar-title { font-size: 16px; font-weight: 600; color: #0F172A; display: block; }
.topbar-sub { font-size: 11px; color: #94A3B8; margin-top: 2px; display: block; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }
.btn-fab { display: inline-flex; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: 999px; background: #2563EB; color: #FFFFFF; box-shadow: 0 2px 6px rgba(37,99,235,0.20); }

.tabbar-row { display: flex; background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.tab-item { flex: 1; padding: 12px 8px; text-align: center; font-size: 13px; font-weight: 500; color: #94A3B8; border-bottom: 2px solid transparent; display: flex; align-items: center; justify-content: center; gap: 6px; }
.tab-on { color: #2563EB; border-bottom-color: #2563EB; }
.badge-amber { background: #FEF3C7; color: #92400E; font-size: 10px; font-weight: 700; padding: 1px 6px; border-radius: 999px; }

.tab-pane { padding: 16px 20px; }
.card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px; box-shadow: 0 1px 2px rgba(15,23,42,0.04); }
.card-padded { padding: 14px; }
.mb-8 { margin-bottom: 8px; }
.mb-12 { margin-bottom: 12px; }
.flex-1 { flex: 1; }

.meeting-head { border-radius: 12px; padding: 12px 14px; color: #FFFFFF; }
.meeting-head-violet { background: linear-gradient(135deg, #8B5CF6, #6366F1); }
.meeting-head-emerald { background: linear-gradient(135deg, #10B981, #14B8A6); }
.meeting-head-slate { background: linear-gradient(135deg, #94A3B8, #64748B); }
.meeting-head-row { display: flex; align-items: center; justify-content: space-between; }
.meeting-no { font-size: 11px; opacity: 0.9; font-weight: 500; }
.meeting-title { display: block; font-size: 15px; font-weight: 700; margin-top: 4px; }
.meeting-meta-row { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.meeting-meta { font-size: 11px; opacity: 0.85; }
.meeting-meta-dot { opacity: 0.6; }

.meeting-body { padding: 12px 4px 0; }
.meeting-section-title { display: block; font-size: 11px; font-weight: 600; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.05em; margin-top: 12px; }
.meeting-text { display: block; font-size: 13px; color: #0F172A; line-height: 1.5; margin-top: 4px; }
.decision-item { display: flex; align-items: flex-start; gap: 8px; padding: 2px 0; }
.bullet { width: 4px; height: 4px; border-radius: 999px; background: #2563EB; margin-top: 8px; flex-shrink: 0; }
.participants { margin-top: 6px; }
.avatar-stack { display: flex; align-items: center; }
.stack-item { border: 2px solid #FFFFFF; }
.stack-item + .stack-item { margin-left: -8px; }
.update-row { display: flex; align-items: center; gap: 6px; padding: 4px 0; }
.update-text { font-size: 12px; }
.update-row-new { display: flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: 8px; margin-bottom: 4px; }
.update-done { background: #ECFDF5; }
.update-pending { background: #FFFBEB; }
.update-text-new { font-size: 11.5px; font-weight: 500; }

.pending-card { background: #FFFBEB; border-color: #FDE68A; }
.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.row-title { font-size: 12.5px; color: #334155; line-height: 1.4; display: block; }
.row-meta { font-size: 11px; color: #64748B; margin-top: 2px; display: block; }
.row-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-bg-amber { background: #FFFBEB; }

.empty { padding: 32px 0; text-align: center; }
.empty-emoji { font-size: 32px; }
.empty-text { display: block; font-size: 13px; color: #94A3B8; margin-top: 6px; }

.sheet-pad { padding: 16px 20px 8px; }
.sheet-title { font-size: 15px; font-weight: 700; color: #0F172A; display: block; margin-bottom: 12px; text-align: center; }
.form-group { margin-bottom: 12px; }
.form-label { font-size: 11px; font-weight: 600; color: #475569; display: block; margin-bottom: 6px; }
.input-wrap { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 0 12px; height: 40px; display: flex; align-items: center; }
.text-input { flex: 1; height: 40px; font-size: 13px; color: #0F172A; background: transparent; border: none; outline: none; padding: 0; }
.chip-row { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 500; }
.chip-blue { background: #EFF6FF; color: #2563EB; border: 1px solid #DBEAFE; }
.chip-outline { background: transparent; color: #475569; border: 1px solid #E2E8F0; }

.btn { display: flex; align-items: center; justify-content: center; gap: 6px; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
.text-faint { color: #94A3B8; }
.fw-700 { font-weight: 700; }
.fs-11 { font-size: 11px; }
</style>
