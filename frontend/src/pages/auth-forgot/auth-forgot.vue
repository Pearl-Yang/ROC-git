<!--
  auth-forgot.vue
  ---------------------------------------------------------
  按快速UI/roc-git-prototype.html line 469-578 实现：
   - 返回按钮
   - 插画（蓝色圆形 + 钥匙）
   - 标题：找回密码
   - 步骤指示器（3 步：验证手机 / 重置密码 / 完成）
   - 表单卡：手机号 + 验证码 + 新密码 + 确认密码
   - 主按钮：重置密码
   - 帮助提示卡
   - 底部：想起密码了？返回登录
-->
<template>
  <view class="auth-screen" :style="{ background: 'linear-gradient(160deg,#f8fafc 0%,#eff6ff 60%,#dbeafe 100%)' }">

    <view class="topbar">
      <view class="back-btn" @tap="goLogin">
        <Icon name="lucide:chevron-left" :size="20" cls="text-slate-700" />
      </view>
    </view>

    <scroll-view scroll-y class="auth-scroll">
      <view class="auth-content">

        <!-- 插画 + 标题 -->
        <view class="hero">
          <view class="hero-circle">
            <Icon name="lucide:key-round" :size="28" color="#2563EB" />
          </view>
          <text class="title">{{ t('auth.forgotTitle') }}</text>
          <text class="subtitle">
            <text>{{ t('auth.forgotSub1') }}</text>
            <text>{{ t('auth.forgotSub2') }}</text>
          </text>
        </view>

        <!-- 步骤指示器 -->
        <view class="steps">
          <view class="step-item">
            <view class="step-num" :class="{ on: true }">1</view>
            <text class="step-text on">{{ t('auth.step1') }}</text>
          </view>
          <view class="step-line" :class="{ on: step >= 2 }"></view>
          <view class="step-item">
            <view class="step-num" :class="{ on: step >= 2 }">2</view>
            <text class="step-text" :class="{ on: step >= 2 }">{{ t('auth.step2') }}</text>
          </view>
          <view class="step-line" :class="{ on: step >= 3 }"></view>
          <view class="step-item">
            <view class="step-num" :class="{ on: step >= 3 }">3</view>
            <text class="step-text" :class="{ on: step >= 3 }">{{ t('auth.step3') }}</text>
          </view>
        </view>

        <!-- 表单卡 -->
        <view class="form-card">

          <!-- 步骤 1：验证手机 -->
          <block v-if="step === 1">
            <view class="field">
              <text class="field-label">{{ t('auth.regPhone') }}</text>
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

            <button class="primary-btn" @tap="goStep2">
              <text class="primary-btn-text">{{ t('auth.nextStep') }}</text>
              <Icon name="lucide:chevron-right" :size="16" color="#FFFFFF" />
            </button>
          </block>

          <!-- 步骤 2：重置密码 -->
          <block v-else-if="step === 2">
            <view class="field">
              <text class="field-label">
                {{ t('auth.newPwd') }}
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
                />
                <view class="eye-btn" @tap="showPwd = !showPwd">
                  <Icon :name="showPwd ? 'lucide:eye' : 'lucide:eye-off'" :size="15" cls="text-slate-400" />
                </view>
              </view>
            </view>

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

            <view class="btn-row">
              <button class="ghost-btn" @tap="step = 1">
                <Icon name="lucide:chevron-left" :size="14" cls="text-slate-600" />
                <text class="ghost-btn-text">{{ t('auth.prevStep') }}</text>
              </button>
              <button class="primary-btn" style="flex:1;" @tap="onReset">
                <Icon name="lucide:check" :size="16" color="#FFFFFF" />
                <text class="primary-btn-text">{{ t('auth.resetPwd') }}</text>
              </button>
            </view>
          </block>

          <!-- 步骤 3：完成 -->
          <block v-else>
            <view class="success-box">
              <view class="success-circle">
                <Icon name="lucide:check" :size="36" color="#059669" />
              </view>
              <text class="success-title">{{ t('auth.resetSuccess') }}</text>
              <text class="success-sub">{{ t('auth.resetSuccessSub') }}</text>
              <button class="primary-btn" style="margin-top:24px;" @tap="goLogin">
                <Icon name="lucide:log-in" :size="16" color="#FFFFFF" />
                <text class="primary-btn-text">{{ t('auth.backToLogin') }}</text>
              </button>
            </view>
          </block>

          <!-- 帮助卡（仅步骤 1 显示） -->
          <view v-if="step === 1" class="help-card">
            <Icon name="lucide:info" :size="14" color="#2563EB" />
            <view class="help-text">
              <text>{{ t('auth.forgotHelp1') }}</text>
              <text class="help-link">support@roc-git.edu</text>
              <text>{{ t('auth.forgotHelp2') }}</text>
            </view>
          </view>
        </view>

        <!-- 底部 -->
        <view v-if="step === 1" class="bottom-link">
          <text class="bottom-link-text">{{ t('auth.rememberPwd') }}</text>
          <text class="bottom-link-action" @tap="goLogin">{{ t('auth.backToLogin') }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import api from '../../utils/api.js'
import { useI18n } from '../../utils/i18n.js'
import Icon from '../../components/Icon.vue'

const { t } = useI18n()

const step = ref(1)  // 1 / 2 / 3
const phone = ref('')
const code = ref('')
const pwd = ref('')
const pwd2 = ref('')
const showPwd = ref(false)
const showPwd2 = ref(false)
const cooldown = ref(0)
const sending = ref(false)

let cooldownTimer = null
onUnmounted(() => { if (cooldownTimer) clearInterval(cooldownTimer) })

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test((phone.value || '').trim()))
const isCodeValid = computed(() => /^\d{6}$/.test((code.value || '').trim()))

