<!--
  auth-register.vue
  ---------------------------------------------------------
  按快速UI/roc-git-prototype.html line 348-466 实现：
   - 返回按钮
   - 标题：创建新账号
   - 表单：手机号 + 短信验证码 + 设置密码（强度条）+ 确认密码 + 协议
   - 主按钮：创建账号
   - 第三方：邮箱验证 / 微信小程序
   - 底部：已有账号？直接登录
-->
<template>
  <view class="auth-screen" :style="{ background: 'linear-gradient(160deg,#f8fafc 0%,#eff6ff 60%,#dbeafe 100%)' }">

    <!-- 顶部返回 -->
    <view class="topbar">
      <view class="back-btn" @tap="goLogin">
        <Icon name="lucide:chevron-left" :size="20" cls="text-slate-700" />
      </view>
    </view>

    <scroll-view scroll-y class="auth-scroll">
      <view class="auth-content">

        <view class="title-block">
          <text class="title">{{ t('auth.regTitle') }}</text>
          <text class="subtitle">{{ t('auth.regSubtitle') }}</text>
        </view>

        <view class="form-card">
          <!-- 手机号 -->
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

          <!-- 验证码 -->
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

          <!-- 设置密码 -->
          <view class="field">
            <text class="field-label">
              {{ t('auth.setPwd') }}
              <text class="hint-soft">{{ t('auth.pwdHint') }}</text>
            </text>
            <view class="field-input">
              <Icon name="lucide:lock" :size="15" cls="text-slate-400" class="field-icon" />
              <input
                v-model="pwd"
                :type="showPwd ? 'text' : 'password'"
                class="native-input"
                :placeholder="t('auth.pwdPlaceholder')"
                placeholder-class="ph"
                @input="onPwdInput"
              />
              <view class="eye-btn" @tap="showPwd = !showPwd">
                <Icon :name="showPwd ? 'lucide:eye' : 'lucide:eye-off'" :size="15" cls="text-slate-400" />
              </view>
            </view>
            <!-- 强度条 -->
            <view class="strength-row">
              <view class="strength-bar" :class="strength >= 1 ? 'on' : ''" :style="strength >= 1 ? barColor(1) : ''"></view>
              <view class="strength-bar" :class="strength >= 2 ? 'on' : ''" :style="strength >= 2 ? barColor(2) : ''"></view>
              <view class="strength-bar" :class="strength >= 3 ? 'on' : ''" :style="strength >= 3 ? barColor(3) : ''"></view>
              <view class="strength-bar" :class="strength >= 4 ? 'on' : ''" :style="strength >= 4 ? barColor(4) : ''"></view>
              <text class="strength-text" :style="{ color: strengthColor }">{{ strengthText }}</text>
            </view>
          </view>

          <!-- 确认密码 -->
          <view class="field">
            <text class="field-label">{{ t('auth.confirmPwd') }}</text>
            <view class="field-input">
              <Icon name="lucide:lock-keyhole" :size="15" cls="text-slate-400" class="field-icon" />
              <input
                v-model="pwd2"
                :type="showPwd2 ? 'text' : 'password'"
                class="native-input"
                :placeholder="t('auth.pwd2Placeholder')"
                placeholder-class="ph"
              />
              <view class="eye-btn" @tap="showPwd2 = !showPwd2">
                <Icon :name="showPwd2 ? 'lucide:eye' : 'lucide:eye-off'" :size="15" cls="text-slate-400" />
              </view>
            </view>
          </view>

          <!-- 协议 -->
          <view class="agree-row" @tap="agreed = !agreed">
            <view class="checkbox" :class="{ on: agreed }">
              <Icon v-if="agreed" name="lucide:check" :size="10" color="#FFFFFF" />
            </view>
            <text class="agree-text">
              {{ t('auth.agreePrefix') }}
              <text class="link-text" @tap.stop="onShowAgreement">{{ t('auth.userAgreement') }}</text>
              {{ t('auth.and') }}
              <text class="link-text" @tap.stop="onShowAgreement">{{ t('auth.privacyPolicy') }}</text>
            </text>
          </view>

          <!-- 主按钮 -->
          <button class="primary-btn" :disabled="loading" @tap="onSubmit">
            <Icon name="lucide:user-plus" :size="16" color="#FFFFFF" />
            <text class="primary-btn-text">{{ loading ? t('auth.regSubmitting') : t('auth.regSubmit') }}</text>
          </button>

          <!-- 分割线 -->
          <view class="divider">
            <view class="divider-line"></view>
            <text class="divider-text">{{ t('auth.otherReg') }}</text>
            <view class="divider-line"></view>
          </view>

          <!-- 第三方 -->
          <view class="third-row">
            <view class="third-item" @tap="onEmailCode">
              <view class="third-circle" style="background:#F0F9FF;">
                <Icon name="lucide:mail" :size="20" color="#0284C7" />
              </view>
              <text class="third-label">{{ t('auth.emailVerify') }}</text>
            </view>
            <view class="third-item" @tap="onMiniProgram">
              <view class="third-circle" style="background:#F0FDF4;">
                <Icon name="lucide:smartphone" :size="20" color="#059669" />
              </view>
              <text class="third-label">{{ t('auth.miniReg') }}</text>
            </view>
          </view>

          <text class="form-tip">{{ t('auth.regTip') }}</text>
        </view>

        <!-- 底部 -->
        <view class="bottom-link">
          <text class="bottom-link-text">{{ t('auth.haveAccount') }}</text>
          <text class="bottom-link-action" @tap="goLogin">{{ t('auth.goLogin') }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 邮箱验证 sheet（简化版，演示用） -->
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
        <text class="form-tip" style="margin-top:12px;">{{ t('auth.emailRegTip') }}</text>
        <button class="sky-btn" @tap="onEmailRegConfirm">
          <Icon name="lucide:check" :size="14" color="#FFFFFF" />
          <text class="sky-btn-text">{{ t('auth.useEmail') }}</text>
        </button>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import api from '../../utils/api.js'
import { useUserStore } from '../../utils/user-store.js'
import { useI18n } from '../../utils/i18n.js'
import Icon from '../../components/Icon.vue'

const { t } = useI18n()
const store = useUserStore()

const phone = ref('')
const code = ref('')
const pwd = ref('')
const pwd2 = ref('')
const showPwd = ref(false)
const showPwd2 = ref(false)
const agreed = ref(true)
const cooldown = ref(0)
const sending = ref(false)
const loading = ref(false)
const sheet = ref('')  // '' | 'email'
const email = ref('')

let cooldownTimer = null
onUnmounted(() => { if (cooldownTimer) clearInterval(cooldownTimer) })

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test((phone.value || '').trim()))
const isCodeValid = computed(() => /^\d{6}$/.test((code.value || '').trim()))

