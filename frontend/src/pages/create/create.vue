<template>
  <view class="page create-page">
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:x" :size="20" color="#475569" />
        </view>
        <view class="topbar-center">
          <text class="topbar-title">创建项目</text>
          <text class="topbar-sub">新建设协作</text>
        </view>
        <view class="btn btn-ghost" style="height:30px; line-height:30px; padding:0 10px; font-size:11px;" @tap="onSaveDraft">
          <RocIcon name="lucide:bookmark" :size="12" color="#475569" />
          <text>保存草稿</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <!-- Project info card -->
      <view class="pane">
        <text class="card-title">项目信息</text>
        <view class="form-group">
          <text class="form-label">项目名称 <text class="text-rose">*</text></text>
          <view class="input-wrap">
            <input class="text-input" v-model="form.name" placeholder="如：校园文化调研" />
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">所属课程 <text class="text-rose">*</text></text>
          <view class="input-wrap" @tap="onPickCourse">
            <text class="text-input text-muted" v-if="!form.course">请选择课程</text>
            <text class="text-input" v-else>{{ form.course }}</text>
            <RocIcon name="lucide:chevron-down" :size="14" color="#94A3B8" />
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">项目类型</text>
          <view class="chip-row">
            <view v-for="t in types" :key="t" :class="['chip', form.type === t ? 'chip-blue' : 'chip-outline']" @tap="form.type = t">
              <text>{{ t }}</text>
            </view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">项目简介</text>
          <view class="input-wrap textarea-wrap">
            <textarea class="text-input" v-model="form.description" placeholder="简要描述项目目标与背景（选填）" />
          </view>
        </view>
      </view>

      <!-- Members -->
      <view class="pane">
        <text class="card-title">小组成员</text>
        <view class="member-list">
          <view v-for="(m, mi) in form.members" :key="mi" :class="['member-card', m.role === 'leader' ? 'member-leader' : '']">
            <view class="row" style="padding:0; gap:10px;">
              <view class="avatar avatar-lg" :style="{ background: m.color, fontSize: '18px' }">{{ m.char }}</view>
              <view class="row-body">
                <view class="row" style="padding:0; gap:6px;">
                  <text class="fs-13 fw-700 text-dark">{{ m.name }}</text>
                  <view v-if="m.role === 'leader'" class="status-pill status-pill-amber"><text>组长</text></view>
                </view>
                <text class="fs-11 text-muted" style="display:block; margin-top:2px;">{{ m.role === 'leader' ? '组长' : '组员' }} · {{ m.duty || '负责访谈整理' }}</text>
              </view>
              <view v-if="m.role !== 'leader'" class="icon-btn-mini" @tap="removeMember(mi)">
                <RocIcon name="lucide:x" :size="12" color="#94A3B8" />
              </view>
            </view>
          </view>
          <view class="add-member-card" @tap="showAddMember = true">
            <RocIcon name="lucide:plus" :size="20" color="#94A3B8" />
            <text class="fs-12 text-muted" style="display:block; margin-top:4px;">添加成员</text>
          </view>
        </view>
      </view>

      <!-- Stages -->
      <view class="pane">
        <text class="card-title">作业阶段</text>
        <view class="stage-list">
          <view v-for="(s, si) in form.stages" :key="s.id" :class="['stage-card', stageCardClass(s)]">
            <view class="row" style="padding:0; gap:10px;">
              <view :class="['stage-circle', stageCircleClass(s)]">
                <RocIcon v-if="s.status === 'done'" name="lucide:check" :size="14" color="#059669" />
                <text v-else class="fs-12 fw-700" :style="{ color: s.status === 'doing' ? '#2563EB' : '#94A3B8' }">{{ si + 1 }}</text>
              </view>
              <view class="row-body">
                <view class="input-wrap stage-input">
                  <input class="text-input" v-model="s.name" :placeholder="'阶段 ' + (si + 1) + ' 名称'" />
                </view>
                <view class="row" style="padding:0; gap:6px; margin-top:4px;">
                  <view class="date-underline" @tap="onPickStageDate(si)">
                    <text :class="['fs-11', s.deadline ? 'text-dark fw-600' : 'text-faint']">
                      {{ s.deadline || '点击设置截止日期' }}
                    </text>
                  </view>
                </view>
              </view>
              <view v-if="form.stages.length > 1" class="icon-btn-mini" @tap="removeStage(si)">
                <RocIcon name="lucide:x" :size="12" color="#94A3B8" />
              </view>
            </view>
          </view>
        </view>
        <view v-if="form.stages.length < 10" class="add-stage" @tap="addStage">
          <RocIcon name="lucide:plus" :size="14" color="#475569" />
          <text class="fs-12 text-body fw-600">添加阶段（最多 10）</text>
        </view>
      </view>

      <!-- Rules -->
      <view class="pane">
        <text class="card-title">协作规则</text>
        <view class="rule-row" @tap="form.reviewRequired = !form.reviewRequired">
          <view class="row-body">
            <text class="fs-13 fw-600 text-dark">挂单审核</text>
            <text class="fs-11 text-muted" style="display:block; margin-top:2px;">上传文件需经审核入库</text>
          </view>
          <view :class="['switch', form.reviewRequired ? 'switch-on' : '']"></view>
        </view>
        <view class="rule-row" @tap="form.dueReminder = !form.dueReminder">
          <view class="row-body">
            <text class="fs-13 fw-600 text-dark">截止提醒</text>
            <text class="fs-11 text-muted" style="display:block; margin-top:2px;">任务临期前 24h 推送通知</text>
          </view>
          <view :class="['switch', form.dueReminder ? 'switch-on' : '']"></view>
        </view>
      </view>

      <view style="height: 100px;"></view>
    </scroll-view>

    <view class="bottom-bar">
      <view class="btn btn-ghost" style="flex:1; height:48px; line-height:48px; padding:0;" @tap="goBack"><text>取消</text></view>
      <view class="btn btn-accent" style="flex:2; height:48px; line-height:48px; padding:0; box-shadow: 0 4px 12px rgba(37,99,235,0.30);" @tap="onCreate">
        <RocIcon name="lucide:rocket" :size="14" color="#FFFFFF" />
        <text class="text-white fw-700">创建项目</text>
      </view>
    </view>

    <!-- Add member sheet -->
    <RocSheet v-if="false" />

    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { textAvatar } from '../../utils/avatar.js'
