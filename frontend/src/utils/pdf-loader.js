/**
 * pdf-loader.js - pdf.js 按需懒加载
 *
 * pdfjs-dist 在小程序 / App 平台无法运行（无 DOM Canvas），所以仅在 H5 平台加载。
 * 用法：
 *   const { renderPage, getDocument } = await loadPdfJs()
 *   const pdf = await getDocument(url).promise
 *   const page = await pdf.getPage(1)
 *   await renderPage(page, canvas)
 */
let _pdfjs = null

export async function loadPdfJs() {
  if (_pdfjs) return _pdfjs
  // #ifdef H5
  if (typeof window === 'undefined') {
    throw new Error('pdf.js 只能在浏览器环境加载')
  }
  const pdfjs = await import('pdfjs-dist/build/pdf.mjs')
  // worker 用本地副本，避免 CDN 跨域
  try {
    pdfjs.GlobalWorkerOptions.workerSrc = require('pdfjs-dist/build/pdf.worker.mjs')
  } catch (e) {
    // 兼容 ESM 浏览器：使用 import.meta.url
    try {
      const workerUrl = (await import('pdfjs-dist/build/pdf.worker.mjs?url')).default
      pdfjs.GlobalWorkerOptions.workerSrc = workerUrl
    } catch (e2) {
      console.warn('[pdf-loader] worker 设置失败，回退到 fake worker', e2)
      pdfjs.GlobalWorkerOptions.workerSrc = ''
    }
  }
  _pdfjs = pdfjs
  return pdfjs
  // #endif
  // #ifndef H5
  throw new Error('当前平台不支持 pdf.js 在线渲染')
  // #endif
}

export function isPdfSupported() {
  // #ifdef H5
  return true
  // #endif
  // #ifndef H5
  return false
  // #endif
}
