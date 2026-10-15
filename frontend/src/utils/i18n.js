/**
 * i18n.js — 极简前端国际化工具（零依赖）
 *
 * 设计：
 *  - zh_CN / en_US 两套词条，写在 src/locales/ 下
 *  - App.vue provide 给全树，组件用 useI18n() 获取 t() 函数
 *  - 当前语言存 uni storage（刷新/切换页面不丢失）
 *  - 支持插值：t('greeting', { name: '林知行' })
 *
 * 用法（<script setup>）：
 *   import { useI18n } from '@/utils/i18n.js'
 *   const t = useI18n()
 *   // 模板：<text>{{ t('login.title') }}</text>
 *   // 代码：uni.showToast({ title: t('common.loading') })
 */

import { ref, reactive } from 'vue'

// ---------- 词条定义（见 src/locales/） ----------
export const LOCALES = [
  { key: 'zh_CN', label: '简体中文' },
  { key: 'en_US', label: 'English' }
]

export const DEFAULT_LOCALE = 'zh_CN'

const STORAGE_KEY = 'rocgit_locale'

let _locale = null  // 全局响应式
function getLocale() {
  if (_locale) return _locale
  try { _locale = ref(uni.getStorageSync(STORAGE_KEY) || DEFAULT_LOCALE) } catch { _locale = ref(DEFAULT_LOCALE) }
  return _locale
}

// ---------- 词条库（响应式 + 懒加载）----------
// 用 reactive 包裹，t() 读取时建立依赖，setLocale() 写入时触发刷新
const _messages = reactive({})
const _ready = ref(false)
const _loaded = new Set()
async function loadLocale(locale) {
  if (_loaded.has(locale)) return
  _loaded.add(locale)
  try {
    const msgs = locale === 'en_US'
      ? await import(/* @vite-ignore */ `../locales/en_US.js?v=${Date.now()}`)
      : await import(/* @vite-ignore */ `../locales/zh_CN.js?v=${Date.now()}`)
    Object.assign(_messages, msgs.default || msgs)
  } catch (e) {
    console.warn('[i18n] load locale failed', locale, e)
  }
}

// ---------- 核心 API ----------
export async function setLocale(locale) {
  const ref_ = getLocale()
  ref_.value = locale
  try { uni.setStorageSync(STORAGE_KEY, locale) } catch { /* ignore */ }
  await loadLocale(locale)
}

export function getCurrentLocale() {
  return getLocale().value
}

/**
 * 启动时初始化：同步触发一次默认语言加载（在 onLaunch 中 await）
 */
export async function initI18n() {
  const loc = getCurrentLocale() || DEFAULT_LOCALE
  await loadLocale(loc)
  _ready.value = true
}

export function isI18nReady() {
  return _ready.value
}

export function getMessages() {
  return _messages
}

/**
 * 获取翻译函数（组件 setup 内调用）
 * 返回 t(key, params?)
 */
export function useI18n() {
  const localeRef = getLocale()
  const messages = _messages

  function t(key, params) {
    let text = messages[key] || messages[key.replace(/^pages\./, '')] || key
    if (params && typeof params === 'object') {
      Object.entries(params).forEach(([k, v]) => {
        text = text.replace(new RegExp(`\\{${k}\\}`, 'g'), String(v))
      })
    }
    return text
  }

  // 响应式语言切换（vue3 setup 闭包捕获 localeRef 与 reactive messages）
  function locale() { return localeRef.value }
  function isZh() { return localeRef.value === 'zh_CN' }
  function isEn() { return localeRef.value === 'en_US' }

  return { t, locale, isZh, isEn, LOCALES, setLocale }
}
