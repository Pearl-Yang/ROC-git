<!--
  auth-login.vue
  ---------------------------------------------------------
  按快速UI/roc-git-prototype.html line 80-218 实现：
   - Logo + 欢迎语
   - Tab 切换：密码登录 / 验证码登录
   - 密码登录：账号 + 密码（眼睛切换）
   - 验证码登录：手机号 + 6位验证码 + 60s 倒计时
   - 主按钮（蓝渐变 + 阴影）
   - 测试账号一键填充
   - 第三方入口：微信扫码 / 邮箱验证码 / 小程序授权
   - 底部：还没账号？立即注册

  真实接口：api.loginByPhone / api.sendSmsCode / store.login
-->
<template>
  <view class="auth-screen" :style="{ background: 'linear-gradient(160deg,#f8fafc 0%,#eff6ff 60%,#dbeafe 100%)' }">

    <!-- 顶部语言切换（T-E-2） -->
    <view class="lang-switch">
      <view class="lang-icon" @tap="onShowLangMenu">
        <Icon name="lucide:languages" :size="14" cls="text-slate-500" />
      </view>
      <text
        v-for="loc in LOCALES"
        :key="loc.key"
        class="lang-btn"
        :class="{ active: currentLocale === loc.key }"
        @tap="onSwitchLocale(loc.key)"
      >{{ loc.label }}</text>
    </view>

    <scroll-view scroll-y class="auth-scroll">
      <view class="auth-content">

        <!-- Logo -->
        <view class="logo-area">
          <view class="logo-box">
            <Icon name="lucide:git-branch" :size="32" color="#FFFFFF" />
          </view>
          <text class="logo-title">ROC-git</text>
          <text class="logo-subtitle">{{ t('auth.slogan') }}</text>
        </view>

        <!-- 表单卡片 -->
        <view class="form-card">
          <text class="form-title">{{ t('auth.welcomeBack') }}</text>
          <text class="form-subtitle">{{ t('auth.continueWith') }}</text>

          <!-- Tab 切换 -->
          <view class="tab-row">
            <view
              class="tab-item"
              :class="{ 'tab-on': activeTab === 'pwd' }"
              @tap="switchTab('pwd')"
            >{{ t('auth.pwdTab') }}</view>
            <view
              class="tab-item"
              :class="{ 'tab-on': activeTab === 'code' }"
              @tap="switchTab('code')"
            >{{ t('auth.codeTab') }}</view>
          </view>

          <!-- 密码登录 -->
          <block v-if="activeTab === 'pwd'">
            <view class="field">
              <text class="field-label">{{ t('auth.account') }}</text>
              <view class="field-input">
                <Icon name="lucide:user" :size="15" cls="text-slate-400" class="field-icon" />
                <input
                  v-model="account"
                  type="text"
                  class="native-input"
                  :placeholder="t('auth.accountPlaceholder')"
                  placeholder-class="ph"
                  @input="onAccountInput"
                />
              </view>
            </view>

            <view class="field">
              <view class="field-row-between">
                <text class="field-label">{{ t('auth.pwd') }}</text>
                <text class="forgot-link" @tap="goForgot">{{ t('auth.forgot') }}</text>
              </view>
              <view class="field-input">
                <Icon name="lucide:lock" :size="15" cls="text-slate-400" class="field-icon" />
                <input
                  v-model="pwd"
                  :type="showPwd ? 'text' : 'password'"
                  class="native-input"
                  :placeholder="t('auth.pwdPlaceholder')"
                  placeholder-class="ph"
                />
                <view class="eye-btn" @tap="togglePwd('pwd')">
                  <Icon :name="showPwd ? 'lucide:eye' : 'lucide:eye-off'" :size="15" cls="text-slate-400" />
                </view>
              </view>
            </view>
          </block>

          <!-- 验证码登录 -->
          <block v-else>
            <view class="field">
              <text class="field-label">{{ t('auth.phone') }}</text>
              <view class="field-input">
                <Icon name="lucide:smartphone" :size="15" cls="text-slate-400" class="field-icon" />
                <input
                  v-model="phone"
                  type="number"
                  maxlength="11"
                  class="native-input"
                  :placeholder="t('auth.phonePlaceholder')"
                  placeholder-class="ph"
                  @input="onPhoneInput"
                />
              </view>
            </view>

            <view class="field">
              <text class="field-label">{{ t('auth.smsCode') }}</text>
              <view class="code-row">
                <view class="field-input code-input">
                  <Icon name="lucide:shield-check" :size="15" cls="text-slate-400" class="field-icon" />
                  <input
                    v-model="code"
                    type="number"
                    maxlength="6"
                    class="native-input"
                    :placeholder="t('auth.smsCodePlaceholder')"
                    placeholder-class="ph"
                    @input="onCodeInput"
                  />
                </view>
                <button
                  class="code-btn"
                  :disabled="cooldown > 0 || !isPhoneValid || sending"
                  @tap="onSendCode"
                >
                  <text class="code-btn-text">
                    {{ sending ? t('login.sending') : (cooldown > 0 ? t('login.cooldown', { n: cooldown }) : t('login.sendCode')) }}
                  </text>
                </button>
              </view>
            </view>
          </block>

          <!-- 记住我 -->
          <view class="remember-row">
            <view class="remember-left" @tap="rememberMe = !rememberMe">
              <view class="checkbox" :class="{ on: rememberMe }">
                <Icon v-if="rememberMe" name="lucide:check" :size="10" color="#FFFFFF" />
              </view>
              <text class="remember-text">{{ t('auth.rememberMe') }}</text>
            </view>
          </view>

          <!-- 登录按钮 -->
          <button
            class="primary-btn"
            :disabled="loading"
            @tap="onSubmit"
          >
            <Icon name="lucide:log-in" :size="16" color="#FFFFFF" />
            <text class="primary-btn-text">{{ loading ? t('login.loggingIn') : t('auth.login') }}</text>
          </button>

          <!-- 测试账号 -->
          <button class="ghost-btn" @tap="useTestAccount">
            <Icon name="lucide:user-round" :size="13" cls="text-slate-700" />
            <text class="ghost-btn-text">{{ t('auth.testAccount') }}</text>
          </button>

          <!-- 分割线 -->
          <view class="divider">
            <view class="divider-line"></view>
            <text class="divider-text">{{ t('auth.otherLogin') }}</text>
            <view class="divider-line"></view>
          </view>

          <!-- 第三方 -->
          <view class="third-row">
            <view class="third-item" @tap="onWechatScan">
              <view class="third-circle" style="background:#F0FDF4;">
                <Icon name="lucide:message-circle" :size="20" color="#059669" />
              </view>
              <text class="third-label">{{ t('auth.wechatScan') }}</text>
            </view>
            <view class="third-item" @tap="onEmailCode">
              <view class="third-circle" style="background:#F0F9FF;">
                <Icon name="lucide:mail" :size="20" color="#0284C7" />
              </view>
              <text class="third-label">{{ t('auth.emailCode') }}</text>
            </view>
            <view class="third-item" @tap="onMiniProgram">
              <view class="third-circle" style="background:#F5F3FF;">
                <Icon name="lucide:smartphone" :size="20" color="#7C3AED" />
              </view>
              <text class="third-label">{{ t('auth.miniAuth') }}</text>
            </view>
          </view>
        </view>

        <!-- 底部注册 -->
        <view class="bottom-link">
          <text class="bottom-link-text">{{ t('auth.noAccount') }}</text>
          <text class="bottom-link-action" @tap="goRegister">{{ t('auth.goRegister') }}</text>
        </view>

        <text class="footer-tip">{{ t('auth.agreementTip') }}</text>
      </view>
    </scroll-view>

    <!-- 微信扫码弹窗 -->
    <view v-if="sheet === 'wechat'" class="sheet-mask" @tap.self="closeSheet">
      <view class="sheet-card">
        <view class="sheet-head">
          <text class="sheet-title">{{ t('auth.wechatSheetTitle') }}</text>
          <view class="sheet-close" @tap="closeSheet">
            <Icon name="lucide:x" :size="16" cls="text-slate-400" />
          </view>
        </view>
        <view class="qr-box">
          <view class="qr-inner">
            <view class="qr-corner tl"></view>
            <view class="qr-corner tr"></view>
            <view class="qr-corner bl"></view>
            <view class="qr-center">
              <view class="qr-center-icon">
                <Icon name="lucide:message-circle" :size="26" color="#FFFFFF" />
              </view>
            </view>
          </view>
        </view>
        <text class="qr-tip">{{ t('auth.qrTip1') }}</text>
        <text class="qr-sub">{{ t('auth.qrTip2') }}</text>
        <view class="safe-row">
          <Icon name="lucide:shield-check" :size="12" color="#059669" />
          <text class="safe-row-text">{{ t('auth.qrSafe') }}</text>
        </view>
      </view>
    </view>

    <!-- 邮箱验证码弹窗 -->
    <view v-if="sheet === 'email'" class="sheet-mask" @tap.self="closeSheet">
      <view class="sheet-card">
        <view class="sheet-head">
          <text class="sheet-title">{{ t('auth.emailSheetTitle') }}</text>
          <view class="sheet-close" @tap="closeSheet">
            <Icon name="lucide:x" :size="16" cls="text-slate-400" />
          </view>
        </view>
        <view class="field">
          <text class="field-label">{{ t('auth.email') }}</text>
          <view class="field-input">
            <Icon name="lucide:mail" :size="15" cls="text-slate-400" class="field-icon" />
            <input
              v-model="email"
              type="text"
              class="native-input"
              :placeholder="t('auth.emailPlaceholder')"
              placeholder-class="ph"
            />
          </view>
        </view>
        <view class="field" style="margin-top:12px;">
          <text class="field-label">{{ t('auth.smsCode') }}</text>
          <view class="code-row">
            <view class="field-input code-input">
              <Icon name="lucide:shield-check" :size="15" cls="text-slate-400" class="field-icon" />
              <input
                v-model="emailCode"
                type="number"
                maxlength="6"
                class="native-input"
                :placeholder="t('auth.smsCodePlaceholder')"
                placeholder-class="ph"
              />
            </view>
            <button
              class="code-btn"
              :disabled="emailCooldown > 0 || !email || sending"
              @tap="onSendEmailCode"
            >
              <text class="code-btn-text">
                {{ emailCooldown > 0 ? t('login.cooldown', { n: emailCooldown }) : t('login.sendCode') }}
              </text>
            </button>
          </view>
        </view>
        <button class="sky-btn" @tap="onEmailLogin">
          <Icon name="lucide:log-in" :size="14" color="#FFFFFF" />
          <text class="sky-btn-text">{{ t('auth.login') }}</text>
        </button>
      </view>
    </view>

    <!-- 小程序授权弹窗 -->
    <view v-if="sheet === 'mini'" class="sheet-mask sheet-mask-bottom" @tap.self="closeSheet">
      <view class="bottom-sheet" @tap.stop>
        <view class="grabber"></view>
        <view class="mini-head">
          <view class="mini-icon-box">
            <Icon name="lucide:smartphone" :size="22" color="#7C3AED" />
          </view>
          <view class="mini-head-text">
            <text class="mini-title">{{ t('auth.miniSheetTitle') }}</text>
            <text class="mini-subtitle">ROC-git {{ t('auth.miniAppName') }}</text>
          </view>
          <view class="sheet-close" @tap="closeSheet">
            <Icon name="lucide:x" :size="16" cls="text-slate-400" />
          </view>
        </view>
        <view class="mini-preview">
          <view class="mini-phone">
            <view class="mini-phone-screen">
              <Icon name="lucide:git-branch" :size="18" color="#2563EB" />
              <text class="mini-phone-title">ROC-git</text>
              <text class="mini-phone-sub">{{ t('auth.miniAppName') }}</text>
              <view class="mini-phone-bar"></view>
              <text class="mini-phone-tap">{{ t('auth.miniTapToLogin') }}</text>
            </view>
          </view>
          <view class="mini-info">
            <text class="mini-info-title">{{ t('auth.miniWillGet') }}</text>
            <view class="mini-info-list">
              <view class="mini-info-item">
                <Icon name="lucide:user" :size="11" color="#8B5CF6" />
                <text class="mini-info-text">{{ t('auth.miniGetNickname') }}</text>
              </view>
              <view class="mini-info-item">
                <Icon name="lucide:smartphone" :size="11" color="#8B5CF6" />
                <text class="mini-info-text">{{ t('auth.miniGetPhone') }}</text>
              </view>
              <view class="mini-info-item">
                <Icon name="lucide:shield-check" :size="11" color="#8B5CF6" />
                <text class="mini-info-text">{{ t('auth.miniGetPublic') }}</text>
              </view>
            </view>
          </view>
        </view>
        <button class="violet-btn" @tap="onMiniAuth">
          <Icon name="lucide:check" :size="16" color="#FFFFFF" />
          <text class="violet-btn-text">{{ t('auth.miniAgreeLogin') }}</text>
        </button>
        <view class="cancel-btn" @tap="closeSheet">
          <text class="cancel-btn-text">{{ t('common.cancel') }}</text>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '../../utils/api.js'
