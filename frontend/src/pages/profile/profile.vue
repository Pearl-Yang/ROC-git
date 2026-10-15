<template>
  <view class="page profile-page">
    <!-- Banner -->
    <view class="banner-amber" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="row" style="padding:0; gap:14px;">
          <view class="avatar avatar-xl" :style="{ background: '#FFFFFF', color: avatarColor }">{{ avatarChar }}</view>
          <view>
            <text class="fs-16 fw-800" style="color:#7C2D12; display:block;">{{ userName }}</text>
            <text class="fs-11" style="color:#9A3412; display:block; margin-top:2px;">{{ userCollege }}</text>
            <view class="row" style="padding:0; gap:6px; margin-top:6px;">
              <view class="role-pill" :class="userRole === 'leader' ? 'role-amber' : 'role-rose'">
                <text>{{ userRole === 'leader' ? '组长' : '助教' }}</text>
              </view>
            </view>
          </view>
        </view>
        <view class="icon-btn" @tap="goSettings">
          <RocIcon name="lucide:settings" :size="18" color="#7C2D12" />
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <!-- Stats -->
      <view class="card-pad">
        <view class="card stats-card">
          <view class="stat-cell">
            <text class="stat-num text-dark">{{ stats.files }}</text>
            <text class="stat-lbl">文件</text>
          </view>
          <view class="stat-cell">
            <text class="stat-num text-dark">{{ stats.versions }}</text>
            <text class="stat-lbl">版本</text>
          </view>
          <view class="stat-cell">
            <text class="stat-num text-dark">{{ stats.pending }}</text>
            <text class="stat-lbl">待审</text>
          </view>
          <view class="stat-cell">
            <text class="stat-num" style="color:#D97706;">{{ stats.archived }}</text>
            <text class="stat-lbl">归档</text>
          </view>
        </view>
      </view>

      <!-- My projects -->
      <view class="section-head">
        <text class="section-title">我的项目</text>
        <text class="more" @tap="goProjectList">全部 ›</text>
      </view>
      <view class="card-pad">
        <view v-for="(p, pi) in myProjects" :key="p.id || pi" class="my-proj" @tap="openProject(p)">
          <view :class="['proj-avatar', projColor(p)]">{{ avatarCharOf(p.name) }}</view>
          <view class="row-body">
            <text class="fs-13 fw-700 text-dark">{{ p.name }}</text>
            <text class="fs-11 text-muted" style="display:block; margin-top:2px;">{{ p.course }} · {{ p.stage }}</text>
            <view class="row" style="padding:0; gap:6px; margin-top:6px;">
              <view class="progress progress-thin" style="flex:1;">
                <view class="progress-fill" :style="{ width: p.progress + '%' }"></view>
              </view>
              <text class="fs-10 text-faint">{{ p.progress }}%</text>
            </view>
          </view>
        </view>
      </view>

      <!-- Quick actions -->
      <view class="card-pad">
        <view class="quick-row">
          <view class="quick-cell" @tap="goInvite">
            <view class="quick-icon icon-bg-emerald"><RocIcon name="lucide:user-plus" :size="20" color="#059669" /></view>
            <text class="fs-11 text-body fw-500" style="display:block; margin-top:4px;">邀请成员</text>
          </view>
          <view class="quick-cell" @tap="goArchive">
            <view class="quick-icon icon-bg-amber"><RocIcon name="lucide:archive" :size="20" color="#D97706" /></view>
            <text class="fs-11 text-body fw-500" style="display:block; margin-top:4px;">期末归档</text>
          </view>
          <view class="quick-cell" @tap="goStorage">
            <view class="quick-icon icon-bg-violet"><RocIcon name="lucide:database" :size="20" color="#7C3AED" /></view>
            <text class="fs-11 text-body fw-500" style="display:block; margin-top:4px;">存储</text>
          </view>
        </view>
      </view>

      <!-- Activity feed -->
      <view class="section-head">
        <text class="section-title">操作记录</text>
      </view>
      <view class="card-pad">
        <view v-for="(a, ai) in activityFeed" :key="ai" class="activity-row">
          <view :class="['row-icon', a.iconBg]">
            <RocIcon :name="a.icon" :size="14" :color="a.color" />
          </view>
          <view class="row-body">
            <text class="fs-12 text-body">{{ a.text }}</text>
            <text class="fs-10 text-faint" style="display:block; margin-top:2px;">{{ a.time }}</text>
          </view>
        </view>
      </view>

      <!-- System entries -->
      <view class="card-pad">
        <view class="settings-list">
          <view class="settings-item" @tap="goSettings">
            <view class="row-icon icon-bg-blue"><RocIcon name="lucide:settings" :size="14" color="#2563EB" /></view>
            <text class="fs-13 fw-600 text-dark flex-1">设置</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="settings-item" @tap="onHelp">
            <view class="row-icon icon-bg-emerald"><RocIcon name="lucide:help-circle" :size="14" color="#059669" /></view>
            <text class="fs-13 fw-600 text-dark flex-1">帮助</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="settings-item" @tap="goAgreement">
            <view class="row-icon icon-bg-violet"><RocIcon name="lucide:file-text" :size="14" color="#7C3AED" /></view>
            <text class="fs-13 fw-600 text-dark flex-1">用户协议</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="settings-item" @tap="goPrivacy">
            <view class="row-icon icon-bg-amber"><RocIcon name="lucide:shield" :size="14" color="#D97706" /></view>
            <text class="fs-13 fw-600 text-dark flex-1">隐私政策</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="settings-item" @tap="onExportData">
            <view class="row-icon icon-bg-sky"><RocIcon name="lucide:download" :size="14" color="#0284C7" /></view>
            <text class="fs-13 fw-600 text-dark flex-1">导出我的数据</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="settings-item" @tap="onCancelAccount">
            <view class="row-icon icon-bg-rose"><RocIcon name="lucide:user-x" :size="14" color="#E11D48" /></view>
            <text class="fs-13 fw-600" style="color:#E11D48; flex:1;">注销账号</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="settings-item" @tap="onAbout">
            <view class="row-icon icon-bg-slate"><RocIcon name="lucide:info" :size="14" color="#475569" /></view>
            <text class="fs-13 fw-600 text-dark flex-1">关于 v1.2.0</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="settings-item" @tap="onSwitchLang">
            <view class="row-icon icon-bg-blue"><RocIcon name="lucide:languages" :size="14" color="#2563EB" /></view>
            <text class="fs-13 fw-600 text-dark flex-1">语言</text>
            <text class="fs-11 text-faint" style="margin-right:6px;">{{ currentLocaleLabel }}</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <!-- Logout -->
      <view class="card-pad">
        <view class="btn btn-danger" @tap="onLogout">退出登录</view>
      </view>

      <view class="safe-bottom" style="height: 24px;"></view>
    </scroll-view>

    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>

    <!-- 语言切换 sheet -->
    <RocSheet :visible="showLang" @update:visible="v => showLang = v">
      <view class="sheet-pad">
        <text class="sheet-title">选择语言 / Language</text>
        <view class="lang-list">
          <view v-for="loc in LOCALES" :key="loc.key" :class="['lang-row', currentLocale === loc.key ? 'lang-row-on' : '']" @tap="onPickLang(loc.key)">
            <view :class="['lang-radio', currentLocale === loc.key ? 'lang-radio-on' : '']"></view>
            <text class="lang-label">{{ loc.label }}</text>
            <text v-if="currentLocale === loc.key" class="lang-check">✓</text>
          </view>
        </view>
      </view>
    </RocSheet>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { useUserStore } from '../../utils/user-store.js'
