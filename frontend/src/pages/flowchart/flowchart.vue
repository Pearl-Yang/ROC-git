<template>
  <view class="page">
    <!-- 顶部 -->
    <view class="topbar">
      <view class="icon-btn" @tap="goBack">
        <Icon name="lucide:chevron-left" :width="20" :height="20" color="#0F172A" />
      </view>
      <view class="topbar-center">
        <text class="topbar-title">系统流程图</text>
        <text class="topbar-sub">点击放大 · 左右滑动浏览</text>
      </view>
      <view class="icon-btn" @tap="load">
        <Icon name="lucide:refresh-cw" :width="18" :height="18" color="#475569" />
      </view>
    </view>

    <!-- 紧急提示 -->
    <view class="banner-tip">
      <Icon name="lucide:lightbulb" :width="14" :height="14" color="#D97706" />
      <text>点击任意流程图可全屏查看，向左滑动浏览更多</text>
    </view>

    <view v-if="loading && !list.length" class="state">
      <text>加载中…</text>
    </view>
    <view v-else-if="!list.length" class="state">
      <view class="empty-art">
        <Icon name="lucide:image-off" :width="32" :height="32" color="#94A3B8" />
      </view>
      <text>暂无流程图</text>
    </view>

    <view v-else class="list">
      <view v-for="f in list" :key="f.no" class="flow-card" @tap="openFlow(f)">
        <view class="flow-img-wrap">
          <image :src="fullUrl(f.url)" class="flow-img" mode="aspectFit" @click.stop="openFlow(f)" />
        </view>
        <view class="flow-meta">
          <view class="flow-tag">
            <text>{{ f.no }}</text>
          </view>
          <text class="flow-name">{{ f.name }}</text>
          <view class="flow-cta">
            <text>点击放大</text>
            <Icon name="lucide:chevron-right" :width="12" :height="12" color="#94A3B8" />
          </view>
        </view>
      </view>
    </view>

    <!-- 全屏查看 -->
    <view v-if="viewer.show" class="viewer-mask" @tap="closeViewer">
      <view class="viewer-toolbar">
        <text class="viewer-title">{{ viewer.name }}</text>
        <view class="viewer-close" @tap="closeViewer">
          <Icon name="lucide:x" :width="20" :height="20" color="#FFFFFF" />
        </view>
      </view>
      <scroll-view scroll-y class="viewer-scroll">
        <image :src="fullUrl(viewer.url)" class="viewer-img" mode="widthFix" />
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '@/utils/api.js'
import Icon from '@/components/Icon.vue'

const list = ref([])
const loading = ref(false)
const viewer = reactive({ show: false, name: '', url: '' })

onLoad(() => { load() })

async function load() {
  loading.value = true
  try {
    const r = await api.flowcharts()
    list.value = Array.isArray(r) ? r : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function fullUrl(u) {
  if (!u) return ''
  if (u.startsWith('http')) return u
  const base = (typeof window !== 'undefined' && window.location) ? window.location.origin : ''
  return base + u
}

function openFlow(f) {
  viewer.show = true
  viewer.name = f.no + ' · ' + f.name
  viewer.url = f.url
}

function closeViewer() { viewer.show = false }

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

.banner-tip {
  margin: 12px 16px 0;
  padding: 10px 12px;
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #92400E;
  font-weight: 600;
}

.state {
  text-align: center;
  padding: 60px 16px;
  color: #94A3B8;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
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

.list {
  padding: 12px 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.flow-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  overflow: hidden;
}
.flow-img-wrap {
  background: #F8FAFC;
  padding: 12px;
  border-bottom: 1px solid #F1F5F9;
}
.flow-img {
  width: 100%;
  height: 220px;
}
.flow-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
}
.flow-tag {
  background: #DBEAFE;
  color: #2563EB;
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  flex-shrink: 0;
}
.flow-name {
  flex: 1;
  font-size: 14px;
  color: #0F172A;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-cta {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: #94A3B8;
  font-weight: 600;
  flex-shrink: 0;
}

.viewer-mask {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.92);
  z-index: 999;
  display: flex;
  flex-direction: column;
}
.viewer-toolbar {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #FFFFFF;
}
.viewer-title { font-size: 14px; font-weight: 600; flex: 1; }
.viewer-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
}
.viewer-scroll { flex: 1; }
.viewer-img { width: 100%; }
</style>
