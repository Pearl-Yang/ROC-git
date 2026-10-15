<template>
  <view class="page">
    <!-- 顶部 -->
    <view class="topbar">
      <view class="icon-btn" @tap="goBack">
        <Icon name="lucide:chevron-left" :width="20" :height="20" color="#0F172A" />
      </view>
      <view class="topbar-center">
        <text class="topbar-title">任务提醒</text>
        <text class="topbar-sub">共 {{ total }} 条 · 未读 {{ unreadCount }}</text>
      </view>
      <view class="scan-btn" :class="{ scanning }" @tap="onScan">
        <Icon name="lucide:radar" :width="14" :height="14" :color="scanning ? '#94A3B8' : '#2563EB'" />
        <text>{{ scanning ? '扫描中…' : '扫描' }}</text>
      </view>
    </view>

    <!-- 紧急提示 -->
    <view v-if="overdueList.length" class="alert">
      <view class="alert-icon">
        <Icon name="lucide:alert-triangle" :width="18" :height="18" color="#DC2626" />
      </view>
      <view class="alert-body">
        <text class="alert-title">{{ overdueList.length }} 项已逾期</text>
        <text class="alert-tip">建议尽快处理或调整截止日期</text>
      </view>
    </view>

    <!-- 过滤 -->
    <view class="filter-bar">
      <view class="chip" :class="{ active: filter === 'all' }" @tap="filter = 'all'">
        <text>全部</text>
      </view>
      <view class="chip" :class="{ active: filter === 'overdue' }" @tap="filter = 'overdue'">
        <text>已逾期</text>
      </view>
      <view class="chip" :class="{ active: filter === 'upcoming' }" @tap="filter = 'upcoming'">
        <text>即将到期</text>
      </view>
    </view>

    <view v-if="loading && !list.length" class="state">加载中…</view>
    <view v-else-if="!filteredList.length" class="state">
      <view class="empty-art">
        <Icon name="lucide:bell-off" :width="32" :height="32" color="#94A3B8" />
      </view>
      <text>暂无提醒</text>
      <text class="empty-tip">点右上角「扫描」可立即触发一次提醒扫描</text>
    </view>

    <view v-else class="list">
      <view v-for="r in filteredList" :key="r.id" class="reminder-card" :class="cardClass(r)">
        <view class="reminder-icon" :class="iconClass(r)">
          <Icon :name="iconName(r)" :width="16" :height="16" :color="iconColor(r)" />
        </view>
        <view class="reminder-body">
          <text class="reminder-title">{{ r.title || r.taskName || '任务提醒' }}</text>
          <text class="reminder-meta" v-if="r.projectName">{{ r.projectName }}</text>
          <text class="reminder-meta">{{ formatTime(r.dueAt || r.deadline || r.createdAt) }}</text>
        </view>
        <view class="reminder-status">
          <text class="status-text" :class="statusClass(r)">{{ statusLabel(r) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '@/utils/api.js'
import Icon from '@/components/Icon.vue'

const list = ref([])
const loading = ref(false)
const scanning = ref(false)
const filter = ref('all')
const unreadCount = ref(0)

const total = computed(() => list.value.length)
const overdueList = computed(() => list.value.filter(r => r.status === 'overdue' || r.overdue))
const filteredList = computed(() => {
  if (filter.value === 'overdue') return overdueList.value
  if (filter.value === 'upcoming') return list.value.filter(r => r.status === 'upcoming' || r.upcoming)
  return list.value
})

function cardClass(r) {
  if (r.status === 'overdue' || r.overdue) return 'danger'
  if (r.status === 'upcoming' || r.upcoming) return 'warn'
  return ''
}
function iconName(r) {
  if (r.status === 'overdue' || r.overdue) return 'lucide:alert-circle'
  if (r.status === 'upcoming' || r.upcoming) return 'lucide:clock'
  return 'lucide:bell'
}
function iconClass(r) {
  if (r.status === 'overdue' || r.overdue) return 'icon-danger'
  if (r.status === 'upcoming' || r.upcoming) return 'icon-warn'
  return 'icon-blue'
}
function iconColor(r) {
  if (r.status === 'overdue' || r.overdue) return '#DC2626'
  if (r.status === 'upcoming' || r.upcoming) return '#D97706'
  return '#2563EB'
}
function statusLabel(r) {
  if (r.status === 'overdue' || r.overdue) return '已逾期'
  if (r.status === 'upcoming' || r.upcoming) return '即将到期'
  return '待处理'
}
function statusClass(r) {
  if (r.status === 'overdue' || r.overdue) return 'status-danger'
  if (r.status === 'upcoming' || r.upcoming) return 'status-warn'
  return 'status-blue'
}
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

async function loadList() {
  loading.value = true
  try {
    const r = await api.taskReminderMy(1, 50)
    list.value = (r && r.list) || (Array.isArray(r) ? r : [])
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
  try {
    const u = await api.taskReminderUnsent()
    unreadCount.value = (u && (u.count || u.total || u)) || 0
  } catch (e) { unreadCount.value = 0 }
}

async function onScan() {
  if (scanning.value) return
  scanning.value = true
  try {
    const r = await api.taskReminderScan()
    const created = (r && (r.created || r.count)) || 0
    const sent = (r && (r.sent || r.pushed)) || 0
    uni.showToast({ title: `新建 ${created} 条，已发 ${sent} 条`, icon: 'none' })
    await loadList()
  } catch (e) {
    uni.showToast({ title: '扫描失败', icon: 'none' })
  } finally {
    scanning.value = false
  }
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/home/home' })
}

onLoad(() => { loadList() })
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #F8FAFC;
  padding-bottom: env(safe-area-inset-bottom);
}

.topbar {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  gap: 12px;
}
.topbar-center { flex: 1; text-align: center; display: flex; flex-direction: column; align-items: center; }
.topbar-title { font-size: 15px; font-weight: 700; color: #0F172A; }
.topbar-sub { font-size: 11px; color: #94A3B8; margin-top: 2px; }
.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #F1F5F9;
}
.scan-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #DBEAFE;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  color: #2563EB;
}
.scan-btn.scanning {
  background: #F1F5F9;
  color: #94A3B8;
}

.alert {
  display: flex;
  gap: 12px;
  margin: 12px 16px 0;
  padding: 12px 14px;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 14px;
  align-items: center;
}
.alert-icon {
  width: 36px;
  height: 36px;
  background: #FEE2E2;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.alert-body { flex: 1; }
.alert-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #DC2626;
}
.alert-tip {
  display: block;
  font-size: 11px;
  color: #B91C1C;
  margin-top: 2px;
}

.filter-bar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  overflow-x: auto;
}
.chip {
  padding: 6px 14px;
  background: #F1F5F9;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  flex-shrink: 0;
}
.chip.active {
  background: #2563EB;
  color: #FFFFFF;
}

.state {
  text-align: center;
  padding: 60px 16px;
  color: #94A3B8;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
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
.empty-tip {
  font-size: 12px;
  color: #94A3B8;
}

.list {
  padding: 0 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reminder-card {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  padding: 12px 14px;
}
.reminder-card.danger {
  border-color: #FECACA;
  background: linear-gradient(180deg, #FEF2F2 0%, #FFFFFF 60%);
}
.reminder-card.warn {
  border-color: #FED7AA;
  background: linear-gradient(180deg, #FFF7ED 0%, #FFFFFF 60%);
}
.reminder-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #DBEAFE;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.reminder-icon.icon-warn { background: #FEF3C7; }
.reminder-icon.icon-danger { background: #FEE2E2; }
.reminder-body { flex: 1; min-width: 0; }
.reminder-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reminder-meta {
  display: block;
  font-size: 11px;
  color: #94A3B8;
  margin-top: 2px;
}
.status-text {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 9999px;
}
.status-text.status-blue { background: #DBEAFE; color: #2563EB; }
.status-text.status-warn { background: #FEF3C7; color: #D97706; }
.status-text.status-danger { background: #FEE2E2; color: #DC2626; }
</style>
