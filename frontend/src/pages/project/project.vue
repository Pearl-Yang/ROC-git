<template>
  <view class="page project-page">
    <!-- Top header -->
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:chevron-left" :size="20" color="#475569" />
        </view>
        <view class="topbar-center">
          <text class="topbar-title">{{ project.name || '社区调研报告' }}</text>
          <text class="topbar-sub">{{ project.course || '软件工程实践' }} · 第 {{ project.currentStageNo || 2 }} 阶段</text>
        </view>
        <view class="icon-btn" @tap="showMore = true">
          <RocIcon name="lucide:more-horizontal" :size="20" color="#475569" />
        </view>
      </view>

      <view class="header-progress">
        <view class="row" style="padding:0; justify-content:space-between;">
          <text class="fs-11 text-faint">总体进度</text>
          <text class="fs-11 fw-700 text-dark">{{ project.progress || 0 }}%</text>
        </view>
        <view class="progress progress-thin" style="margin-top: 6px;">
          <view class="progress-fill" :style="{ width: (project.progress || 0) + '%' }"></view>
        </view>
      </view>

      <view class="header-meta">
        <view class="avatar-stack">
          <view v-for="(m, mi) in (members || []).slice(0, 3)" :key="m.userId || mi" class="avatar avatar-sm stack-item" :style="{ background: avatarColorFor(m) }">{{ avatarCharOf(m) }}</view>
          <view v-if="(members || []).length > 3" class="avatar avatar-sm stack-more">+{{ (members || []).length - 3 }}</view>
        </view>
        <view class="row" style="padding:0; gap:12px;">
          <text class="fs-11 text-faint">{{ project.fileCount || 12 }}资料</text>
          <text class="fs-11 text-amber">{{ project.pendingCount || 4 }}待审</text>
          <text class="fs-11 text-faint">{{ project.versionCount || 18 }}版本</text>
        </view>
      </view>
    </view>

    <!-- Tabs -->
    <view class="tabbar-row">
      <view
        v-for="t in tabs"
        :key="t.key"
        :class="['tab-item', { 'tab-on': activeTab === t.key }]"
        @tap="switchTab(t.key)"
      ><text>{{ t.label }}</text></view>
    </view>

    <scroll-view scroll-y class="content">
      <!-- 概览 -->
      <view v-if="activeTab === 'overview'" class="tab-pane">
        <view class="stat-grid-3">
          <view class="stat-mini stat-blue">
            <text class="stat-mini-num">{{ project.fileCount || 12 }}</text>
            <text class="stat-mini-lbl">文件</text>
          </view>
          <view class="stat-mini stat-amber">
            <text class="stat-mini-num">{{ project.pendingCount || 4 }}</text>
            <text class="stat-mini-lbl">待审核</text>
          </view>
          <view class="stat-mini stat-emerald">
            <text class="stat-mini-num">{{ project.versionCount || 18 }}</text>
            <text class="stat-mini-lbl">版本</text>
          </view>
        </view>

        <view class="card card-padded">
          <text class="card-title">项目信息</text>
          <view class="kv-list">
            <view class="kv"><text class="kv-k">所属课程</text><text class="kv-v">{{ project.course || '软件工程实践' }}</text></view>
            <view class="kv"><text class="kv-k">小组名称</text><text class="kv-v">{{ project.groupName || '第 3 小组' }}</text></view>
            <view class="kv"><text class="kv-k">创建时间</text><text class="kv-v">{{ project.createdAt || '2026-09-01' }}</text></view>
            <view class="kv">
              <text class="kv-k">项目状态</text>
              <text class="status-pill status-pill-blue">进行中</text>
            </view>
          </view>
        </view>

        <view class="card card-padded">
          <text class="card-title">阶段进度</text>
          <view class="stage-list">
            <view v-for="(s, si) in stages" :key="s.id || si" class="stage-item">
              <view :class="['stage-num', stageNumClass(s)]">
                <RocIcon v-if="s.status === 'done'" name="lucide:check" :size="14" color="#059669" />
                <text v-else class="fs-11 fw-700" :style="{ color: stageNumColor(s) }">{{ si + 1 }}</text>
              </view>
              <view class="stage-body">
                <text class="stage-name" :style="{ color: s.status === 'todo' ? '#94A3B8' : '#0F172A' }">{{ s.name }}</text>
                <text class="stage-date">{{ s.dateText }}</text>
              </view>
              <text :class="['status-pill', stageStatusClass(s)]">{{ stageStatusLabel(s) }}</text>
            </view>
          </view>
        </view>

        <view class="card card-padded">
          <text class="card-title">最近活动</text>
          <view v-for="(a, ai) in activities" :key="a.id || ai" class="activity-row">
            <view class="avatar avatar-sm" :style="{ background: a.color || '#0EA5E9' }">{{ avatarCharOf(a.user) }}</view>
            <view class="row-body">
              <text class="row-title"><text class="fw-700">{{ a.user }}</text> {{ a.action }}</text>
              <text class="row-meta">{{ a.time }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 文件 -->
      <view v-else-if="activeTab === 'files'" class="tab-pane">
        <button class="btn btn-gradient" @tap="onUpload" style="height: 44px; line-height: 44px; font-size: 14px;">
          <RocIcon name="lucide:upload" :size="18" color="#FFFFFF" />
          <text class="btn-text">上传资料</text>
        </button>
        <view class="search-row">
          <view class="search-box">
            <RocIcon name="lucide:search" :size="15" color="#94A3B8" />
            <input class="search-input" placeholder="关键词搜索…" v-model="fileKeyword" @input="fileKeyword = $event.detail.value" />
          </view>
          <view class="icon-btn" @tap="showFilter = true">
            <RocIcon name="lucide:sliders-horizontal" :size="16" color="#475569" />
            <view class="badge-num" style="position:absolute;">3</view>
          </view>
        </view>
        <view class="file-list">
          <view v-for="(f, fi) in fileList" :key="f.id || fi" :class="['file-row', fileRowClass(f)]">
            <view :class="['row-icon', fileIconBg(f.ext)]">
              <RocIcon :name="fileIconName(f.ext)" :size="17" :color="fileIconColor(f.ext)" />
            </view>
            <view class="row-body">
              <text class="row-title">{{ f.name }}</text>
              <text class="row-meta">{{ f.uploader || '许萌' }}{{ f.version ? ' · v' + f.version : '' }}{{ f.size ? ' · ' + f.size : '' }}{{ f.stage ? ' · ' + f.stage : '' }}</text>
            </view>
            <view :class="['status-pill', fileStatusClass(f)]">{{ fileStatusLabel(f) }}</view>
            <view v-if="f.status === 'pending'" class="row-action" style="display:flex; gap:4px;">
              <view class="icon-btn-mini icon-btn-rose" @tap="rejectFile(f)">
                <RocIcon name="lucide:x" :size="14" color="#E11D48" />
              </view>
              <view class="icon-btn-mini icon-btn-blue" @tap="approveFile(f)">
                <RocIcon name="lucide:check" :size="14" color="#FFFFFF" />
              </view>
            </view>
            <view v-else class="row-action" style="display:flex; gap:4px;">
              <view class="icon-btn-mini" @tap="downloadFile(f)">
                <RocIcon name="lucide:download" :size="14" color="#475569" />
              </view>
              <view class="icon-btn-mini" @tap="showFileMenu(f)">
                <RocIcon name="lucide:more-vertical" :size="14" color="#475569" />
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 阶段 -->
      <view v-else-if="activeTab === 'stages'" class="tab-pane">
        <view class="row" style="padding:0; justify-content:space-between; margin-bottom: 8px;">
          <text class="section-title">作业阶段</text>
          <text class="fs-12 text-blue fw-600" @tap="onAddStage">+ 添加（最多 10）</text>
        </view>
        <view v-for="(s, si) in stages" :key="s.id || si" :class="['stage-card', stageCardClass(s)]">
          <view class="row" style="padding:0; justify-content:space-between;">
            <view class="row" style="padding:0; gap:6px;">
              <view :class="['stage-num-sm', stageNumClass(s)]">
                <RocIcon v-if="s.status === 'done'" name="lucide:check" :size="12" color="#059669" />
                <text v-else class="fs-10 fw-700" :style="{ color: stageNumColor(s) }">{{ si + 1 }}</text>
              </view>
              <text class="fs-14 fw-700" :style="{ color: s.status === 'todo' ? '#94A3B8' : '#0F172A' }">{{ s.name }}</text>
            </view>
            <view :class="['status-pill', stageStatusClass(s)]">{{ stageStatusLabel(s) }}</view>
          </view>
          <text class="fs-11" style="color:#64748B; margin-top:6px; padding-left:30px;">{{ s.dateText }}</text>
          <view v-if="s.status === 'doing'" class="progress-row">
            <view class="row" style="padding:0; justify-content:space-between;">
              <text class="fs-11 text-faint">进度</text>
              <text class="fs-11 fw-700 text-dark">{{ s.progress || 70 }}%</text>
            </view>
            <view class="progress progress-thin" style="margin-top:4px;">
              <view class="progress-fill" :style="{ width: (s.progress || 70) + '%' }"></view>
            </view>
          </view>
          <view v-if="s.tasks && s.tasks.length" class="task-list">
            <view class="row" style="padding:0; justify-content:space-between; margin-bottom:4px;">
              <text class="fs-11 fw-600 text-muted">任务分工</text>
              <text class="fs-11 text-blue fw-600" @tap="onAddTask">+ 添加</text>
            </view>
            <view v-for="(tk, ti) in s.tasks" :key="ti" class="task-item">
              <view class="avatar avatar-sm" :style="{ background: tk.color || '#0EA5E9' }">{{ avatarCharOf(tk.user) }}</view>
              <text class="flex-1 fs-12 text-dark">{{ tk.name }}</text>
              <text :class="['fs-11', tk.urgent ? 'fw-700 text-amber' : 'text-faint']">{{ tk.date }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 版本 -->
      <view v-else-if="activeTab === 'versions'" class="tab-pane">
        <view class="card">
          <view class="row" style="padding:14px;">
            <view class="row-icon icon-bg-sky">
              <RocIcon name="lucide:file-text" :size="18" color="#0EA5E9" />
            </view>
            <view class="row-body">
              <text class="row-title">成果大纲.docx</text>
              <text class="row-meta">许萌 · v4（当前）</text>
            </view>
            <view class="status-pill status-pill-emerald"><text>已确认</text></view>
          </view>
          <view class="version-timeline-wrap">
            <view class="row" style="padding:0 14px 8px; justify-content:space-between;">
              <text class="fs-11 text-faint">全部版本 · v4</text>
              <text class="fs-11 text-blue fw-600" @tap="onVersionDiff">版本对比</text>
            </view>
            <view class="h-scroll version-row">
              <view v-for="v in [1,2,3,4]" :key="v" class="version-cell">
                <view :class="['version-circle', v === 4 ? 'version-current' : 'version-normal']">
                  <text v-if="v < 4" class="fs-10 fw-700" :style="{ color: v === 1 ? '#1E40AF' : v === 2 ? '#0369A1' : '#6D28D9' }">{{ v }}</text>
                  <text v-else class="fs-10 fw-700" style="color:#FFFFFF;">{{ v }}</text>
                </view>
                <text class="fs-10 text-faint">{{ v === 1 ? '9月01' : v === 2 ? '9月03' : v === 3 ? '9月07' : '昨天' }}</text>
                <text class="fs-10 fw-600" :style="{ color: v === 4 ? '#2563EB' : v === 1 ? '#1E40AF' : v === 2 ? '#0369A1' : '#6D28D9' }">
                  {{ v === 1 ? '初稿' : v === 2 ? '大纲' : v === 3 ? '分析' : 'v4 当前' }}
                </text>
              </view>
            </view>
          </view>
        </view>
        <view class="row" style="padding:0; justify-content:space-between; margin: 12px 0 8px;">
          <text class="section-title">版本历史</text>
          <text class="fs-11 text-faint">共 4 个版本</text>
        </view>
        <view v-for="(v, vi) in versionHistory" :key="v.version" class="version-history">
          <view class="version-history-marker">
            <view :class="['version-circle', v.version === 4 ? 'version-current' : 'version-past']">
              <RocIcon v-if="v.status === 'done'" name="lucide:check" :size="12" color="#FFFFFF" />
              <text v-else class="fs-10 fw-700" style="color:#FFFFFF;">{{ v.version }}</text>
            </view>
            <view v-if="vi < versionHistory.length - 1" class="version-history-line"></view>
          </view>
          <view class="version-history-body">
            <view class="row" style="padding:0; justify-content:space-between;">
              <view class="row" style="padding:0; gap:6px;">
                <text class="fs-12 fw-700 text-dark">{{ v.user }} {{ v.action }}</text>
                <view :class="['type-chip', v.version === 4 ? 'type-docx' : v.version === 3 ? 'type-img' : v.version === 2 ? 'type-pptx' : 'type-zip']"><text>v{{ v.version }}</text></view>
              </view>
              <text class="fs-10 text-faint">{{ v.time }}</text>
            </view>
            <text class="fs-11 text-emerald fw-600" style="margin-top:2px;">✓ 已确认入库</text>
            <view class="version-history-diff">
              <text v-for="(d, di) in v.diff" :key="di" class="fs-11 text-body">{{ d }}</text>
            </view>
            <text v-if="v.version === 4" class="fs-11 text-blue fw-600" style="margin-top:4px;" @tap="onVersionDiff">查看 v3 → v4 差异 →</text>
          </view>
        </view>
      </view>

      <!-- 任务 -->
      <view v-else-if="activeTab === 'tasks'" class="tab-pane">
        <view class="row" style="padding:0; justify-content:space-between; margin-bottom: 8px;">
          <text class="section-title">任务中心</text>
          <text class="fs-12 text-blue fw-600" @tap="onAddTask">+ 新建任务</text>
        </view>
        <view class="mb-12">
          <text class="eyebrow" style="margin-bottom:6px; display:block;">进行中 · 3</text>
          <view v-for="(tk, ti) in doingTasks" :key="tk.id || ti" :class="['card', 'card-padded', 'mb-8', tk.daysLeft <= 2 ? '' : '']">
            <view class="row" style="padding:0;">
              <view :class="['row-icon', tk.daysLeft <= 2 ? 'icon-bg-rose' : 'icon-bg-blue']">
                <RocIcon :name="tk.icon || 'lucide:clipboard-list'" :size="16" :color="tk.daysLeft <= 2 ? '#F43F5E' : '#2563EB'" />
              </view>
              <view class="row-body">
                <text class="row-title">{{ tk.name }}</text>
                <text class="row-meta">截止 {{ tk.deadline }} · {{ tk.assignee }}负责</text>
              </view>
              <text :class="['status-pill', tk.daysLeft <= 2 ? 'status-pill-rose' : 'status-pill-amber']">{{ tk.daysLeft }}天</text>
            </view>
            <view v-if="tk.progress" class="progress progress-thin" style="margin-top: 10px;">
              <view class="progress-fill" :style="{ width: tk.progress + '%' }"></view>
            </view>
          </view>
        </view>
        <view>
          <text class="eyebrow" style="margin-bottom:6px; display:block;">待认领 · 1</text>
          <view class="card card-padded dashed">
            <view class="row" style="padding:0;">
              <view class="row-icon icon-bg-slate">
                <RocIcon name="lucide:file-text" :size="16" color="#94A3B8" />
              </view>
              <view class="row-body">
                <text class="row-title text-muted">文献综述撰写</text>
                <text class="row-meta">截止 09/20</text>
              </view>
              <text class="fs-12 text-blue fw-600" @tap="onClaimTask">认领</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 成员 -->
      <view v-else-if="activeTab === 'members'" class="tab-pane">
        <view class="row" style="padding:0; justify-content:space-between; margin-bottom: 8px;">
          <text class="section-title">小组成员 · {{ members.length || 4 }}人</text>
          <text class="fs-12 text-blue fw-600" @tap="showInvite = true">+ 邀请</text>
        </view>
        <view v-for="(m, mi) in members" :key="mi" :class="['card', 'card-padded', 'mb-8', m.role === 'leader' ? 'card-leader' : 'card-member']">
          <view class="row" style="padding:0;">
            <view class="avatar avatar-lg" :style="{ background: avatarColorFor(m.nickname) }">{{ avatarCharOf(m.nickname) }}</view>
            <view class="row-body">
              <text class="row-title">{{ m.nickname }}</text>
              <text class="row-meta">{{ m.role === 'leader' ? '组长' : '组员' }} · {{ m.duty || '负责大纲撰写' }}</text>
            </view>
            <view :class="['status-pill', m.role === 'leader' ? 'status-pill-amber' : 'status-pill-slate']">
              <text>{{ m.role === 'leader' ? '组长' : (m.joined ? '已加入' : '组员') }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="safe-bottom" style="height: 24px;"></view>
    </scroll-view>

    <!-- More sheet -->
    <RocSheet :visible="showMore" @update:visible="v => showMore = v" :showHandle="true">
      <view class="sheet-pad">
        <text class="sheet-title">更多操作</text>
        <view class="sheet-item" @tap="onCopyProject"><RocIcon name="lucide:copy" :size="18" color="#475569" /><text>复制项目（带任务）</text></view>
        <view class="sheet-item" @tap="onCopyOnlyStages"><RocIcon name="lucide:layers" :size="18" color="#475569" /><text>复制项目（仅阶段）</text></view>
        <view class="sheet-item" @tap="onContribution"><RocIcon name="lucide:bar-chart" :size="18" color="#475569" /><text>查看贡献度排行</text></view>
        <view class="sheet-item" @tap="onFlowchart"><RocIcon name="lucide:git-branch" :size="18" color="#475569" /><text>系统流程图</text></view>
        <view class="sheet-item" @tap="onComplete"><RocIcon name="lucide:check-circle" :size="18" color="#059669" /><text class="text-emerald">结项</text></view>
        <view class="sheet-item" @tap="onDelete"><RocIcon name="lucide:trash" :size="18" color="#E11D48" /><text class="text-rose">删除项目</text></view>
        <view class="sheet-cancel" @tap="showMore = false"><text>取消</text></view>
      </view>
    </RocSheet>

    <!-- File menu sheet -->
    <RocSheet :visible="showFileMenu" @update:visible="v => showFileMenu = v">
      <view class="sheet-pad">
        <text class="sheet-title">{{ activeFile ? activeFile.name : '' }}</text>
        <view class="sheet-item" @tap="onFilePreview"><RocIcon name="lucide:eye" :size="18" color="#475569" /><text>预览</text></view>
        <view class="sheet-item" @tap="onFileShare"><RocIcon name="lucide:share" :size="18" color="#475569" /><text>分享</text></view>
        <view class="sheet-item" @tap="downloadFile(activeFile)"><RocIcon name="lucide:download" :size="18" color="#475569" /><text>下载</text></view>
        <view class="sheet-item" @tap="onFileVersion"><RocIcon name="lucide:history" :size="18" color="#475569" /><text>版本历史</text></view>
        <view class="sheet-item" @tap="onFileIdle"><RocIcon name="lucide:clock" :size="18" color="#475569" /><text>标记闲置</text></view>
        <view class="sheet-item" @tap="onFileRedundant"><RocIcon name="lucide:copy" :size="18" color="#475569" /><text>标记冗余</text></view>
        <view class="sheet-cancel" @tap="showFileMenu = false"><text>取消</text></view>
      </view>
    </RocSheet>

    <!-- Invite sheet -->
    <RocSheet :visible="showInvite" @update:visible="v => showInvite = v">
      <view class="sheet-pad">
        <text class="sheet-title">邀请新成员</text>
        <view class="invite-link-box">
          <text class="invite-link-text">https://roc-git.com/join/abc1234x</text>
          <text class="invite-copy" @tap="onCopyInvite">复制</text>
        </view>
        <text class="eyebrow" style="display:block; margin: 12px 0 6px;">分享至</text>
        <view class="invite-row">
          <view class="invite-cell" @tap="showToast('已发送微信邀请')"><view class="invite-icon" style="background:#10B981;"><RocIcon name="lucide:message-circle" :size="14" color="#FFFFFF" /></view><text class="invite-label">微信</text></view>
          <view class="invite-cell" @tap="showToast('已生成二维码')"><view class="invite-icon" style="background:#475569;"><RocIcon name="lucide:qr-code" :size="14" color="#FFFFFF" /></view><text class="invite-label">二维码</text></view>
          <view class="invite-cell" @tap="onCopyInvite"><view class="invite-icon" style="background:#475569;"><RocIcon name="lucide:link" :size="14" color="#FFFFFF" /></view><text class="invite-label">链接</text></view>
        </view>
        <text class="eyebrow" style="display:block; margin: 12px 0 6px;">加入身份</text>
        <view class="role-row">
          <view :class="['role-pill', { 'role-on': inviteRole === 'member' }]" @tap="inviteRole = 'member'"><text>组员</text></view>
          <view :class="['role-pill', { 'role-on': inviteRole === 'viewer' }]" @tap="inviteRole = 'viewer'"><text>只读</text></view>
        </view>
        <view class="sheet-cancel" @tap="showInvite = false"><text>完成</text></view>
      </view>
    </RocSheet>

    <!-- Toast -->
    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>
  </view>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { textAvatar } from '../../utils/avatar.js'
import RocIcon from '../../components/Icon.vue'
import RocSheet from '../../components/Sheet.vue'

const safeTop = ref(20)
const tabs = [
  { key: 'overview', label: '概览' },
  { key: 'files', label: '文件' },
  { key: 'stages', label: '阶段' },
  { key: 'versions', label: '版本' },
  { key: 'tasks', label: '任务' },
  { key: 'members', label: '成员' }
]
const activeTab = ref('overview')
const projectId = ref(1)
const project = ref({})
const members = ref([])
const stages = ref([])
const activities = ref([])
const fileList = ref([])
const fileKeyword = ref('')
const doingTasks = ref([])
const versionHistory = ref([])

const showMore = ref(false)
const showFileMenu = ref(false)
const showInvite = ref(false)
const activeFile = ref(null)
const inviteRole = ref('member')

const toast = ref({ show: false, text: '' })

onLoad((q) => {
  projectId.value = Number(q && q.id) || 1
  refreshSafeTop()
  loadAll()
})

function refreshSafeTop() {
  try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {}
}

async function loadAll() {
  await Promise.all([loadProject(), loadStages(), loadMembers(), loadActivities()])
  await loadFiles()
  await loadTasks()
}

async function loadProject() {
  try {
    const p = await api.projectDetail(projectId.value)
    project.value = p || {}
  } catch (e) {
    project.value = {
      name: '社区调研报告', course: '软件工程实践',
      currentStageNo: 2, progress: 68, fileCount: 12,
      pendingCount: 4, versionCount: 18,
      groupName: '第 3 小组', createdAt: '2026-09-01'
    }
  }
}

async function loadStages() {
  try {
    const list = await api.projectStages(projectId.value)
    if (list && list.length) {
      stages.value = list.map((s, i) => ({
        ...s,
        status: s.status || (i === 0 ? 'done' : i === 1 ? 'doing' : 'todo'),
        dateText: s.dateText || (i === 0 ? '9月03日完成' : i === 1 ? '9月14日截止 · 还剩 4 天' : '待开始'),
        progress: s.progress || (i === 1 ? 70 : 0),
        tasks: s.tasks || (i === 1 ? [
          { user: '许萌', name: '访谈整理', date: '09/12', color: '#0EA5E9' },
          { user: '王宁', name: '问卷分析', date: '09/14', color: '#F43F5E', urgent: true },
          { user: '林知行', name: '大纲撰写', date: '09/14', color: '#F59E0B' }
        ] : [])
      }))
    } else {
      stages.value = demoStages()
    }
  } catch (e) { stages.value = demoStages() }
}
function demoStages() {
  return [
    { id: 1, name: '01 选题与分工', status: 'done', dateText: '9月03日完成' },
    { id: 2, name: '02 资料归集', status: 'doing', dateText: '9月14日截止 · 还剩 4 天', progress: 70, tasks: [
      { user: '许萌', name: '访谈整理', date: '09/12', color: '#0EA5E9' },
      { user: '王宁', name: '问卷分析', date: '09/14', color: '#F43F5E', urgent: true },
      { user: '林知行', name: '大纲撰写', date: '09/14', color: '#F59E0B' }
    ] },
    { id: 3, name: '03 分析与成稿', status: 'todo', dateText: '待开始' }
  ]
}

async function loadMembers() {
  try {
    if (project.value.groupId) {
      const res = await api.groupMembers(project.value.groupId, { pageSize: 100 })
      // 后端返回 { total, list, pageNum, pageSize }，list 中每项 { userId, nickname, avatarUrl, role, joinedAt, status }
      const list = (res && (res.list || res.records || res.members)) || []
      if (list.length) {
        members.value = list.map(m => ({
          nickname: m.nickname || ('用户#' + m.userId),
          role: m.role || 'member',
          duty: m.duty || m.remark || '',
          joined: m.status !== 0,
          userId: m.userId,
          avatarUrl: m.avatarUrl
        }))
        return
      }
    }
    members.value = demoMembers()
  } catch (e) { members.value = demoMembers() }
}
function demoMembers() {
  return [
    { nickname: '林知行', role: 'leader', duty: '负责大纲撰写', joined: true },
    { nickname: '许萌', role: 'member', duty: '负责访谈整理', joined: true },
    { nickname: '王宁', role: 'member', duty: '负责问卷分析', joined: true },
    { nickname: '陈思', role: 'member', duty: '负责数据分析', joined: true }
  ]
}

async function loadActivities() {
  try {
    const list = await api.projectActivities(projectId.value, { pageSize: 5 })
    if (list && list.length) {
      activities.value = list.map(a => ({
        ...a,
        user: a.user || (a.userNickname) || ('用户#' + (a.userId || '')),
        action: a.action || a.description || a.title || '',
        time: a.time || a.createdAt || '',
        color: a.color || '#0EA5E9'
      }))
    } else {
      activities.value = [
        { user: '许萌', action: '上传了访谈记录 v2', time: '今天 10:18', color: '#0EA5E9' },
        { user: '王宁', action: '更新了成果大纲 v4', time: '昨天 20:36', color: '#F43F5E' }
      ]
    }
  } catch (e) {
    activities.value = [
      { user: '许萌', action: '上传了访谈记录 v2', time: '今天 10:18', color: '#0EA5E9' },
      { user: '王宁', action: '更新了成果大纲 v4', time: '昨天 20:36', color: '#F43F5E' }
    ]
  }
}

function fileExtOf(name) {
  if (!name) return 'file'
  const i = name.lastIndexOf('.')
  return i < 0 ? 'file' : name.substring(i + 1).toLowerCase()
}
function humanSize(bytes) {
  if (bytes == null) return ''
  if (typeof bytes === 'string') return bytes
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB'
}

async function loadFiles() {
  try {
    const res = await api.request({ url: '/v1/file/list', params: { projectId: projectId.value, pageSize: 100 } })
    const rows = (res && (res.records || res.list || res.items || res)) || []
    if (Array.isArray(rows) && rows.length) {
      fileList.value = rows.map(vo => {
        const u = vo.uploader || {}
        return {
          id: vo.fileId,
          fileId: vo.fileId,
          name: vo.name,
          ext: (vo.type || fileExtOf(vo.name)).toLowerCase(),
          uploader: u.nickname || '',
          version: vo.version || 1,
          size: humanSize(vo.size),
          stage: vo.stageName || '',
          status: (vo.status || 'approved').toLowerCase(),
          url: vo.url
        }
      })
      return
    }
  } catch (e) { /* fall through */ }
  fileList.value = [
    { id: 1, name: '社区访谈整理.docx', ext: 'docx', uploader: '许萌', version: 2, size: '18 KB', stage: '资料归集', status: 'approved' },
    { id: 2, name: '访谈录音转写稿.docx', ext: 'docx', uploader: '许萌', time: '今天 09:42', status: 'pending' },
    { id: 3, name: '调研问卷原始数据.xlsx', ext: 'xlsx', uploader: '林知行', status: 'draft' },
    { id: 4, name: '成果大纲.docx', ext: 'docx', uploader: '王宁', version: 4, status: 'approved' }
  ]
}

async function loadTasks() {
  try {
    const list = await api.taskList(projectId.value, { pageSize: 50 })
    const rows = (list && (list.records || list.list || list.items || list)) || []
    if (Array.isArray(rows) && rows.length) {
      doingTasks.value = rows.map(t => {
        const dl = t.daysLeft != null ? t.daysLeft : (t.dueDate ? Math.ceil((new Date(t.dueDate) - Date.now()) / 86400000) : 0)
        const u = t.assignee || {}
        return {
          id: t.taskId || t.id,
          name: t.title || t.name,
          icon: t.icon || 'lucide:clipboard-list',
          deadline: t.dueDate || t.deadline || '',
          assignee: u.nickname || t.assigneeName || '',
          daysLeft: dl,
          progress: t.progress || 0
        }
      })
      return
    }
  } catch (e) { /* fall through */ }
  doingTasks.value = [
    { id: 1, name: '访谈记录整理', icon: 'lucide:clipboard-list', deadline: '09/12', assignee: '许萌', daysLeft: 4, progress: 60 },
    { id: 2, name: '调研问卷分析', icon: 'lucide:pencil', deadline: '09/14', assignee: '王宁', daysLeft: 2 },
    { id: 3, name: '大纲撰写', icon: 'lucide:file-text', deadline: '09/14', assignee: '林知行', daysLeft: 2 }
  ]
}

// 版本历史：基于项目内第一个文件拉版本（典型 case），更严谨做法是聚合所有文件版本
async function loadVersionHistory() {
  versionHistory.value = [
    { version: 4, user: '王宁', action: '更新成果大纲', time: '昨天 20:36', status: 'done', diff: ['+ 新增第 4 章「问题分析」章节，新增约 1200 字', '~ 修改第 2 章结构，新增小节'] },
    { version: 3, user: '王宁', action: '更新成果大纲', time: '9月07日', status: 'done', diff: ['+ 新增第 3 章「调研方法」', '~ 重新排版，添加目录'] },
    { version: 2, user: '王宁', action: '上传成果大纲', time: '9月03日', status: 'done', diff: ['+ 上传正式版成果大纲，包含目录框架'] },
    { version: 1, user: '王宁', action: '上传成果初稿', time: '9月01日', status: 'done', diff: ['+ 首次上传成果初稿'] }
  ]
  // 尝试从真实接口拉取首个文件的版本
  const first = fileList.value[0]
  if (first && first.fileId) {
    try {
      const versions = await api.fileVersions(first.fileId)
      if (Array.isArray(versions) && versions.length) {
        versionHistory.value = versions.slice(0, 4).map((v, i) => ({
          version: v.version || (versions.length - i),
          user: (v.uploader && v.uploader.nickname) || v.user || '成员',
          action: v.changeLog || v.action || '更新文件',
          time: v.createdAt || v.time || '',
          status: 'done',
          diff: v.diff || []
        }))
      }
    } catch (e) { /* keep demo */ }
  }
}

function switchTab(k) { activeTab.value = k }

function goBack() { uni.navigateBack() }
function onUpload() {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      if (res.tempFilePaths && res.tempFilePaths[0]) {
        api.uploadFile(res.tempFilePaths[0], { projectId: projectId.value })
          .then(() => { showToast('已上传'); loadFiles() })
          .catch(e => showToast((e && e.message) || '上传失败'))
      }
    }
  })
}
function approveFile(f) { showToast('已通过 ' + f.name); f.status = 'approved' }
function rejectFile(f) { showToast('已驳回 ' + f.name); fileList.value = fileList.value.filter(x => x !== f) }
function downloadFile(f) { showToast('开始下载 ' + f.name) }
function showFileMenuFor(f) { activeFile.value = f; showFileMenu.value = true }

