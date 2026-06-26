<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '../stores/orders.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const orderStore = useOrderStore()

const ctx = computed(() => orderStore.paymentContext)
const payResult = ref(null)
const orderNo = ref('')
const polling = ref(false)

// 倒计时 15 分钟
const remainSeconds = ref(900)
let timer = null

function startCountdown() {
  timer = setInterval(() => {
    remainSeconds.value--
    if (remainSeconds.value <= 0) {
      clearInterval(timer)
      payResult.value = 'timeout'
    }
  }, 1000)
}

const countdown = computed(() => {
  const m = Math.floor(remainSeconds.value / 60)
  const s = remainSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

onMounted(() => {
  if (!ctx.value?.showtimeId || !ctx.value?.seatIds?.length || !ctx.value?.lockToken) {
    ElMessage.error('订单参数无效，请重新选座')
    router.replace('/')
    return
  }
  startCountdown()
})

onUnmounted(() => { clearInterval(timer) })

async function handlePay() {
  polling.value = true
  try {
    const result = await orderStore.createOrder(ctx.value.showtimeId, ctx.value.seatIds, ctx.value.lockToken)
    orderNo.value = result.orderNo
    clearInterval(timer)

    const status = await orderStore.pollOrderStatus(result.orderNo)
    if (status.status === 'paid') {
      payResult.value = 'success'
      orderStore.clearPaymentContext()
    } else if (status.status === 'cancelled') {
      // 订单失败（余额不足、锁过期等）
      ElMessage.error(status.message || '支付失败')
      payResult.value = 'fail'
    } else if (status.status === 'timeout') {
      payResult.value = 'timeout'
    } else {
      payResult.value = 'fail'
    }
  } catch (e) {
    ElMessage.error(e.message || '支付失败')
    payResult.value = 'fail'
  } finally {
    polling.value = false
  }
}

function goOrders() { router.push('/my') }
</script>

<template>
  <div class="payment-page">
    <!-- Success -->
    <div v-if="payResult === 'success'" class="result-card success">
      <div class="result-icon">&#10004;</div>
      <h2>支付成功</h2>
      <p>请提前15分钟到达影院取票</p>
      <div class="result-info">
        <p>订单号：{{ orderNo }}</p>
      </div>
      <button class="btn-action" @click="goOrders">查看购票记录</button>
    </div>

    <!-- Fail / Timeout -->
    <div v-else-if="payResult === 'fail' || payResult === 'timeout'" class="result-card fail">
      <div class="result-icon">&#10008;</div>
      <h2>{{ payResult === 'timeout' ? '支付超时' : '支付失败' }}</h2>
      <p>{{ payResult === 'timeout' ? '座位已释放，请返回重新选座' : '支付异常，请返回重试' }}</p>
      <button class="btn-action" @click="router.push('/')">返回首页</button>
    </div>

    <!-- Payment form -->
    <template v-else-if="ctx">
      <button class="back-btn" @click="router.back()">&larr; 返回修改</button>

      <div class="pay-card">
        <h3>确认支付</h3>

        <!-- 订单摘要 -->
        <div class="order-summary">
          <div class="summary-row" v-if="ctx.movieTitle"><label>影片</label><span>{{ ctx.movieTitle }}</span></div>
          <div class="summary-row" v-if="ctx.cinemaName"><label>影院</label><span>{{ ctx.cinemaName }}</span></div>
          <div class="summary-row" v-if="ctx.hallName"><label>影厅</label><span>{{ ctx.hallName }}</span></div>
          <div class="summary-row" v-if="ctx.showDate"><label>场次</label><span>{{ ctx.showDate }} {{ ctx.showTime }}</span></div>
          <div class="summary-row"><label>座位</label><span>{{ ctx.seats?.join('、') || ctx.seatIds?.length + ' 个' }}</span></div>
          <div class="summary-divider"></div>
          <div class="summary-row total"><label>合计</label><span class="price">&yen;{{ ctx.totalAmount?.toFixed(2) || '--' }}</span></div>
        </div>

        <!-- 倒计时 -->
        <div class="countdown" :class="{ urgent: remainSeconds < 120 }">
          <span class="cd-label">剩余支付时间</span>
          <span class="cd-time">{{ countdown }}</span>
        </div>

        <button class="btn-pay" :disabled="polling" @click="handlePay">
          {{ polling ? '支付处理中...' : '确认支付' }}
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.payment-page { max-width: 600px; margin: 0 auto; padding: 24px 20px 40px; }
.back-btn { background: none; font-size: 15px; color: var(--primary); margin-bottom: 20px; padding: 6px 0; }
.back-btn:hover { opacity: 0.8; }

.pay-card { background: #fff; border-radius: 14px; padding: 32px; box-shadow: var(--shadow); }
.pay-card h3 { font-size: 20px; margin-bottom: 20px; color: #111; text-align: center; }

.order-summary { background: #f9fafb; border-radius: 10px; padding: 18px 20px; margin-bottom: 20px; }
.summary-row { display: flex; justify-content: space-between; align-items: baseline; padding: 6px 0; font-size: 14px; }
.summary-row label { color: var(--text-light); flex-shrink: 0; min-width: 48px; }
.summary-row span { color: #333; text-align: right; }
.summary-row.total { padding-top: 12px; }
.summary-row.total label { font-weight: 600; color: #333; }
.price { font-size: 22px; font-weight: 700; color: var(--primary); }
.summary-divider { border-top: 1px dashed #e0e0e0; margin: 8px 0; }

.countdown { text-align: center; margin-bottom: 24px; }
.cd-label { font-size: 13px; color: var(--text-light); margin-right: 8px; }
.cd-time { font-size: 28px; font-weight: 700; font-variant-numeric: tabular-nums; color: #333; }
.countdown.urgent .cd-time { color: #e53935; animation: pulse 1s infinite; }
@keyframes pulse { 50% { opacity: 0.5; } }

.btn-pay { display: block; width: 100%; padding: 14px; background: var(--primary); color: #fff; font-size: 17px; font-weight: 700; border-radius: 12px; transition: all 0.2s; }
.btn-pay:hover:not(:disabled) { background: var(--primary-hover); box-shadow: 0 4px 16px rgba(231,76,60,0.35); }
.btn-pay:disabled { opacity: 0.5; cursor: not-allowed; }

.result-card { max-width: 480px; margin: 60px auto; background: #fff; border-radius: 16px; padding: 40px 32px; text-align: center; box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
.result-icon { font-size: 56px; margin-bottom: 12px; }
.success .result-icon { color: #4caf50; }
.fail .result-icon { color: #e53935; }
.result-card h2 { font-size: 24px; margin-bottom: 8px; }
.result-card > p { color: var(--text-light); margin-bottom: 24px; }
.result-info { background: #f9fafb; border-radius: 8px; padding: 16px 20px; margin-bottom: 24px; }
.result-info p { font-size: 14px; color: #555; }
.btn-action { padding: 11px 40px; background: var(--primary); color: #fff; font-size: 15px; font-weight: 600; border-radius: 8px; transition: all 0.2s; }
.btn-action:hover { background: var(--primary-hover); }
</style>
