<template>
  <view class="page files-page">
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:chevron-left" :size="20" color="#475569" />
        </view>
        <view class="topbar-center">
          <text class="topbar-title">文件库</text>
          <text class="topbar-sub">多维筛选 · 精准定位</text>
        </view>
        <view class="icon-btn" @tap="showFilter = true" style="position:relative;">
          <RocIcon name="lucide:sliders-horizontal" :size="18" color="#475569" />
          <view v-if="activeFilterCount" class="badge-num" style="position:absolute; top:-2px; right:-2px;">{{ activeFilterCount }}</view>
        </view>
      </view>
    </view>

    <!-- Search -->
    <view class="search-wrap">
      <view class="search-box">
        <RocIcon name="lucide:search" :size="14" color="#94A3B8" />
        <input class="search-input" placeholder="搜索文件名、上传者、阶段…" v-model="keyword" @input="keyword = $event.detail.value" />
        <view class="kbd">⌘K</view>
      </view>
      <scroll-view scroll-x class="h-scroll" :show-scrollbar="false">
        <view class="filter-chips">
          <view v-for="f in activeFilters" :key="f.key" :class="['filter-chip', `filter-chip-${f.tone}`]">
            <RocIcon :name="f.icon" :size="10" :color="f.color" />
            <text>{{ f.label }}</text>
            <view @tap.stop="removeFilter(f.key)">
              <RocIcon name="lucide:x" :size="11" :color="f.color" />
            </view>
          </view>
          <view class="filter-chip filter-chip-add" @tap="showFilter = true">
            <RocIcon name="lucide:plus" :size="10" color="#475569" />
            <text>添加筛选</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- Toolbar -->
    <view class="toolbar">
      <text class="fs-12 text-muted">{{ filteredFiles.length }} 个文件</text>
      <view class="row" style="padding:0; gap:8px;">
        <view class="toolbar-item" @tap="onSort">
          <text class="fs-12 text-body">按时间排序</text>
          <RocIcon name="lucide:chevron-down" :size="12" color="#64748B" />
        </view>
        <view class="toolbar-divider"></view>
        <view :class="['toolbar-view', viewMode === 'list' ? 'toolbar-view-on' : '']" @tap="viewMode = 'list'">
          <RocIcon name="lucide:list" :size="14" :color="viewMode === 'list' ? '#2563EB' : '#94A3B8'" />
        </view>
        <view :class="['toolbar-view', viewMode === 'grid' ? 'toolbar-view-on' : '']" @tap="viewMode = 'grid'">
          <RocIcon name="lucide:layout-grid" :size="14" :color="viewMode === 'grid' ? '#2563EB' : '#94A3B8'" />
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <view v-if="loading" class="files-loading">
        <text class="fs-12 text-muted">正在加载文件…</text>
      </view>
      <view v-else-if="loadError" class="files-loading">
        <text class="fs-12 text-muted">{{ loadError }}</text>
        <view class="btn btn-ghost" style="margin-top:8px; height:30px; line-height:30px; padding:0 12px;" @tap="loadFiles"><text class="fs-12">重试</text></view>
      </view>
      <view v-else-if="!files.length && !hiddenFiles.length" class="files-loading">
        <text class="fs-12 text-muted">暂无文件</text>
      </view>
      <view v-for="(g, gi) in groupedFiles" :key="g.id" class="group-block">
        <view class="group-head">
          <view class="row" style="padding:0; gap:8px;">
            <view :class="['group-dot', `group-dot-${gi % 4}`]"></view>
            <text class="fs-13 fw-700 text-dark">{{ g.name }}</text>
            <text class="fs-11 text-faint">{{ g.files.length }} 份</text>
          </view>
          <view class="icon-btn-mini">
            <RocIcon :name="g.collapsed ? 'lucide:chevron-down' : 'lucide:chevron-up'" :size="14" color="#94A3B8" />
          </view>
        </view>
        <view v-if="!g.collapsed" :class="['file-rows', viewMode === 'grid' ? 'file-rows-grid' : '']">
          <view v-for="f in g.files" :key="f.id" :class="['file-row', fileRowClass(f)]" @tap="openFile(f)">
            <view :class="['row-icon', fileIconBg(f.ext)]">
              <RocIcon :name="fileIconName(f.ext)" :size="16" :color="fileIconColor(f.ext)" />
            </view>
            <view class="row-body">
              <text class="fs-13 fw-600 text-dark" style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">{{ f.name }}</text>
              <view class="row" style="padding:0; gap:6px; margin-top:2px;">
                <view class="avatar avatar-sm" :style="{ width: '14px', height: '14px', fontSize: '8px', background: f.uploaderColor }">{{ f.uploaderChar }}</view>
                <text class="fs-10 text-muted">{{ f.uploader }}</text>
                <text v-if="f.version" class="fs-10 text-faint">v{{ f.version }}</text>
                <text v-if="f.size" class="fs-10 text-faint">{{ f.size }}</text>
                <text v-if="f.stage" class="fs-10 text-faint">{{ f.stage }}</text>
              </view>
            </view>
            <view :class="['status-pill', fileStatusClass(f)]"><text>{{ fileStatusLabel(f) }}</text></view>
            <view :class="['type-chip', `type-${f.ext}`]"><text>{{ (f.ext || 'FILE').toUpperCase() }}</text></view>
            <view v-if="f.status === 'pending'" class="row-action file-action-group" style="display:flex; gap:4px;">
              <view class="icon-btn-mini" @tap.stop="onReject(f)"><RocIcon name="lucide:x" :size="12" color="#E11D48" /></view>
              <view class="icon-btn-mini icon-btn-blue" @tap.stop="onApprove(f)"><RocIcon name="lucide:check" :size="12" color="#FFFFFF" /></view>
            </view>
            <view v-else class="row-action file-action-group">
              <view class="icon-btn-mini" @tap.stop="onFileMore(f)">
                <RocIcon name="lucide:more-vertical" :size="14" color="#475569" />
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- Hidden files -->
      <view v-if="!hiddenExpanded && hiddenFiles.length" class="hidden-toggle" @tap="hiddenExpanded = true">
        <RocIcon name="lucide:eye-off" :size="13" color="#94A3B8" />
        <text class="fs-12 text-muted">查看已隐藏文件（{{ hiddenFiles.length }}）</text>
      </view>
      <view v-if="hiddenExpanded" class="group-block">
        <view class="group-head">
          <view class="row" style="padding:0; gap:8px;">
            <view class="group-dot group-dot-slate"></view>
            <text class="fs-13 fw-700 text-muted">已隐藏</text>
            <text class="fs-11 text-faint">{{ hiddenFiles.length }} 份</text>
          </view>
        </view>
        <view v-for="f in hiddenFiles" :key="f.id" class="file-row file-row-hidden">
          <view class="row-icon icon-bg-slate">
            <RocIcon name="lucide:file-text" :size="16" color="#94A3B8" />
          </view>
          <view class="row-body">
            <text class="fs-13 fw-600 text-muted" style="text-decoration:line-through;">{{ f.name }}</text>
            <view class="row" style="padding:0; gap:6px; margin-top:2px;">
              <RocIcon name="lucide:archive" :size="10" color="#94A3B8" />
              <text class="fs-10 text-faint">已归档</text>
              <text class="fs-10 text-faint">· 闲置 &gt; 30 天</text>
            </view>
          </view>
          <text class="fs-12 text-blue fw-600" @tap.stop="onRestore(f)">恢复</text>
        </view>
      </view>

      <view class="safe-bottom" style="height: 24px;"></view>
    </scroll-view>

    <!-- Filter sheet -->
    <RocSheet :visible="showFilter" @update:visible="v => showFilter = v">
      <view class="sheet-pad">
        <text class="sheet-title">多维筛选</text>
        <text class="filter-section">阶段</text>
        <view class="chip-row">
          <view v-for="s in ['选题与分工', '资料归集', '分析与成稿', '结题']" :key="s" :class="['chip', stageFilter.includes(s) ? 'chip-blue' : 'chip-outline']" @tap="toggleStage(s)"><text>{{ s }}</text></view>
        </view>
        <text class="filter-section">文件类型</text>
        <view class="chip-row">
          <view v-for="e in ['.docx', '.xlsx', '.pdf', '.pptx', '.zip']" :key="e" :class="['chip', extFilter.includes(e) ? 'chip-violet' : 'chip-outline']" @tap="toggleExt(e)"><text>{{ e }}</text></view>
        </view>
        <text class="filter-section">状态</text>
        <view class="chip-row">
          <view v-for="s in ['未隐藏', '已隐藏', '待审核', '已入库']" :key="s" :class="['chip', statusFilter.includes(s) ? 'chip-emerald' : 'chip-outline']" @tap="toggleStatus(s)"><text>{{ s }}</text></view>
        </view>
        <view class="row" style="padding:0; gap:8px; margin-top:20px;">
          <view class="btn btn-ghost flex-1" style="height:44px; line-height:44px; padding:0;" @tap="resetFilters"><text>清空</text></view>
          <view class="btn btn-accent flex-1" style="height:44px; line-height:44px; padding:0; width:auto;" @tap="showFilter = false"><text>应用筛选</text></view>
        </view>
      </view>
    </RocSheet>

    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>

    <FilePreviewSheet v-model:visible="previewVisible" :file-id="previewFileId" :file-name="previewFileName" @close="onPreviewClose" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { textAvatar } from '../../utils/avatar.js'
