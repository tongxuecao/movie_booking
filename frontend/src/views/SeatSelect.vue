<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '../stores/orders.js'
import { apiGetSeats, apiGetShowtime } from '../services/api.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const showtimeId = Number(route.params.showtimeId)
const seatData = ref(null)
const basePrice = ref(0)
const loading = ref(true)

onMounted(async () => {
  try {
    const [seats, showtime] = await Promise.all([
      apiGetSeats(showtimeId),
      apiGetShowtime(showtimeId).catch(() => null),
    ])
    seatData.value = seats
    basePrice.value = showtime?.price || 0
  } catch {
    ElMessage.error('获取座位信息失败')
    router.replace('/')
  } finally {
    loading.value = false
  }
})

const selected = ref({})
const maxSelect = 6

function toggleSeat(seat) {
  if (seat.status !== 'available') return
  const key = seat.id
  if (selected.value[key]) {
    const next = { ...selected.value }; delete next[key]; selected.value = next
  } else {
    if (Object.keys(selected.value).length >= maxSelect) {
      ElMessage.warning(`最多选择${maxSelect}个座位`)
      return
    }
    selected.value = { ...selected.value, [key]: seat }
  }
}

const selectedList = computed(() => Object.values(selected.value))

function getSeatPrice(seat) {
  const base = basePrice.value || 0
  if (seat.type === 'vip') return base * 1.5
  if (seat.type === 'couple') return base * 2
  return base
}

const totalAmount = computed(() => selectedList.value.reduce((sum, s) => sum + getSeatPrice(s), 0))

const locking = ref(false)
async function handleConfirm() {
  if (!selectedList.value.length) { ElMessage.warning('请至少选择一个座位'); return }
  locking.value = true
  try {
    const seatIds = selectedList.value.map(s => s.id)
    const lockData = await orderStore.lockSeats(showtimeId, seatIds)
    const lockSeats = lockData.seats || []
    const lockTotal = lockData.totalAmount || totalAmount.value
    // 锁座成功后立即创建订单
    const orderResult = await orderStore.createOrder(showtimeId, seatIds, lockData.lockToken)
    orderStore.setPaymentContext({
      showtimeId,
      seatIds,
      lockToken: lockData.lockToken,
      orderNo: orderResult.orderNo,
      movieTitle: seatData.value?.movieTitle || '',
      hallName: seatData.value?.hallName || '',
      showDate: seatData.value?.showDate || '',
      showTime: seatData.value?.showTime || '',
      cinemaName: seatData.value?.cinemaName || '',
      seats: lockSeats.length ? lockSeats.map(s => `${s.row}排${s.col}座`) : selectedList.value.map(s => `${s.row}排${s.col}座`),
      totalAmount: Number(lockTotal) || 0,
    })
    router.push('/payment')
  } catch (e) {
    ElMessage.error(e.message || '锁座失败，请重试')
  } finally {
    locking.value = false
  }
}

function getSeatClass(seat) {
  if (seat.status === 'sold') return 'seat-sold'
  if (seat.status === 'locked') return 'seat-sold'
  if (selected.value[seat.id]) return 'seat-selected'
  if (seat.type === 'vip') return 'seat-vip'
  if (seat.type === 'couple') return 'seat-couple'
  return 'seat-normal'
}

// Group seats by row for grid rendering
const rows = computed(() => {
  if (!seatData.value) return []
  const groups = {}
  for (const seat of seatData.value.seats || []) {
    if (!groups[seat.row]) groups[seat.row] = []
    groups[seat.row].push(seat)
  }
  return Object.entries(groups).sort(([a], [b]) => a.localeCompare(b)).map(([row, seats]) => ({ row, seats: seats.sort((a, b) => a.col - b.col) }))
})

const cols = computed(() => seatData.value?.cols || 10)
</script>

