import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // H5 开发期 proxy：/api -> 后端 8080，避免不同人改 .env
  const proxyTarget = env.VITE_API_PROXY || 'http://127.0.0.1:8080'
  return {
    plugins: [uni()],
    server: {
      host: '127.0.0.1',
      port: 5173,
      proxy: {
        '/api': { target: proxyTarget, changeOrigin: true, secure: false }
      }
    }
  }
})
