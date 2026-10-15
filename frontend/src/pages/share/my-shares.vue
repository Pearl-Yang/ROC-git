<template>
  <view class="page">
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:chevron-left" :size="20" color="#475569" />
        </view>
        <text class="topbar-title">我的分享</text>
        <view style="width:36px;"></view>
      </view>
    </view>
    <scroll-view scroll-y class="content">
      <view v-if="loading" class="state">
        <text class="fs-12 text-faint">加载中...</text>
      </view>
      <view v-else-if="!shares.length" class="state">
        <RocIcon name="lucide:link-2" :size="36" color="#CBD5E1" />
        <text class="fs-13 text-muted" style="display:block; margin-top:8px;">暂无分享链接</text>
        <text class="fs-11 text-faint" style="display:block; margin-top:4px;">去项目页归档并分享后，链接会出现在这里</text>
      </view>
      <view v-for="s in shares" :key="s.shareId" class="card-flat share-card">
        <view class="row" style="padding:0; justify-content:space-between;">
          <text class="fs-13 fw-700 text-dark">#{{ s.shareId }} {{ s.projectName || ('项目 ' + s.projectId) }}</text>
          <text class="fs-10 text-blue fw-600">{{ permissionLabel(s.permissionLevel) }}</text>
        </view>
        <text class="fs-11 text-muted share-url" selectable>{{ s.shareUrl }}</text>
        <view class="row" style="padding:0; gap:6px; margin-top:8px;">
          <text class="btn-mini" @tap="copy(s.shareUrl)">复制</text>
          <text class="btn-mini btn-mini-danger" @tap="del(s.shareId)">删除</text>
        </view>
      </view>
      <view style="height: 24px;"></view>
    </scroll-view>
    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import RocIcon from '../../components/Icon.vue'

const safeTop = ref(20)
const shares = ref([])
const loading = ref(false)
const toast = ref({ show: false, text: '' })

onShow(() => load())

async function load() {
  loading.value = true
  try {
    const list = await api.shareMyShares()
    shares.value = Array.isArray(list) ? list : (list && list.list) || []
  } catch (e) {
    shares.value = []
  } finally {
    loading.value = false
  }
}

function permissionLabel(level) {
  return ({ view_only: '仅查看', view_with_comment: '可评论', view_with_download: '可下载' })[level] || (level || '未知')
}

function copy(url) {
  if (!url) { showToast('链接为空'); return }
  uni.setClipboardData({ data: url, success: () => showToast('已复制') })
}

async function del(id) {
  uni.showModal({
    title: '删除分享',
    content: '删除后访客将无法访问，确定继续？',
    confirmText: '删除',
    confirmColor: '#DC2626',
    success: async ({ confirm }) => {
      if (!confirm) return
      try {
        await api.shareDelete(id)
        showToast('已删除')
        await load()
      } catch (e) { showToast(e.message || '删除失败') }
    }
  })
}

function goBack() { uni.navigateBack() }
function showToast(text) { toast.value = { show: true, text }; setTimeout(() => { toast.value.show = false }, 1500) }
</script>

<style scoped>
.page { background: var(--bg-system); min-height: 100vh; }
.content { padding: 12px 20px 0; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; display: flex; align-items: center; justify-content: space-between; }
.topbar-title { font-size: 16px; font-weight: 600; color: #0F172A; flex: 1; text-align: center; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }

.state { padding: 80px 0; display: flex; flex-direction: column; align-items: center; }
.share-card { padding: 12px; margin-bottom: 10px; }
.share-url { display: block; margin-top: 6px; word-break: break-all; }

.btn-mini { padding: 4px 10px; background: #2563EB; color: #FFFFFF; font-size: 11px; font-weight: 600; border-radius: 6px; }
.btn-mini-danger { background: #FEE2E2; color: #DC2626; }

.row { display: flex; align-items: center; gap: 12px; }
.fw-600 { font-weight: 600; }
.fw-700 { font-weight: 700; }
.fs-10 { font-size: 10px; } .fs-11 { font-size: 11px; } .fs-12 { font-size: 12px; } .fs-13 { font-size: 13px; }
.text-dark { color: #0F172A; }
.text-muted { color: #64748B; }
.text-faint { color: #94A3B8; }
.text-blue { color: #2563EB; }
.card-flat { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; box-shadow: 0 1px 2px rgba(15,23,42,0.04); }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
</style>
