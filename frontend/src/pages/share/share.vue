<template>
  <view class="page share-page">
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:chevron-left" :size="20" color="#475569" />
        </view>
        <view class="topbar-center">
          <text class="topbar-title">分享项目</text>
          <text class="topbar-sub">归档后可分享完整过程资料</text>
        </view>
        <view class="icon-btn" @tap="showPreview = true">
          <RocIcon name="lucide:eye" :size="18" color="#475569" />
        </view>
      </view>
      <view class="my-shares-bar" @tap="goMyShares">
        <RocIcon name="lucide:list" :size="14" color="#2563EB" />
        <text class="fs-11 text-blue fw-600" style="margin-left:6px;">查看我的分享</text>
        <RocIcon name="lucide:chevron-right" :size="12" color="#94A3B8" style="margin-left:auto;" />
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <!-- 访客预览 -->
      <view class="pane">
        <view class="row" style="padding:0; gap:6px; margin-bottom:8px;">
          <view style="width:4px; height:14px; background:#F59E0B; border-radius:2px;"></view>
          <text class="section-title">访客预览</text>
          <text class="fs-10 text-faint">实时更新</text>
        </view>
        <view class="preview-card">
          <view class="preview-banner">
            <view class="banner-pattern"></view>
            <view class="preview-badge preview-badge-on">
              <RocIcon :name="permissionIcon" :size="9" color="#FFFFFF" />
              <text class="preview-badge-text">{{ permissionLabel }}</text>
            </view>
            <view class="preview-guest-badge">
              <RocIcon name="lucide:user" :size="9" color="#FFFFFF" />
              <text class="preview-badge-text">访客视角</text>
            </view>
          </view>
          <view class="preview-body">
            <view class="preview-avatar">社</view>
            <view class="row" style="padding:0; justify-content:space-between; margin-top:6px;">
              <text class="fs-15 fw-700 text-dark">{{ projectName }}</text>
              <view class="status-pill status-pill-amber"><text>已结项</text></view>
            </view>
            <text class="fs-11 text-muted" style="display:block; margin-top:2px;">{{ course }} · 9月01日 - 9月20日</text>
            <view class="row" style="padding:0; gap:6px; margin-top:6px;">
              <view class="avatar-stack">
                <view v-for="(p, pi) in previewMembers" :key="pi" class="avatar avatar-sm stack-item" :style="{ background: p.color, fontSize: '8px' }">{{ p.char }}</view>
              </view>
              <text class="fs-10 text-muted">{{ projectOwner }} 等 {{ previewMembers.length }} 人</text>
            </view>
            <view class="stage-grid">
              <view v-for="(s, si) in stages" :key="si" :class="['stage-cell', stageCellClass(s)]">
                <RocIcon :name="stageIcon(s)" :size="14" :color="stageIconColor(s)" />
                <text :class="['fs-10 fw-600', stageTextClass(s)]" style="display:block; margin-top:2px;">{{ s.label }}</text>
              </view>
            </view>
            <view style="margin-top: 12px;">
              <text class="fs-10 fw-600 text-faint" style="text-transform:uppercase; letter-spacing:0.05em;">文件清单 · 12 份</text>
              <view v-for="(f, fi) in previewFiles" :key="fi" class="preview-file-row">
                <RocIcon :name="f.icon" :size="12" :color="f.color" />
                <text class="fs-10 text-body flex-1" style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ f.name }}</text>
                <text class="fs-10 text-faint">{{ f.size }}</text>
              </view>
              <text class="fs-10 text-blue fw-600" style="display:block; text-align:center; padding:4px 0;">查看全部 12 份 ›</text>
            </view>
            <view class="preview-foot">
              <view class="row" style="padding:0; gap:4px;">
                <RocIcon name="lucide:clock" :size="11" color="#94A3B8" />
                <text class="fs-10 text-muted">{{ expireText }}</text>
              </view>
              <view class="row" style="padding:0; gap:4px;">
                <RocIcon name="lucide:users" :size="10" color="#94A3B8" />
                <text class="fs-10 text-faint">公开链接</text>
              </view>
            </view>
            <view class="preview-actions">
              <view :class="['preview-action', permission === 'view' ? 'preview-action-off' : 'preview-action-on']">
                <RocIcon name="lucide:message-square" :size="11" :color="permission === 'view' ? '#94A3B8' : '#FFFFFF'" />
                <text :class="permission === 'view' ? 'text-faint' : 'text-white'" style="font-size:10px; font-weight:600;">评论{{ permission === 'view' ? '（不可用）' : '' }}</text>
              </view>
              <view :class="['preview-action', permission === 'view' ? 'preview-action-off' : 'preview-action-on']">
                <RocIcon name="lucide:download" :size="11" :color="permission === 'view' ? '#94A3B8' : '#FFFFFF'" />
                <text :class="permission === 'view' ? 'text-faint' : 'text-white'" style="font-size:10px; font-weight:600;">下载{{ permission === 'view' ? '（不可用）' : '' }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 权限 -->
      <view class="pane">
        <view class="row" style="padding:0; gap:6px; margin-bottom:8px;">
          <view style="width:4px; height:14px; background:#2563EB; border-radius:2px;"></view>
          <text class="section-title">访问权限</text>
          <text class="fs-10 text-faint">必选</text>
        </view>
        <view v-for="p in permissions" :key="p.key" :class="['perm-card', permission === p.key ? 'perm-on' : '']" @tap="permission = p.key">
          <view :class="['perm-icon', p.iconBg]">
            <RocIcon :name="p.icon" :size="18" :color="p.iconColor" />
          </view>
          <view class="row-body">
            <view class="row" style="padding:0; gap:6px;">
              <text class="fs-13 fw-700 text-dark">{{ p.title }}</text>
              <view v-if="p.tag" class="status-pill status-pill-emerald"><text>{{ p.tag }}</text></view>
            </view>
            <text class="fs-11 text-muted" style="display:block; margin-top:2px; line-height:1.5;">{{ p.desc }}</text>
          </view>
          <RocIcon v-if="permission === p.key" name="lucide:check-circle-2" :size="18" color="#2563EB" />
          <view v-else class="perm-radio"></view>
        </view>
      </view>

      <!-- 有效日期 -->
      <view class="pane">
        <view class="row" style="padding:0; gap:6px; margin-bottom:8px;">
          <view style="width:4px; height:14px; background:#7C3AED; border-radius:2px;"></view>
          <text class="section-title">有效日期</text>
          <text class="fs-10 text-faint">到期后链接自动失效</text>
        </view>
        <view class="expire-grid">
          <view v-for="e in expires" :key="e.key" :class="['expire-card', expire === e.key ? 'expire-on' : '']" @tap="expire = e.key">
            <RocIcon :name="e.icon" :size="16" :color="expire === e.key ? '#7C3AED' : '#64748B'" />
            <text :class="['fs-11 fw-600', expire === e.key ? 'text-violet' : 'text-dark']" style="display:block;">{{ e.label }}</text>
            <text :class="['fs-10', expire === e.key ? 'text-violet' : 'text-faint']">{{ e.tip }}</text>
          </view>
        </view>
        <view class="row" style="padding:0; gap:4px; margin-top:6px;">
          <RocIcon name="lucide:info" :size="10" color="#94A3B8" />
          <text class="fs-10 text-faint">到期前可在「我的项目 → 已归档」中续期</text>
        </view>
      </view>

      <!-- 分享范围 -->
      <view class="pane">
        <view class="row" style="padding:0; gap:6px; margin-bottom:8px;">
          <view style="width:4px; height:14px; background:#06B6D4; border-radius:2px;"></view>
          <text class="section-title">分享范围</text>
        </view>
        <view class="card-flat" style="padding:4px;">
          <view :class="['scope-row', scope === 'public' ? 'scope-on' : '']" @tap="scope = 'public'">
            <view :class="['radio', scope === 'public' ? 'radio-on' : '']"></view>
            <view class="row-body">
              <text class="fs-12 fw-600 text-dark">公开链接</text>
              <text class="fs-10 text-faint" style="display:block; margin-top:2px;">任何持有链接者可查看</text>
            </view>
            <RocIcon name="lucide:globe" :size="14" color="#94A3B8" />
          </view>
          <view :class="['scope-row', scope === 'member' ? 'scope-on' : '']" @tap="scope = 'member'">
            <view :class="['radio', scope === 'member' ? 'radio-on' : '']"></view>
            <view class="row-body">
              <text class="fs-12 fw-600 text-dark">仅项目成员</text>
              <text class="fs-10 text-faint" style="display:block; margin-top:2px;">成员可查看完整过程</text>
            </view>
            <RocIcon name="lucide:users" :size="14" color="#94A3B8" />
          </view>
        </view>
      </view>

      <!-- 包含内容 -->
      <view class="pane">
        <view class="row" style="padding:0; gap:6px; margin-bottom:8px;">
          <view style="width:4px; height:14px; background:#10B981; border-radius:2px;"></view>
          <text class="section-title">包含内容</text>
        </view>
        <view class="card-flat" style="padding:6px;">
          <view v-for="c in contents" :key="c.key" :class="['content-row', c.checked ? 'content-on' : '']" @tap="c.checked = !c.checked">
            <view :class="['checkbox', c.checked ? 'checkbox-on' : '']">
              <RocIcon v-if="c.checked" name="lucide:check" :size="10" color="#FFFFFF" />
            </view>
            <RocIcon :name="c.icon" :size="14" :color="c.iconColor" />
            <view class="row-body">
              <text class="fs-12 fw-600 text-dark">{{ c.title }}</text>
              <text class="fs-10 text-faint" style="display:block; margin-top:2px;">{{ c.tip }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 分享链接 -->
      <view class="pane">
        <view class="row" style="padding:0; gap:6px; margin-bottom:8px;">
          <view style="width:4px; height:14px; background:#64748B; border-radius:2px;"></view>
          <text class="section-title">分享链接</text>
        </view>
        <view class="link-box">
          <RocIcon name="lucide:link" :size="14" color="#94A3B8" />
          <text class="fs-11 text-muted flex-1" style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ shareUrl }}</text>
          <text class="link-copy" @tap="onCopy">复制</text>
        </view>
      </view>

      <!-- 分享至 -->
      <view class="pane">
        <view class="row" style="padding:0; gap:6px; margin-bottom:8px;">
          <view style="width:4px; height:14px; background:#10B981; border-radius:2px;"></view>
          <text class="section-title">分享至</text>
        </view>
        <view class="share-to-grid">
          <view class="share-to-cell" @tap="showToast('已发送微信')">
            <view class="share-to-icon" style="background:#10B981;"><RocIcon name="lucide:message-circle" :size="22" color="#FFFFFF" /></view>
            <text class="fs-10 text-body fw-500">微信</text>
          </view>
          <view class="share-to-cell" @tap="showToast('已生成二维码')">
            <view class="share-to-icon" style="background:#2563EB;"><RocIcon name="lucide:qr-code" :size="22" color="#FFFFFF" /></view>
            <text class="fs-10 text-body fw-500">二维码</text>
          </view>
          <view class="share-to-cell" @tap="onCopy">
            <view class="share-to-icon" style="background:#475569;"><RocIcon name="lucide:link-2" :size="22" color="#FFFFFF" /></view>
            <text class="fs-10 text-body fw-500">复制链接</text>
          </view>
          <view class="share-to-cell" @tap="showToast('已发送邮件')">
            <view class="share-to-icon" style="background:#7C3AED;"><RocIcon name="lucide:mail" :size="22" color="#FFFFFF" /></view>
            <text class="fs-10 text-body fw-500">邮件</text>
          </view>
        </view>
      </view>

      <!-- 提示 -->
      <view class="pane">
        <view class="info-banner">
          <RocIcon name="lucide:shield-check" :size="16" color="#2563EB" cls="mt-1" />
          <view style="flex:1;">
            <text class="fs-12 fw-700 text-blue" style="display:block; color:#1E3A8A;">分享安全说明</text>
            <text class="fs-10 text-blue" style="display:block; margin-top:2px; color:#1D4ED8; line-height:1.5;">结项归档后项目进入只读模式。链接到期前可随时关闭，关闭后访客将无法访问。</text>
          </view>
        </view>
      </view>

      <view style="height: 80px;"></view>
    </scroll-view>

    <!-- Bottom action bar -->
    <view class="bottom-bar">
      <view class="btn btn-ghost" style="flex:1; height:48px; line-height:48px; padding:0;" @tap="goBack">
        <text>取消</text>
      </view>
      <view class="btn btn-amber" style="flex:2; height:48px; line-height:48px; padding:0;" @tap="onConfirm">
        <RocIcon name="lucide:archive" :size="14" color="#FFFFFF" />
        <text class="text-white fw-700">归档并生成分享</text>
      </view>
    </view>

    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { textAvatar } from '../../utils/avatar.js'
