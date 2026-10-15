/**
 * api.js - ROC-git 前端真实接口请求层
 *
 * 设计说明：
 *  - 基于 uni.request 封装，H5 与微信小程序双端通用。
 *  - baseURL 使用相对路径 '/api'，在 H5 部署时由 nginx 反向代理到后端 8080；
 *    小程序端可通过 manifest 配置或此处覆盖为完整域名。
 *  - 后端 JWT 拦截器要求除 health/config/login 外均需携带 token，
 *    本层在登录后自动把 token 存入缓存并在每次请求注入 Authorization。
 *  - 后端统一返回 { code, message, data }，code === 0 表示成功。
 */

// 可用环境变量覆盖 baseURL（如生产小程序需填 https://your-domain/api）
// 通过 frontend/.env.development / .env.production 设置 VITE_API_BASE
// H5 部署：默认 '/api'，由 nginx 反代到后端 8080
// APK 模式：可填 'http://127.0.0.1:8080/api'（搭配 adb reverse tcp:8080 tcp:8080）
// 小程序：需填 https://your-domain/api（要求 ICP 备案 + 合法域名）
function readEnvBase() {
  try {
    // uni-app 编译期会把 import.meta.env.VITE_API_BASE 注入为字符串
    // 在 H5 / MP-WEIXIN / APP 三端均可在 build 时通过 .env.* 配置
    if (typeof import.meta !== 'undefined' && import.meta && import.meta.env) {
      return import.meta.env.VITE_API_BASE || ''
    }
  } catch (e) { /* ignore */ }
  return ''
}

// 三端 baseURL 默认值（uni-app 的 #ifdef/#endif 注释会被编译器按平台裁剪）
// 微信小程序走宿主直连（要求 urlCheck: false），APK 走模拟器 10.0.2.2 访问宿主，H5 走相对路径
// #ifdef MP-WEIXIN
const PLATFORM_BASE = 'http://127.0.0.1:18080/api'
// #endif
// #ifdef APP-PLUS
const PLATFORM_BASE = 'http://10.0.2.2:18080/api'
// #endif
// #ifdef H5
const PLATFORM_BASE = '/api'
// #endif

const envBase = readEnvBase()
const BASE_URL = envBase || PLATFORM_BASE
// 演示：前端默认登录所用昵称/头像（后端 login 接口会原样回显）
const DEMO_NICKNAME = '林知行'
const DEMO_AVATAR = 'https://cdn.rocgit.com/avatar/demo.png'

const TOKEN_KEY = 'rocgit_token'
const USER_KEY = 'rocgit_user'

// 正在跳转登录页时置位，避免多个并发 401 重复 reLaunch
let redirectingToLogin = false

function getToken() {
  try { return uni.getStorageSync(TOKEN_KEY) || '' } catch (e) { return '' }
}
function setToken(token) {
  try { uni.setStorageSync(TOKEN_KEY, token) } catch (e) { /* ignore */ }
}
function getCachedUser() {
  try { return uni.getStorageSync(USER_KEY) || null } catch (e) { return null }
}
function setCachedUser(user) {
  try { uni.setStorageSync(USER_KEY, user) } catch (e) { /* ignore */ }
}

/** 凭证失效：清理本地登录态并跳登录页（仅触发一次） */
function handleUnauthorized() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  logout()
  // T-26：跳转到新原型 auth-login（兼容旧路径）
  uni.reLaunch({ url: '/pages/auth-login/auth-login' })
  setTimeout(() => { redirectingToLogin = false }, 1500)
}

/**
 * 核心请求方法
 * @param {Object} opts { url, method, data, params, silent }
 * @returns {Promise<any>} 解析后的 data 字段；HTTP 失败或 code!=0 时 reject
 */
// T-11：GET 缓存 key（按 url+params 生成），默认缓存 60s
const CACHE_PREFIX = 'rocgit_cache_'
const CACHE_TTL_MS = 60_000

