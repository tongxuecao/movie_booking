import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiGetOrders, apiGetOrder, apiGetOrderStatus, apiLockSeats, apiCreateOrder, apiCancelOrder, apiCancelPreview, apiPayOrder } from '../services/api.js'
import { useAuthStore } from './auth.js'

const PAY_CTX_KEY = 'mt-payment-ctx'

export const useOrderStore = defineStore('orders', () => {
  const orders = ref([])
  const total = ref(0)

  // 支付上下文：持久化到 localStorage 防止刷新丢失
  const paymentContext = ref(loadPaymentContext())

  function loadPaymentContext() {
    try {
      return JSON.parse(localStorage.getItem(PAY_CTX_KEY)) || null
    } catch { return null }
  }

  function setPaymentContext(ctx) {
    paymentContext.value = ctx
    localStorage.setItem(PAY_CTX_KEY, JSON.stringify(ctx))
  }

  function clearPaymentContext() {
    paymentContext.value = null
    localStorage.removeItem(PAY_CTX_KEY)
  }

  async function fetchOrders(params = {}) {
    const data = await apiGetOrders(params)
    orders.value = data.list || []
    total.value = data.total || 0
    return data
  }

  async function getOrderDetail(orderNo) {
    return await apiGetOrder(orderNo)
  }

  async function lockSeats(showtimeId, seatIds) {
    return await apiLockSeats(showtimeId, seatIds)
  }

  async function createOrder(showtimeId, seatIds, lockToken) {
    return await apiCreateOrder(showtimeId, seatIds, lockToken)
  }

  async function pollOrderStatus(orderNo, maxAttempts = 20) {
    // 等待 1 秒再开始轮询，给后端处理时间
    await new Promise(r => setTimeout(r, 1000))
    for (let i = 0; i < maxAttempts; i++) {
      try {
        const data = await apiGetOrderStatus(orderNo)
        if (data.status !== 'processing') return data
      } catch {
        // 忽略单次轮询错误，继续重试
      }
      await new Promise(r => setTimeout(r, 2000))
    }
    return { status: 'timeout', message: '订单处理超时' }
  }

  async function cancelOrder(orderNo) {
    return await apiCancelOrder(orderNo)
  }

  async function previewCancel(orderNo) {
    return await apiCancelPreview(orderNo)
  }

  async function payOrder(orderNo, password) {
    return await apiPayOrder(orderNo, password)
  }

  async function refreshProfile() {
    const authStore = useAuthStore()
    await authStore.refreshProfile()
  }

  return { orders, total, paymentContext, setPaymentContext, clearPaymentContext, fetchOrders, getOrderDetail, lockSeats, createOrder, pollOrderStatus, cancelOrder, previewCancel, payOrder, refreshProfile }
})