import RocIcon from '../../components/Icon.vue'

const safeTop = ref(20)
const projectId = ref(1)
const projectName = ref('社区调研报告')
const course = ref('软件工程实践')
const projectOwner = ref('林知行')
const permission = ref('view_only')
const expire = ref('30d')
const scope = ref('public')
const shareUrl = ref('')
const shareToken = ref('')
const showPreview = ref(false)
const toast = ref({ show: false, text: '' })
const livePreview = ref(null)

const permissions = [
  { key: 'view_only', title: '仅查看', icon: 'lucide:eye', iconBg: 'icon-bg-emerald', iconColor: '#059669', desc: '访客可查看全部过程资料，不可下载、不可评论', tag: '推荐' },
  { key: 'view_with_comment', title: '可评论', icon: 'lucide:message-square', iconBg: 'icon-bg-blue', iconColor: '#2563EB', desc: '查看全部内容 + 可对文件添加批注评论' },
  { key: 'view_with_download', title: '可下载', icon: 'lucide:download', iconBg: 'icon-bg-amber', iconColor: '#D97706', desc: '下载文件到本地，可能造成资料外传' }
]

const expires = [
  { key: '1d', label: '1 天', tip: '短期演示', icon: 'lucide:clock-1' },
  { key: '7d', label: '7 天', tip: '评审周', icon: 'lucide:clock-3' },
  { key: '30d', label: '30 天', tip: '推荐', icon: 'lucide:calendar-days' },
  { key: 'forever', label: '永久', tip: '公开展示', icon: 'lucide:infinity' }
]

