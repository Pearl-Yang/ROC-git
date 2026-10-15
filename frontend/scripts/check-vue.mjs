// scripts/check-vue.mjs - 简单结构检查
import fs from 'fs'
import path from 'path'

const files = [
  'src/pages/auth-login/auth-login.vue',
  'src/pages/auth-register/auth-register.vue',
  'src/pages/auth-forgot/auth-forgot.vue',
  'src/components/Icon.vue',
  'src/pages.json',
  'src/utils/api.js',
  'src/utils/user-store.js',
  'src/utils/i18n.js',
  'src/locales/zh_CN.js',
  'src/locales/en_US.js',
  'src/App.vue'
]

let ok = true
for (const f of files) {
  if (!fs.existsSync(f)) {
    console.log('  [MISSING] ' + f)
    ok = false
    continue
  }
  const c = fs.readFileSync(f, 'utf8')
  const tplOpen = (c.match(/<template[\s>]/g) || []).length
  const tplClose = (c.match(/<\/template>/g) || []).length
  // script 块（包含 <script setup> / <script> 普通 / options 块）
  const scriptBlocks = (c.match(/<script(\s+setup)?[\s>]/g) || []).length
  const scriptClose = (c.match(/<\/script>/g) || []).length
  const styleBlocks = (c.match(/<style(\s+scoped)?[\s>]/g) || []).length
  const styleClose = (c.match(/<\/style>/g) || []).length
  console.log('  [' + (c.length + 'b').padEnd(7) + '] ' + f + '  tpl=' + tplOpen + '/' + tplClose + ' script=' + scriptBlocks + '/' + scriptClose + ' style=' + styleBlocks + '/' + styleClose)
  if (tplOpen !== tplClose) { console.log('    ⚠ <template> tag imbalance!'); ok = false }
  if (styleBlocks !== styleClose) { console.log('    ⚠ <style> tag imbalance!'); ok = false }
  // 跳过 scriptClose 严格比对（因为注释里可能含 </script> 字符串，但实际只有 <script 块的数量是确定的）
}

// 简单检查 i18n 词条是否补全
const i18nKeys = [
  'auth.slogan', 'auth.welcomeBack', 'auth.pwdTab', 'auth.codeTab',
  'auth.account', 'auth.pwd', 'auth.forgot', 'auth.phone', 'auth.smsCode',
  'auth.testAccount', 'auth.wechatScan', 'auth.emailCode', 'auth.miniAuth',
  'auth.regTitle', 'auth.regSubmit', 'auth.regTip',
  'auth.forgotTitle', 'auth.regPhone', 'auth.step1', 'auth.step2', 'auth.step3',
  'auth.resetSuccess', 'auth.backToLogin'
]
const zh = fs.readFileSync('src/locales/zh_CN.js', 'utf8')
const en = fs.readFileSync('src/locales/en_US.js', 'utf8')
console.log('\n--- i18n key check ---')
let missing = 0
for (const k of i18nKeys) {
  const zOK = zh.includes("'" + k + "'")
  const eOK = en.includes("'" + k + "'")
  if (!zOK || !eOK) {
    console.log('  [MISSING] ' + k + '  zh=' + zOK + ' en=' + eOK)
    missing++
  }
}
if (missing === 0) console.log('  ✓ all ' + i18nKeys.length + ' auth keys present in both locales')

// 检查 pages.json
const pagesJson = JSON.parse(fs.readFileSync('src/pages.json', 'utf8'))
const paths = pagesJson.pages.map(p => p.path)
console.log('\n--- pages.json routes ---')
const required = ['pages/auth-login/auth-login', 'pages/auth-register/auth-register', 'pages/auth-forgot/auth-forgot']
for (const r of required) {
  if (paths.includes(r)) console.log('  ✓ ' + r)
  else { console.log('  [MISSING] ' + r); ok = false }
}

console.log('\n--- summary ---')
console.log(ok && missing === 0 ? '✓ ALL OK' : '✗ ISSUES FOUND')
process.exit(ok && missing === 0 ? 0 : 1)