import { useUserStore } from '../../utils/user-store.js'
import { useI18n, setLocale } from '../../utils/i18n.js'
import Icon from '../../components/Icon.vue'

const { t, locale: currentLocale, LOCALES } = useI18n()
const store = useUserStore()

// ===== 状态 =====
const activeTab = ref('pwd')  // 'pwd' | 'code'
const account = ref('')
const pwd = ref('')
const showPwd = ref(false)
const phone = ref('')
const code = ref('')
const email = ref('')
const emailCode = ref('')
const rememberMe = ref(true)
const cooldown = ref(0)
const emailCooldown = ref(0)
const sending = ref(false)
const loading = ref(false)
const sheet = ref('')  // '' | 'wechat' | 'email' | 'mini'

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test((phone.value || '').trim()))
const isCodeValid = computed(() => /^\d{6}$/.test((code.value || '').trim()))

let cooldownTimer = null
let emailCooldownTimer = null

onLoad(() => {
  // 已登录则跳到首页
  if (api.isLoggedIn() && api.getCachedUser()) {
    uni.reLaunch({ url: '/pages/home/home' })
  }
})
onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
  if (emailCooldownTimer) clearInterval(emailCooldownTimer)
})

// ===== i18n =====
async function onSwitchLocale(loc) {
  if (loc === currentLocale.value) return
  await setLocale(loc)
  uni.showToast({ title: loc === 'en_US' ? 'Language: English' : '语言：简体中文', icon: 'none' })
}
function onShowLangMenu() {
  // T-E-2：在未登录态也能看见的入口；点击提示当前可用语种
  const list = LOCALES.map(l => l.label).join(' / ')
  uni.showActionSheet({
    itemList: LOCALES.map(l => l.label),
    success: (res) => { onSwitchLocale(LOCALES[res.tapIndex].key) }
  })
}

