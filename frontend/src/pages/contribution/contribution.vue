<template>
  <view class="page">
    <!-- 顶部 -->
    <view class="topbar">
      <view class="icon-btn" @tap="goBack">
        <Icon name="lucide:chevron-left" :width="20" :height="20" color="#0F172A" />
      </view>
      <view class="topbar-center">
        <text class="topbar-title">贡献度排行</text>
        <text class="topbar-sub">{{ groupName || '我的小组' }} · 实时统计</text>
      </view>
      <view class="icon-btn" @tap="loadData">
        <Icon name="lucide:refresh-cw" :width="18" :height="18" color="#475569" />
      </view>
    </view>

    <view v-if="loading" class="state">
      <text>加载中…</text>
    </view>
    <view v-else-if="!list.length" class="state">
      <view class="empty-art">
        <Icon name="lucide:bar-chart-3" :width="36" :height="36" color="#94A3B8" />
      </view>
      <text>暂无贡献数据</text>
    </view>

    <view v-else class="content">
      <!-- 头三甲 -->
      <view v-if="top3.length >= 3" class="podium">
        <view class="podium-card silver">
          <view class="podium-rank">
            <text>2</text>
          </view>
          <view class="podium-avatar" :style="{ background: avatarColor(top3[1].nickname) }">
            <text>{{ firstChar(top3[1].nickname) }}</text>
          </view>
          <text class="podium-name">{{ top3[1].nickname }}</text>
          <text class="podium-score">{{ top3[1].contributions || 0 }} 分</text>
          <view class="podium-bar">
            <text>2</text>
          </view>
        </view>
        <view class="podium-card gold">
          <view class="podium-rank">
            <Icon name="lucide:crown" :width="14" :height="14" color="#fff" />
            <text>1</text>
          </view>
          <view class="podium-avatar" :style="{ background: avatarColor(top3[0].nickname) }">
            <text>{{ firstChar(top3[0].nickname) }}</text>
          </view>
          <text class="podium-name">{{ top3[0].nickname }}</text>
          <text class="podium-score">{{ top3[0].contributions || 0 }} 分</text>
          <view class="podium-bar">
            <text>1</text>
          </view>
        </view>
        <view class="podium-card bronze">
          <view class="podium-rank">
            <text>3</text>
          </view>
          <view class="podium-avatar" :style="{ background: avatarColor(top3[2].nickname) }">
            <text>{{ firstChar(top3[2].nickname) }}</text>
          </view>
          <text class="podium-name">{{ top3[2].nickname }}</text>
          <text class="podium-score">{{ top3[2].contributions || 0 }} 分</text>
          <view class="podium-bar">
            <text>3</text>
          </view>
        </view>
      </view>

      <!-- 完整排行 -->
      <view class="card">
        <view class="card-head">
          <text class="card-title">完整排行</text>
          <text class="card-tip">基于上传 / 审核 / 研讨统计</text>
        </view>
        <view v-for="(item, idx) in list" :key="item.userId" class="rank-row">
          <view class="rank-num" :class="rankClass(idx)">
            <text>{{ idx + 1 }}</text>
          </view>
          <view class="rank-avatar" :style="{ background: avatarColor(item.nickname) }">
            <text>{{ firstChar(item.nickname) }}</text>
          </view>
          <view class="rank-body">
            <text class="rank-name">{{ item.nickname }}</text>
            <text class="rank-meta">{{ idx < 3 ? '核心成员' : '协作成员' }}</text>
          </view>
          <view class="rank-score">
            <text class="rank-score-num">{{ item.contributions || 0 }}</text>
            <text class="rank-score-unit">分</text>
          </view>
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
import { textAvatar } from '@/utils/avatar.js'

const loading = ref(false)
const list = ref([])
const groupName = ref('')

const top3 = computed(() => list.value.slice(0, 3))

