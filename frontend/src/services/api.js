import { API_BASE, resolveImageUrl } from '../config.js'

// ==================== JWT 管理 ====================
const TOKEN_KEY = 'mt-token'
const USER_KEY = 'mt-user'

export function getToken() { return localStorage.getItem(TOKEN_KEY) }
export function setToken(token) { localStorage.setItem(TOKEN_KEY, token) }
export function removeToken() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY) }
export function getStoredUser() { try { return JSON.parse(localStorage.getItem(USER_KEY)) } catch { return null } }
export function setStoredUser(user) { localStorage.setItem(USER_KEY, JSON.stringify(user)) }

// ==================== 核心请求 ====================
const IMAGE_KEYS = new Set(['poster', 'avatar', 'moviePoster', 'url'])

function resolveUrls(obj) {
  if (Array.isArray(obj)) return obj.map(v => typeof v === 'string' && v.startsWith('/uploads/') ? resolveImageUrl(v) : resolveUrls(v))
  if (obj && typeof obj === 'object') {
    const out = {}
    for (const [k, v] of Object.entries(obj)) {
      out[k] = IMAGE_KEYS.has(k) && typeof v === 'string' && v && !v.startsWith('http')
        ? resolveImageUrl(v)
        : resolveUrls(v)
    }
    return out
  }
  return obj
}

async function request(method, path, body) {
  const opts = { method, headers: {} }
  const token = getToken()
  if (token) opts.headers['Authorization'] = `Bearer ${token}`
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json'
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(`${API_BASE}${path}`, opts)
  const json = await res.json()
  if (json.code !== 200) {
    if (json.code === 401) {
      removeToken()
      // 保存当前页面路径，登录后跳转回来
      const currentPath = window.location.pathname
      if (currentPath !== '/login' && currentPath !== '/register') {
        window.location.href = '/login?redirect=' + encodeURIComponent(currentPath)
      }
    }
    throw new Error(json.message || `请求失败 (${json.code})`)
  }
  return resolveUrls(json.data)
}

function get(path, params) {
  const qs = params ? '?' + Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '').map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&') : ''
  return request('GET', path + qs)
}

function post(path, body) { return request('POST', path, body) }
function put(path, body) { return request('PUT', path, body) }
function del(path) { return request('DELETE', path) }

async function upload(file) {
  const form = new FormData()
  form.append('file', file)
  const token = getToken()
  const headers = {}
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(`${API_BASE}/upload/image`, { method: 'POST', headers, body: form })
  const json = await res.json()
  if (json.code !== 200) throw new Error(json.message || '上传失败')
  return resolveUrls(json.data)
}

// ==================== 用户 ====================
export function apiRegister(username, password, phone) { return post('/user/register', { username, password, phone }) }
export async function apiLogin(username, password) {
  const data = await post('/user/login', { username, password })
  setToken(data.token)
  setStoredUser(data.userInfo)
  return data
}
export function apiGetProfile() { return get('/user/profile') }
export function apiUpdateProfile(data) { return put('/user/profile', data) }
export function apiChangePassword(oldPassword, newPassword) { return put('/user/password', { oldPassword, newPassword }) }
export function apiRecharge(amount, password) { return post('/user/recharge', { amount, password }) }

// ==================== 电影 ====================
export function apiGetMovies(params) { return get('/movie/list', params) }
export function apiGetMovie(id) { return get(`/movie/${id}`) }
export function apiGetMostExpectedMovies(limit = 5) { return get('/movie/most-expected', { limit }) }
export function apiToggleWish(movieId) { return post(`/movie/${movieId}/wish`) }
export function apiGetWishStatus(movieId) { return get(`/movie/${movieId}/wish-status`) }
export function apiGetUserWishList() { return get('/movie/user/wish-list') }

// ==================== 票房 ====================
export function apiGetBoxOffice(type = 'today') { return get('/box-office/today', { type }) }

