<template>
  <view class="page settings-page">
    <view class="topbar" :style="{ paddingTop: safeTop + 'px' }">
      <view class="topbar-row">
        <view class="icon-btn" @tap="goBack">
          <RocIcon name="lucide:chevron-left" :size="20" color="#475569" />
        </view>
        <view class="topbar-center">
          <text class="topbar-title">系统设置</text>
        </view>
        <view style="width:36px;"></view>
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <!-- Account -->
      <view class="section">
        <text class="section-name">账号</text>
        <view class="card">
          <view class="row-item">
            <view class="row-body">
              <text class="fs-13 fw-600 text-dark">账号信息</text>
              <text class="fs-11 text-muted" style="display:block; margin-top:2px;">178****6021</text>
            </view>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="row-item">
            <view class="row-body">
              <text class="fs-13 fw-600 text-dark">微信</text>
              <text class="fs-11 text-emerald" style="display:block; margin-top:2px;">已绑定</text>
            </view>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <!-- Notifications -->
      <view class="section">
        <text class="section-name">通知</text>
        <view class="card">
          <view class="row-item" @tap="settings.push = !settings.push">
            <text class="fs-13 fw-600 text-dark">推送通知</text>
            <view :class="['switch', settings.push ? 'switch-on' : '']"></view>
          </view>
          <view class="row-item" @tap="settings.sound = !settings.sound">
            <text class="fs-13 fw-600 text-dark">声音提醒</text>
            <view :class="['switch', settings.sound ? 'switch-on' : '']"></view>
          </view>
          <view class="row-item" @tap="settings.vibrate = !settings.vibrate">
            <text class="fs-13 fw-600 text-dark">震动</text>
            <view :class="['switch', settings.vibrate ? 'switch-on' : '']"></view>
          </view>
          <view class="row-item">
            <text class="fs-13 fw-600 text-dark flex-1">通知规则</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <!-- Appearance -->
      <view class="section">
        <text class="section-name">外观</text>
        <view class="card">
          <view class="row-item" @tap="settings.dark = !settings.dark">
            <text class="fs-13 fw-600 text-dark">深色模式</text>
            <view :class="['switch', settings.dark ? 'switch-on' : '']"></view>
          </view>
          <view class="row-item">
            <text class="fs-13 fw-600 text-dark flex-1">字号</text>
            <text class="fs-12 text-muted">标准</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="row-item" @tap="onPickLocale">
            <text class="fs-13 fw-600 text-dark flex-1">语言</text>
            <text class="fs-12 text-muted">{{ currentLocaleLabel }}</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <!-- Storage -->
      <view class="section">
        <text class="section-name">存储与数据</text>
        <view class="card">
          <view class="storage-info">
            <view class="row" style="padding:0; justify-content:space-between;">
              <text class="fs-13 fw-600 text-dark">已用空间</text>
              <text class="fs-12 text-muted">1.2 GB / 5 GB</text>
            </view>
            <view class="progress progress-thin" style="margin-top: 8px;">
              <view class="progress-fill" style="width: 24%;"></view>
            </view>
          </view>
          <view class="row-item">
            <text class="fs-13 fw-600 text-dark flex-1">下载管理</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="row-item" @tap="onClearCache">
            <text class="fs-13 fw-600 text-dark flex-1">清理缓存</text>
            <text class="fs-12 text-muted">28.5 MB</text>
          </view>
          <view class="row-item" @tap="settings.wifiOnly = !settings.wifiOnly">
            <text class="fs-13 fw-600 text-dark">仅 Wi-Fi 下载</text>
            <view :class="['switch', settings.wifiOnly ? 'switch-on' : '']"></view>
          </view>
        </view>
      </view>

      <!-- Privacy -->
      <view class="section">
        <text class="section-name">隐私</text>
        <view class="card">
          <view class="row-item" @tap="settings.hideActivity = !settings.hideActivity">
            <text class="fs-13 fw-600 text-dark">隐藏项目动态</text>
            <view :class="['switch', settings.hideActivity ? 'switch-on' : '']"></view>
          </view>
          <view class="row-item">
            <text class="fs-13 fw-600 text-dark flex-1">隐私协议</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="row-item">
            <text class="fs-13 fw-600 text-dark flex-1">用户协议</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <!-- Other -->
      <view class="section">
        <text class="section-name">其他</text>
        <view class="card">
          <view class="row-item" @tap="showToast('打开帮助中心')">
            <text class="fs-13 fw-600 text-dark flex-1">帮助中心</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="row-item" @tap="showToast('意见反馈已发送')">
            <text class="fs-13 fw-600 text-dark flex-1">意见反馈</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
          <view class="row-item" @tap="showToast('感谢您的支持！')">
            <text class="fs-13 fw-600 text-dark flex-1">给 ROC-git 评分</text>
            <RocIcon name="lucide:external-link" :size="14" color="#94A3B8" />
          </view>
          <view class="row-item">
            <text class="fs-13 fw-600 text-dark flex-1">关于 v1.2.0</text>
            <RocIcon name="lucide:chevron-right" :size="14" color="#CBD5E1" />
          </view>
        </view>
      </view>

      <view class="logout-wrap">
        <view class="btn-logout" @tap="onLogout">退出登录</view>
      </view>

      <view class="safe-bottom" style="height: 24px;"></view>
    </scroll-view>

    <view v-if="toast.show" class="toast"><text class="toast-text">{{ toast.text }}</text></view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '../../utils/user-store.js'