import RocIcon from '../../components/Icon.vue'
import RocSheet from '../../components/Sheet.vue'

const safeTop = ref(20)
const types = ['调研报告', '课程作业', '毕业设计', '创新创业']
const showAddMember = ref(false)
const toast = ref({ show: false, text: '' })

const form = ref({
  name: '校园文化调研',
  course: '',
  type: '调研报告',
  description: '面向全校学生的校园文化活动参与度与满意度调研',
  members: [
    { name: '林知行', char: '林', color: '#F59E0B', role: 'leader', duty: '负责大纲撰写' },
    { name: '许萌', char: '许', color: '#0EA5E9', role: 'member', duty: '负责访谈整理' }
  ],
  stages: [
    { id: 1, name: '选题与分工', deadline: '2026/09/03', status: 'done' },
    { id: 2, name: '资料归集', deadline: '2026/09/14', status: 'doing' },
    { id: 3, name: '分析与成稿', deadline: '', status: 'todo' }
  ],
  reviewRequired: true,
  dueReminder: true
})

onLoad(() => { refreshSafeTop() })
function refreshSafeTop() { try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {} }

function stageCardClass(s) {
  if (s.status === 'doing') return 'stage-card-doing'
  if (s.status === 'done') return 'stage-card-done'
  return 'stage-card-todo'
}
function stageCircleClass(s) {
  if (s.status === 'doing') return 'stage-circle-doing'
  if (s.status === 'done') return 'stage-circle-done'
  return 'stage-circle-todo'
}
function addStage() {
  form.value.stages.push({ id: Date.now(), name: '', deadline: '', status: 'todo' })
}
function removeStage(i) { form.value.stages.splice(i, 1) }
function removeMember(i) { form.value.members.splice(i, 1) }

function onPickCourse() {
  uni.showActionSheet({
    itemList: ['软件工程实践', '统计学原理', '创业基础', '高等数学', '其他'],
    success: (res) => {
      const list = ['软件工程实践', '统计学原理', '创业基础', '高等数学', '其他']
      form.value.course = list[res.tapIndex]
    }
  })
}
function onPickStageDate(i) {
  // 演示：用 prompt 输入日期
  uni.showActionSheet({
    itemList: ['2026/09/14', '2026/09/20', '2026/09/30', '2026/10/15'],
    success: (res) => {
      const list = ['2026/09/14', '2026/09/20', '2026/09/30', '2026/10/15']
      form.value.stages[i].deadline = list[res.tapIndex]
    }
  })
}

function onSaveDraft() { showToast('已保存为草稿') }
async function onCreate() {
  if (!form.value.name) return showToast('请填写项目名称')
  if (!form.value.course) return showToast('请选择课程')
  try {
    await api.createProject({
      name: form.value.name,
      description: form.value.description,
      course: form.value.course,
      type: form.value.type,
      members: form.value.members.map(m => m.name),
      stages: form.value.stages,
      reviewRequired: form.value.reviewRequired,
      dueReminder: form.value.dueReminder
    })
    showToast('项目已创建')
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) {
    showToast((e && e.message) || '创建失败')
  }
}
function goBack() { uni.navigateBack() }