function cacheKey(url, params) {
  const k = url + (params ? '?' + JSON.stringify(params) : '')
  return CACHE_PREFIX + k.replace(/[^a-zA-Z0-9_?=&.%-]/g, '_').slice(0, 200)
}

function getCache(key) {
  try {
    const raw = uni.getStorageSync(key)
    if (!raw) return null
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!obj || !obj.t || !obj.d) return null
    if (Date.now() - obj.t > CACHE_TTL_MS) return null
    return obj.d
  } catch (e) { return null }
}
function setCache(key, data) {
  try { uni.setStorageSync(key, JSON.stringify({ t: Date.now(), d: data })) } catch (e) { }
}

// T-E-2：读当前语言，拼到 Accept-Language header
function getAcceptLanguage() {
  try {
    const loc = uni.getStorageSync('rocgit_locale') || 'zh_CN'
    if (loc === 'en_US') return 'en-US,en;q=0.9'
    return 'zh-CN,zh;q=0.9'
  } catch (e) { return 'zh-CN,zh;q=0.9' }
}

function request(opts = {}) {
  const { url, method = 'GET', data = null, params = null, silent = false, noCache = false } = opts

  let query = ''
  if (params && Object.keys(params).length) {
    const qs = Object.keys(params)
      .filter(k => params[k] !== undefined && params[k] !== null && params[k] !== '')
      .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
      .join('&')
    if (qs) query = '?' + qs
  }

  // T-11：GET 缓存命中直接返（仅缓存 data 部分）
  const isGet = (method || 'GET').toUpperCase() === 'GET'
  const ckey = isGet ? cacheKey(url, params) : null
  if (isGet && !noCache) {
    const cached = getCache(ckey)
    if (cached !== null) return Promise.resolve(cached)
  }

  return new Promise((resolve, reject) => {
    const urlPath = (BASE_URL + url).replace(/^https?:\/\/[^/]+/, '')
    // 这些接口本身不需要鉴权，不参与 401 跳转
    const isPublic = /\/(login|health|config)\b/.test(urlPath)
    uni.request({
      url: BASE_URL + url + query,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        // T-E-2：把当前语言透传给后端，后端按 Locale 翻译错误消息
        'Accept-Language': getAcceptLanguage(),
        Authorization: getToken() ? `Bearer ${getToken()}` : ''
      },
      success: (res) => {
        if (res.statusCode === 401 && !isPublic) {
          handleUnauthorized()
          reject(new Error('未登录或登录已过期'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const body = res.data
          if (body && typeof body === 'object' && 'code' in body) {
            // 后端业务码表示未授权/凭证失效时同样清理登录态
            if (!isPublic && (body.code === 401 || body.code === 40101 || body.code === 1001)) {
              handleUnauthorized()
              reject(new Error(body.message || '未登录或登录已过期'))
              return
            }
            if (body.code === 0) {
              if (isGet) setCache(ckey, body.data)
              resolve(body.data)
            } else {
              const msg = body.message || '请求失败'
              if (!silent) uni.showToast({ title: String(msg), icon: 'none' })
              reject(new Error(msg))
            }
          } else {
            // 非标准结构，原样返回
            if (isGet) setCache(ckey, body)
            resolve(body)
          }
        } else {
          // T-11：网络 5xx → 读缓存
          if (isGet) {
            const cached = getCache(ckey)
            if (cached !== null) { resolve(cached); return }
          }
          if (!silent) uni.showToast({ title: `请求失败(${res.statusCode})`, icon: 'none' })
          reject(new Error(`HTTP ${res.statusCode}`))
        }
      },
      fail: (err) => {
        // T-11：网络断 → 读缓存降级
        if (isGet) {
          const cached = getCache(ckey)
          if (cached !== null) { resolve(cached); return }
        }
        if (!silent) uni.showToast({ title: '网络异常，请检查服务是否可用', icon: 'none' })
        reject(err)
      }
    })
  })
}

// ============ 认证 ============
/**
 * 微信登录（演示：前端直接调用后端 login 获取 token）
 * @returns {Promise<Object>} LoginVO { token, userId, nickname, ... }
 */
function login(payload = {}) {
  const body = {
    nickname: payload.nickname || DEMO_NICKNAME,
    avatarUrl: payload.avatarUrl || DEMO_AVATAR,
    code: payload.code || ('wx_user_' + Date.now())
  }
  return request({ url: '/v1/user/login', method: 'POST', data: body, silent: true })
    .then(user => {
      if (user && user.token) {
        setToken(user.token)
        setCachedUser(user)
      }
      return user
    })
}

/** 确保已有有效 token；无 token 时返回 null（不再自动登录） */
function ensureLogin() {
  if (getToken()) return Promise.resolve(getCachedUser() || { token: getToken() })
  return Promise.resolve(null)
}

/** 是否已登录 */
function isLoggedIn() {
  return !!getToken()
}

/** 退出登录，清除 token 和用户缓存 */
function logout() {
  try { uni.removeStorageSync(TOKEN_KEY) } catch (e) { /* ignore */ }
  try { uni.removeStorageSync(USER_KEY) } catch (e) { /* ignore */ }
}

// ============ 通用 ============
function health() { return request({ url: '/v1/health', silent: true }) }
function getConfig() { return request({ url: '/v1/config', silent: true }) }

/** 兼容旧的 JSON upload() 函数（保留，避免破坏外部引用） */
function upload() { return request({ url: '/v1/upload', method: 'POST', silent: true }) }

/**
 * T-01 上传文件（multipart/form-data）。
 * @param {string} filePath  本地文件路径（来自 uni.chooseImage / chooseMessageFile 的 tempFilePaths）
 * @param {Object} formData  附加字段：{ projectId, stageId, remark, name? }
 * @returns {Promise<FileVO>}
 */
function uploadFile(filePath, formData = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: BASE_URL + '/v1/file/upload',
      filePath,
      name: 'file',
      formData: Object.assign({}, formData, { projectId: formData.projectId, stageId: formData.stageId }),
      header: { Authorization: token ? `Bearer ${token}` : '' },
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (body && body.code === 0) {
            resolve(body.data)
          } else {
            const msg = (body && body.message) || '上传失败'
            uni.showToast({ title: msg, icon: 'none' })
            reject(new Error(msg))
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '上传失败：' + (err.errMsg || '网络异常'), icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 取得文件下载 URL（H5 直接 a[href] 触发；APP/MP 走 openDocument）
 */
function fileDownloadUrl(fileId) {
  return BASE_URL + '/v1/file/' + fileId + '/download'
}

// ============ 用户 ============
function getProfile() { return request({ url: '/v1/user/profile' }) }
function updateProfile(data) { return request({ url: '/v1/user/profile', method: 'PUT', data }) }
function loginHistory() { return request({ url: '/v1/user/login-history' }) }
function contributionStats() { return request({ url: '/v1/user/contribution-stats' }) }
// P3-5：注销账号（软删 + 30 天物理删除）
function cancelAccount(reason) { return request({ url: '/v1/user/cancel', method: 'POST', data: { reason } }) }
// P3-5：导出我的数据
function dataExport() { return request({ url: '/v1/user/data-export' }) }
// W3：openid 绑定状态
function openidStatus() { return request({ url: '/v1/user/openid-status' }) }
// W3：文件锁三件套
function fileLockAcquire(fileId) { return request({ url: `/v1/file/${fileId}/lock`, method: 'POST', noCache: true }) }
function fileLockRelease(fileId) { return request({ url: `/v1/file/${fileId}/unlock`, method: 'POST', noCache: true }) }
function fileLockStatus(fileId) { return request({ url: `/v1/file/${fileId}/lock`, noCache: true }) }

// ============ T-25 手机号+验证码登录 ============
/**
 * 发送短信验证码（公开接口）
 * @param {{phone:string, purpose?:string}} data
 * @returns {Promise<void>}
 */
function sendSmsCode(data) {
  return request({ url: '/v1/sms/send-code', method: 'POST', data, silent: true })
}

/**
 * 手机号+验证码登录（公开接口；未注册自动创建账号）
 * 与微信登录完全等价：返回 LoginVO 并写入 token / 缓存用户
 * @param {{phone:string, code:string, nickname?:string, avatarUrl?:string}} data
 * @returns {Promise<LoginVO>}
 */
function loginByPhone(data) {
  return request({ url: '/v1/sms/login-by-phone', method: 'POST', data, silent: true })
    .then(user => {
      if (user && user.token) {
        setToken(user.token)
        setCachedUser(user)
      }
      return user
    })
}

// ============ 小组 ============
function myGroups() { return request({ url: '/v1/group/my-list' }) }
function groupDetail(groupId) { return request({ url: `/v1/group/${groupId}` }) }
function groupStats(groupId) { return request({ url: `/v1/group/${groupId}/stats` }) }
function groupMembers(groupId, params) { return request({ url: `/v1/group/${groupId}/members`, params }) }
function groupProjects(groupId) { return request({ url: '/v1/project/list', params: { groupId } }) }

// T-18：流程图列表
function flowcharts() { return request({ url: '/v1/config/flowcharts' }) }
function groupMemberStats(groupId) { return request({ url: `/v1/group/${groupId}/member-stats` }) }
function createGroup(data) { return request({ url: '/v1/group/create', method: 'POST', data }) }
function joinGroup(data) { return request({ url: '/v1/group/join', method: 'POST', data }) }
function validateInviteCode(data) { return request({ url: '/v1/group/validate-invite-code', method: 'POST', data }) }

// ============ 项目 ============
function projectList(groupId) { return request({ url: '/v1/project/list', params: { groupId } }) }
function projectDetail(projectId) { return request({ url: `/v1/project/${projectId}` }) }
function projectProgress(projectId) { return request({ url: `/v1/project/${projectId}/progress` }) }
function projectActivities(projectId, params) { return request({ url: `/v1/project/${projectId}/activities`, params }) }
function projectTaskStats(projectId) { return request({ url: `/v1/project/${projectId}/task-stats` }) }
function projectStages(projectId) { return request({ url: `/v1/project/${projectId}/stage` }) }
function projectTemplateList() { return request({ url: '/v1/project/template/list' }) }
function createProject(data) { return request({ url: '/v1/project/create', method: 'POST', data }) }
// T-05：复制项目
function projectCopy(projectId, data) { return request({ url: `/v1/project/${projectId}/copy`, method: 'POST', data }) }
// T-06：项目成员贡献度统计
function projectContribution(projectId) { return request({ url: `/v1/project/${projectId}/contribution-stats` }) }
// 项目编辑/删除/结项
function projectUpdate(projectId, data) { return request({ url: `/v1/project/${projectId}`, method: 'PUT', data }) }
function projectDelete(projectId) { return request({ url: `/v1/project/${projectId}`, method: 'DELETE' }) }
function projectComplete(projectId) { return request({ url: `/v1/project/${projectId}/complete`, method: 'POST' }) }

// ============ 任务 ============
function myTasks(params) { return request({ url: '/v1/task/my-tasks', params }) }
function taskList(projectId, params) { return request({ url: '/v1/task/list', params: Object.assign({ projectId }, params) }) }
function taskDetail(taskId) { return request({ url: `/v1/task/${taskId}` }) }
function dueSoonTasks(days) { return request({ url: '/v1/task/due-soon', params: { days } }) }
function createTask(data) { return request({ url: '/v1/task/create', method: 'POST', data }) }
function completeTask(taskId, data) { return request({ url: `/v1/task/${taskId}/complete`, method: 'POST', data: data || {} }) }
function taskUpdateProgress(taskId, progress) { return request({ url: `/v1/task/${taskId}/progress`, method: 'POST', data: { progress } }) }

// ============ 消息 ============
function unreadCount() { return request({ url: '/v1/message/unread-count' }) }
function messageList(params) { return request({ url: '/v1/message/list', params }) }
function readAll() { return request({ url: '/v1/message/read-all', method: 'POST' }) }

// ============ 研讨/决策/公告（扩展，接口已预留） ============
function discussionList(projectId, params) { return request({ url: '/v1/discussion/list', params: Object.assign({ projectId }, params) }) }
function discussionCreate(data) { return request({ url: '/v1/discussion/create', method: 'POST', data }) }
function decisionList(projectId, params) { return request({ url: '/v1/decision/list', params: Object.assign({ projectId }, params) }) }
function decisionCreate(data) { return request({ url: '/v1/decision/create', method: 'POST', data }) }
function announcementList(params) { return request({ url: '/v1/announcement/list', params }) }

// ============ 分享 T-02 ============
function shareCreate(data) { return request({ url: '/v1/share/create', method: 'POST', data }) }
function shareUpdate(shareId, data) { return request({ url: `/v1/share/${shareId}`, method: 'PUT', data }) }
function shareDelete(shareId) { return request({ url: `/v1/share/${shareId}`, method: 'DELETE' }) }
function shareMyShares() { return request({ url: '/v1/share/my-shares' }) }
function shareStats(shareId) { return request({ url: `/v1/share/${shareId}/stats` }) }
function shareAccessLogs(shareId) { return request({ url: `/v1/share/${shareId}/access-logs` }) }
function shareRecordVisit(shareId) { return request({ url: `/v1/share/${shareId}/visit`, method: 'POST', silent: true }) }
/**
 * 访客视图（公开接口，无需 token）。返回 { project, stages, files?, permissionLevel, shareToken, ... }
 * @param {string} token 分享 token
 * @param {string} [password] 访问密码
 */
function shareVisitorView(token, password) {
  return request({ url: `/v1/share/visitor/${token}`, params: password ? { password } : null, silent: true })
}

// ============ 文件版本/统计 T-03 ============
/** 文件历史版本列表（走 file_version 表） */
function fileVersions(fileId) { return request({ url: `/v1/file/${fileId}/versions` }) }
/** 版本对比（行级 diff） */
function fileVersionCompare(fileId, from, to) { return request({ url: `/v1/version/file/${fileId}/compare`, params: { from, to } }) }
/** 回滚到指定版本 */
function fileVersionRollback(fileId, targetVersion, remark) {
  return request({ url: `/v1/version/file/${fileId}/rollback`, method: 'POST', data: { targetVersion, remark: remark || '' } })
}
/** 项目内文件类型统计（doc/image/video/other/total） */
function fileTypeStats(projectId) { return request({ url: '/v1/file/type-stats', params: { projectId } }) }
/** 文件下载记录 */
function fileDownloadRecords(fileId) { return request({ url: `/v1/file/${fileId}/download-records` }) }
/** 文件审核日志 */
function fileAuditLogs(fileId) { return request({ url: `/v1/file/${fileId}/audit-logs` }) }
/** T-12：在线预览元信息（按 Content-Type 分发） */
function filePreview(fileId) { return request({ url: `/v1/preview/${fileId}`, noCache: true }) }

// ============ 阶段 T-04 ============
function addStage(projectId, data) { return request({ url: `/v1/project/${projectId}/stage`, method: 'POST', data }) }
function updateStage(projectId, stageId, data) { return request({ url: `/v1/project/${projectId}/stage/${stageId}`, method: 'PUT', data }) }
function deleteStage(projectId, stageId) { return request({ url: `/v1/project/${projectId}/stage/${stageId}`, method: 'DELETE' }) }
function stageDetail(projectId, stageId) { return request({ url: `/v1/project/${projectId}/stage/${stageId}` }) }

// ============ 任务扩展 T-04 ============
function updateTask(taskId, data) { return request({ url: `/v1/task/${taskId}`, method: 'PUT', data }) }
function deleteTask(taskId) { return request({ url: `/v1/task/${taskId}`, method: 'DELETE' }) }
function transferTask(taskId, toUserId, remark) {
  return request({ url: `/v1/task/${taskId}/transfer`, method: 'POST', data: { toUserId, remark: remark || '' } })
}

// ============ 任务提醒 T-09 ============
function taskReminderMy(pageNum, pageSize) { return request({ url: '/v1/task-reminder/my', params: { pageNum, pageSize } }) }
function taskReminderUnsent() { return request({ url: '/v1/task-reminder/unsent-count' }) }
function taskReminderScan() { return request({ url: '/v1/task-reminder/scan', method: 'POST' }) }

// T-11：清空所有 GET 缓存（写操作后或刷新按钮）
function clearAllCache() {
  try {
    const info = uni.getStorageInfoSync()
    for (const k of info.keys) {
      if (k.indexOf(CACHE_PREFIX) === 0) uni.removeStorageSync(k)
    }
  } catch (e) { }
}

/**
 * T-11：乐观更新 helper
 * 用法：optimistic({ apply: ()=>{ list.value.push(x) }, revert: ()=>{ list.value.pop() },
 *                     request: api.someWriteApi(payload) })
 * 写失败时自动 revert
 */
function optimistic({ apply, revert, request: r }) {
  if (typeof apply === 'function') apply()
  return r.then(
    (res) => { clearAllCache(); return res },
    (err) => { if (typeof revert === 'function') revert(); throw err }
  )
}

export default {
  BASE_URL,
  getToken,
  setToken,
  getCachedUser,
  request,
  login,
  ensureLogin,
  isLoggedIn,
  logout,
  health,
  getConfig,
  upload,
  uploadFile,
  uploadFile,
  fileDownloadUrl,
  getProfile,
  updateProfile,
  loginHistory,
  contributionStats,
  cancelAccount,
  dataExport,
  openidStatus,
  fileLockAcquire,
  fileLockRelease,
  fileLockStatus,
  // T-25 手机号+验证码登录
  sendSmsCode,
  loginByPhone,
  myGroups,
  groupDetail,
  groupStats,
  groupMembers,
  groupProjects,
  flowcharts,
  groupMemberStats,
  createGroup,
  joinGroup,
  validateInviteCode,
  projectList,
  projectDetail,
  projectProgress,
  projectActivities,
  projectTaskStats,
  projectStages,
  projectTemplateList,
  projectCopy,
  projectContribution,
  projectUpdate,
  projectDelete,
  projectComplete,
  createProject,
  myTasks,
  taskList,
  taskDetail,
  dueSoonTasks,
  createTask,
  completeTask,
  taskUpdateProgress,
  unreadCount,
  messageList,
  readAll,
  discussionList,
  discussionCreate,
  decisionList,
  decisionCreate,
  announcementList,
  shareCreate,
  shareUpdate,
  shareDelete,
  shareMyShares,
  shareStats,
  shareAccessLogs,
  shareRecordVisit,
  shareVisitorView,
  fileVersions,
  fileVersionCompare,
  fileVersionRollback,
  fileTypeStats,
  fileDownloadRecords,
  fileAuditLogs,
  filePreview,
  addStage,
  updateStage,
  deleteStage,
  stageDetail,
  updateTask,
  deleteTask,
  transferTask,
  taskReminderMy,
  taskReminderUnsent,
  taskReminderScan,
  clearAllCache,
  optimistic
}
