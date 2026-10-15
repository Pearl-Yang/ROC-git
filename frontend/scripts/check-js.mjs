// scripts/check-js.mjs - 用 Function 构造器校验（剥离 import/export）
import fs from 'fs'

const VUE_FILES = [
  'src/pages/auth-login/auth-login.vue',
  'src/pages/auth-register/auth-register.vue',
  'src/pages/auth-forgot/auth-forgot.vue',
  'src/components/Icon.vue'
]

// 把 ESM 语法剥离以让 Function 构造器能解析（仅用于语法检查，不执行）
function stripEsm(code) {
  // 移除 import 整行
  code = code.replace(/^\s*import[\s\S]*?['"][^'"]+['"];?/gm, '')
  // 移除 export default {...}，把 default 后的对象保留
  code = code.replace(/export\s+default\s+/g, '')
  // 移除 export const/function
  code = code.replace(/^\s*export\s+(const|function|class|let|var|async)\s+/gm, '$1 ')
  // import.meta 替换成 ({})
  code = code.replace(/import\.meta/g, '({})')
  return code
}

let issues = 0
for (const f of VUE_FILES) {
  const c = fs.readFileSync(f, 'utf8')
  const scriptBlocks = [...c.matchAll(/<script(\s+setup)?[\s>]([\s\S]*?)<\/script>/g)]
  for (const m of scriptBlocks) {
    const kind = m[1] ? 'setup' : 'options'
    const code = stripEsm(m[2])
    try {
      // Function 构造器会 parse 但不执行
      new Function(code)
    } catch (e) {
      console.log('  ✗ ' + f + ' [' + kind + ']: ' + e.message.split('\n')[0])
      issues++
    }
  }
  if (issues === 0 || scriptBlocks.length === 1) {
    console.log('  ✓ ' + f + '（' + scriptBlocks.length + ' script blocks）')
  }
}

// 校验 JS 文件
const JS_FILES = ['src/utils/api.js', 'src/utils/user-store.js', 'src/utils/i18n.js', 'src/locales/zh_CN.js', 'src/locales/en_US.js']
for (const f of JS_FILES) {
  const c = fs.readFileSync(f, 'utf8')
  const code = stripEsm(c)
  try {
    new Function(code)
    console.log('  ✓ ' + f)
  } catch (e) {
    console.log('  ✗ ' + f + ': ' + e.message.split('\n')[0])
    issues++
  }
}

console.log('\n' + (issues === 0 ? '✓ ALL JS PARSED' : '✗ ' + issues + ' JS ISSUE(S)'))
process.exit(issues === 0 ? 0 : 1)