import { textAvatar } from '../../utils/avatar.js'
import { LOCALES, setLocale, getCurrentLocale } from '../../utils/i18n.js'
import RocIcon from '../../components/Icon.vue'
import RocSheet from '../../components/Sheet.vue'

const store = useUserStore()
const safeTop = ref(20)
const stats = ref({ files: 18, versions: 42, pending: 4, archived: 7 })
const myProjects = ref([])
const activityFeed = ref([])
const toast = ref({ show: false, text: '' })
const showLang = ref(false)
const currentLocale = ref(getCurrentLocale())
const currentLocaleLabel = computed(() => (LOCALES.find(l => l.key === currentLocale.value) || {}).label || '简体中文')

const userName = computed(() => (store.user.value && store.user.value.nickname) || '林知行')
const avatarChar = computed(() => textAvatar(userName.value).char)
const avatarColor = computed(() => textAvatar(userName.value).color)
const userCollege = computed(() => (store.user.value && store.user.value.college) || '软件工程学院')
const userRole = ref('leader')

onLoad(() => { refreshSafeTop() })
onShow(() => { load() })

function refreshSafeTop() { try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {} }

async function load() {
  // 个人资料
  try {
    const profile = await api.getProfile()
    if (profile) {
      stats.value = {
        files: profile.fileCount || stats.value.files,
        versions: profile.versionCount || stats.value.versions,
        pending: profile.pendingCount || stats.value.pending,
        archived: profile.archivedCount || stats.value.archived
      }
      if (profile.role) userRole.value = profile.role
    }
  } catch (e) {}
  // 贡献度
  try {
    const c = await api.contributionStats()
    if (c) {
      stats.value = {
        files: c.uploadedFiles || stats.value.files,
        versions: c.approvedFiles || stats.value.versions,
        pending: c.pending || stats.value.pending,
        archived: c.archived || stats.value.archived
      }
    }
  } catch (e) {}
  // 登录历史（用于 activityFeed 顶部「最近登录」）
  try {
    const h = await api.loginHistory()
    const list = Array.isArray(h) ? h : (h && h.list) || []
    if (list.length) {
      activityFeed.value = list.slice(0, 5).map((it, i) => ({
        icon: i === 0 ? 'lucide:log-in' : 'lucide:clock',
        iconBg: i === 0 ? 'icon-bg-emerald' : 'icon-bg-slate',
        color: i === 0 ? '#059669' : '#475569',
        text: '你在 ' + new Date(it.loginTime || it.createdAt || Date.now()).toLocaleString('zh-CN') + ' 登录',
        time: it.device || (it.ip || 'unknown')
      }))
    }
  } catch (e) {}
  // 我的项目：从 myGroups -> 每个 group 的 projects 聚合（最多 5 个）
  try {
    const groups = await api.myGroups()
    const list = Array.isArray(groups) ? groups : (groups && groups.list) || []
    const projects = []
    for (const g of list.slice(0, 3)) {
      try {
        const ps = await api.groupProjects(g.id)
        const arr = Array.isArray(ps) ? ps : (ps && ps.list) || []
        for (const p of arr.slice(0, 2)) {
          projects.push({
            id: p.id || p.projectId,
            name: p.name,
            course: g.name,
            stage: p.currentStage || p.stage || '进行中',
            progress: p.progress || 0
          })
          if (projects.length >= 5) break
        }
      } catch (e) {}
      if (projects.length >= 5) break
    }
    if (projects.length) myProjects.value = projects
  } catch (e) {}
}