import { useI18n, setLocale, LOCALES } from '../../utils/i18n.js'
import RocIcon from '../../components/Icon.vue'

const store = useUserStore()
const { locale: currentLocale } = useI18n()
const safeTop = ref(20)
const toast = ref({ show: false, text: '' })
const settings = ref({
  push: true, sound: true, vibrate: false, dark: false, wifiOnly: true, hideActivity: false
})

const currentLocaleLabel = computed(() => {
  const m = { zh_CN: '简体中文', en_US: 'English' }
  return m[currentLocale.value] || '简体中文'
})

onLoad(() => { refreshSafeTop() })
function refreshSafeTop() { try { safeTop.value = uni.getSystemInfoSync().statusBarHeight || 20 } catch (e) {} }

function goBack() { uni.navigateBack() }
async function onPickLocale() {
  uni.showActionSheet({
    itemList: LOCALES.map(l => l.label),
    success: async (res) => {
      const target = LOCALES[res.tapIndex]
      if (target) {
        await setLocale(target.key)
        showToast('语言已切换为 ' + target.label)
      }
    }
  })
}
function onClearCache() { showToast('已清理 28.5 MB 缓存') }
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
function showToast(text) {
  toast.value = { show: true, text }
  setTimeout(() => { toast.value.show = false }, 1500)
}
</script>

<style scoped>
.settings-page { background: var(--bg-system); }
.content { flex: 1; }
.topbar { background: #FFFFFF; border-bottom: 1px solid #F1F5F9; }
.topbar-row { padding: 8px 20px 10px; display: flex; align-items: center; justify-content: space-between; }
.topbar-center { flex: 1; text-align: center; }
.topbar-title { font-size: 16px; font-weight: 600; color: #0F172A; display: block; }
.icon-btn { width: 36px; height: 36px; border-radius: 999px; background: #F1F5F9; display: flex; align-items: center; justify-content: center; }

.section { padding: 16px 20px 0; }
.section-name { font-size: 11px; font-weight: 600; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 8px; padding-left: 4px; }
.card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 16px; overflow: hidden; }
.row-item { display: flex; align-items: center; gap: 10px; padding: 14px; border-bottom: 1px solid #F1F5F9; }
.row-item:last-of-type { border-bottom: none; }
.row-body { flex: 1; min-width: 0; }
.flex-1 { flex: 1; }

.storage-info { padding: 14px; border-bottom: 1px solid #F1F5F9; }
.progress { position: relative; width: 100%; height: 6px; background: #F1F5F9; border-radius: 999px; overflow: hidden; }
.progress-thin { height: 4px; }
.progress-fill { position: absolute; top: 0; left: 0; bottom: 0; background: linear-gradient(90deg, #3B82F6, #2563EB); border-radius: 999px; }

.switch { position: relative; display: inline-block; width: 40px; height: 22px; background: #CBD5E1; border-radius: 999px; flex-shrink: 0; transition: background 0.2s ease; }
.switch::after { content: ''; position: absolute; top: 2px; left: 2px; width: 18px; height: 18px; background: #FFFFFF; border-radius: 999px; transition: left 0.2s ease; box-shadow: 0 1px 3px rgba(0,0,0,0.15); }
.switch-on { background: #2563EB; }
.switch-on::after { left: 20px; }

.logout-wrap { padding: 24px 20px 0; }
.btn-logout { background: #FFFFFF; color: #E11D48; border: 1.5px solid #FECDD3; height: 48px; line-height: 44px; border-radius: 12px; font-size: 14px; font-weight: 600; text-align: center; }

.text-faint { color: #94A3B8; }
.text-muted { color: #64748B; }
.text-dark { color: #0F172A; }
.text-emerald { color: #047857; }
.fw-600 { font-weight: 600; }
.fs-11 { font-size: 11px; } .fs-12 { font-size: 12px; } .fs-13 { font-size: 13px; }

.toast { position: fixed; left: 50%; bottom: 100px; transform: translateX(-50%); background: rgba(15,23,42,0.85); color: #FFFFFF; padding: 8px 16px; border-radius: 999px; font-size: 12px; font-weight: 500; z-index: 99; }
.toast-text { color: #FFFFFF; }
</style>
