<template>
  <RocSheet v-model:visible="show" :max-height="'90vh'">
    <view class="fps-wrap">
      <view class="fps-head">
        <view class="fps-title">
          <text class="fps-name">{{ fileName }}</text>
          <text class="fps-meta">{{ meta && meta.previewType ? meta.previewType : meta ? meta.reason : '' }}</text>
        </view>
        <view class="fps-close" @tap="close">
          <text class="fps-close-text">关闭</text>
        </view>
      </view>

      <view v-if="loading" class="fps-state">
        <text class="fps-state-text">正在加载预览…</text>
      </view>

      <view v-else-if="error" class="fps-state">
        <text class="fps-state-text">{{ error }}</text>
      </view>

      <scroll-view v-else-if="meta" scroll-y class="fps-body">
        <!-- 图片预览 -->
        <view v-if="meta.previewType === 'image'">
          <image class="fps-image" :src="meta.url" mode="widthFix" @error="onImageError" />
        </view>

        <!-- PDF 预览（H5 用 pdf.js，小程序/App 给下载入口） -->
        <view v-else-if="meta.previewType === 'pdf'">
          <view v-if="pdfSupported" class="fps-pdf">
            <view class="fps-pdf-toolbar">
              <text class="fps-pdf-info">第 {{ pdfPage }} / {{ pdfTotal }} 页</text>
              <view class="row" style="padding:0; gap:8px;">
                <view class="fps-pdf-btn" @tap="prevPage"><text class="fps-pdf-btn-text">上一页</text></view>
                <view class="fps-pdf-btn" @tap="nextPage"><text class="fps-pdf-btn-text">下一页</text></view>
              </view>
            </view>
            <canvas
              ref="pdfCanvas"
              canvas-id="pdfCanvas"
              class="fps-pdf-canvas"
              :style="{ height: pdfCanvasHeight + 'px' }"
            ></canvas>
          </view>
          <view v-else class="fps-fallback">
            <text class="fps-fallback-text">当前平台暂不支持 PDF 在线渲染</text>
            <view class="btn btn-primary" @tap="downloadOriginal">
              <text class="btn-text">下载 PDF</text>
            </view>
          </view>
        </view>

        <!-- 文本预览 -->
        <view v-else-if="meta.previewType === 'text'">
          <view class="fps-text-toolbar">
            <text class="fps-text-info">共 {{ formatSize(meta.totalSize) }}{{ meta.truncated ? '（已截断）' : '' }}</text>
            <view class="fps-pdf-btn" @tap="copyText"><text class="fps-pdf-btn-text">复制全文</text></view>
          </view>
          <view class="fps-text-body">
            <text class="fps-text-content">{{ meta.content || '(空)' }}</text>
          </view>
        </view>

        <!-- drawio：跳转外链 -->
        <view v-else-if="meta.previewType === 'drawio'">
          <view class="fps-fallback">
            <text class="fps-fallback-text">draw.io 文件请下载后用 draw.io 打开，或在新窗口预览</text>
            <view class="row" style="padding:0; gap:8px; margin-top: 12px;">
              <view class="btn btn-ghost" @tap="openExternal">
                <text class="btn-text">新窗口打开</text>
              </view>
              <view class="btn btn-primary" @tap="downloadOriginal">
                <text class="btn-text">下载文件</text>
              </view>
            </view>
          </view>
        </view>

        <!-- Office 转码中 -->
        <view v-else-if="meta.previewType === 'convert'">
          <view class="fps-fallback">
            <text class="fps-fallback-text">正在将 Office 文件转为 PDF…{{ meta.reason === 'needs_convert' ? '（首次需要 30-90 秒）' : '' }}</text>
            <view class="row" style="padding:0; gap:8px; margin-top: 12px;">
              <view class="btn btn-ghost" @tap="pollConvert">
                <text class="btn-text">刷新状态</text>
              </view>
              <view class="btn btn-primary" @tap="downloadOriginal">
                <text class="btn-text">下载原文件</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 不支持 -->
        <view v-else>
          <view class="fps-fallback">
            <text class="fps-fallback-text">此文件类型暂不支持在线预览</text>
            <view class="btn btn-primary" style="margin-top:12px;" @tap="downloadOriginal">
              <text class="btn-text">下载文件</text>
            </view>
          </view>
        </view>

        <view class="fps-foot">
          <view class="btn btn-ghost" @tap="downloadOriginal">
            <text class="btn-text">下载原文件</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </RocSheet>
