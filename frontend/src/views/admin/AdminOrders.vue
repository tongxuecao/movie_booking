<script setup>
import { ref, onMounted } from 'vue'
import { apiGetAdminOrders, apiGetAdminOrderDetail } from '../../services/api.js'
import { ElMessage } from 'element-plus'

const adminOrders = ref([])
const ordersLoading = ref(false)
const orderStatusFilter = ref('')
const searchKW = ref('')
const orderPage = ref(1)
const orderTotal = ref(0)
const ORDER_PAGE_SIZE = 10
const detailOrder = ref(null)
const statusLabels = { pending: '待支付', paid: '已支付', cancelled: '已取消', refunded: '已退款' }
const statusClasses = { pending: 'status-pending', paid: 'status-paid', cancelled: 'status-cancelled', refunded: 'status-refunded' }

onMounted(() => loadOrders())

async function loadOrders() {
  ordersLoading.value = true
  try {
    const params = { page: orderPage.value, size: ORDER_PAGE_SIZE }
    if (orderStatusFilter.value) params.status = orderStatusFilter.value
    params.keyword = searchKW.value || undefined
    const data = await apiGetAdminOrders(params)
    adminOrders.value = data.list || []
    orderTotal.value = data.total || 0
  } catch {} finally { ordersLoading.value = false }
}

async function viewDetail(orderNo) {
  try {
    detailOrder.value = await apiGetAdminOrderDetail(orderNo)
  } catch (e) { ElMessage.error(e.message || '获取订单详情失败') }
}

function closeDetail() { detailOrder.value = null }

function fmtTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 16)
}

function fmtAmount(v) {
  return Number(v || 0).toFixed(1)
}
</script>

<template>
  <div class="content-section">
    <div class="section-header">
      <h3>订单管理</h3>
      <div class="toolbar">
        <div class="filter-group">
          <el-select v-model="orderStatusFilter" placeholder="订单状态" clearable @change="orderPage = 1; loadOrders()" style="width:110px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="pending" />
            <el-option label="已支付" value="paid" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="已退款" value="refunded" />
          </el-select>
          <el-input v-model="searchKW" placeholder="搜索用户名/手机号…" clearable style="max-width:260px" @input="orderPage = 1; loadOrders()" />
        </div>
      </div>
    </div>
    <div v-if="ordersLoading" class="empty">加载中...</div>
    <div v-else-if="adminOrders.length" class="table-wrap">
      <table>
        <thead><tr><th>订单号</th><th>用户</th><th>影片</th><th>影院</th><th>日期</th><th>座位数</th><th>金额</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="o in adminOrders" :key="o.orderNo" class="order-row" @click="viewDetail(o.orderNo)">
            <td class="mono">{{ o.orderNo }}</td>
            <td>{{ o.username || '-' }}</td>
            <td>{{ o.movieTitle }}</td>
            <td>{{ o.cinemaName }}</td>
            <td>{{ o.showDate }} {{ o.showTime }}</td>
            <td>{{ o.seatCount }}</td>
            <td>&yen;{{ o.totalAmount?.toFixed(1) }}</td>
            <td><span class="status-badge" :class="o.status">{{ o.status }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="empty">暂无订单</div>
    <div class="pagination-wrap" v-if="orderTotal > ORDER_PAGE_SIZE">
      <el-pagination v-model:current-page="orderPage" :page-size="ORDER_PAGE_SIZE" :total="orderTotal" layout="prev,pager,next,total" background @current-change="loadOrders" />
    </div>

    <!-- 订单详情弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="detailOrder" class="detail-overlay" @click.self="closeDetail">
          <div class="detail-modal">
            <button class="detail-close" @click="closeDetail">&times;</button>
            <h3>订单详情</h3>
            <div class="detail-body">
              <div class="detail-row"><label>订单号</label><span class="mono">{{ detailOrder.orderNo }}</span></div>
              <div class="detail-row"><label>状态</label><span :class="statusClasses[detailOrder.status]">{{ statusLabels[detailOrder.status] || detailOrder.status }}</span></div>
              <div class="detail-row"><label>用户</label><span>{{ detailOrder.username || '-' }}</span></div>
              <div class="detail-row"><label>手机号</label><span>{{ detailOrder.phone || '-' }}</span></div>
              <div class="detail-row"><label>电影</label><span>{{ detailOrder.movieTitle }}</span></div>
              <div class="detail-row"><label>影院</label><span>{{ detailOrder.cinemaName }}</span></div>
              <div class="detail-row"><label>影厅</label><span>{{ detailOrder.hallName }}</span></div>
              <div class="detail-row"><label>场次</label><span>{{ detailOrder.showDate }} {{ detailOrder.showTime }}</span></div>
              <div class="detail-row"><label>座位</label><span>{{ detailOrder.seats?.map(s => s.row + '排' + s.col + '座').join('、') || '-' }}</span></div>
              <div class="detail-row"><label>金额</label><span class="detail-price">&yen;{{ fmtAmount(detailOrder.totalAmount) }}</span></div>
              <div class="detail-row"><label>下单时间</label><span>{{ fmtTime(detailOrder.createdAt) }}</span></div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.content-section {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 1px 8px rgba(0,0,0,0.06);
}

.section-header {
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 16px 0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.table-wrap {
  background: #fff;
  border-radius: 10px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 600px;
}

th {
  text-align: left;
  padding: 14px 16px;
  font-size: 13px;
  color: #999;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

td {
  padding: 12px 16px;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.mono {
  font-family: monospace;
  font-size: 12px;
}

.status-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.status-badge.paid {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.pending {
  background: #fff3e0;
  color: #ff9800;
}

.status-badge.cancelled {
  background: #f5f5f5;
  color: #999;
}

.status-badge.refunded {
  background: #e3f2fd;
  color: #2196f3;
}

.status-badge.failed {
  background: #fce4ec;
  color: #e53935;
}

.empty {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.order-row { cursor: pointer; }
.order-row:hover { background: #fafafa; }

/* 详情弹窗 */
.detail-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(2px);
  z-index: 1000;
  display: flex; align-items: flex-start; justify-content: center;
  padding: 40px 20px; overflow-y: auto;
}
.detail-modal {
  background: #fff; border-radius: 14px;
  max-width: 500px; width: 100%;
  padding: 28px; position: relative;
}
.detail-close {
  position: absolute; top: 16px; right: 16px;
  font-size: 24px; background: none; border: none;
  color: #999; cursor: pointer;
}
.detail-close:hover { color: #333; }
.detail-modal h3 { font-size: 18px; margin: 0 0 20px; color: #333; }
.detail-body { display: flex; flex-direction: column; gap: 12px; }
.detail-row { display: flex; align-items: center; }
.detail-row label { width: 72px; font-size: 13px; color: #999; flex-shrink: 0; }
.detail-row span { font-size: 14px; color: #333; }
.detail-price { font-size: 18px; font-weight: 700; color: #d32f2f; }

.status-pending { color: #ff9800; font-weight: 500; }
.status-paid { color: #4caf50; font-weight: 500; }
.status-cancelled { color: #999; }
.status-refunded { color: #2196f3; font-weight: 500; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
