<template>
  <view class="page">
    <view class="topbar">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <Icon name="lucide:chevron-left" :width="20" :height="20" color="#0F172A" />
        </view>
        <text class="topbar-title">访客视图</text>
        <view class="topbar-meta">
          <text>本视图由分享链接进入，按权限级别裁剪展示内容</text>
        </view>
      </view>
    </view>

    <view v-if="needPassword" class="pwd-card">
      <text class="pwd-title">该分享需要密码</text>
      <input v-model="passwordInput" placeholder="请输入密码" password class="pwd-input" maxlength="32" />
      <button class="btn-primary" @tap="loadView">验证</button>
    </view>

    <view v-else-if="loading" class="primary-row"><text class="primary-text">加载中...</text></view>

    <scroll-view v-else-if="data && data.project" scroll-y class="content">
      <!-- 分享来源 banner -->
      <view class="share-banner">
        <view class="share-banner-icon">
          <Icon name="lucide:link" :width="14" :height="14" color="#FFFFFF" />
        </view>
        <view class="share-banner-body">
          <text class="share-banner-title">通过分享链接访问</text>
          <text class="share-banner-tip">项目所有者：{{ data.project.ownerName || '林知行' }} · 仅展示已归档内容</text>
        </view>
        <view class="share-banner-perm">
          <text class="share-banner-perm-text">{{ permLabel(data.permissionLevel) }}</text>
        </view>
      </view>

      <!-- 项目信息 -->
      <view class="card">
        <view class="proj-row">
          <text class="proj-name">{{ data.project.name }}</text>
          <text class="proj-stage">{{ data.project.currentStage || '' }}</text>
        </view>
        <text v-if="data.project.description" class="proj-desc">{{ data.project.description }}</text>
        <view class="progress-row">
          <text class="progress-label">进度</text>
          <text class="progress-value">{{ data.project.progress || 0 }}%</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: (data.project.progress || 0) + '%' }"></view>
        </view>
        <view class="perm-tip">
          <text class="perm-tip-text">当前权限：{{ permLabel(data.permissionLevel) }}</text>
        </view>
      </view>

      <!-- 阶段 -->
      <view class="card">
        <text class="card-title">项目阶段</text>
        <view v-if="!data.stages || !data.stages.length" class="empty"><text class="empty-text">暂无阶段</text></view>
        <view v-for="s in data.stages" :key="s.id" class="stage-row">
          <view class="stage-dot" :class="dotClass(s.status)"></view>
          <view class="stage-body">
            <text class="stage-name">第 {{ s.orderNum }} 阶段 · {{ s.name }}</text>
            <text class="stage-time" v-if="s.startDate || s.endDate">{{ s.startDate || '' }} ~ {{ s.endDate || '' }}</text>
          </view>
        </view>
      </view>

      <!-- 文件（仅 preview/download 权限才显示） -->
      <view v-if="data.files" class="card">
        <text class="card-title">项目文件</text>
        <view v-if="!data.files.length" class="empty"><text class="empty-text">暂无文件</text></view>
        <view v-for="f in data.files" :key="f.fileId" class="file-row">
          <view class="file-icon"><text class="file-icon-text">{{ fileExt(f.name) }}</text></view>
          <view class="file-body">
            <text class="file-name">{{ f.name }}</text>
            <text class="file-meta">{{ f.type || '' }} · {{ formatSize(f.size) }} · v{{ f.version || 1 }}</text>
          </view>
          <view v-if="f.url" class="file-action" @tap="openUrl(f.url)">
            <text class="file-action-text">下载</text>
          </view>
          <view v-else class="file-action disabled">
            <text class="file-action-text">仅预览</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view v-else class="primary-row">
      <text class="primary-text">{{ errorMsg || '无效的分享链接' }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '@/utils/api.js'
import Icon from '@/components/Icon.vue'

const shareToken = ref('')
const passwordInput = ref('')
const data = ref(null)
const loading = ref(false)
const needPassword = ref(false)
const errorMsg = ref('')

const permOptions = [
  { value: 'view_only',         label: '仅查看' },
  { value: 'view_with_preview', label: '可预览' },
  { value: 'view_with_download', label: '可下载' }
]

const permLabel = (v) => {
  const o = permOptions.find(x => x.value === v)
  return o ? o.label : (v || '')
}
const dotClass = (s) => {
  if (s === 'completed') return 'dot-done'
  if (s === 'active') return 'dot-active'
  return 'dot-pending'
}
const fileExt = (n) => {
  if (!n) return 'FILE'
  const i = n.lastIndexOf('.')
  return i < 0 ? 'FILE' : n.substring(i + 1).toUpperCase().substring(0, 4)
}
const formatSize = (b) => {
  if (!b) return ''
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
}

async function fetchView() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.shareVisitorView(shareToken.value, passwordInput.value || undefined)
    data.value = res
    needPassword.value = false
    // 记录访问（失败不影响主流程）
    try { await api.shareRecordVisit(shareToken.value) } catch (e) { /* ignore */ }
  } catch (e) {
    const msg = (e && e.message) || '加载失败'
    if (msg.indexOf('密码') >= 0 || msg.indexOf('401') >= 0 || msg.indexOf('403') >= 0) {
      needPassword.value = true
    } else {
      errorMsg.value = msg
    }
    data.value = null
  } finally {
    loading.value = false
  }
}

function loadView() {
  if (!shareToken.value) {
    errorMsg.value = '分享 token 缺失'
    return
  }
  fetchView()
}

