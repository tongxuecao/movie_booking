<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useOrderStore } from '../stores/orders.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const orderStore = useOrderStore()

if (!auth.isLoggedIn) { router.replace('/login?redirect=/my') }

const loading = ref(true)

onMounted(async () => {
  try {
    await orderStore.fetchOrders()
  } catch {} finally {
    loading.value = false
  }
})

const statusLabels = {
  paid: '已支付',
  processing: '处理中',
  cancelled: '已取消',
  failed: '支付失败',
  refunded: '已退款',
}

const statusClasses = {
  paid: 'status-paid',
  processing: 'status-processing',
  cancelled: 'status-cancelled',
  failed: 'status-failed',
  refunded: 'status-refunded',
}

function fmtTime(t) {
  if (!t) return ''
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

async function handleCancel(orderNo) {
  try {
    await orderStore.cancelOrder(orderNo)
    ElMessage.success('订单已取消')
    await orderStore.fetchOrders()
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}
</script>

<template>
  <div class="profile-page" v-if="auth.isLoggedIn">
    <div class="profile-header">
      <div class="profile-avatar">{{ auth.currentUsername[0]?.toUpperCase() }}</div>
      <div class="profile-summary">
        <h2>{{ auth.currentUsername }}</h2>
        <p class="profile-role">{{ auth.isAdmin ? '管理员' : '普通用户' }}</p>
      </div>
    </div>

    <h3 class="section-title">购票记录</h3>

    <div v-if="!loading && orderStore.orders.length" class="order-list">
      <div v-for="o in orderStore.orders" :key="o.orderNo" class="order-card">
        <div class="order-left">
          <div class="order-movie">{{ o.movieTitle }}</div>
          <div class="order-meta">{{ o.showDate }} {{ o.showTime }}</div>
          <div class="order-meta">{{ o.cinemaName }} · {{ o.hallName }}</div>
          <div class="order-meta">座位：{{ o.seatCount }} 个</div>
        </div>
        <div class="order-right">
          <div class="order-price">&yen;{{ o.totalAmount?.toFixed(1) }}</div>
          <div class="order-status" :class="statusClasses[o.status]">{{ statusLabels[o.status] || o.status }}</div>
          <div class="order-time">{{ fmtTime(o.createdAt) }}</div>
          <button v-if="o.status === 'paid'" class="btn-cancel" @click="handleCancel(o.orderNo)">取消订单</button>
        </div>
      </div>
    </div>
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-icon">🎫</div>
      <p>暂无购票记录</p>
      <router-link to="/" class="btn-go">去逛逛</router-link>
    </div>
    <div v-else class="loading">加载中...</div>
  </div>
</template>

<style scoped>
.profile-page { max-width: 860px; margin: 0 auto; padding: 24px 20px 40px; }
.loading { text-align: center; padding: 60px 0; color: var(--text-light); }

.profile-header { display: flex; align-items: center; gap: 20px; background: #fff; border-radius: 14px; padding: 28px 32px; box-shadow: var(--shadow); margin-bottom: 24px; }
.profile-avatar { width: 64px; height: 64px; border-radius: 50%; background: var(--miku); color: #fff; font-size: 28px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.profile-summary h2 { font-size: 22px; margin-bottom: 2px; color: #111; }
.profile-role { font-size: 13px; color: var(--text-light); }

.section-title { font-size: 18px; color: #111; margin-bottom: 16px; }

.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card { background: #fff; border-radius: 10px; padding: 18px 22px; box-shadow: 0 1px 8px rgba(0,0,0,0.06); display: flex; justify-content: space-between; align-items: center; transition: box-shadow 0.2s; }
.order-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.order-left { flex: 1; }
.order-movie { font-size: 16px; font-weight: 600; color: #111; margin-bottom: 4px; }
.order-meta { font-size: 12px; color: var(--text-light); margin-bottom: 2px; }
.order-right { text-align: right; flex-shrink: 0; margin-left: 20px; }
.order-price { font-size: 20px; font-weight: 700; color: var(--primary); }
.order-status { font-size: 12px; margin: 2px 0; }
.status-paid { color: #4caf50; }
.status-processing { color: #ff9800; }
.status-cancelled { color: #999; }
.status-failed { color: #e53935; }
.status-refunded { color: #2196f3; }
.order-time { font-size: 11px; color: #ccc; }
.btn-cancel { margin-top: 6px; padding: 4px 12px; background: none; border: 1px solid #e53935; color: #e53935; font-size: 11px; border-radius: 4px; }
.btn-cancel:hover { background: #fce4ec; }

.empty-state { text-align: center; padding: 60px 0; color: var(--text-light); }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-state p { font-size: 14px; margin-bottom: 16px; }
.btn-go { display: inline-block; padding: 8px 28px; background: var(--primary); color: #fff; border-radius: 6px; font-size: 14px; transition: background 0.2s; }
.btn-go:hover { background: var(--primary-hover); }
</style>