<template>
  <div class="seat-page" v-if="!loading && seatData">
    <button class="back-btn" @click="router.back()">&larr; 返回</button>

    <div class="seat-header">
      <h2>{{ seatData.hallName || '选座' }}</h2>
      <p v-if="seatData.hallType">{{ seatData.hallType }}</p>
    </div>

    <div class="screen-area"><div class="screen">银 幕 中 心</div></div>

    <div class="seat-grid-wrap">
      <div class="seat-grid" :style="{ gridTemplateColumns: `32px repeat(${cols}, 34px)` }">
        <template v-for="r in rows" :key="r.row">
          <span class="row-label">{{ r.row }}</span>
          <button
            v-for="seat in r.seats"
            :key="seat.id"
            class="seat-btn"
            :class="getSeatClass(seat)"
            :disabled="seat.status !== 'available'"
            @click="toggleSeat(seat)"
          >{{ seat.col }}</button>
        </template>
      </div>
    </div>

    <div class="seat-legend">
      <span><i class="dot normal"></i>普通座</span>
      <span><i class="dot vip"></i>VIP座</span>
      <span><i class="dot couple"></i>情侣座</span>
      <span><i class="dot sold"></i>已售</span>
      <span><i class="dot selected"></i>已选</span>
    </div>

    <div class="seat-bottom" v-if="selectedList.length">
      <div class="selected-info">
        <span>已选：{{ selectedList.map(s => `${s.row}排${s.col}座`).join('、') }}</span>
        <span class="total-price">&yen;{{ totalAmount.toFixed(2) }}</span>
      </div>
      <button class="btn-confirm" :disabled="locking" @click="handleConfirm">
        {{ locking ? '锁座中...' : '确认选座' }}
      </button>
    </div>
    <div class="seat-bottom-empty" v-else>
      <p>请点击上方座位图选择座位（最多{{ maxSelect }}个）</p>
    </div>
  </div>
  <div v-else-if="loading" class="loading">加载中...</div>
</template>

<style scoped>
.seat-page { max-width: 1100px; margin: 0 auto; padding: 20px 20px 40px; }
.loading { text-align: center; padding: 80px 0; color: var(--text-light); }
.back-btn { background: none; font-size: 15px; color: var(--primary); margin-bottom: 20px; padding: 6px 0; }
.back-btn:hover { opacity: 0.8; }
.seat-header { margin-bottom: 24px; }
.seat-header h2 { font-size: 20px; margin-bottom: 4px; }
.seat-header p { font-size: 13px; color: var(--text-light); }
.screen-area { display: flex; justify-content: center; margin-bottom: 32px; }
.screen { background: linear-gradient(to bottom, #e0e0e0, #f5f5f5); border: 2px solid #ccc; border-radius: 0 0 50% 50%; width: 60%; text-align: center; padding: 12px 0 8px; font-size: 15px; color: #999; letter-spacing: 6px; font-weight: 600; }
.seat-grid-wrap { overflow-x: auto; padding-bottom: 8px; }
.seat-grid { display: grid; gap: 4px; justify-content: center; min-width: fit-content; }
.row-label { display: flex; align-items: center; justify-content: center; font-size: 11px; color: var(--text-light); font-weight: 600; }
.seat-btn { width: 30px; height: 26px; border-radius: 3px 3px 7px 7px; border: 1px solid #ddd; background: #fff; font-size: 9px; color: #bbb; cursor: pointer; transition: all 0.15s; display: flex; align-items: center; justify-content: center; }
.seat-btn:hover:not(.seat-sold) { border-color: var(--accent); color: var(--accent); }
.seat-normal { border-color: #c8e6c9; background: #e8f5e9; color: #66bb6a; }
.seat-vip { border-color: #ffe0b2; background: #fff3e0; color: #ff9800; }
.seat-couple { border-color: #f8bbd0; background: #fce4ec; color: #ec407a; }
.seat-sold { border-color: #ddd; background: #f5f5f5; color: #ccc; cursor: not-allowed; }
.seat-selected { border-color: var(--accent); background: var(--accent); color: #fff; }
.seat-legend { display: flex; gap: 20px; justify-content: center; margin-top: 24px; flex-wrap: wrap; font-size: 12px; color: var(--text-light); }
.seat-legend span { display: flex; align-items: center; gap: 4px; }
.dot { width: 14px; height: 10px; border-radius: 2px 2px 4px 4px; display: inline-block; border: 1px solid #ddd; }
.dot.normal { background: #e8f5e9; border-color: #c8e6c9; }
.dot.vip { background: #fff3e0; border-color: #ffe0b2; }
.dot.couple { background: #fce4ec; border-color: #f8bbd0; }
.dot.sold { background: #f5f5f5; border-color: #ddd; }
.dot.selected { background: var(--accent); border-color: var(--accent); }
.seat-bottom { position: sticky; bottom: 0; background: #fff; border-top: 1px solid #eee; padding: 16px 24px; margin-top: 32px; display: flex; align-items: center; justify-content: space-between; border-radius: 12px; box-shadow: 0 -2px 12px rgba(0,0,0,0.06); }
.selected-info { display: flex; flex-direction: column; gap: 2px; font-size: 14px; }
.total-price { font-size: 22px; font-weight: 700; color: var(--primary); }
.btn-confirm { padding: 12px 40px; background: var(--primary); color: #fff; font-size: 16px; font-weight: 600; border-radius: 24px; transition: all 0.2s; }
.btn-confirm:hover:not(:disabled) { background: var(--primary-hover); box-shadow: 0 4px 16px rgba(231,76,60,0.4); }
.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
.seat-bottom-empty { text-align: center; padding: 20px 0; margin-top: 24px; color: var(--text-light); font-size: 14px; }
</style>