/** 密码强度 0-4 */
const strength = computed(() => {
  const v = pwd.value || ''
  if (!v) return 0
  let s = 0
  if (v.length >= 6) s++
  if (v.length >= 10) s++
  if (/[A-Z]/.test(v) && /[a-z]/.test(v)) s++
  if (/\d/.test(v) && /[^A-Za-z0-9]/.test(v)) s++
  return Math.min(4, s)
})
const strengthText = computed(() => {
  if (strength.value <= 0) return ''
  return ['', t('auth.pwdWeak'), t('auth.pwdMedium'), t('auth.pwdStrong'), t('auth.pwdVeryStrong')][strength.value]
})
const strengthColor = computed(() => {
  return ['#94A3B8', '#EF4444', '#F59E0B', '#3B82F6', '#10B981'][strength.value]
})
function barColor(level) {
  const map = { 1: '#EF4444', 2: '#F59E0B', 3: '#3B82F6', 4: '#10B981' }
  return `background:${map[level]};`
}

function onPhoneInput(e) { phone.value = (e.detail.value || '').replace(/\D/g, '').slice(0, 11) }
function onCodeInput(e)  { code.value = (e.detail.value || '').replace(/\D/g, '').slice(0, 6) }
function onPwdInput(e)   { pwd.value = e.detail.value }

function startCooldown(s) {
  cooldown.value = s
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}

async function onSendCode() {
  if (!isPhoneValid.value) {
    uni.showToast({ title: t('login.invalidPhone'), icon: 'none' })
    return
  }
  sending.value = true
  try {
    await api.sendSmsCode({ phone: phone.value, purpose: 'register' })
    uni.showToast({ title: t('login.codeSent'), icon: 'none' })
    startCooldown(60)
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    sending.value = false
  }
}