function onCopyProject() { showMore.value = false; showToast('已复制（带任务）') }
function onCopyOnlyStages() { showMore.value = false; showToast('已复制（仅阶段）') }
function onContribution() { showMore.value = false; uni.navigateTo({ url: '/pages/contribution/contribution?projectId=' + projectId.value }) }
function onFlowchart() { showMore.value = false; uni.navigateTo({ url: '/pages/flowchart/flowchart' }) }
function onComplete() { showMore.value = false; api.projectComplete(projectId.value).then(() => showToast('已结项')).catch(() => showToast('已结项')) }
function onDelete() { showMore.value = false; uni.showModal({ title: '删除项目', content: '确定要删除项目吗?', success: r => { if (r.confirm) api.projectDelete(projectId.value).then(() => { showToast('已删除'); setTimeout(goBack, 600) }) } }) }
function onAddStage() { showToast('添加阶段') }
function onAddTask() { showToast('添加任务') }
function onClaimTask() { showToast('已认领') }
function onVersionDiff() { showToast('打开版本对比') }
function onCopyInvite() { uni.setClipboardData({ data: 'https://roc-git.com/join/abc1234x' }); showToast('已复制') }
function onFilePreview() { showFileMenu.value = false; showToast('预览 ' + (activeFile.value && activeFile.value.name)) }
function onFileShare() { showFileMenu.value = false; showToast('打开分享') }
function onFileVersion() { showFileMenu.value = false; switchTab('versions') }
function onFileIdle() { showFileMenu.value = false; showToast('已标记闲置') }
function onFileRedundant() { showFileMenu.value = false; showToast('已标记冗余') }

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}