const stages = ref([])

const previewFiles = ref([])

const previewMembers = computed(() => {
  const members = (livePreview.value && livePreview.value.members) || []
  if (members.length) {
    return members.map(m => {
      const ta = textAvatar(m.nickname || m.name || '匿名')
      return { name: m.nickname || m.name, char: ta.char, color: ta.color }
    })
  }
  return []
})

const contents = ref([
  { key: 'files', title: '各阶段文件', tip: '12 份 · 含全部版本历史', icon: 'lucide:folder-open', iconColor: '#0EA5E9', checked: true },
  { key: 'discussions', title: '研讨记录', tip: '3 次 · 议题、决策、参与者', icon: 'lucide:users', iconColor: '#10B981', checked: true },
  { key: 'members', title: '操作人员', tip: '4 人 · 分工与贡献统计', icon: 'lucide:user-check', iconColor: '#7C3AED', checked: true },
  { key: 'timeline', title: '过程留痕时间线', tip: '从创建到结项的全部变更', icon: 'lucide:git-commit', iconColor: '#F43F5E', checked: false }
])

const permissionLabel = computed(() => ({ view_only: '仅查看', view_with_comment: '可评论', view_with_download: '可下载' })[permission.value])
const permissionIcon = computed(() => ({ view_only: 'lucide:eye', view_with_comment: 'lucide:message-square', view_with_download: 'lucide:download' })[permission.value] || 'lucide:eye')

