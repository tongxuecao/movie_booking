<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '../stores/orders.js'
import { apiRecharge, apiPayOrder } from '../services/api.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const orderStore = useOrderStore()

const ctx = computed(() => orderStore.paymentContext)
const payResult = ref(null)
const orderNo = ref('')
const paying = ref(false)
const failReason = ref('')
const payPassword = ref('')

// 支付弹窗
const showPayDialog = ref(false)

// 充值相关
const showRecharge = ref(false)
const rechargeAmount = ref('')
const rechargePassword = ref('')
const recharging = ref(false)

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
  if (!ctx.value?.orderNo) {
    ElMessage.error('订单参数无效，请重新选座')
    router.replace('/')
    return
  }
  orderNo.value = ctx.value.orderNo
  startCountdown()
})

onUnmounted(() => { clearInterval(timer) })

function openPayDialog() {
  payPassword.value = ''
  showPayDialog.value = true
}

async function handlePay() {
  if (!payPassword.value) {
    ElMessage.warning('请输入支付密码')
    return
  }
  paying.value = true
  try {
    await apiPayOrder(orderNo.value, payPassword.value)
    clearInterval(timer)
    showPayDialog.value = false
    payResult.value = 'success'
    orderStore.clearPaymentContext()
  } catch (e) {
    const msg = e.message || '支付失败'
    showPayDialog.value = false
    if (msg.includes('余额不足')) {
      failReason.value = msg
      payResult.value = 'pending'
    } else {
      failReason.value = msg
      payResult.value = 'fail'
    }
  } finally {
    paying.value = false
  }
}