function openUrl(url) {
  if (!url) return
  // H5 端直接 window.open；其它端用 downloadFile
  // #ifdef H5
  window.open(url, '_blank')
  // #endif
  // #ifndef H5
  uni.setClipboardData({ data: url, success: () => uni.showToast({ title: '已复制链接', icon: 'none' }) })
  // #endif
}

onLoad((options) => {
  if (options && options.token) shareToken.value = options.token
  loadView()
})
</script>

<style scoped>
/* ============================================================
   iOS 26 · Liquid Glass · Visitor View
   ============================================================ */

.page {
  min-height: 100vh;
  min-height: 100dvh;
  background: #F8FAFC;
}

.topbar {
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
}

.topbar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.topbar-title {
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
  flex: 1;
  text-align: center;
}

.topbar-meta {
  padding: 0 16px 12px;
}

.topbar-meta text {
  font-size: 11px;
  color: #94A3B8;
  display: block;
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

.pwd-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  margin: 16px;
  padding: 18px 16px;
  border-radius: 16px;
}

.pwd-title {
  font-size: 14px;
  color: #0F172A;
  display: block;
  margin-bottom: 12px;
  font-weight: 600;
}

.pwd-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  font-size: 14px;
  color: #0F172A;
  outline: none;
  margin-bottom: 12px;
}
.pwd-input:focus { border-color: #2563EB; }
.pwd-input::placeholder { color: #64748B; }

.btn-primary {
  background: #2563EB;
  color: #fff;
  border-radius: 10px;
  height: 48px;
  line-height: 48px;
  font-size: 15px;
  font-weight: 700;
  border: none;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.20);
}
.btn-primary[disabled] { opacity: 0.5; }
.btn-primary::after { border: none; }

.primary-row {
  padding: 24px;
  text-align: center;
}

.primary-text {
  font-size: 13px;
  color: #64748B;
}

.content {
  flex: 1;
  padding: 14px 16px 100px;
}

/* 分享来源 banner */
.share-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #1E293B, #0F172A);
  border-radius: 14px;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.15);
}
.share-banner-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.share-banner-body { flex: 1; min-width: 0; }
.share-banner-title {
  font-size: 12.5px;
  color: #FFFFFF;
  font-weight: 600;
  display: block;
}
.share-banner-tip {
  font-size: 10.5px;
  color: #94A3B8;
  margin-top: 2px;
  display: block;
}
.share-banner-perm {
  background: rgba(245, 158, 11, 0.9);
  padding: 4px 10px;
  border-radius: 999px;
  flex-shrink: 0;
}
.share-banner-perm-text {
  font-size: 10px;
  font-weight: 700;
  color: #FFFFFF;
}

.card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
  display: block;
  margin-bottom: 12px;
}

.empty {
  padding: 24px 0;
  text-align: center;
}
.empty-text {
  font-size: 13px;
  color: #64748B;
}

.proj-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  gap: 8px;
}
.proj-name {
  font-size: 18px;
  font-weight: 800;
  color: #0F172A;
  letter-spacing: -0.02em;
}
.proj-stage {
  font-size: 12px;
  color: #2563EB;
  background: rgba(94, 158, 255, 0.15);
  padding: 3px 10px;
  border-radius: 9999px;
  border: 1px solid rgba(94, 158, 255, 0.30);
  font-weight: 600;
  flex-shrink: 0;
}
.proj-desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  display: block;
  margin-bottom: 12px;
}

.progress-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.progress-label { font-size: 12px; color: #64748B; }
.progress-value {
  font-size: 13px;
  color: #2563EB;
  font-weight: 700;
}
.progress-bar {
  height: 5px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #2563EB;
  border-radius: 3px;
}

.perm-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(94, 158, 255, 0.10);
  border-radius: 10px;
  border: 1px solid rgba(94, 158, 255, 0.20);
}
.perm-tip-text {
  font-size: 12px;
  color: #475569;
}

.stage-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #E2E8F0;
}
.stage-row:last-child { border-bottom: none; }

.stage-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.20);
  margin-right: 12px;
  flex-shrink: 0;
}
.dot-active {
  background: #2563EB;
  box-shadow: 0 0 0 4px rgba(94, 158, 255, 0.20);
}
.dot-done { background: #10B981; }

.stage-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.stage-name {
  font-size: 14px;
  color: #0F172A;
  font-weight: 600;
  margin-bottom: 2px;
}
.stage-time {
  font-size: 12px;
  color: #64748B;
}

.file-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #E2E8F0;
}
.file-row:last-child { border-bottom: none; }

.file-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: rgba(94, 158, 255, 0.15);
  border: 1px solid rgba(94, 158, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}
.file-icon-text {
  font-size: 10px;
  color: #2563EB;
  font-weight: 800;
}

.file-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.file-name {
  font-size: 14px;
  color: #0F172A;
  font-weight: 600;
}
.file-meta {
  font-size: 11px;
  color: #64748B;
  margin-top: 2px;
}

.file-action {
  background: #2563EB;
  padding: 7px 14px;
  border-radius: 9999px;
  margin-left: 8px;
  border: none;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.20);
}
.file-action::after { border: none; }
.file-action.disabled {
  background: rgba(255, 255, 255, 0.10);
  border: 1px solid #E2E8F0;
  box-shadow: none;
}

.file-action-text {
  font-size: 12px;
  color: #fff;
  font-weight: 600;
}
.file-action.disabled .file-action-text {
  color: #64748B;
}
</style>