const expireText = computed(() => {
  const m = { '1d': '1 天后过期', '7d': '7 天后过期', '30d': '30 天后过期', 'forever': '永久有效' }
  return m[expire.value]
})

function expireToDate(key) {
  const map = { '1d': 1, '7d': 7, '30d': 30, 'forever': 36500 }
  const days = map[key] || 30
  if (days >= 36500) return null // 永久
  return new Date(Date.now() + days * 86400_000).toISOString()
}

function stageCellClass(s) {
  if (s.status === 'done') return 'stage-cell-done'
  if (s.status === 'doing') return 'stage-cell-doing'
  return 'stage-cell-todo'
}
function stageIcon(s) {
  if (s.status === 'done') return 'lucide:check-circle-2'
  if (s.status === 'doing') return 'lucide:loader'
  return 'lucide:circle'
}
function stageIconColor(s) {
  if (s.status === 'done') return '#059669'
  if (s.status === 'doing') return '#2563EB'
  return '#94A3B8'
}
function stageTextClass(s) {
  if (s.status === 'done') return 'text-emerald'
  if (s.status === 'doing') return 'text-blue'
  return 'text-muted'
}

onLoad((q) => {
  projectId.value = Number(q && q.id) || 1
  refreshSafeTop()
  load()
})

function refreshSafeTop() { try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {} }