function firstChar(name) {
  if (!name) return 'U'
  return String(name).slice(0, 1)
}
function avatarColor(name) {
  return textAvatar(name).color
}
function rankClass(idx) {
  if (idx === 0) return 'rank-1'
  if (idx === 1) return 'rank-2'
  if (idx === 2) return 'rank-3'
  return ''
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/home/home' })
}

async function loadData() {
  loading.value = true
  try {
    const me = await api.request({ url: '/v1/user/profile' })
    const groupId = me && me.groupId
    if (groupId) {
      const arr = await api.request({ url: '/v1/group/' + groupId + '/member-stats' })
      list.value = Array.isArray(arr) ? arr : []
    } else {
      list.value = []
    }
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

onLoad(() => {
  loadData()
})
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
.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #F1F5F9;
}
.topbar-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.topbar-title {
  font-size: 15px;
  font-weight: 700;
  color: #0F172A;
}
.topbar-sub {
  font-size: 11px;
  color: #94A3B8;
  margin-top: 2px;
}

.state {
  text-align: center;
  padding: 80px 16px;
  color: #94A3B8;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.empty-art {
  width: 64px;
  height: 64px;
  background: #F1F5F9;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content {
  padding: 16px;
}

/* 领奖台 */
.podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  margin-bottom: 18px;
}
.podium-card {
  flex: 1;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 14px 10px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.podium-card.gold {
  background: linear-gradient(180deg, #FEF3C7 0%, #FFFFFF 50%);
  border-color: #FCD34D;
  transform: translateY(-8px);
}
.podium-card.silver {
  background: linear-gradient(180deg, #F1F5F9 0%, #FFFFFF 60%);
}
.podium-card.bronze {
  background: linear-gradient(180deg, #FED7AA 0%, #FFFFFF 60%);
  border-color: #FED7AA;
}
.podium-rank {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F1F5F9;
  font-size: 11px;
  font-weight: 700;
  color: #475569;
}
.podium-card.gold .podium-rank {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: #fff;
}
.podium-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #FFFFFF;
  margin-top: 6px;
  border: 2px solid #FFFFFF;
  box-shadow: 0 2px 4px rgba(15, 23, 42, 0.08);
}
.podium-name {
  font-size: 12px;
  font-weight: 700;
  color: #0F172A;
  margin-top: 8px;
  text-align: center;
  max-width: 90%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.podium-score {
  font-size: 11px;
  color: #64748B;
  margin-top: 2px;
}
.podium-bar {
  margin-top: 10px;
  width: 100%;
  height: 36px;
  background: #F1F5F9;
  border-radius: 8px 8px 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  color: #94A3B8;
}
.podium-card.gold .podium-bar {
  background: linear-gradient(180deg, #FBBF24, #F59E0B);
  color: #FFFFFF;
}

/* 完整排行 */
.card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  overflow: hidden;
}
.card-head {
  padding: 14px 16px;
  border-bottom: 1px solid #F1F5F9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}
.card-tip {
  font-size: 11px;
  color: #94A3B8;
}
.rank-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 12px;
  border-bottom: 1px solid #F1F5F9;
}
.rank-row:last-child { border-bottom: none; }
.rank-num {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #F1F5F9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #64748B;
}
.rank-num.rank-1 { background: linear-gradient(135deg, #F59E0B, #D97706); color: #fff; }
.rank-num.rank-2 { background: linear-gradient(135deg, #94A3B8, #64748B); color: #fff; }
.rank-num.rank-3 { background: linear-gradient(135deg, #FB923C, #C2410C); color: #fff; }
.rank-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #FFFFFF;
}
.rank-body { flex: 1; min-width: 0; }
.rank-name {
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
  display: block;
}
.rank-meta {
  font-size: 11px;
  color: #94A3B8;
  margin-top: 2px;
  display: block;
}
.rank-score {
  text-align: right;
  display: flex;
  align-items: baseline;
  gap: 2px;
}
.rank-score-num {
  font-size: 16px;
  font-weight: 800;
  color: #2563EB;
}
.rank-score-unit {
  font-size: 11px;
  color: #94A3B8;
}
</style>
