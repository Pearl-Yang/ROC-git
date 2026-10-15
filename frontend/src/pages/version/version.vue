<template>
  <view class="page">
    <view class="topbar">
      <view class="icon-btn" @tap="goBack">
        <Icon name="lucide:chevron-left" :width="20" :height="20" color="#0F172A" />
      </view>
      <view class="topbar-center">
        <text class="topbar-title">版本历史</text>
        <text class="topbar-sub" v-if="fileName">{{ fileName }} · #{{ fileId }}</text>
      </view>
      <view class="icon-btn" @tap="loadVersions">
        <Icon name="lucide:refresh-cw" :width="18" :height="18" color="#475569" />
      </view>
    </view>

    <view class="toolbar">
      <view class="tool-btn" :class="{ active: compareMode }" @tap="compareMode = !compareMode">
        <Icon :name="compareMode ? 'lucide:x' : 'lucide:git-compare'" :width="14" :height="14" :color="compareMode ? '#fff' : '#475569'" />
        <text>{{ compareMode ? '退出对比' : '对比模式' }}</text>
      </view>
    </view>

    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="!versions.length" class="state">暂无版本记录</view>

    <view v-else class="list">
      <view v-for="(v, i) in versions" :key="v.versionId" class="version-card"
        :class="{ selected: selected.includes(v.versionId), latest: i === 0 }">
        <view class="version-row" @tap="onTapVersion(v)">
          <view class="version-radio" v-if="compareMode">
            <view v-if="selected.includes(v.versionId)" class="version-radio-dot"></view>
          </view>
          <view class="version-body">
            <view class="version-top">
              <text class="version-num">v{{ v.versionNumber }}</text>
              <text v-if="i === 0" class="version-tag">最新</text>
              <text v-else-if="v.remark && v.remark.indexOf('回滚') === 0" class="version-tag rollback">回滚</text>
            </view>
            <text class="version-remark" v-if="v.remark">{{ v.remark }}</text>
            <text class="version-time">{{ formatTime(v.createdAt) }}</text>
          </view>
        </view>
        <view class="version-ops" v-if="!compareMode && i !== 0">
          <text class="op-btn" @tap.stop="onRollback(v)">回滚到该版本</text>
        </view>
      </view>
    </view>

    <!-- 对比结果面板 -->
    <view v-if="compareMode && selected.length === 2" class="compare-bar">
      <text class="compare-tip">已选 {{ selected.length }} 个版本</text>
      <view class="btn-primary small" @tap="onCompare"><text>开始对比</text></view>
    </view>

    <!-- 对比结果 -->
    <view v-if="compareResult" class="compare-result">
      <view class="cr-head">
        <text class="cr-title">v{{ compareResult.fromVersion }} → v{{ compareResult.toVersion }}</text>
        <text class="cr-meta" v-if="!compareResult.error">
          +{{ compareResult.added || compareResult.additions || 0 }} 行
          / -{{ compareResult.removed || compareResult.deletions || 0 }} 行
        </text>
        <text class="cr-meta" v-else style="color:#DC2626">{{ compareResult.error }}</text>
      </view>
      <view v-if="compareResult.diffLines && compareResult.diffLines.length" class="diff-list">
        <view v-for="(d, k) in compareResult.diffLines" :key="k" class="diff-line" :class="'diff-' + d.type">
          <text class="diff-mark">{{ d.type === 'add' ? '+' : (d.type === 'del' ? '-' : ' ') }}</text>
          <text class="diff-content">{{ d.content }}</text>
        </view>
      </view>
      <view v-else class="cr-empty">无差异内容</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '@/utils/api.js'
import Icon from '@/components/Icon.vue'

const fileId = ref(0)
const fileName = ref('')
const loading = ref(true)
const versions = ref([])
const compareMode = ref(false)
const selected = ref([])
const compareResult = ref(null)

onLoad((opts) => {
  fileId.value = Number(opts.fileId || 0)
  fileName.value = opts.fileName || ''
  if (!fileId.value) {
    uni.showToast({ title: '缺少 fileId', icon: 'none' })
    return
  }
  loadVersions()
})

onMounted(() => {})

async function loadVersions() {
  loading.value = true
  compareResult.value = null
  selected.value = []
  try {
    versions.value = await api.fileVersions(fileId.value)
  } catch (e) {
    versions.value = []
  } finally {
    loading.value = false
  }
}

function onTapVersion(v) {
  if (!compareMode.value) return
  const i = selected.value.indexOf(v.versionId)
  if (i >= 0) selected.value.splice(i, 1)
  else if (selected.value.length < 2) selected.value.push(v.versionId)
  else selected.value = [selected.value[1], v.versionId]
}