async function load() {
  try {
    const p = await api.projectDetail(projectId.value)
    if (p) {
      projectName.value = p.name || projectName.value
      course.value = p.course || p.description || course.value
      projectOwner.value = p.ownerName || p.creatorName || projectOwner.value
    }
  } catch (e) {}
  // 阶段列表
  try {
    const st = await api.projectStages(projectId.value)
    const list = Array.isArray(st) ? st : (st && st.list) || []
    stages.value = list.map((s, i) => ({
      status: s.status || (i < list.length - 1 ? 'done' : 'doing'),
      label: (s.name || ('阶段' + (i + 1)))
    }))
  } catch (e) { stages.value = [] }
  // 已生成的 shareToken（如有）→ 拉访客预览
  try {
    const list = await api.shareMyShares()
    const arr = Array.isArray(list) ? list : (list && list.list) || []
    const mine = arr.find(s => s.projectId === projectId.value || s.projectId === Number(projectId.value))
    if (mine && mine.shareToken) {
      shareToken.value = mine.shareToken
      shareUrl.value = mine.shareUrl || (typeof location !== 'undefined' ? location.origin : '') + '/pages/view/view?token=' + mine.shareToken
      await loadVisitorPreview()
    }
  } catch (e) {}
}

async function loadVisitorPreview() {
  if (!shareToken.value) { livePreview.value = null; return }
  try {
    livePreview.value = await api.shareVisitorView(shareToken.value)
    if (livePreview.value && Array.isArray(livePreview.value.files)) {
      previewFiles.value = livePreview.value.files.map(f => ({
        name: f.name || f.fileName,
        icon: 'lucide:file-text',
        color: '#2563EB',
        size: f.size || ''
      }))
    } else {
      previewFiles.value = []
    }
  } catch (e) { livePreview.value = null; previewFiles.value = [] }
}