function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.create-page { background: var(--bg-system); }
.content { flex: 1; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; display: flex; align-items: center; justify-content: space-between; }
.topbar-center { flex: 1; text-align: center; }
.topbar-title { font-size: 16px; font-weight: 600; color: #0F172A; display: block; }
.topbar-sub { font-size: 11px; color: #94A3B8; margin-top: 2px; display: block; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }

.pane { padding: 16px 20px 0; }
.card-title { font-size: 14px; font-weight: 600; color: #0F172A; display: block; margin-bottom: 12px; }

.form-group { margin-bottom: 12px; }
.form-label { font-size: 11px; font-weight: 600; color: #475569; display: block; margin-bottom: 6px; }
.input-wrap { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 0 12px; height: 40px; display: flex; align-items: center; }
.textarea-wrap { height: 80px; align-items: flex-start; padding: 8px 12px; }
.text-input { flex: 1; height: 40px; font-size: 13px; color: #0F172A; background: transparent; border: none; outline: none; padding: 0; }

.chip-row { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { display: inline-flex; align-items: center; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 500; transition: all 0.15s ease; }
.chip:active { transform: scale(0.96); }
.chip-blue { background: #EFF6FF; color: #2563EB; border: 1px solid #DBEAFE; }
.chip-outline { background: transparent; color: #475569; border: 1px solid #E2E8F0; }

.member-list { display: flex; flex-direction: column; gap: 8px; }
.member-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; padding: 12px; }
.member-leader { background: #FFFBEB; border-color: #FDE68A; }
.add-member-card { background: transparent; border: 2px dashed #CBD5E1; border-radius: 12px; padding: 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.icon-btn-mini { width: 24px; height: 24px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }

.row { display: flex; align-items: center; gap: 12px; }
.row-body { flex: 1; min-width: 0; }
.avatar { display: inline-flex; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: 999px; font-size: 14px; font-weight: 700; color: #FFFFFF; flex-shrink: 0; }
.avatar-lg { width: 44px; height: 44px; font-size: 16px; }

.stage-list { display: flex; flex-direction: column; gap: 8px; }
.stage-card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; padding: 12px; transition: transform 0.15s ease, box-shadow 0.15s ease; }
.stage-card:active { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(15,23,42,0.05); }
.stage-card-doing { border-color: #2563EB; border-width: 2px; }
.stage-card-done { border-color: #10B981; border-width: 2px; }
.stage-card-todo { border-style: dashed; }
.stage-circle { width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stage-circle-done { background: #D1FAE5; }
.stage-circle-doing { background: #DBEAFE; border: 2px solid #2563EB; }
.stage-circle-todo { background: #F1F5F9; border: 2px dashed #CBD5E1; }
.stage-input { background: transparent; border: none; padding: 0; height: 24px; margin-top: 4px; }
.stage-input .text-input { height: 24px; }
.date-underline { padding: 4px 0; border-bottom: 1px dashed #CBD5E1; flex: 1; }
.add-stage { display: flex; align-items: center; gap: 4px; padding: 10px; border: 2px dashed #CBD5E1; border-radius: 12px; margin-top: 8px; justify-content: center; }

.rule-row { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
.rule-row:last-of-type { border-bottom: none; }
.switch { position: relative; display: inline-block; width: 40px; height: 22px; background: #CBD5E1; border-radius: 999px; flex-shrink: 0; transition: background 0.2s ease; }
.switch::after { content: ''; position: absolute; top: 2px; left: 2px; width: 18px; height: 18px; background: #FFFFFF; border-radius: 999px; transition: left 0.2s ease; box-shadow: 0 1px 3px rgba(0,0,0,0.15); }
.switch-on { background: #2563EB; }
.switch-on::after { left: 20px; }

.bottom-bar { display: flex; gap: 8px; padding: 12px 20px calc(12px + env(safe-area-inset-bottom)); background: #FFFFFF; border-top: 1px solid #F1F5F9; }

.text-faint { color: #94A3B8; }
.text-muted { color: #64748B; }
.text-dark { color: #0F172A; }
.text-body { color: #334155; }
.text-rose { color: #E11D48; }
.text-white { color: #FFFFFF; }
.fw-600 { font-weight: 600; }
.fw-700 { font-weight: 700; }
.fs-11 { font-size: 11px; } .fs-12 { font-size: 12px; } .fs-13 { font-size: 13px; }
.status-pill { display: inline-flex; align-items: center; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 700; }
.status-pill-amber { background: #FEF3C7; color: #92400E; }

.btn { display: flex; align-items: center; justify-content: center; gap: 6px; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
</style>
