// scripts/parse-sfc.mjs - 使用 @vue/compiler-sfc 解析 .vue
import { parse } from '@vue/compiler-sfc'
import fs from 'fs'
import path from 'path'

const files = [
  'src/pages/auth-login/auth-login.vue',
  'src/pages/auth-register/auth-register.vue',
  'src/pages/auth-forgot/auth-forgot.vue',
  'src/components/Icon.vue'
]

let ok = true
for (const f of files) {
  const c = fs.readFileSync(f, 'utf8')
  const { descriptor, errors } = parse(c, { filename: f })
  if (errors && errors.length) {
    console.log('  ✗ ' + f + ' PARSE ERRORS:')
    for (const e of errors) console.log('    - ' + e.message)
    ok = false
    continue
  }
  const tplErr = (descriptor.template && descriptor.template.errors) || []
  const scpErr = (descriptor.scriptSetup && descriptor.scriptSetup.errors) || []
  const allErr = tplErr.concat(scpErr)
  if (allErr.length) {
    console.log('  ✗ ' + f + ' INNER ERRORS:')
    for (const e of allErr) console.log('    - ' + (e.message || e))
    ok = false
    continue
  }
  console.log('  ✓ ' + f)
  console.log('    template: ' + (descriptor.template ? descriptor.template.content.length + 'b' : 'NONE'))
  console.log('    scriptSetup: ' + (descriptor.scriptSetup ? descriptor.scriptSetup.content.length + 'b' : 'NONE'))
  if (descriptor.script) console.log('    script: ' + descriptor.script.content.length + 'b')
  if (descriptor.styles && descriptor.styles.length) {
    for (const s of descriptor.styles) console.log('    style: ' + s.content.length + 'b (scoped=' + s.scoped + ')')
  }
}
console.log(ok ? '\n✓ ALL .vue FILES PARSED OK' : '\n✗ ISSUES')
process.exit(ok ? 0 : 1)