function avatarCharOf(n) {
  const s = typeof n === 'string' ? n : (n && (n.nickname || n.name)) || '?'
  return textAvatar(s || '?').char
}
function avatarColorFor(n) {
  const s = typeof n === 'string' ? n : (n && (n.nickname || n.name)) || '?'
  return textAvatar(s || '?').color
}

function stageNumClass(s) {
  if (s.status === 'done') return 'stage-num-done'
  if (s.status === 'doing') return 'stage-num-doing'
  return 'stage-num-todo'
}
function stageNumColor(s) { return s.status === 'doing' ? '#2563EB' : '#94A3B8' }
function stageStatusClass(s) {
  if (s.status === 'done') return 'status-pill-emerald'
  if (s.status === 'doing') return 'status-pill-blue'
  return 'status-pill-slate'
}
function stageStatusLabel(s) {
  if (s.status === 'done') return '已完成'
  if (s.status === 'doing') return '进行中'
  return '未开始'
}
function stageCardClass(s) {
  if (s.status === 'doing') return 'card-doing'
  if (s.status === 'done') return 'card-done'
  return 'card-todo'
}
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
</script>

<style scoped>
.project-page { background: var(--bg-system); }
.content { flex: 1; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; padding-bottom: 12px; }
.topbar-row { padding: 8px 16px 6px; display: flex; align-items: center; justify-content: space-between; }
.topbar-center { flex: 1; text-align: center; }
.topbar-title { font-size: 15px; font-weight: 600; color: #0F172A; display: block; }
.topbar-sub { font-size: 11px; color: #94A3B8; margin-top: 2px; display: block; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; position: relative; }
.header-progress { padding: 0 20px 4px; }
.header-meta { padding: 8px 20px 0; display: flex; align-items: center; justify-content: space-between; }

.avatar-stack { display: flex; align-items: center; }
.stack-item { border: 2px solid #FFFFFF; }
.stack-item + .stack-item { margin-left: -8px; }
.stack-more { background: #F1F5F9; color: #64748B; border: 2px solid #FFFFFF; }

.tabbar-row { display: flex; background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.tab-item { flex: 1; padding: 12px 8px; text-align: center; font-size: 13px; font-weight: 500; color: #94A3B8; border-bottom: 2px solid transparent; white-space: nowrap; }
.tab-on { color: #2563EB; border-bottom-color: #2563EB; }

.tab-pane { padding: 16px 20px; }

.stat-grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.stat-mini { border-radius: 16px; padding: 12px; text-align: center; transition: transform 0.15s ease, box-shadow 0.15s ease; }
.stat-mini:active { transform: translateY(-1px); box-shadow: 0 4px 10px rgba(15,23,42,0.08); }
.stat-mini-num { display: block; font-size: 20px; font-weight: 700; line-height: 1.1; }
.stat-mini-lbl { display: block; font-size: 11px; margin-top: 4px; }
.stat-blue { background: #EFF6FF; }
.stat-blue .stat-mini-num, .stat-blue .stat-mini-lbl { color: #2563EB; }
.stat-amber { background: #FFFBEB; }
.stat-amber .stat-mini-num, .stat-amber .stat-mini-lbl { color: #D97706; }
.stat-emerald { background: #ECFDF5; }
.stat-emerald .stat-mini-num, .stat-emerald .stat-mini-lbl { color: #059669; }

.card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px; box-shadow: 0 1px 2px rgba(15,23,42,0.04); margin-top: 12px; }
.card-padded { padding: 14px; }
.card-title { font-size: 13px; font-weight: 600; color: #0F172A; display: block; margin-bottom: 12px; }

.kv-list { display: flex; flex-direction: column; gap: 10px; }
.kv { display: flex; justify-content: space-between; align-items: center; }
.kv-k { font-size: 12px; color: #64748B; }
.kv-v { font-size: 12px; color: #0F172A; }

.stage-list { display: flex; flex-direction: column; gap: 10px; }
.stage-item { display: flex; align-items: center; gap: 10px; }
.stage-num { width: 28px; height: 28px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stage-num-done { background: #D1FAE5; }
.stage-num-doing { background: #DBEAFE; }
.stage-num-todo { background: #F1F5F9; }
.stage-num-sm { width: 24px; height: 24px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.stage-body { flex: 1; min-width: 0; }
.stage-name { font-size: 12px; font-weight: 500; display: block; }
.stage-date { font-size: 10px; color: #94A3B8; margin-top: 2px; display: block; }
.stage-card { background: #FFFFFF; border-radius: 16px; padding: 14px; margin-bottom: 12px; border: 1px solid #E2E8F0; }
.card-doing { border-color: #2563EB; border-width: 2px; }
.card-done { border-color: #10B981; border-width: 2px; }
.card-todo { border-style: dashed; }
.progress-row { margin-top: 10px; }

.task-list { margin-top: 10px; padding-top: 10px; border-top: 1px solid #F1F5F9; }
.task-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
.flex-1 { flex: 1; }

.status-pill { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; }
.status-pill-rose { background: #FFF1F2; color: #BE123C; }
.status-pill-amber { background: #FFFBEB; color: #92400E; }
.status-pill-emerald { background: #ECFDF5; color: #047857; }
.status-pill-blue { background: #EFF6FF; color: #1D4ED8; }
.status-pill-slate { background: #F1F5F9; color: #475569; }

.activity-row { display: flex; align-items: center; gap: 10px; padding: 8px 4px; border-radius: 8px; transition: background 0.15s ease; }
.activity-row:active { background: #F8FAFC; }
.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.row-title { font-size: 12px; color: #334155; line-height: 1.4; display: block; }
.row-meta { font-size: 10px; color: #94A3B8; margin-top: 2px; display: block; }
.row-action { flex-shrink: 0; }
.row-icon { width: 36px; height: 36px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-bg-blue { background: #EFF6FF; }
.icon-bg-sky { background: #F0F9FF; }
.icon-bg-violet { background: #F5F3FF; }
.icon-bg-emerald { background: #ECFDF5; }
.icon-bg-amber { background: #FFFBEB; }
.icon-bg-rose { background: #FFF1F2; }
.icon-bg-slate { background: #F1F5F9; }

.btn { display: flex; align-items: center; justify-content: center; gap: 6px; }
.btn-text { color: #FFFFFF; font-weight: 600; }

.search-row { display: flex; gap: 8px; margin-top: 12px; }
.search-box { flex: 1; height: 40px; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; display: flex; align-items: center; padding: 0 12px; gap: 6px; }
.search-input { flex: 1; height: 40px; font-size: 13px; color: #0F172A; background: transparent; border: none; outline: none; }

.file-list { margin-top: 12px; }
.file-row { display: flex; align-items: center; gap: 12px; background: #FFFFFF; border-radius: 16px; padding: 12px; margin-bottom: 8px; border: 1px solid #E2E8F0; }
.file-pending { background: #FFFBEB; border-color: #FEF3C7; }
.file-draft { background: #FFFFFF; border-style: dashed; }
.file-approved { background: #FFFFFF; }
.icon-btn-mini { width: 28px; height: 28px; border-radius: 8px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }
.icon-btn-rose { background: #FEE2E2; }
.icon-btn-blue { background: #2563EB; }
.mb-8 { margin-bottom: 8px; }
.mb-12 { margin-bottom: 12px; }
.dashed { border-style: dashed; }

.version-timeline-wrap { padding: 0 14px 14px; }
.version-row { gap: 12px; padding: 8px 0; }
.version-cell { display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 44px; }
.version-circle { width: 24px; height: 24px; border-radius: 999px; display: flex; align-items: center; justify-content: center; }
.version-current { background: #2563EB; box-shadow: 0 0 0 2px #BFDBFE; }
.version-normal { background: #DBEAFE; }
.version-past { background: #7C3AED; }

.version-history { display: flex; gap: 12px; margin-bottom: 4px; }
.version-history-marker { display: flex; flex-direction: column; align-items: center; }
.version-history-line { width: 1px; flex: 1; background: #E2E8F0; margin-top: 4px; }
.version-history-body { flex: 1; padding-bottom: 16px; }
.version-history-diff { background: #F8FAFC; border-radius: 12px; padding: 8px 10px; margin-top: 6px; display: flex; flex-direction: column; gap: 2px; }

.type-chip { display: inline-flex; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 700; }
.type-docx { background: #EFF6FF; color: #2563EB; }
.type-img { background: #F5F3FF; color: #7C3AED; }
.type-pptx { background: #F0F9FF; color: #0284C7; }
.type-zip { background: #F1F5F9; color: #475569; }

.eyebrow { font-size: 11px; font-weight: 600; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.05em; }
.section-title { font-size: 14px; font-weight: 600; color: #0F172A; }
.progress { position: relative; width: 100%; height: 6px; background: #F1F5F9; border-radius: 999px; overflow: hidden; }
.progress-thin { height: 4px; }
.progress-fill { position: absolute; top: 0; left: 0; bottom: 0; background: linear-gradient(90deg, #3B82F6, #2563EB); border-radius: 999px; }

.text-faint { color: #94A3B8; }
.text-muted { color: #64748B; }
.text-dark { color: #0F172A; }
.text-blue { color: #2563EB; }
.text-emerald { color: #059669; }
.text-rose { color: #E11D48; }
.text-amber { color: #D97706; }
.fw-600 { font-weight: 600; }
.fw-700 { font-weight: 700; }
.fs-10 { font-size: 10px; } .fs-11 { font-size: 11px; } .fs-12 { font-size: 12px; } .fs-13 { font-size: 13px; } .fs-14 { font-size: 14px; }
.card-leader { background: #FFFBEB; border-color: #FDE68A; }
.card-member { background: #FFFFFF; }

.sheet-pad { padding: 16px 20px 8px; }
.sheet-title { font-size: 15px; font-weight: 700; color: #0F172A; display: block; margin-bottom: 12px; text-align: center; }
.sheet-item { display: flex; align-items: center; gap: 12px; padding: 14px 4px; border-bottom: 1px solid #F1F5F9; font-size: 14px; color: #0F172A; }
.sheet-item:last-of-type { border-bottom: none; }
.sheet-cancel { margin: 12px -20px -8px; padding: 16px 0; text-align: center; font-size: 14px; color: #64748B; background: #F8FAFC; border-top: 8px solid #FFFFFF; }

.invite-link-box { display: flex; align-items: center; gap: 8px; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 10px 12px; }
.invite-link-text { flex: 1; font-size: 11px; color: #64748B; word-break: break-all; }
.invite-copy { font-size: 12px; font-weight: 600; color: #2563EB; flex-shrink: 0; }
.invite-row { display: flex; gap: 8px; }
.invite-cell { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.invite-icon { width: 36px; height: 36px; border-radius: 999px; display: flex; align-items: center; justify-content: center; }
.invite-label { font-size: 11px; color: #64748B; }
.role-row { display: flex; gap: 8px; }
.role-pill { flex: 1; padding: 10px; text-align: center; background: #F1F5F9; color: #475569; border-radius: 10px; font-size: 13px; font-weight: 500; }
.role-on { background: #EFF6FF; color: #2563EB; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
</style>
