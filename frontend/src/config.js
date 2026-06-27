export const API_BASE = '/api'

export function resolveImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  // 统一去掉 /api 前缀和前导 /，再加回 /api/ 前缀，走 Vite 代理
  let clean = url.replace(/^\/api(?=\/uploads)/, '').replace(/^\/+/, '')
  return '/api/' + clean
}
