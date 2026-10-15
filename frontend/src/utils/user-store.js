/**
 * user-store.js - 轻量级跨页面用户状态管理
 *
 * 用法（任一 .vue 页面）：
 *   import { useUserStore } from '@/utils/user-store.js'
 *   const store = useUserStore()
 *   await store.login({ nickname: '林知行', code: 'wx_user_dev_demo' })
 *   store.user  // reactive
 *   store.token // reactive string
 *   store.isLoggedIn // boolean
 */

import { reactive, computed } from 'vue'
import api from './api.js'

const TOKEN_KEY = 'rocgit_token'
const USER_KEY = 'rocgit_user'

const state = reactive({
  token: readStorage(TOKEN_KEY, ''),
  user: readStorage(USER_KEY, null)
})

function readStorage(key, fallback) {
  try {
    const v = uni.getStorageSync(key)
    return v === '' || v === undefined || v === null ? fallback : v
  } catch (e) {
    return fallback
  }
}

function writeStorage(key, val) {
  try {
    if (val === undefined || val === null || val === '') {
      uni.removeStorageSync(key)
    } else {
      uni.setStorageSync(key, val)
    }
  } catch (e) { /* ignore */ }
}

export function useUserStore() {
  return {
    state,
    token: computed(() => state.token),
    user: computed(() => state.user),
    isLoggedIn: computed(() => !!state.token),

    async login(payload = {}) {
      const res = await api.login(payload)
      if (res && res.token) {
        state.token = res.token
        state.user = res
        writeStorage(TOKEN_KEY, res.token)
        writeStorage(USER_KEY, res)
      }
      return res
    },

    /**
     * T-25：手机号+验证码登录（自动注册）
     * 用法：await store.loginByPhone({ phone, code, nickname? })
     */
    async loginByPhone(payload = {}) {
      const res = await api.loginByPhone(payload)
      if (res && res.token) {
        state.token = res.token
        state.user = res
        writeStorage(TOKEN_KEY, res.token)
        writeStorage(USER_KEY, res)
      }
      return res
    },

    refresh() {
      state.token = readStorage(TOKEN_KEY, '')
      state.user = readStorage(USER_KEY, null)
    },

    setUser(user) {
      state.user = user
      writeStorage(USER_KEY, user)
    },

    logout() {
      api.logout()
      state.token = ''
      state.user = null
    }
  }
}

export default useUserStore
