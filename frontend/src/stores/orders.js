import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiGetOrders, apiGetOrder, apiGetOrderStatus, apiLockSeats, apiCreateOrder, apiCancelOrder } from '../services/api.js'

export const useOrderStore = defineStore('orders', () => {
  const orders = ref([])
  const total = ref(0)

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

  async function pollOrderStatus(orderNo, maxAttempts = 15) {
    for (let i = 0; i < maxAttempts; i++) {
      const data = await apiGetOrderStatus(orderNo)
      if (data.status !== 'processing') return data
      await new Promise(r => setTimeout(r, 2000))
    }
    return { status: 'timeout', message: '订单处理超时' }
  }

  async function cancelOrder(orderNo) {
    return await apiCancelOrder(orderNo)
  }

  return { orders, total, fetchOrders, getOrderDetail, lockSeats, createOrder, pollOrderStatus, cancelOrder }
})