import RocIcon from '../../components/Icon.vue'
import RocSheet from '../../components/Sheet.vue'
import FilePreviewSheet from '../../components/FilePreviewSheet.vue'

const safeTop = ref(20)
const keyword = ref('')
const viewMode = ref('list')
const showFilter = ref(false)
const hiddenExpanded = ref(false)
const stageFilter = ref(['资料归集'])
const extFilter = ref(['.docx'])
const statusFilter = ref(['未隐藏', '已入库'])
const toast = ref({ show: false, text: '' })

// T-26：进入页面即从后端拉真文件，覆盖写入 files 与 hiddenFiles
const loading = ref(false)
const loadError = ref('')

function fileExtFromName(name) {
  if (!name) return 'file'
  const i = name.lastIndexOf('.')
  return i < 0 ? 'file' : name.substring(i + 1).toLowerCase()
}

function humanFileSize(bytes) {
  if (bytes == null) return ''
  if (typeof bytes === 'string') return bytes
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}

function uploaderInfoOf(vo) {
  const u = vo.uploader || {}
  const nickname = u.nickname || ''
  const char = nickname ? nickname.substring(0, 1) : '?'
  return { nickname, char, color: textAvatar(nickname || '?') }
}

/** 把后端 FileVO 拍平为前端 UI 用的 row */
function flattenFile(vo, projectName, projectColor) {
  const u = uploaderInfoOf(vo)
  return {
    id: vo.fileId,
    fileId: vo.fileId,
    projectId: vo.projectId,
    projectName: projectName || ('项目#' + vo.projectId),
    projectColor: projectColor || 0,
    name: vo.name,
    ext: (vo.type || fileExtFromName(vo.name)).toLowerCase(),
    uploader: u.nickname,
    uploaderChar: u.char,
    uploaderColor: u.color,
    version: vo.version || 1,
    size: humanFileSize(vo.size),
    stage: vo.stageName || '',
    status: (vo.status || 'approved').toLowerCase(),
    url: vo.url,
    isFavorite: !!vo.isFavorite,
    uploadedAt: vo.uploadedAt,
    rejectReason: vo.rejectReason
  }
}

