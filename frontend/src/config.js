export const API_BASE = '/api'
// 图片由 Vite 中间件直接提供静态文件，不需要 /api 代理前缀
export const UPLOAD_BASE = ''

export function resolveImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    // 局域网场景：把 localhost/127.0.0.1 的绝对 URL 转为相对路径
    if (url.includes('localhost') || url.includes('127.0.0.1') || url.includes('0.0.0.0')) {
      const path = url.replace(/^https?:\/\/[^/]+/i, '')
      return resolveImageUrl(path)
    }
    return url
  }
  // 去掉可能存在的 /api 前缀（旧数据兼容）
  let clean = url.startsWith('/api/') ? url.slice(4) : url
  if (!clean.startsWith('/')) clean = '/' + clean
  return clean
}