</template>

<script setup>
/**
 * FilePreviewSheet · T-12 在线预览抽屉
 * props: visible (model), fileId, fileName
 * events: update:visible, close
 */
import { ref, watch, computed, nextTick } from 'vue'
import api from '@/utils/api.js'
import { loadPdfJs, isPdfSupported } from '@/utils/pdf-loader.js'
import Sheet from '@/components/Sheet.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  fileId: { type: [Number, String], default: 0 },
  fileName: { type: String, default: '' }
})
const emit = defineEmits(['update:visible', 'close'])

const show = computed({
  get: () => props.visible,
  set: v => emit('update:visible', v)
})

const loading = ref(false)
const error = ref('')
const meta = ref(null)
const pdfPage = ref(1)
const pdfTotal = ref(1)
const pdfCanvasHeight = ref(480)
const pdfDoc = ref(null)
const pdfSupported = ref(false)
const pdfCanvas = ref(null)

function close() {
  show.value = false
  emit('close')
}

async function loadMeta() {
  if (!props.fileId) {
    error.value = '文件 ID 缺失'
    return
  }
  loading.value = true
  error.value = ''
  meta.value = null
  pdfDoc.value = null
  pdfPage.value = 1
  try {
    const res = await api.filePreview(props.fileId)
    meta.value = res
    if (res && res.previewType === 'pdf' && isPdfSupported()) {
      pdfSupported.value = true
      await nextTick()
      await renderPdf(res.url || res.convertedUrl)
    }
  } catch (e) {
    error.value = (e && e.message) || '加载预览失败'
  } finally {
    loading.value = false
  }
}

watch(() => [props.visible, props.fileId], async ([v, fid], [ov, ofid]) => {
  if (v && (v !== ov || fid !== ofid)) {
    await loadMeta()
  }
})

async function renderPdf(url) {
  try {
    const pdfjs = await loadPdfJs()
    if (!url) {
      error.value = 'PDF 地址缺失'
      return
    }
    const loadingTask = pdfjs.getDocument({ url, withCredentials: false })
    const pdf = await loadingTask.promise
    pdfDoc.value = pdf
    pdfTotal.value = pdf.numPages
    await renderPage(1)
  } catch (e) {
    console.error('[FilePreviewSheet] PDF 渲染失败', e)
    error.value = 'PDF 渲染失败：' + (e && e.message ? e.message : '未知错误')
  }
}

async function renderPage(pageNum) {
  if (!pdfDoc.value) return
  const page = await pdfDoc.value.getPage(pageNum)
  const viewport = page.getViewport({ scale: 1.2 })
  // #ifdef H5
  const canvas = document.querySelector('.fps-pdf-canvas')
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = viewport.width
  canvas.height = viewport.height
  pdfCanvasHeight.value = viewport.height
  await page.render({ canvasContext: ctx, viewport }).promise
  // #endif
  pdfPage.value = pageNum
}

async function prevPage() {
  if (pdfPage.value > 1) await renderPage(pdfPage.value - 1)
}
async function nextPage() {
  if (pdfPage.value < pdfTotal.value) await renderPage(pdfPage.value + 1)
}