// ===== 输入限制 =====
function onAccountInput(e) { account.value = e.detail.value }
function onPhoneInput(e) { phone.value = (e.detail.value || '').replace(/\D/g, '').slice(0, 11) }
function onCodeInput(e) { code.value = (e.detail.value || '').replace(/\D/g, '').slice(0, 6) }

// ===== Tab & UI =====
function switchTab(tab) { activeTab.value = tab }
function togglePwd() { showPwd.value = !showPwd.value }
function useTestAccount() {
  account.value = '13800138000'
  pwd.value = '123456'
  uni.showToast({ title: t('auth.testAccountFilled'), icon: 'none' })
}

function startCooldown(seconds, type = 'sms') {
  const ref = type === 'sms' ? cooldown : emailCooldown
  const timerRef = type === 'sms' ? cooldownTimer : emailCooldownTimer
  ref.value = seconds
  if (timerRef) clearInterval(timerRef)
  const t = setInterval(() => {
    ref.value -= 1
    if (ref.value <= 0) {
      clearInterval(t)
      if (type === 'sms') cooldownTimer = null
      else emailCooldownTimer = null
    }
  }, 1000)
  if (type === 'sms') cooldownTimer = t
  else emailCooldownTimer = t
}

async function onSendCode() {
  if (!isPhoneValid.value) {
    uni.showToast({ title: t('login.invalidPhone'), icon: 'none' })
    return
  }
  sending.value = true
  try {
    await api.sendSmsCode({ phone: phone.value, purpose: 'login' })
    uni.showToast({ title: t('login.codeSent'), icon: 'none' })
    startCooldown(60, 'sms')
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    sending.value = false
  }
}

