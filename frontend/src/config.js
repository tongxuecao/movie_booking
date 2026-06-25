const hostname = window.location.hostname
const BACKEND_PORT = 8080

export const API_BASE = `http://${hostname}:${BACKEND_PORT}/api`
export const UPLOAD_BASE = `http://${hostname}:${BACKEND_PORT}/api/uploads`

export function resolveImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `${UPLOAD_BASE}${url.startsWith('/') ? '' : '/'}${url}`
}