async function onSubmit() {
  if (!isPhoneValid.value)  { uni.showToast({ title: t('login.invalidPhone'), icon: 'none' }); return }
  if (!isCodeValid.value)   { uni.showToast({ title: t('login.invalidCode'), icon: 'none' }); return }
  if (!pwd.value)           { uni.showToast({ title: t('auth.enterPwd'), icon: 'none' }); return }
  if (pwd.value.length < 6) { uni.showToast({ title: t('auth.pwdTooShort'), icon: 'none' }); return }
  if (pwd.value !== pwd2.value) { uni.showToast({ title: t('auth.pwdMismatch'), icon: 'none' }); return }
  if (!agreed.value)        { uni.showToast({ title: t('auth.needAgree'), icon: 'none' }); return }

  loading.value = true
  try {
    // 后端 loginByPhone 已支持「未注册自动创建」逻辑
    await store.loginByPhone({ phone: phone.value, code: code.value })
    uni.reLaunch({ url: '/pages/home/home' })
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function onEmailCode() { sheet.value = 'email' }
function closeSheet() { sheet.value = '' }

function onEmailRegConfirm() {
  if (!email.value) { uni.showToast({ title: t('auth.enterEmail'), icon: 'none' }); return }
  closeSheet()
  uni.showToast({ title: t('auth.emailRegTipToast'), icon: 'none' })
}

function onMiniProgram() {
  loading.value = true
  store.login({ nickname: '微信用户', code: 'wx_user_mini' })
    .then(() => uni.reLaunch({ url: '/pages/home/home' }))
    .catch(e => uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' }))
    .finally(() => loading.value = false)
}

function onShowAgreement() {
  uni.showModal({ title: t('auth.userAgreement'), content: t('auth.agreementContent'), showCancel: false, confirmText: t('common.confirm') })
}

function goLogin() { uni.reLaunch({ url: '/pages/auth-login/auth-login' }) }
</script>

<style scoped>
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
}

.topbar {
  padding: 12px 16px 0;
}
.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(226, 232, 240, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-btn:active { background: rgba(241, 245, 249, 0.95); }

.title-block { margin-top: 12px; margin-bottom: 20px; }
.title { font-size: 24px; font-weight: 700; color: #0F172A; letter-spacing: -0.01em; }
.subtitle { font-size: 12px; color: #64748B; margin-top: 4px; }

.form-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  padding: 20px 18px 16px;
}

.field { margin-bottom: 12px; }
.field-label {
  font-size: 11px;
  font-weight: 600;
  color: #475569;
}
.hint-soft { color: #CBD5E1; font-weight: 400; }
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
.field-icon { position: absolute; left: 12px; }
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
.ph { color: #94A3B8; }
.eye-btn {
  position: absolute;
  right: 8px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.code-row { display: flex; gap: 8px; margin-top: 6px; }
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
.code-btn-text { color: #2563EB; font-size: 12px; font-weight: 600; }

/* 强度条 */
.strength-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}
.strength-bar {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: #E2E8F0;
  transition: background 0.15s ease;
}
.strength-text { font-size: 10px; font-weight: 600; margin-left: 6px; }

/* 协议 */
.agree-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  margin-bottom: 16px;
}
.checkbox {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  border: 1px solid #CBD5E1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFFFFF;
  margin-top: 2px;
  flex-shrink: 0;
}
.checkbox.on { background: #2563EB; border-color: #2563EB; }
.agree-text { font-size: 11px; color: #475569; line-height: 1.6; }
.link-text { color: #2563EB; }

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
}
.primary-btn::after { border: none; }
.primary-btn:active { transform: scale(0.98); }
.primary-btn[disabled] { opacity: 0.6; }
.primary-btn-text { color: #FFFFFF; font-size: 14px; font-weight: 600; }

/* 分割 */
.divider {
  display: flex;
  align-items: center;
  margin: 16px 0 12px;
}
.divider-line { flex: 1; height: 1px; background: #E2E8F0; }
.divider-text { font-size: 10px; color: #94A3B8; padding: 0 12px; }

.third-row { display: flex; justify-content: center; gap: 20px; }
.third-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.third-circle {
  width: 44px;
  height: 44px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.third-label { font-size: 10px; color: #64748B; }
.form-tip { font-size: 10px; color: #94A3B8; text-align: center; margin-top: 12px; }

.bottom-link {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 16px;
  font-size: 12px;
}
.bottom-link-text { color: #64748B; }
.bottom-link-action { color: #2563EB; font-weight: 600; margin-left: 4px; }

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
.sheet-card {
  width: 100%;
  max-width: 320px;
  background: #FFFFFF;
  border-radius: 16px;
  padding: 20px 18px 18px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
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

.sky-btn {
  width: 100%;
  height: 40px;
  background: #0284C7;
  color: #FFFFFF;
  border: none;
  border-radius: 12px;
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}
.sky-btn::after { border: none; }
.sky-btn-text { color: #FFFFFF; }
</style>