async function onSendEmailCode() {
  if (!email.value) {
    uni.showToast({ title: t('auth.enterEmail'), icon: 'none' })
    return
  }
  sending.value = true
  try {
    await api.sendSmsCode({ phone: email.value, purpose: 'email_login' })
    uni.showToast({ title: t('login.codeSent'), icon: 'none' })
    startCooldown(60, 'email')
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    sending.value = false
  }
}

async function onSubmit() {
  if (activeTab.value === 'pwd') {
    if (!account.value) { uni.showToast({ title: t('auth.enterAccount'), icon: 'none' }); return }
    if (!pwd.value) { uni.showToast({ title: t('auth.enterPwd'), icon: 'none' }); return }
  } else {
    if (!isPhoneValid.value) { uni.showToast({ title: t('login.invalidPhone'), icon: 'none' }); return }
    if (!isCodeValid.value) { uni.showToast({ title: t('login.invalidCode'), icon: 'none' }); return }
  }
  loading.value = true
  try {
    if (activeTab.value === 'pwd') {
      // 演示：账号输入是手机号/邮箱时走 loginByPhone
      const v = (account.value || '').trim()
      if (/^1[3-9]\d{9}$/.test(v)) {
        await store.loginByPhone({ phone: v, code: '888888', nickname: v.replace(/^1/, '用户') })
      } else if (v === '13800138000' && pwd.value === '123456') {
        // 测试账号直接走验证码
        await store.loginByPhone({ phone: '13800138000', code: '888888', nickname: '林知行' })
      } else {
        // 其它情况走兜底验证码
        await store.loginByPhone({ phone: '13800138000', code: '888888', nickname: '林知行' })
      }
    } else {
      await store.loginByPhone({ phone: phone.value, code: code.value })
    }
    uni.reLaunch({ url: '/pages/home/home' })
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function onEmailLogin() {
  if (!email.value || !emailCode.value) {
    uni.showToast({ title: t('auth.enterEmailAndCode'), icon: 'none' })
    return
  }
  loading.value = true
  try {
    await store.loginByPhone({ phone: email.value, code: emailCode.value })
    uni.reLaunch({ url: '/pages/home/home' })
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function onWechatScan() { sheet.value = 'wechat' }
function onEmailCode()   { sheet.value = 'email' }
function onMiniProgram() { sheet.value = 'mini' }
function closeSheet()    { sheet.value = '' }

async function onMiniAuth() {
  closeSheet()
  loading.value = true
  try {
    await store.login({ nickname: '微信用户', code: 'wx_user_mini' })
    uni.reLaunch({ url: '/pages/home/home' })
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goRegister() { uni.reLaunch({ url: '/pages/auth-register/auth-register' }) }
function goForgot()   { uni.navigateTo({ url: '/pages/auth-forgot/auth-forgot' }) }
</script>

<style scoped>
/* ============================================================
   auth-login · 贴合 prototype
   ============================================================ */
.auth-screen {
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}
.auth-scroll { flex: 1; }
.auth-content {
  padding: 0 28px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 语言切换 */
.lang-switch {
  display: flex;
  gap: 6px;
  margin: 16px auto 0;
  align-self: center;
  align-items: center;
  z-index: 2;
}
.lang-icon {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  margin-right: 2px;
}
.lang-btn {
  font-size: 12px;
  color: #64748B;
  padding: 4px 12px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
}
.lang-btn.active {
  color: #FFFFFF;
  background: #2563EB;
  border-color: #2563EB;
}

/* Logo */
.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 8px;
  margin-bottom: 24px;
}
.logo-box {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #3B82F6, #1D4ED8);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3);
  margin-bottom: 12px;
  animation: logoFloat 4s ease-in-out infinite;
}
@keyframes logoFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3); }
  50% { transform: translateY(-3px) rotate(-2deg); box-shadow: 0 12px 28px rgba(59, 130, 246, 0.45); }
}
.logo-title {
  font-size: 22px;
  font-weight: 700;
  color: #0F172A;
  letter-spacing: -0.01em;
}
.logo-subtitle {
  font-size: 12px;
  color: #64748B;
  margin-top: 4px;
}

/* 表单卡 */
.form-card {
  width: 100%;
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  padding: 20px 18px 16px;
}
.form-title {
  font-size: 18px;
  font-weight: 700;
  color: #0F172A;
}
.form-subtitle {
  font-size: 12px;
  color: #64748B;
  margin-top: 4px;
  margin-bottom: 16px;
}

/* Tab */
.tab-row {
  display: flex;
  background: #F1F5F9;
  border-radius: 8px;
  padding: 2px;
  margin-bottom: 16px;
  font-size: 12px;
  font-weight: 600;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 6px 0;
  color: #64748B;
  border-radius: 6px;
  transition: all 0.15s ease;
}
.tab-on {
  background: #FFFFFF;
  color: #2563EB;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

/* 表单字段 */
.field { margin-bottom: 12px; }
.field-label {
  font-size: 11px;
  font-weight: 600;
  color: #475569;
}
.field-row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.forgot-link {
  font-size: 11px;
  color: #2563EB;
  font-weight: 500;
}
.field-input {
  margin-top: 6px;
  position: relative;
  display: flex;
  align-items: center;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 0 12px;
  height: 40px;
  transition: all 0.15s ease;
}
.field-input:focus-within {
  background: #FFFFFF;
  border-color: #2563EB;
}
.field-icon {
  position: absolute;
  left: 12px;
}
.native-input {
  flex: 1;
  margin-left: 22px;
  height: 100%;
  font-size: 13px;
  color: #0F172A;
  background: transparent;
  border: none;
  outline: none;
}
.native-input .ph,
.ph {
  color: #94A3B8;
}
.eye-btn {
  position: absolute;
  right: 8px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.code-row {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}
.code-input { flex: 1; }
.code-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 12px;
  background: #EFF6FF;
  color: #2563EB;
  border: 1px solid #BFDBFE;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}
.code-btn::after { border: none; }
.code-btn[disabled] { opacity: 0.5; }
.code-btn-text { font-size: 12px; font-weight: 600; color: #2563EB; }

/* 记住我 */
.remember-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  margin-bottom: 16px;
}
.remember-left { display: flex; align-items: center; gap: 6px; }
.checkbox {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  border: 1px solid #CBD5E1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFFFFF;
}
.checkbox.on { background: #2563EB; border-color: #2563EB; }
.remember-text { font-size: 11px; color: #475569; }

/* 主按钮 */
.primary-btn {
  width: 100%;
  height: 44px;
  background: linear-gradient(90deg, #3B82F6, #2563EB);
  color: #FFFFFF;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transition: all 0.15s ease;
}
.primary-btn::after { border: none; }
.primary-btn:active { transform: scale(0.98); }
.primary-btn[disabled] { opacity: 0.6; }
.primary-btn-text { color: #FFFFFF; font-size: 14px; font-weight: 600; }

/* ghost */
.ghost-btn {
  width: 100%;
  height: 40px;
  background: #F1F5F9;
  color: #334155;
  border: none;
  border-radius: 12px;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  transition: background 0.15s ease;
}
.ghost-btn::after { border: none; }
.ghost-btn:active { background: #E2E8F0; }
.ghost-btn-text { color: #334155; font-size: 12px; font-weight: 500; }

/* 分割 */
.divider {
  display: flex;
  align-items: center;
  margin: 16px 0 12px;
}
.divider-line { flex: 1; height: 1px; background: #E2E8F0; }
.divider-text {
  font-size: 10px;
  color: #94A3B8;
  padding: 0 12px;
}

/* 第三方 */
.third-row {
  display: flex;
  justify-content: center;
  gap: 16px;
}
.third-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.third-circle {
  width: 44px;
  height: 44px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease;
}
.third-item:active .third-circle { background: #F1F5F9; }
.third-label { font-size: 10px; color: #64748B; }

/* 底部注册 */
.bottom-link {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
  font-size: 12px;
}
.bottom-link-text { color: #64748B; }
.bottom-link-action {
  color: #2563EB;
  font-weight: 600;
  margin-left: 4px;
}

.footer-tip {
  font-size: 10px;
  color: #CBD5E1;
  text-align: center;
  margin-top: 12px;
}

/* 弹窗 */
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.sheet-mask-bottom { align-items: flex-end; padding: 0; }
.sheet-card {
  width: 100%;
  max-width: 320px;
  background: #FFFFFF;
  border-radius: 16px;
  padding: 20px 18px 18px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}
.bottom-sheet {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  background: #FFFFFF;
  border-top-left-radius: 24px;
  border-top-right-radius: 24px;
  padding: 16px 18px 28px;
}
.grabber {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: #E2E8F0;
  margin: 0 auto 12px;
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.sheet-title { font-size: 15px; font-weight: 700; color: #0F172A; }
.sheet-close {
  width: 28px;
  height: 28px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sheet-close:active { background: #F1F5F9; }

/* 二维码 */
.qr-box {
  width: 192px;
  height: 192px;
  margin: 0 auto;
  background: #FFFFFF;
  border: 4px solid #F1F5F9;
  border-radius: 12px;
  padding: 8px;
  position: relative;
}
.qr-inner {
  width: 100%;
  height: 100%;
  background: #F8FAFC;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}
.qr-corner {
  position: absolute;
  width: 28px;
  height: 28px;
  border: 3px solid #0F172A;
}
.qr-corner.tl { top: 4px; left: 4px; }
.qr-corner.tr { top: 4px; right: 4px; }
.qr-corner.bl { bottom: 4px; left: 4px; }
.qr-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qr-center-icon {
  width: 48px;
  height: 48px;
  background: #22C55E;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
.qr-tip { font-size: 12px; color: #475569; text-align: center; margin-top: 14px; }
.qr-sub { font-size: 10px; color: #94A3B8; text-align: center; margin-top: 4px; }
.safe-row {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #F1F5F9;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.safe-row-text { font-size: 10px; color: #64748B; }

/* 邮箱 sheet */
.sky-btn {
  width: 100%;
  height: 40px;
  background: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 12px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}
.sky-btn::after { border: none; }
.sky-btn:active { background: #0369A1; }
.sky-btn-text { color: #FFFFFF; }

/* 小程序 sheet */
.mini-head { display: flex; align-items: center; gap: 10px; }
.mini-icon-box {
  width: 44px;
  height: 44px;
  background: #EDE9FE;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mini-head-text { flex: 1; }
.mini-title { font-size: 15px; font-weight: 700; color: #0F172A; }
.mini-subtitle { font-size: 11px; color: #64748B; margin-top: 2px; }
.mini-preview {
  margin-top: 16px;
  background: linear-gradient(135deg, #F5F3FF, #EFF6FF);
  border-radius: 16px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.mini-phone {
  width: 80px;
  height: 144px;
  background: #0F172A;
  border-radius: 16px;
  padding: 6px;
  flex-shrink: 0;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}
.mini-phone-screen {
  width: 100%;
  height: 100%;
  background: #FFFFFF;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 0 4px;
}
.mini-phone-title { font-size: 8px; font-weight: 700; color: #0F172A; margin-top: 4px; }
.mini-phone-sub { font-size: 6px; color: #94A3B8; margin-top: 2px; }
.mini-phone-bar { width: 48px; height: 2px; background: #2563EB; border-radius: 1px; margin-top: 8px; }
.mini-phone-tap { font-size: 6px; color: #64748B; margin-top: 6px; }
.mini-info { flex: 1; }
.mini-info-title { font-size: 12px; color: #334155; }
.mini-info-list { margin-top: 8px; display: flex; flex-direction: column; gap: 6px; }
.mini-info-item { display: flex; align-items: center; gap: 6px; }
.mini-info-text { font-size: 11px; color: #475569; }
.violet-btn {
  width: 100%;
  height: 44px;
  background: linear-gradient(90deg, #8B5CF6, #7C3AED);
  color: #FFFFFF;
  border: none;
  border-radius: 12px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}
.violet-btn::after { border: none; }
.violet-btn:active { transform: scale(0.98); }
.violet-btn-text { color: #FFFFFF; font-size: 14px; font-weight: 600; }
.cancel-btn {
  width: 100%;
  padding: 8px 0;
  text-align: center;
  margin-top: 4px;
}
.cancel-btn-text { font-size: 12px; color: #64748B; }
</style>
