<template>
  <!-- H5: 渲染 @iconify/vue 的 svg -->
  <!-- #ifdef H5 -->
  <Icon :icon="name" :width="size" :height="size" :class="cls" :style="iconStyle" />
  <!-- #endif -->

  <!-- MP / APP: 降级为 lucide 关键字映射的 emoji 字符或纯色文字 -->
  <!-- #ifdef MP-WEIXIN || APP-PLUS -->
  <text :class="cls" :style="iconStyle" class="icon-fallback">{{ fallbackChar }}</text>
  <!-- #endif -->
</template>

<script>
// 跨端 icon 组件
// H5：使用 @iconify/vue 渲染 lucide 图标
// 小程序/App：使用 emoji 字符兜底
import { computed } from 'vue'
// #ifdef H5
import { Icon } from '@iconify/vue'
// #endif

export default {
  name: 'RocIcon',
  components: {
    // #ifdef H5
    Icon
    // #endif
  },
  props: {
    name: { type: String, default: 'lucide:circle' },
    size: { type: [Number, String], default: 16 },
    cls: { type: String, default: '' },
    color: { type: String, default: '' }
  },
  setup(props) {
    // lucide: 关键字 → emoji 兜底（仅在小程序/App 端使用）
    const ICON_FALLBACK = {
      'lucide:git-branch': '⌥',
      'lucide:home': '⌂',
      'lucide:user': '☻',
      'lucide:user-round': '☻',
      'lucide:lock': '⚿',
      'lucide:eye': '◉',
      'lucide:eye-off': '◌',
      'lucide:mail': '✉',
      'lucide:smartphone': '▣',
      'lucide:shield-check': '✓',
      'lucide:chevron-left': '‹',
      'lucide:chevron-right': '›',
      'lucide:chevron-down': '⌄',
      'lucide:chevron-up': '⌃',
      'lucide:x': '×',
      'lucide:check': '✓',
      'lucide:plus': '＋',
      'lucide:bell': '♪',
      'lucide:search': '⌕',
      'lucide:log-in': '→',
      'lucide:log-out': '←',
      'lucide:user-plus': '＋',
      'lucide:message-circle': '✉',
      'lucide:key-round': '⚷',
      'lucide:info': 'i',
      'lucide:settings': '⚙',
      'lucide:folder': '▢',
      'lucide:folder-open': '▣',
      'lucide:file': '▤',
      'lucide:file-text': '▤',
      'lucide:file-plus': '▤',
      'lucide:download': '↓',
      'lucide:upload': '↑',
      'lucide:share': '↗',
      'lucide:copy': '⎘',
      'lucide:trash': '🗑',
      'lucide:trash-2': '🗑',
      'lucide:edit': '✎',
      'lucide:more-horizontal': '⋯',
      'lucide:more-vertical': '⋮',
      'lucide:arrow-left': '←',
      'lucide:arrow-right': '→',
      'lucide:arrow-up': '↑',
      'lucide:arrow-down': '↓',
      'lucide:calendar': '▦',
      'lucide:clock': '◔',
      'lucide:hash': '#',
      'lucide:list': '☰',
      'lucide:layout-grid': '▦',
      'lucide:star': '★',
      'lucide:heart': '♡',
      'lucide:bookmark': '☆',
      'lucide:tag': '⚐',
      'lucide:flag': '⚑',
      'lucide:zap': '⚡',
      'lucide:circle': '●',
      'lucide:circle-dot': '●',
      'lucide:check-circle': '✓',
      'lucide:x-circle': '×',
      'lucide:alert-circle': '!',
      'lucide:help-circle': '?',
      'lucide:activity': '∿',
      'lucide:bar-chart': '▥',
      'lucide:pie-chart': '◑',
      'lucide:trending-up': '↗',
      'lucide:trending-down': '↘',
      'lucide:globe': '◯',
      'lucide:wifi': '◉',
      'lucide:wifi-off': '⊘',
      'lucide:moon': '☾',
      'lucide:sun': '☀',
      'lucide:database': '▥',
      'lucide:archive': '▣',
      'lucide:history': '↻',
      'lucide:rotate-cw': '↻',
      'lucide:refresh-cw': '↻',
      'lucide:filter': '⌕',
      'lucide:send': '➤',
      'lucide:image': '▣',
      'lucide:video': '▷',
      'lucide:music': '♪',
      'lucide:type': 'A',
      'lucide:monitor': '▭',
      'lucide:paperclip': '◐',
      'lucide:link': '⇎',
      'lucide:external-link': '↗',
      'lucide:qr-code': '▦',
      'lucide:book-open': '☲',
      'lucide:briefcase': '▣',
      'lucide:users': '☰',
      'lucide:user-check': '✓',
      'lucide:user-x': '⊗',
      'lucide:map-pin': '◉',
      'lucide:rocket': '➤',
      'lucide:shield': '◈',
      'lucide:award': '★',
      'lucide:target': '◉',
      'lucide:layers': '▤',
      'lucide:package': '▣',
      'lucide:box': '▣',
      'lucide:git-commit': '◉',
      'lucide:git-merge': '⇋',
      'lucide:git-pull-request': '⇋',
      'lucide:git-fork': '⇌',
      'lucide:lock-keyhole': '⚿',
      'lucide:key': '⚷',
      'lucide:cog': '⚙',
      'lucide:sliders-horizontal': '⚙',
      'lucide:maximize-2': '⛶',
      'lucide:minimize-2': '◱',
      'lucide:eye-icon': '◉',
      'lucide:eye-fallback': '◉',
      // P0-5 补齐缺的兜底
      'lucide:upload-cloud': '⇪',
      'lucide:file-search': '⌕',
      'lucide:plus-circle': '⊕',
      'lucide:file-up': '⇧',
      'lucide:file-check': '✓',
      'lucide:file-spreadsheet': '▥',
      'lucide:file-type': 'T',
      'lucide:check-circle-2': '✓',
      'lucide:git-compare': '⇄',
      'lucide:scan': '⌖',
      'lucide:archive': '▣',
      'lucide:building-2': '🏢',
      'lucide:clipboard-list': '☰'
    }
    const fallbackChar = computed(() => ICON_FALLBACK[props.name] || '●')
    const iconStyle = computed(() => {
      const s = `font-size:${Number(props.size)}px;line-height:1;`
      return props.color ? s + `color:${props.color};` : s
    })
    return { fallbackChar, iconStyle }
  }
}
</script>

<style scoped>
.icon-fallback { display: inline-block; font-style: normal; text-align: center; vertical-align: middle; }
</style>