async function loadFiles() {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    // 1. 拉用户所有小组
    const groups = await api.myGroups() || []
    // 2. 拿到所有项目（按小组聚合），同时记录 projectId → 项目名
    const projectMeta = new Map() // projectId -> { name, color }
    let colorIdx = 0
    const allFiles = []
    for (const g of groups) {
      let projects = []
      try { projects = await api.projectList(g.groupId) || [] } catch (e) { projects = [] }
      for (const p of projects) {
        projectMeta.set(p.projectId || p.id, { name: p.name, color: colorIdx++ % 4 })
      }
    }
    // 3. 对每个项目调 fileList 拉文件
    for (const [projectId, meta] of projectMeta.entries()) {
      let list = null
      try { list = await api.request({ url: '/v1/file/list', params: { projectId, pageSize: 200 } }) } catch (e) { list = null }
      const rows = (list && (list.records || list.list || list.items || list)) || []
      for (const vo of rows) {
        if (!vo || !vo.fileId) continue
        allFiles.push(flattenFile(vo, meta.name, meta.color))
      }
    }
    // 4. 区分隐藏/正常
    files.value = allFiles.filter(f => f.status !== 'hidden' && f.status !== 'archived')
    hiddenFiles.value = allFiles.filter(f => f.status === 'hidden' || f.status === 'archived')
  } catch (e) {
    loadError.value = (e && e.message) || '加载失败'
    // 加载失败时清空，避免误导
    files.value = []
    hiddenFiles.value = []
  } finally {
    loading.value = false
  }
}