// ==================== 通知 ====================
export function apiGetNotifications(params) { return get('/notification/list', params) }
export function apiGetUnreadCount() { return get('/notification/unread-count') }
export function apiMarkRead(id) { return put(`/notification/${id}/read`) }

// ==================== 评价 ====================
export function apiCreateReview(data) { return post('/review', data) }
export function apiGetReviewList(movieId, page = 1, size = 10) { return get('/review/list', { movieId, page, size }) }
export function apiGetReviewStatus(movieId) { return get(`/review/status/${movieId}`) }

// ==================== 影院 ====================
export function apiGetCinemas(params) { return get('/cinema/list', params) }

// ==================== 排片 ====================
export function apiGetShowtimes(params) { return get('/showtime/list', params) }
export function apiGetShowtime(id) { return get(`/showtime/${id}`) }
export function apiGetSeats(showtimeId) { return get(`/showtime/${showtimeId}/seats`) }

// ==================== 订单 ====================
export function apiLockSeats(showtimeId, seatIds) { return post('/order/lock', { showtimeId, seatIds }) }
export function apiCreateOrder(showtimeId, seatIds, lockToken) { return post('/order/create', { showtimeId, seatIds, lockToken }) }
export function apiGetOrderStatus(orderNo) { return get(`/order/status/${orderNo}`) }
export function apiGetOrder(orderNo) { return get(`/order/${orderNo}`) }
export function apiGetOrders(params) { return get('/order/list', params) }
export function apiCancelOrder(orderNo) { return post(`/order/cancel/${orderNo}`) }
export function apiCancelPreview(orderNo) { return get(`/order/cancel-preview/${orderNo}`) }
export function apiPayOrder(orderNo, password) { return post(`/order/pay/${orderNo}`, password ? { password } : undefined) }

// ==================== 管理员 ====================
export async function apiAdminLogin(username, password) {
  const data = await post('/admin/login', { username, password })
  setToken(data.token)
  setStoredUser(data.userInfo)
  return data
}
export function apiCreateMovie(data) { return post('/admin/movie', data) }
export function apiUpdateMovie(id, data) { return put(`/admin/movie/${id}`, data) }
export function apiDeleteMovie(id) { return del(`/admin/movie/${id}`) }
export function apiCreateShowtime(data) { return post('/admin/showtime', data) }
export function apiGetAdminShowtimes(params) { return get('/admin/showtime/list', params) }
export function apiDeleteShowtime(id) { return del(`/admin/showtime/${id}`) }
export function apiGetAdminCinemas(params) { return get('/admin/cinema/list', params) }
export function apiGetCinemaHalls(cinemaId) { return get(`/admin/cinema/${cinemaId}/halls`) }
export function apiCreateCinema(data) { return post('/admin/cinema', data) }
export function apiUpdateCinema(id, data) { return put(`/admin/cinema/${id}`, data) }
export function apiDeleteCinema(id) { return del(`/admin/cinema/${id}`) }
export function apiUpdateHall(id, data) { return put(`/admin/hall/${id}`, data) }
export function apiDeleteHall(id) { return del(`/admin/hall/${id}`) }
export function apiCreateHall(data) { return post('/admin/hall', data) }
export function apiGetAdminOrders(params) { return get('/admin/order/list', params) }
export function apiGetAdminOrderDetail(orderNo) { return get(`/admin/order/${orderNo}`) }
export function apiGetStatistics() { return get('/admin/statistics') }
// ==================== 管理员 - 用户管理 ====================
export function apiGetAdminUsers(params) { return get('/admin/user/list', params) }
export function apiGetAdminUser(id) { return get(`/admin/user/${id}`) }
export function apiUpdateAdminUser(id, data) { return put(`/admin/user/${id}`, data) }
export function apiToggleUserStatus(id) { return put(`/admin/user/${id}/status`) }

export { upload as apiUploadImage }