function goSettings() { uni.navigateTo({ url: '/pages/settings/settings' }) }
function goProjectList() { uni.switchTab({ url: '/pages/home/home' }) }
function openProject(p) { uni.navigateTo({ url: '/pages/project/project?id=' + p.id }) }
function goInvite() { uni.showToast({ title: '打开邀请', icon: 'none' }) }
function goArchive() { uni.showToast({ title: '打开归档', icon: 'none' }) }
function goStorage() { uni.navigateTo({ url: '/pages/settings/settings' }) }
function onHelp() { uni.showToast({ title: '帮助中心', icon: 'none' }) }
function onAbout() { uni.showToast({ title: 'ROC-git v1.2.0', icon: 'none' }) }
function onLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        store.logout()
        uni.reLaunch({ url: '/pages/auth-login/auth-login' })
      }
    }
  })
}

function goAgreement() { uni.navigateTo({ url: '/pages/agreement/agreement' }) }
function goPrivacy() { uni.navigateTo({ url: '/pages/privacy/privacy' }) }

function onSwitchLang() { showLang.value = true }
async function onPickLang(key) {
  if (key === currentLocale.value) { showLang.value = false; return }
  await setLocale(key)
  currentLocale.value = key
  showLang.value = false
  uni.showToast({ title: key === 'zh_CN' ? '已切换到简体中文' : 'Switched to English', icon: 'none' })
}