function onPhoneInput(e) { phone.value = (e.detail.value || '').replace(/\D/g, '').slice(0, 11) }
function onCodeInput(e)  { code.value = (e.detail.value || '').replace(/\D/g, '').slice(0, 6) }

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
    await api.sendSmsCode({ phone: phone.value, purpose: 'reset' })
    uni.showToast({ title: t('login.codeSent'), icon: 'none' })
    startCooldown(60)
  } catch (e) {
    uni.showToast({ title: (e && e.message) || t('common.fail'), icon: 'none' })
  } finally {
    sending.value = false
  }
}

function goStep2() {
  if (!isPhoneValid.value)  { uni.showToast({ title: t('login.invalidPhone'), icon: 'none' }); return }
  if (!isCodeValid.value)   { uni.showToast({ title: t('login.invalidCode'), icon: 'none' }); return }
  step.value = 2
}

function onReset() {
  if (!pwd.value)            { uni.showToast({ title: t('auth.enterPwd'), icon: 'none' }); return }
  if (pwd.value.length < 6)  { uni.showToast({ title: t('auth.pwdTooShort'), icon: 'none' }); return }
  if (pwd.value !== pwd2.value) { uni.showToast({ title: t('auth.pwdMismatch'), icon: 'none' }); return }
  // 后端目前没有「通过短信验证码重置密码」接口；演示阶段直接走 toast
  // 真实接入时调用 api.resetPasswordByPhone({ phone, code, newPassword: pwd })
  step.value = 3
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

.topbar { padding: 12px 16px 0; }
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

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 8px;
  margin-bottom: 20px;
}
.hero-circle {
  width: 64px;
  height: 64px;
  border-radius: 32px;
  background: #DBEAFE;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.title {
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
  letter-spacing: -0.01em;
}
.subtitle {
  font-size: 12px;
  color: #64748B;
  margin-top: 6px;
  text-align: center;
  line-height: 1.6;
}
.subtitle text { display: block; }

/* 步骤指示器 */
.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.step-num {
  width: 24px;
  height: 24px;
  border-radius: 12px;
  background: #E2E8F0;
  color: #64748B;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-num.on { background: #2563EB; color: #FFFFFF; }
.step-text {
  font-size: 11px;
  color: #94A3B8;
  font-weight: 500;
}
.step-text.on { color: #334155; font-weight: 600; }
.step-line {
  width: 24px;
  height: 1px;
  background: #E2E8F0;
  margin: 0 8px;
}
.step-line.on { background: #2563EB; }

/* 表单卡 */
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
.primary-btn-text { color: #FFFFFF; font-size: 14px; font-weight: 600; }

.btn-row { display: flex; gap: 8px; align-items: center; }
.ghost-btn {
  height: 44px;
  padding: 0 14px;
  background: #F1F5F9;
  color: #475569;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}
.ghost-btn::after { border: none; }
.ghost-btn-text { color: #475569; }

/* 帮助卡 */
.help-card {
  margin-top: 16px;
  padding: 10px 12px;
  background: #EFF6FF;
  border: 1px solid #DBEAFE;
  border-radius: 8px;
  display: flex;
  gap: 6px;
  align-items: flex-start;
}
.help-text {
  font-size: 10px;
  color: #1E3A8A;
  line-height: 1.6;
  flex: 1;
}
.help-text text { display: block; }
.help-link { font-weight: 600; color: #1E3A8A; }

/* 成功卡 */
.success-box {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.success-circle {
  width: 72px;
  height: 72px;
  border-radius: 36px;
  background: #D1FAE5;
  display: flex;
  align-items: center;
  justify-content: center;
}
.success-title {
  font-size: 17px;
  font-weight: 700;
  color: #0F172A;
  margin-top: 16px;
}
.success-sub {
  font-size: 12px;
  color: #64748B;
  margin-top: 6px;
  text-align: center;
}

.bottom-link {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 16px;
  font-size: 12px;
}
.bottom-link-text { color: #64748B; }
.bottom-link-action { color: #2563EB; font-weight: 600; margin-left: 4px; }
</style>