async function pollConvert() {
  if (!meta.value || !props.fileId) return
  try {
    const res = await api.filePreview(props.fileId)
    meta.value = res
    if (res && res.previewType === 'pdf') {
      pdfSupported.value = true
      await nextTick()
      await renderPdf(res.url || res.convertedUrl)
    }
  } catch (e) {
    error.value = (e && e.message) || '刷新失败'
  }
}

function copyText() {
  if (!meta.value || !meta.value.content) return
  // #ifdef H5
  navigator.clipboard.writeText(meta.value.content).then(
    () => uni.showToast({ title: '已复制', icon: 'none' }),
    () => uni.showToast({ title: '复制失败', icon: 'none' })
  )
  // #endif
  // #ifndef H5
  uni.setClipboardData({ data: meta.value.content, success: () => uni.showToast({ title: '已复制', icon: 'none' }) })
  // #endif
}

function openExternal() {
  if (!meta.value || !meta.value.url) return
  // #ifdef H5
  window.open(meta.value.url, '_blank')
  // #endif
  // #ifndef H5
  uni.setClipboardData({ data: meta.value.url, success: () => uni.showToast({ title: '已复制链接', icon: 'none' }) })
  // #endif
}

function downloadOriginal() {
  if (!props.fileId) return
  const url = api.fileDownloadUrl(props.fileId)
  // #ifdef H5
  window.open(url, '_blank')
  // #endif
  // #ifndef H5
  uni.setClipboardData({ data: url, success: () => uni.showToast({ title: '已复制下载链接', icon: 'none' }) })
  // #endif
}

function onImageError() {
  error.value = '图片加载失败'
}

function formatSize(b) {
  if (!b) return '0 B'
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<script>
export default {
  components: { RocSheet: Sheet }
}
</script>

<style scoped>
.fps-wrap { display: flex; flex-direction: column; min-height: 60vh; }
.fps-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px; border-bottom: 1px solid #E2E8F0;
}
.fps-title { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
.fps-name { font-size: 15px; font-weight: 700; color: #0F172A; }
.fps-meta { font-size: 11px; color: #64748B; }
.fps-close {
  background: #F1F5F9; border-radius: 9999px; padding: 6px 12px;
}
.fps-close-text { font-size: 12px; color: #475569; }
.fps-state { padding: 60px 16px; text-align: center; }
.fps-state-text { font-size: 13px; color: #64748B; }
.fps-body { flex: 1; padding: 14px 16px 24px; }
.fps-image { width: 100%; background: #F8FAFC; border-radius: 8px; }
.fps-pdf-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 0 10px; gap: 8px;
}
.fps-pdf-info { font-size: 12px; color: #475569; }
.fps-pdf-btn {
  background: #F1F5F9; border-radius: 8px; padding: 6px 12px;
}
.fps-pdf-btn-text { font-size: 12px; color: #0F172A; }
.fps-pdf-canvas { width: 100%; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 8px; }
.fps-text-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 0 10px;
}
.fps-text-info { font-size: 12px; color: #64748B; }
.fps-text-body {
  background: #0F172A; color: #E2E8F0; padding: 14px;
  border-radius: 10px; max-height: 50vh; overflow: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.fps-text-content {
  font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all;
  color: #E2E8F0;
}
.fps-fallback {
  padding: 40px 16px; display: flex; flex-direction: column; align-items: center; gap: 12px;
  background: #F8FAFC; border-radius: 12px;
}
.fps-fallback-text { font-size: 13px; color: #475569; text-align: center; }
.fps-foot { margin-top: 16px; display: flex; justify-content: center; }
.btn {
  border-radius: 10px; padding: 10px 20px; min-width: 100px;
  text-align: center;
}
.btn-primary { background: #2563EB; }
.btn-primary .btn-text { color: #FFFFFF; font-size: 13px; font-weight: 600; }
.btn-ghost { background: #F1F5F9; border: 1px solid #E2E8F0; }
.btn-ghost .btn-text { color: #0F172A; font-size: 13px; font-weight: 600; }
</style>