async function onRollback(v) {
  const ok = await new Promise((resolve) => {
    uni.showModal({
      title: '回滚确认',
      content: `将基于「${fileName.value}」v${v.versionNumber} 创建一个新的回滚版本，确认？`,
      editable: true,
      placeholderText: '可选：填写回滚说明',
      success: (r) => resolve(r.confirm ? r : false)
    })
  })
  if (!ok) return
  try {
    await api.fileVersionRollback(fileId.value, v.versionNumber, ok.content || '')
    uni.showToast({ title: '已回滚', icon: 'success' })
    loadVersions()
  } catch (e) {}
}

async function onCompare() {
  if (selected.value.length !== 2) return
  const [aId, bId] = selected.value
  const a = versions.value.find((x) => x.versionId === aId)
  const b = versions.value.find((x) => x.versionId === bId)
  if (!a || !b) return
  // from = 较早的（createdAt 小），to = 较晚的
  const from = (a.createdAt && b.createdAt && a.createdAt > b.createdAt) ? b : a
  const to = (a.createdAt && b.createdAt && a.createdAt > b.createdAt) ? a : b
  try {
    compareResult.value = await api.fileVersionCompare(fileId.value, from.versionNumber, to.versionNumber)
  } catch (e) {
    compareResult.value = { error: (e && e.message) || '对比失败' }
  }
}

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 19)
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/home/home' })
}
</script>

<style scoped>
/* ============================================================
   iOS 26 · Liquid Glass · Version History
   ============================================================ */

.page {
  min-height: 100vh;
  min-height: 100dvh;
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
.topbar-sub { font-size: 11px; color: #94A3B8; margin-top: 2px; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: #F1F5F9;
}

.toolbar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1px solid #E2E8F0;
  border-radius: 9999px;
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  background: #FFFFFF;
}
.tool-btn.active {
  background: #2563EB;
  color: #fff;
  border-color: #2563EB;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.20);
}

.state {
  text-align: center;
  padding: 60px 16px;
  color: #64748B;
  font-size: 14px;
}

.list {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.version-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 16px;
  padding: 14px 16px;
}
.version-card.selected {
  border-color: #2563EB;
  background: rgba(94, 158, 255, 0.12);
  box-shadow: 0 0 0 1px #2563EB;
}
.version-card.latest {
  border-left: 4px solid #10B981;
}

.version-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.version-radio {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.40);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.version-card.selected .version-radio { border-color: #2563EB; }
.version-radio-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #2563EB;
}

.version-body { flex: 1; }

.version-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.version-num {
  font-size: 16px;
  font-weight: 800;
  color: #0F172A;
  letter-spacing: -0.01em;
}
.version-tag {
  font-size: 10px;
  padding: 2px 8px;
  background: rgba(52, 211, 153, 0.18);
  color: #059669;
  border-radius: 9999px;
  font-weight: 700;
}
.version-tag.rollback {
  background: rgba(252, 211, 77, 0.18);
  color: #D97706;
}

.version-remark {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #475569;
}
.version-time {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: #64748B;
}

.version-ops {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
.op-btn {
  font-size: 12px;
  color: #2563EB;
  padding: 4px 12px;
  background: rgba(94, 158, 255, 0.12);
  border-radius: 9999px;
  border: 1px solid rgba(94, 158, 255, 0.25);
  font-weight: 600;
}

.compare-bar {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #FFFFFF;
  border-top: 1px solid #E2E8F0;
}
.compare-tip { font-size: 13px; color: #0F172A; font-weight: 600; }
.btn-primary.small {
  padding: 8px 18px;
  background: #2563EB;
  color: #fff;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.20);
}
.btn-primary.small::after { border: none; }

.compare-result {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  margin: 12px 16px;
  border-radius: 16px;
  padding: 14px 16px;
}

.cr-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #E2E8F0;
}
.cr-title {
  font-size: 14px;
  font-weight: 700;
  color: #0F172A;
}
.cr-meta {
  font-size: 12px;
  color: #475569;
}

.cr-empty {
  padding: 20px 0;
  text-align: center;
  color: #64748B;
  font-size: 13px;
}

.diff-list {
  max-height: 500rpx;
  overflow-y: auto;
}

.diff-line {
  display: flex;
  font-family: ui-monospace, Menlo, Monaco, monospace;
  font-size: 12px;
  padding: 3px 6px;
  border-radius: 4px;
  margin-top: 1px;
}
.diff-add {
  background: rgba(52, 211, 153, 0.18);
  color: #059669;
}
.diff-del {
  background: rgba(248, 113, 113, 0.18);
  color: #DC2626;
}
.diff-same { color: #475569; }

.diff-mark {
  width: 18px;
  flex-shrink: 0;
  text-align: center;
  font-weight: 700;
}
.diff-content {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
