<script setup>
import { ref, onMounted } from 'vue'
import { apiGetAdminOrders } from '../../services/api.js'

const adminOrders = ref([])
const ordersLoading = ref(false)
const orderStatusFilter = ref('')
const orderPage = ref(1)
const orderTotal = ref(0)
const ORDER_PAGE_SIZE = 10

onMounted(() => loadOrders())

async function loadOrders() {
  ordersLoading.value = true
  try {
    const params = { page: orderPage.value, size: ORDER_PAGE_SIZE }
    if (orderStatusFilter.value) params.status = orderStatusFilter.value
    const data = await apiGetAdminOrders(params)
    adminOrders.value = data.list || []
    orderTotal.value = data.total || 0
  } catch {} finally { ordersLoading.value = false }
}
</script>

<template>
  <div class="content-section">
    <div class="section-header">
      <h3>订单管理</h3>
      <div class="toolbar">
        <el-select v-model="orderStatusFilter" placeholder="订单状态" clearable @change="orderPage = 1; loadOrders()">
          <el-option label="全部" value="" />
          <el-option label="待支付" value="pending" />
          <el-option label="已支付" value="paid" />
          <el-option label="已取消" value="cancelled" />
          <el-option label="已退款" value="refunded" />
        </el-select>
      </div>
    </div>
    <div v-if="ordersLoading" class="empty">加载中...</div>
    <div v-else-if="adminOrders.length" class="table-wrap">
      <table>
        <thead><tr><th>订单号</th><th>影片</th><th>影院</th><th>日期</th><th>座位数</th><th>金额</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="o in adminOrders" :key="o.orderNo">
            <td class="mono">{{ o.orderNo }}</td>
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
</style>
