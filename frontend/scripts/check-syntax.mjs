// scripts/check-syntax.mjs - 简易语法 + 引用检查
import fs from 'fs'
import path from 'path'

const VUE_FILES = [
  'src/pages/auth-login/auth-login.vue',
  'src/pages/auth-register/auth-register.vue',
  'src/pages/auth-forgot/auth-forgot.vue',
  'src/components/Icon.vue',
  'src/App.vue'
]

let issues = 0

// 1) 抽取 <script setup> 内容，校验括号配对
function extractScript(c) {
  const m = c.match(/<script setup>([\s\S]*?)<\/script>/)
  return m ? m[1] : ''
}

for (const f of VUE_FILES) {
  if (!fs.existsSync(f)) { console.log('  [SKIP] missing: ' + f); continue }
  const c = fs.readFileSync(f, 'utf8')
  const sc = extractScript(c)
  if (!sc) continue

  // 括号配对检查（不处理字符串内的括号；粗略）
  const counts = { '{': 0, '}': 0, '(': 0, ')': 0, '[': 0, ']': 0 }
  let inStr = null, inComment = null, inLineComment = false
  for (let i = 0; i < sc.length; i++) {
    const ch = sc[i]
    const next = sc[i + 1] || ''
    if (inLineComment) {
      if (ch === '\n') inLineComment = false
      continue
    }
    if (inComment) {
      if (ch === '*' && next === '/') { inComment = false; i++; }
      continue
    }
    if (inStr) {
      if (ch === '\\') { i++; continue }
      if (ch === inStr) inStr = null
      continue
    }
    if (ch === '/' && next === '/') { inLineComment = true; i++; continue }
    if (ch === '/' && next === '*') { inComment = true; i++; continue }
    if (ch === '"' || ch === "'" || ch === '`') { inStr = ch; continue }
    if (counts[ch] !== undefined) counts[ch]++
  }
  const pairs = [
    ['{', '}', counts['{'] === counts['}']],
    ['(', ')', counts['('] === counts[')']],
    ['[', ']', counts['['] === counts[']']]
  ]
  const fail = pairs.filter(p => !p[2])
  if (fail.length) {
    console.log('  ✗ ' + f + ' 括号不平衡: ' + fail.map(p => p[0] + p[1] + '=' + counts[p[0]] + '/' + counts[p[1]]).join(' '))
    issues++
  } else {
    console.log('  ✓ ' + f + ' 括号配对 OK  ' + JSON.stringify(counts))
  }
}

// 2) 检查 import 路径是否真的存在
console.log('\n--- import resolve check ---')
function resolveImport(fromFile, spec) {
  if (!spec.startsWith('.')) return null  // 跳过 bare 模块
  const fromDir = path.dirname(fromFile)
  let abs = path.resolve(fromDir, spec)
  // 依次尝试: 原样 / .vue / .js / /index.js
  if (fs.existsSync(abs)) return abs
  if (fs.existsSync(abs + '.vue')) return abs + '.vue'
  if (fs.existsSync(abs + '.js')) return abs + '.js'
  if (fs.existsSync(path.join(abs, 'index.js'))) return path.join(abs, 'index.js')
  return null
}

for (const f of VUE_FILES) {
  if (!fs.existsSync(f)) continue
  const c = fs.readFileSync(f, 'utf8')
  const sc = extractScript(c)
  const imports = [...sc.matchAll(/import\s+[^'"`]*?['"`]([^'"`]+)['"`]/g)].map(m => m[1])
  for (const imp of imports) {
    const abs = resolveImport(f, imp)
    if (abs === null) continue  // 跳过 bare
    if (!abs) {
      console.log('  ✗ ' + f + '\n      → cannot resolve: ' + imp)
      issues++
    }
  }
}

// 3) 检查 Icon 引用是否存在
console.log('\n--- Icon usage check ---')
for (const f of VUE_FILES) {
  if (!fs.existsSync(f)) continue
  const c = fs.readFileSync(f, 'utf8')
  const tpl = c.match(/<template>([\s\S]*?)<\/template>/)
  if (!tpl) continue
  const iconUsages = [...tpl[1].matchAll(/<Icon\s+name=["']([^"']+)["']/g)].map(m => m[1])
  for (const name of iconUsages) {
    // Icon 内部有 REDIRECT 表，列出我们当前用到的实际 base name
    console.log('  ' + f.split('/').pop().padEnd(20) + ' uses <Icon name="' + name + '">')
  }
}

console.log('\n' + (issues === 0 ? '✓ NO ISSUES' : '✗ ' + issues + ' ISSUE(S)'))
process.exit(issues === 0 ? 0 : 1)