onLoad(() => { refreshSafeTop(); loadFiles() })
onShow(() => { if (api.isLoggedIn()) loadFiles() })

function refreshSafeTop() { try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {} }

const filteredFiles = computed(() => {
  let list = files.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(f => (f.name + f.uploader + (f.stage || '')).toLowerCase().includes(kw))
  }
  if (stageFilter.value.length) list = list.filter(f => stageFilter.value.includes(f.stage))
  if (extFilter.value.length) list = list.filter(f => extFilter.value.some(e => f.name.toLowerCase().endsWith(e.replace('.', ''))))
  if (statusFilter.value.length) {
    list = list.filter(f => {
      if (statusFilter.value.includes('未隐藏') && f.status !== 'hidden') return true
      if (statusFilter.value.includes('已隐藏') && f.status === 'hidden') return true
      if (statusFilter.value.includes('待审核') && f.status === 'pending') return true
      if (statusFilter.value.includes('已入库') && f.status === 'approved') return true
      return false
    })
  }
  return list
})

const groupedFiles = computed(() => {
  const groups = {}
  filteredFiles.value.forEach(f => {
    if (!groups[f.projectName]) groups[f.projectName] = { id: f.projectId, name: f.projectName, files: [], collapsed: false }
    groups[f.projectName].files.push(f)
  })
  return Object.values(groups)
})

const activeFilters = computed(() => {
  const list = []
  if (stageFilter.value.length) list.push({ key: 'stage', label: stageFilter.value[0] + (stageFilter.value.length > 1 ? ' +' + (stageFilter.value.length - 1) : ''), icon: 'lucide:layers', tone: 'blue', color: '#2563EB' })
  if (extFilter.value.length) list.push({ key: 'ext', label: extFilter.value[0], icon: 'lucide:file-type', tone: 'violet', color: '#7C3AED' })
  if (statusFilter.value.length) list.push({ key: 'status', label: '未隐藏', icon: 'lucide:check-circle-2', tone: 'emerald', color: '#059669' })
  return list
})
const activeFilterCount = computed(() => stageFilter.value.length + extFilter.value.length + statusFilter.value.length)

function toggleStage(s) { stageFilter.value = stageFilter.value.includes(s) ? stageFilter.value.filter(x => x !== s) : [...stageFilter.value, s] }
function toggleExt(e) { extFilter.value = extFilter.value.includes(e) ? extFilter.value.filter(x => x !== e) : [...extFilter.value, e] }
function toggleStatus(s) { statusFilter.value = statusFilter.value.includes(s) ? statusFilter.value.filter(x => x !== s) : [...statusFilter.value, s] }
function removeFilter(key) {
  if (key === 'stage') stageFilter.value = []
  else if (key === 'ext') extFilter.value = []
  else if (key === 'status') statusFilter.value = []
}
function resetFilters() { stageFilter.value = []; extFilter.value = []; statusFilter.value = [] }

function fileIconName(ext) {
  const m = { docx: 'lucide:file-text', xlsx: 'lucide:file-spreadsheet', pdf: 'lucide:file-text', img: 'lucide:image', pptx: 'lucide:monitor', zip: 'lucide:package' }
  return m[ext] || 'lucide:file'
}
function fileIconBg(ext) {
  const m = { docx: 'icon-bg-sky', xlsx: 'icon-bg-emerald', pdf: 'icon-bg-rose', img: 'icon-bg-violet', pptx: 'icon-bg-amber', zip: 'icon-bg-slate' }
  return m[ext] || 'icon-bg-sky'
}
function fileIconColor(ext) {
  const m = { docx: '#0EA5E9', xlsx: '#059669', pdf: '#E11D48', img: '#7C3AED', pptx: '#D97706', zip: '#475569' }
  return m[ext] || '#0EA5E9'
}
function fileStatusClass(f) {
  if (f.status === 'approved') return 'status-pill-emerald'
  if (f.status === 'pending') return 'status-pill-amber'
  return 'status-pill-slate'
}
function fileStatusLabel(f) {
  if (f.status === 'approved') return '已入库'
  if (f.status === 'pending') return '待审核'
  return '草稿'
}
function fileRowClass(f) {
  if (f.status === 'pending') return 'file-pending'
  if (f.status === 'draft') return 'file-draft'
  return 'file-approved'
}