function onExportData() {
  uni.showLoading({ title: '导出中...' })
  api.dataExport().then((data) => {
    uni.hideLoading()
    // 把 JSON 转成可下载字符串；H5 触发下载，其它平台复制到剪贴板
    const text = JSON.stringify(data, null, 2)
    // #ifdef H5
    const blob = new Blob([text], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'rocgit-mydata-' + Date.now() + '.json'
    a.click()
    URL.revokeObjectURL(url)
    // #endif
    // #ifndef H5
    uni.setClipboardData({ data: text, success: () => {
      uni.showToast({ title: '已复制到剪贴板', icon: 'success' })
    } })
    // #endif
  }).catch((err) => {
    uni.hideLoading()
    uni.showToast({ title: '导出失败：' + (err.message || '网络异常'), icon: 'none' })
  })
}

function onCancelAccount() {
  uni.showModal({
    title: '注销账号',
    content: '注销后 30 天内账号数据可恢复，30 天后将被永久删除。是否继续？',
    confirmText: '继续注销',
    confirmColor: '#E11D48',
    success: (res) => {
      if (!res.confirm) return
      uni.showModal({
        title: '确认注销',
        content: '请最后确认：注销后您将无法登录当前账号，所有项目/文件/任务记录将被保留 30 天后清除。',
        confirmText: '确认注销',
        confirmColor: '#E11D48',
        editable: true,
        placeholderText: '（可选）告诉我们注销原因',
        success: (r2) => {
          if (!r2.confirm) return
          api.cancelAccount(r2.content || '').then((data) => {
            uni.showModal({
              title: '已提交注销',
              content: '账号将于 ' + new Date(data.purgeAt).toLocaleDateString() + ' 永久删除。30 天内重新登录可恢复。',
              showCancel: false,
              success: () => {
                store.logout()
                uni.reLaunch({ url: '/pages/auth-login/auth-login' })
              }
            })
          }).catch((err) => {
            uni.showToast({ title: '注销失败：' + (err.message || '网络异常'), icon: 'none' })
          })
        }
      })
    }
  })
}

function avatarCharOf(n) { return textAvatar(n).char }
function projColor(p) { return p.id % 2 === 0 ? 'proj-color-emerald' : 'proj-color-blue' }

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.profile-page { background: var(--bg-system); }
.content { flex: 1; }
.banner-amber { background: linear-gradient(135deg, #FCD34D, #F59E0B); padding-bottom: 24px; }
.topbar-row { padding: 8px 20px 16px; display: flex; align-items: flex-start; justify-content: space-between; }
.avatar { display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; font-weight: 700; color: #FFFFFF; }
.avatar-xl { width: 64px; height: 64px; font-size: 24px; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: rgba(255,255,255,0.30); display: flex; align-items: center; justify-content: center; }

.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.flex-1 { flex: 1; }

.role-pill { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; font-size: 10px; font-weight: 700; }
.role-amber { background: rgba(255,255,255,0.5); color: #7C2D12; }
.role-rose { background: rgba(255,255,255,0.5); color: #881337; }

.card-pad { padding: 12px 20px 0; }
.card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px; box-shadow: 0 1px 2px rgba(15,23,42,0.04); }
.stats-card { display: grid; grid-template-columns: repeat(4, 1fr); padding: 12px 0; }
.stat-cell { text-align: center; padding: 6px 0; border-right: 1px solid #F1F5F9; }
.stat-cell:last-child { border-right: none; }
.stat-num { font-size: 18px; font-weight: 800; line-height: 1.1; display: block; }
.stat-lbl { font-size: 11px; color: #64748B; margin-top: 4px; display: block; }

.section-head { display: flex; align-items: center; justify-content: space-between; padding: 0 20px; margin: 16px 0 8px; }
.section-title { font-size: 15px; font-weight: 600; color: #0F172A; }
.more { font-size: 12px; color: #2563EB; font-weight: 600; }

.my-proj { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid #F1F5F9; transition: background 0.15s ease; border-radius: 8px; }
.my-proj:active { background: #F8FAFC; }
.my-proj:last-child { border-bottom: none; }
.proj-avatar { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; color: #FFFFFF; flex-shrink: 0; }
.proj-color-blue { background: linear-gradient(135deg, #3B82F6, #2563EB); }
.proj-color-emerald { background: linear-gradient(135deg, #10B981, #059669); }
.proj-color-violet { background: linear-gradient(135deg, #8B5CF6, #7C3AED); }
.proj-color-amber { background: linear-gradient(135deg, #F59E0B, #D97706); }

.quick-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px; padding: 12px; }
.quick-cell { display: flex; flex-direction: column; align-items: center; padding: 8px 0; }
.quick-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.icon-bg-blue { background: #EFF6FF; }
.icon-bg-sky { background: #F0F9FF; }
.icon-bg-violet { background: #F5F3FF; }
.icon-bg-emerald { background: #ECFDF5; }
.icon-bg-amber { background: #FFFBEB; }
.icon-bg-rose { background: #FFF1F2; }
.icon-bg-slate { background: #F1F5F9; }

.activity-row { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #F1F5F9; background: #FFFFFF; padding: 10px 12px; border-bottom: 1px solid #F1F5F9; border-radius: 0; }
.activity-row:last-child { border-bottom: none; }
.row-icon { width: 32px; height: 32px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

.settings-list { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px; overflow: hidden; }
.settings-item { display: flex; align-items: center; gap: 10px; padding: 12px; border-bottom: 1px solid #F1F5F9; transition: background 0.15s ease; }
.settings-item:active { background: #F8FAFC; }
.settings-item:last-child { border-bottom: none; }

.btn-danger { background: #FFFFFF; color: #E11D48; border: 1.5px solid #FECDD3; height: 44px; line-height: 42px; border-radius: 12px; font-size: 14px; font-weight: 600; display: flex; align-items: center; justify-content: center; }

.text-faint { color: #94A3B8; }
.text-muted { color: #64748B; }
.text-dark { color: #0F172A; }
.text-body { color: #334155; }
.fw-500 { font-weight: 500; }
.fw-600 { font-weight: 600; }
.fw-700 { font-weight: 700; }
.fw-800 { font-weight: 800; }
.fs-10 { font-size: 10px; } .fs-11 { font-size: 11px; } .fs-12 { font-size: 12px; } .fs-13 { font-size: 13px; } .fs-16 { font-size: 16px; }

.progress { position: relative; width: 100%; height: 6px; background: #F1F5F9; border-radius: 999px; overflow: hidden; }
.progress-thin { height: 4px; }
.progress-fill { position: absolute; top: 0; left: 0; bottom: 0; background: linear-gradient(90deg, #3B82F6, #2563EB); border-radius: 999px; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }

/* 语言 sheet */
.sheet-pad { padding: 16px 20px 8px; }
.sheet-title { font-size: 15px; font-weight: 700; color: #0F172A; display: block; margin-bottom: 12px; text-align: center; }
.lang-list { display: flex; flex-direction: column; gap: 8px; }
.lang-row { display: flex; align-items: center; gap: 12px; padding: 12px 14px; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; transition: all 0.15s ease; }
.lang-row-on { background: #EFF6FF; border-color: #2563EB; }
.lang-radio { width: 18px; height: 18px; border-radius: 999px; border: 2px solid #CBD5E1; flex-shrink: 0; transition: all 0.15s ease; }
.lang-radio-on { border-color: #2563EB; background: #2563EB; box-shadow: inset 0 0 0 2px #FFFFFF; }
.lang-label { flex: 1; font-size: 14px; font-weight: 600; color: #0F172A; }
.lang-check { font-size: 16px; font-weight: 700; color: #2563EB; }
</style>