function goBack() { uni.navigateBack() }
function goMyShares() { uni.navigateTo({ url: '/pages/share/my-shares' }) }
function onCopy() { if (!shareUrl.value) { showToast('请先生成分享'); return } uni.setClipboardData({ data: shareUrl.value }); showToast('链接已复制') }
function onConfirm() {
  const payload = {
    projectId: projectId.value,
    permissionLevel: permission.value,
    expiresAt: expireToDate(expire.value),
    needPassword: false,
    includeFiles: contents.value.find(c => c.key === 'files').checked,
    includeDiscussions: contents.value.find(c => c.key === 'discussions').checked,
    includeMembers: contents.value.find(c => c.key === 'members').checked,
    includeTimeline: contents.value.find(c => c.key === 'timeline').checked
  }
  uni.showLoading({ title: '生成中...' })
  api.shareCreate(payload).then((res) => {
    uni.hideLoading()
    if (res && res.shareToken) {
      shareToken.value = res.shareToken
      const origin = (typeof window !== 'undefined' && window.location && window.location.origin) || ''
      shareUrl.value = (res.shareUrl && /^https?:\/\//.test(res.shareUrl))
        ? res.shareUrl
        : (origin ? origin + '/pages/view/view?token=' + res.shareToken : res.shareUrl || ('/pages/view/view?token=' + res.shareToken))
      showToast('分享链接已生成')
      loadVisitorPreview()
    } else {
      showToast('生成分享失败')
    }
  }).catch((err) => {
    uni.hideLoading()
    showToast(err.message || '生成分享失败')
  })
}

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.share-page { background: var(--bg-system); }
.content { flex: 1; padding-bottom: 0; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; display: flex; align-items: center; justify-content: space-between; }
.topbar-center { flex: 1; text-align: center; }
.topbar-title { font-size: 16px; font-weight: 600; color: #0F172A; display: block; }
.topbar-sub { font-size: 11px; color: #94A3B8; margin-top: 2px; display: block; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }

.my-shares-bar { margin: 8px 20px 0; padding: 10px 12px; background: #EFF6FF; border: 1px solid #DBEAFE; border-radius: 10px; display: flex; align-items: center; }

.pane { padding: 16px 20px 0; }
.section-title { font-size: 13px; font-weight: 600; color: #1E293B; }

.preview-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px; overflow: hidden; box-shadow: 0 1px 2px rgba(15,23,42,0.04); }
.preview-banner { height: 80px; background: linear-gradient(135deg, #0F172A, #1E293B, #78350F); position: relative; overflow: hidden; }
.banner-pattern { position: absolute; inset: 0; opacity: 0.3; background-image: radial-gradient(circle at 20% 50%, rgba(245,158,11,0.4) 0%, transparent 40%), radial-gradient(circle at 80% 30%, rgba(59,130,246,0.3) 0%, transparent 40%); }
.preview-badge { position: absolute; top: 10px; display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 999px; backdrop-filter: blur(8px); }
.preview-badge-on { left: 10px; background: rgba(16,185,129,0.9); }
.preview-guest-badge { right: 10px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.2); }
.preview-badge-text { color: #FFFFFF; font-size: 9px; font-weight: 600; }
.preview-body { padding: 0 16px 16px; margin-top: -28px; position: relative; }
.preview-avatar { width: 56px; height: 56px; border-radius: 16px; background: linear-gradient(135deg, #3B82F6, #2563EB); display: flex; align-items: center; justify-content: center; color: #FFFFFF; font-size: 18px; font-weight: 700; box-shadow: 0 4px 12px rgba(0,0,0,0.10), 0 0 0 4px #FFFFFF; }
.avatar-stack { display: flex; align-items: center; }
.stack-item { border: 2px solid #FFFFFF; }
.stage-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-top: 12px; }
.stage-cell { text-align: center; padding: 8px 4px; border-radius: 8px; }
.stage-cell-done { background: #ECFDF5; }
.stage-cell-doing { background: #EFF6FF; }
.stage-cell-todo { background: #F8FAFC; }
.preview-file-row { display: flex; align-items: center; gap: 6px; padding: 3px 0; }
.flex-1 { flex: 1; }
.preview-foot { display: flex; align-items: center; justify-content: space-between; padding-top: 10px; border-top: 1px solid #F1F5F9; margin-top: 10px; }
.preview-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 10px; }
.preview-action { padding: 8px; border-radius: 8px; display: flex; align-items: center; justify-content: center; gap: 4px; }
.preview-action-off { background: #F1F5F9; }
.preview-action-on { background: #2563EB; }

.perm-card { display: flex; align-items: flex-start; gap: 12px; background: #FFFFFF; border: 2px solid #E2E8F0; border-radius: 16px; padding: 14px; margin-bottom: 8px; }
.perm-on { border-color: #2563EB; background: #EFF6FF; }
.perm-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.icon-bg-emerald { background: #D1FAE5; }
.icon-bg-blue { background: #DBEAFE; }
.icon-bg-amber { background: #FEF3C7; }
.icon-bg-violet { background: #EDE9FE; }
.perm-radio { width: 18px; height: 18px; border-radius: 999px; border: 2px solid #CBD5E1; flex-shrink: 0; }

.expire-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.expire-card { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 10px 4px; border-radius: 12px; border: 2px solid #E2E8F0; background: #FFFFFF; }
.expire-on { border-color: #7C3AED; background: #F5F3FF; }

.scope-row { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 12px; }
.scope-on { background: #EFF6FF; }
.radio { width: 16px; height: 16px; border-radius: 999px; border: 2px solid #CBD5E1; flex-shrink: 0; }
.radio-on { border-color: #2563EB; background: radial-gradient(circle, #2563EB 0%, #2563EB 40%, #FFFFFF 40%, #FFFFFF 100%); }

.checkbox { width: 16px; height: 16px; border-radius: 4px; border: 1.5px solid #CBD5E1; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.checkbox-on { background: #2563EB; border-color: #2563EB; }
.content-row { display: flex; align-items: center; gap: 10px; padding: 10px; border-radius: 10px; }

.link-box { display: flex; align-items: center; gap: 6px; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 12px; }
.link-copy { padding: 4px 10px; background: #2563EB; color: #FFFFFF; font-size: 10px; font-weight: 600; border-radius: 6px; }

.share-to-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.share-to-cell { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.share-to-icon { width: 48px; height: 48px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }

.info-banner { display: flex; gap: 8px; background: #EFF6FF; border: 1px solid #DBEAFE; border-radius: 12px; padding: 12px; }
.mt-1 { margin-top: 4px; }

.bottom-bar { display: flex; gap: 8px; padding: 12px 20px calc(12px + env(safe-area-inset-bottom)); background: #FFFFFF; border-top: 1px solid #F1F5F9; }
.btn-amber {
  background: #F59E0B; color: #FFFFFF;
  box-shadow: 0 4px 12px rgba(245,158,11,0.30);
  position: relative;
  overflow: hidden;
}
.btn-amber::before {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(120deg, transparent 30%, rgba(255,255,255,0.4) 50%, transparent 70%);
  animation: btnShine 2.4s linear infinite;
}
@keyframes btnShine {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.status-pill { display: inline-flex; align-items: center; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 600; }
.status-pill-amber { background: #FEF3C7; color: #92400E; }
.status-pill-emerald { background: #D1FAE5; color: #047857; }

.text-faint { color: #94A3B8; }
.text-muted { color: #64748B; }
.text-dark { color: #0F172A; }
.text-body { color: #334155; }
.text-blue { color: #2563EB; }
.text-emerald { color: #059669; }
.text-violet { color: #7C3AED; }
.text-white { color: #FFFFFF; }
.fw-500 { font-weight: 500; }
.fw-600 { font-weight: 600; }
.fw-700 { font-weight: 700; }
.fs-10 { font-size: 10px; } .fs-11 { font-size: 11px; } .fs-12 { font-size: 12px; } .fs-13 { font-size: 13px; } .fs-15 { font-size: 15px; }

.btn { display: flex; align-items: center; justify-content: center; gap: 6px; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
</style>