function goBack() { uni.navigateBack() }
function onSort() { showToast('切换排序方式') }

// T-12：在线预览抽屉
const previewVisible = ref(false)
const previewFileId = ref(0)
const previewFileName = ref('')
function openFile(f) {
  // 优先走预览抽屉
  previewFileId.value = f.fileId
  previewFileName.value = f.name
  previewVisible.value = true
}
function onPreviewClose() {
  // 抽屉关闭后清空 ID，避免下次打开时残留
  previewFileId.value = 0
  previewFileName.value = ''
}

function onApprove(f) {
  // 后端无单文件 approve（只有 batch-audit / resubmit），按 batch-audit=approve 处理
  const originalStatus = f.status
  api.optimistic({
    apply: () => { f.status = 'approved' },
    revert: () => { f.status = originalStatus },
    request: api.request({ url: '/v1/file/batch-audit', method: 'POST', data: { fileIds: [f.fileId], action: 'approve' } })
  }).then(() => {
    showToast('已通过 ' + f.name)
  }).catch(err => {
    showToast((err && err.message) || '操作失败')
  })
}

function onReject(f) {
  const idx = files.value.findIndex(x => x.fileId === f.fileId)
  if (idx < 0) return
  api.optimistic({
    apply: () => { files.value.splice(idx, 1) },
    revert: () => { if (!files.value.includes(f)) files.value.splice(idx, 0, f) },
    request: api.request({ url: `/v1/file/${f.fileId}/reject`, method: 'POST', data: { reason: '驳回' } })
  }).then(() => {
    showToast('已驳回 ' + f.name)
  }).catch(err => {
    showToast((err && err.message) || '操作失败')
  })
}

