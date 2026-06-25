<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '../stores/orders.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const showtimeId = Number(route.query.showtimeId)
const seatIds = (route.query.seatIds || '').split(',').filter(Boolean).map(Number)
const lockToken = route.query.lockToken

const payResult = ref(null)
const orderNo = ref('')
const polling = ref(false)

onMounted(async () => {
  if (!showtimeId || !seatIds.length || !lockToken) {
    ElMessage.error('订单参数无效')
    router.replace('/')
    return
  }
})

async function handlePay() {
  polling.value = true
  try {
    const result = await orderStore.createOrder(showtimeId, seatIds, lockToken)
    orderNo.value = result.orderNo

    const status = await orderStore.pollOrderStatus(result.orderNo)
    if (status.status === 'paid') {
      payResult.value = 'success'
    } else if (status.status === 'failed') {
      payResult.value = 'fail'
    } else {
      payResult.value = 'timeout'
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
      <div class="result-icon">✅</div>
      <h2>支付成功</h2>
      <p>请提前15分钟到达影院取票</p>
      <div class="result-info">
        <p>订单号：{{ orderNo }}</p>
      </div>
      <button class="btn-back" @click="goOrders">查看购票记录</button>
    </div>

    <!-- Fail / Timeout -->
    <div v-else-if="payResult === 'fail' || payResult === 'timeout'" class="result-card fail">
      <div class="result-icon">❌</div>
      <h2>{{ payResult === 'timeout' ? '支付超时' : '支付失败' }}</h2>
      <p>{{ payResult === 'timeout' ? '订单处理超时，请稍后查看订单状态' : '支付异常，请返回重试' }}</p>
      <button class="btn-back" @click="router.back()">返回重试</button>
    </div>

    <!-- Payment form -->
    <template v-else>
      <button class="back-btn" @click="router.back()">&larr; 返回修改</button>

      <div class="pay-card">
        <h3>确认支付</h3>
        <p class="pay-desc">座位已锁定，请在15分钟内完成支付</p>
        <p class="pay-seats">已选 {{ seatIds.length }} 个座位</p>
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

.pay-card { background: #fff; border-radius: 14px; padding: 32px; box-shadow: var(--shadow); text-align: center; }
.pay-card h3 { font-size: 20px; margin-bottom: 12px; color: #111; }
.pay-desc { font-size: 14px; color: var(--text-light); margin-bottom: 8px; }
.pay-seats { font-size: 15px; color: #555; margin-bottom: 24px; }
.btn-pay { padding: 14px 60px; background: var(--miku); color: #fff; font-size: 17px; font-weight: 700; border-radius: 12px; transition: all 0.2s; }
.btn-pay:hover:not(:disabled) { background: var(--miku-hover); box-shadow: 0 4px 16px rgba(57,197,187,0.45); }
.btn-pay:disabled { opacity: 0.5; cursor: not-allowed; }

.result-card { max-width: 480px; margin: 60px auto; background: #fff; border-radius: 16px; padding: 40px 32px; text-align: center; box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
.result-icon { font-size: 56px; margin-bottom: 12px; }
.result-card h2 { font-size: 24px; margin-bottom: 8px; }
.result-card > p { color: var(--text-light); margin-bottom: 24px; }
.result-info { background: #f9fafb; border-radius: 8px; padding: 16px 20px; margin-bottom: 24px; }
.result-info p { font-size: 14px; color: #555; }
.btn-back { padding: 11px 40px; background: var(--miku); color: #fff; font-size: 15px; font-weight: 600; border-radius: 8px; transition: all 0.2s; }
.btn-back:hover { background: var(--miku-hover); }
</style>