async function handleRecharge() {
  if (!rechargeAmount.value || Number(rechargeAmount.value) <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (!rechargePassword.value) {
    ElMessage.warning('请输入密码')
    return
  }

  recharging.value = true
  try {
    await apiRecharge(Number(rechargeAmount.value), rechargePassword.value)
    ElMessage.success('充值成功')
    showRecharge.value = false
    rechargeAmount.value = ''
    rechargePassword.value = ''

    // 刷新用户信息
    await orderStore.refreshProfile()

    // 尝试重新支付
    await handleRetryPay()
  } catch (e) {
    ElMessage.error(e.message || '充值失败')
  } finally {
    recharging.value = false
  }
}

async function handleRetryPay() {
  if (!orderNo.value) return
  payPassword.value = ''
  showPayDialog.value = true
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

    <!-- Pending - 余额不足 -->
    <div v-else-if="payResult === 'pending'" class="result-card pending">
      <div class="result-icon">&#9888;</div>
      <h2>余额不足</h2>
      <p>{{ failReason }}</p>
      <div class="result-info">
        <p>订单号：{{ orderNo }}</p>
        <p>待支付金额：{{ ctx?.totalAmount?.toFixed(2) || '--' }} 元</p>
      </div>
      <div class="action-buttons">
        <button class="btn-recharge" @click="showRecharge = true">立即充值</button>
        <button class="btn-secondary" @click="handleRetryPay">重新支付</button>
        <button class="btn-secondary" @click="router.push('/')">稍后支付</button>
      </div>
    </div>

    <!-- Fail -->
    <div v-else-if="payResult === 'fail'" class="result-card fail">
      <div class="result-icon">&#10008;</div>
      <h2>支付失败</h2>
      <p>{{ failReason }}</p>
      <button class="btn-action" @click="router.push('/')">返回首页</button>
    </div>

    <!-- Timeout -->
    <div v-else-if="payResult === 'timeout'" class="result-card fail">
      <div class="result-icon">&#10008;</div>
      <h2>支付超时</h2>
      <p>座位已释放，请返回重新选座</p>
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
          <div class="summary-row" v-if="orderNo"><label>订单号</label><span>{{ orderNo }}</span></div>
          <div class="summary-divider"></div>
          <div class="summary-row total"><label>合计</label><span class="price">&yen;{{ ctx.totalAmount?.toFixed(2) || '--' }}</span></div>
        </div>

        <!-- 倒计时 -->
        <div class="countdown" :class="{ urgent: remainSeconds < 120 }">
          <span class="cd-label">剩余支付时间</span>
          <span class="cd-time">{{ countdown }}</span>
        </div>

        <button class="btn-pay" :disabled="paying" @click="openPayDialog">
          确认支付
        </button>
      </div>
    </template>

    <!-- 支付弹窗（模拟微信支付） -->
    <el-dialog v-model="showPayDialog" width="380px" :show-close="false" :close-on-click-modal="false" class="pay-dialog">
      <div class="wx-pay">
        <div class="wx-pay-header">
          <button class="wx-close" @click="showPayDialog = false">&times;</button>
          <span class="wx-title">请输入支付密码</span>
        </div>
        <div class="wx-amount">
          <span class="wx-currency">&yen;</span>
          <span class="wx-price">{{ ctx?.totalAmount?.toFixed(2) || '--' }}</span>
        </div>
        <div class="wx-info">
          <div class="wx-info-row"><label>商品</label><span>电影票 - {{ ctx?.movieTitle || '' }}</span></div>
          <div class="wx-info-row"><label>收款方</label><span>电影票预订系统</span></div>
        </div>
        <div class="wx-password">
          <el-input v-model="payPassword" type="password" placeholder="请输入支付密码" show-password @keyup.enter="handlePay" />
        </div>
        <button class="wx-pay-btn" :disabled="paying" @click="handlePay">
          {{ paying ? '处理中...' : '确认支付' }}
        </button>
      </div>
    </el-dialog>

    <!-- 充值弹窗 -->
    <el-dialog v-model="showRecharge" title="钱包充值" width="400px">
      <div class="recharge-form">
        <div class="form-item">
          <label>充值金额（元）</label>
          <el-input v-model="rechargeAmount" type="number" placeholder="请输入充值金额" min="0.01" step="0.01" />
        </div>
        <div class="form-item">
          <label>登录密码</label>
          <el-input v-model="rechargePassword" type="password" placeholder="请输入登录密码" show-password />
        </div>
        <p class="recharge-hint">输入密码确认充值，以账号密码代替支付密码</p>
      </div>
      <template #footer>
        <el-button @click="showRecharge = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="handleRecharge">确认充值</el-button>
      </template>
    </el-dialog>
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
.pending .result-icon { color: #ff9800; }
.result-card h2 { font-size: 24px; margin-bottom: 8px; }
.result-card > p { color: var(--text-light); margin-bottom: 24px; }
.result-info { background: #f9fafb; border-radius: 8px; padding: 16px 20px; margin-bottom: 24px; }
.result-info p { font-size: 14px; color: #555; margin: 4px 0; }
.btn-action { padding: 11px 40px; background: var(--primary); color: #fff; font-size: 15px; font-weight: 600; border-radius: 8px; transition: all 0.2s; }
.btn-action:hover { background: var(--primary-hover); }

.action-buttons { display: flex; gap: 12px; justify-content: center; }
.btn-recharge { padding: 11px 40px; background: #ff9800; color: #fff; font-size: 15px; font-weight: 600; border-radius: 8px; transition: all 0.2s; }
.btn-recharge:hover { background: #f57c00; }
.btn-secondary { padding: 11px 40px; background: #eee; color: #666; font-size: 15px; font-weight: 600; border-radius: 8px; transition: all 0.2s; }
.btn-secondary:hover { background: #ddd; }

.recharge-form { padding: 10px 0; }
.form-item { margin-bottom: 16px; }
.form-item label { display: block; font-size: 14px; color: #333; margin-bottom: 8px; }
.recharge-hint { font-size: 12px; color: var(--text-light); margin-top: 8px; }

.password-field { margin-bottom: 20px; }
.password-field label { display: block; font-size: 14px; color: #333; margin-bottom: 8px; }

/* 微信支付弹窗 */
.wx-pay { padding: 0 4px; }
.wx-pay-header { text-align: center; position: relative; margin-bottom: 20px; }
.wx-close { position: absolute; left: 0; top: -4px; background: none; border: none; font-size: 24px; color: #999; cursor: pointer; line-height: 1; }
.wx-close:hover { color: #333; }
.wx-title { font-size: 17px; font-weight: 600; color: #333; }
.wx-amount { text-align: center; margin-bottom: 24px; }
.wx-currency { font-size: 20px; font-weight: 600; color: #333; vertical-align: top; margin-right: 2px; }
.wx-price { font-size: 40px; font-weight: 700; color: #333; }
.wx-info { background: #f9f9f9; border-radius: 8px; padding: 14px 16px; margin-bottom: 20px; }
.wx-info-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; }
.wx-info-row label { color: #999; }
.wx-info-row span { color: #333; }
.wx-password { margin-bottom: 20px; }
.wx-pay-btn { display: block; width: 100%; padding: 13px; background: #07c160; color: #fff; font-size: 16px; font-weight: 600; border-radius: 8px; border: none; cursor: pointer; transition: all 0.2s; }
.wx-pay-btn:hover:not(:disabled) { background: #06ad56; }
.wx-pay-btn:disabled { opacity: 0.5; cursor: not-allowed; }

:deep(.pay-dialog .el-dialog__header) { display: none; }
:deep(.pay-dialog .el-dialog__body) { padding: 28px 24px 24px; }
</style>