function onFileMore(f) { showToast('更多操作') }
function onRestore(f) {
  // 后端没有 restore 接口，hidden/archived 状态由 file.move/列表过滤；这里重新拉一次列表
  showToast('正在恢复 ' + f.name + '…')
  loadFiles()
}

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.files-page { background: var(--bg-system); }
.content { flex: 1; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; display: flex; align-items: center; justify-content: space-between; }
.topbar-center { flex: 1; text-align: center; }
.topbar-title { font-size: 16px; font-weight: 600; color: #0F172A; display: block; }
.topbar-sub { font-size: 11px; color: #94A3B8; margin-top: 2px; display: block; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; position: relative; }
.badge-num { position: absolute; top: -2px; right: -2px; min-width: 16px; height: 16px; padding: 0 4px; border-radius: 999px; background: #2563EB; color: #FFFFFF; font-size: 10px; font-weight: 700; line-height: 16px; text-align: center; border: 2px solid #FFFFFF; }

.search-wrap { padding: 12px 20px 0; background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.search-box { background: #F1F5F9; border: 1px solid transparent; border-radius: 12px; padding: 0 12px; height: 40px; display: flex; align-items: center; gap: 6px; transition: border-color 0.15s ease, background 0.15s ease; }
.search-input { flex: 1; height: 40px; font-size: 13px; color: #0F172A; background: transparent; border: none; outline: none; }
.kbd { font-size: 10px; font-weight: 600; color: #64748B; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 4px; padding: 0 6px; height: 20px; line-height: 18px; }

.filter-chips { display: flex; gap: 6px; padding: 10px 0; }
.filter-chip { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 500; border: 1px solid transparent; }
.filter-chip-blue { background: #EFF6FF; color: #1D4ED8; border-color: #DBEAFE; }
.filter-chip-violet { background: #F5F3FF; color: #6D28D9; border-color: #EDE9FE; }
.filter-chip-emerald { background: #ECFDF5; color: #047857; border-color: #D1FAE5; }
.filter-chip-add { background: #F8FAFC; color: #475569; border-color: #E2E8F0; }

.toolbar { display: flex; align-items: center; justify-content: space-between; padding: 8px 20px; background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.toolbar-item { display: flex; align-items: center; gap: 4px; padding: 4px 8px; border-radius: 8px; }
.toolbar-divider { width: 1px; height: 16px; background: #E2E8F0; }
.toolbar-view { width: 28px; height: 28px; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.toolbar-view-on { background: #EFF6FF; }

.group-block { padding: 12px 20px 0; }
.group-head { display: flex; align-items: center; justify-content: space-between; padding: 0 4px 8px; }
.group-dot { width: 8px; height: 8px; border-radius: 2px; }
.group-dot-0 { background: #2563EB; }
.group-dot-1 { background: #7C3AED; }
.group-dot-2 { background: #10B981; }
.group-dot-3 { background: #F59E0B; }
.group-dot-slate { background: #94A3B8; }

.file-rows { display: flex; flex-direction: column; gap: 6px; }
.file-row { display: flex; align-items: center; gap: 10px; background: #FFFFFF; border: 1px solid transparent; border-radius: 12px; padding: 10px; transition: background 0.15s ease, border-color 0.15s ease; }
.file-row:hover, .file-row:active { background: #F8FAFC; border-color: #E2E8F0; }
.file-approved { border-color: #E2E8F0; }
.file-pending { background: #FFFBEB; border-color: #FDE68A; }
.file-pending:hover, .file-pending:active { background: #FEF3C7; }
.file-draft { background: #FFFFFF; border-style: dashed; border-color: #CBD5E1; }
.file-row-hidden { background: #F8FAFC; border-color: #E2E8F0; opacity: 0.7; }
.file-action-group { display: flex; gap: 4px; opacity: 0; transition: opacity 0.15s ease; }
.file-row:active .file-action-group, .file-row:focus-within .file-action-group { opacity: 1; }
.file-pending .file-action-group { opacity: 1; }
.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.row-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-bg-blue { background: #EFF6FF; }
.icon-bg-sky { background: #F0F9FF; }
.icon-bg-violet { background: #F5F3FF; }
.icon-bg-emerald { background: #ECFDF5; }
.icon-bg-amber { background: #FFFBEB; }
.icon-bg-rose { background: #FFF1F2; }
.icon-bg-slate { background: #F1F5F9; }
.avatar { display: inline-flex; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: 999px; font-size: 14px; font-weight: 700; color: #FFFFFF; flex-shrink: 0; }
.avatar-sm { width: 20px; height: 20px; font-size: 10px; }
.icon-btn-mini { width: 24px; height: 24px; border-radius: 6px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }
.icon-btn-blue { background: #2563EB; }

.status-pill { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; font-size: 10px; font-weight: 600; }
.status-pill-amber { background: #FEF3C7; color: #92400E; }
.status-pill-emerald { background: #D1FAE5; color: #047857; }
.status-pill-slate { background: #F1F5F9; color: #475569; }

.type-chip { display: inline-flex; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 700; }
.type-docx { background: #DBEAFE; color: #2563EB; }
.type-xlsx { background: #D1FAE5; color: #059669; }
.type-pdf { background: #FFE4E6; color: #BE123C; }
.type-pptx { background: #E0F2FE; color: #0284C7; }
.type-zip { background: #F1F5F9; color: #475569; }

.hidden-toggle { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 12px; }

.sheet-pad { padding: 16px 20px 8px; }
.sheet-title { font-size: 15px; font-weight: 700; color: #0F172A; display: block; margin-bottom: 12px; text-align: center; }
.filter-section { font-size: 11px; font-weight: 600; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin: 12px 0 6px; }
.chip-row { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { display: inline-flex; align-items: center; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 500; }
.chip-blue { background: #EFF6FF; color: #2563EB; border: 1px solid #DBEAFE; }
.chip-violet { background: #F5F3FF; color: #7C3AED; border: 1px solid #EDE9FE; }
.chip-emerald { background: #ECFDF5; color: #047857; border: 1px solid #D1FAE5; }
.chip-outline { background: transparent; color: #475569; border: 1px solid #E2E8F0; }

.text-faint { color: #94A3B8; }
.text-muted { color: #64748B; }
.text-dark { color: #0F172A; }
.text-body { color: #334155; }
.text-blue { color: #2563EB; }
.fw-600 { font-weight: 600; }
.fw-700 { font-weight: 700; }
.fs-10 { font-size: 10px; } .fs-11 { font-size: 11px; } .fs-12 { font-size: 12px; } .fs-13 { font-size: 13px; }
.flex-1 { flex: 1; }

.btn { display: flex; align-items: center; justify-content: center; gap: 6px; }

.files-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; gap: 6px; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
</style>
