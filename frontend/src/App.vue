<script>
import { initI18n, getCurrentLocale } from './utils/i18n.js'

export default {
  async onLaunch() {
    // T-E-2：i18n 启动时先 await 加载默认语言词条，避免首屏出现 key 名（"auth.slogan"）
    //   - getCurrentLocale() 默认 'zh_CN'，用户手动切到 en_US 时读 storage
    //   - 用户要求"先都显示中文"：如果 storage 里有 en_US 但用户没要求切，就保持 zh_CN
    //     → 启动时如果 storage 是 en_US，强制刷回 zh_CN
    try { uni.removeStorageSync('rocgit_locale') } catch (e) { /* ignore */ }
    await initI18n()  // → 加载 zh_CN

    // 路由启动判断
    try {
      const hasToken = !!uni.getStorageSync('rocgit_token')
      uni.reLaunch({ url: hasToken ? '/pages/home/home' : '/pages/auth-login/auth-login' })
    } catch (e) {
      uni.reLaunch({ url: '/pages/auth-login/auth-login' })
    }
  },
  onShow() {
    // 保留预留 hook（热启动 / 回到前台）
  },
  onHide() {
    // 保留预留 hook
  }
}
</script>

<style>
@import './styles/base.css';
</style>
