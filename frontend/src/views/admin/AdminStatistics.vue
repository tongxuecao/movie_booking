<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { apiGetStatistics } from '../../services/api.js'
import * as echarts from 'echarts'

const stats = ref(null)
const statsLoading = ref(false)
const trendChartRef = ref(null)
const revenueChartRef = ref(null)
const wishChartRef = ref(null)
let trendChart = null
let revenueChart = null
let wishChart = null

onMounted(() => loadStats())

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await apiGetStatistics()
    await nextTick()
    setTimeout(() => initCharts(), 200)
  } catch {} finally { statsLoading.value = false }
}

function initCharts() {
  if (!stats.value) return
  initTrendChart()
  initRevenueChart()
  initWishChart()
}

function initTrendChart() {
  const chartDom = trendChartRef.value
  if (!chartDom) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(chartDom)

  if (!stats.value.recent7Days || stats.value.recent7Days.length === 0) {
    trendChart.setOption({ title: { text: '近7天趋势', left: 'center', subtext: '暂无数据' } })
    return
  }

  const dates = stats.value.recent7Days.map(d => d.date)
  const orderCounts = stats.value.recent7Days.map(d => d.orderCount)
  const revenues = stats.value.recent7Days.map(d => d.revenue)

  trendChart.setOption({
    title: { text: '近7天趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '收入'], bottom: 0 },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      { type: 'value', name: '订单数' },
      { type: 'value', name: '收入(元)' }
    ],
    series: [
      { name: '订单数', type: 'line', data: orderCounts, smooth: true },
      { name: '收入', type: 'line', yAxisIndex: 1, data: revenues, smooth: true }
    ]
  })
}

function initRevenueChart() {
  const chartDom = revenueChartRef.value
  if (!chartDom) return
  if (revenueChart) revenueChart.dispose()
  revenueChart = echarts.init(chartDom)

  const data = stats.value.topMoviesByRevenue
  if (!data || data.length === 0) {
    revenueChart.setOption({ title: { text: '电影票房排行', left: 'center', subtext: '暂无数据' } })
    return
  }

  const titles = data.map(m => m.title)
  const revenues = data.map(m => m.revenue)

  revenueChart.setOption({
    title: { text: '电影票房排行 TOP10', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: titles, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '票房(元)' },
    series: [{ name: '票房', type: 'bar', data: revenues, itemStyle: { color: '#e74c3c' } }]
  })
}

function initWishChart() {
  const chartDom = wishChartRef.value
  if (!chartDom) return
  if (wishChart) wishChart.dispose()
  wishChart = echarts.init(chartDom)

  const data = stats.value.topMoviesByWishCount
  if (!data || data.length === 0) {
    wishChart.setOption({ title: { text: '电影想看排行', left: 'center', subtext: '暂无数据' } })
    return
  }

  const titles = data.map(m => m.title)
  const wishCounts = data.map(m => m.wishCount)

  wishChart.setOption({
    title: { text: '电影想看排行 TOP10', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: titles, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '想看数' },
    series: [{ name: '想看数', type: 'bar', data: wishCounts, itemStyle: { color: '#f39c12' } }]
  })
}
</script>

<template>
  <div class="content-section">
    <div class="section-header">
      <h3>数据统计</h3>
    </div>
    <div v-if="statsLoading" class="empty">加载中...</div>
    <div v-else-if="stats">
      <div class="stats-grid">
        <div class="stat-card"><div class="stat-num">{{ stats.todayOrderCount }}</div><div class="stat-label">今日订单</div></div>
        <div class="stat-card"><div class="stat-num">&yen;{{ stats.todayRevenue?.toFixed(1) }}</div><div class="stat-label">今日收入</div></div>
        <div class="stat-card"><div class="stat-num">&yen;{{ stats.totalBoxOffice?.toFixed(1) }}</div><div class="stat-label">总票房</div></div>
        <div class="stat-card"><div class="stat-num">{{ stats.totalUsers }}</div><div class="stat-label">总用户数</div></div>
        <div class="stat-card"><div class="stat-num">{{ stats.totalMovies }}</div><div class="stat-label">总电影数</div></div>
      </div>
      <div class="chart-container" ref="trendChartRef" style="min-height: 400px;"></div>
      <div class="chart-container" ref="revenueChartRef" style="min-height: 400px;"></div>
      <div class="chart-container" ref="wishChartRef" style="min-height: 400px;"></div>
      <!-- 票房排行表格 -->
      <div v-if="stats?.topMoviesByRevenue?.length" style="margin-top:20px">
        <h4 style="margin-bottom:12px">电影票房排行</h4>
        <div class="table-wrap"><table>
          <thead><tr><th>排名</th><th>电影名</th><th>订单数</th><th>票房</th></tr></thead>
          <tbody><tr v-for="(m, i) in stats.topMoviesByRevenue" :key="m.movieId"><td>{{ i + 1 }}</td><td>{{ m.title }}</td><td>{{ m.orderCount }}</td><td>&yen;{{ m.revenue?.toFixed(1) }}</td></tr></tbody>
        </table></div>
      </div>
      <!-- 想看排行表格 -->
      <div v-if="stats?.topMoviesByWishCount?.length" style="margin-top:20px">
        <h4 style="margin-bottom:12px">电影想看排行</h4>
        <div class="table-wrap"><table>
          <thead><tr><th>排名</th><th>电影名</th><th>想看数</th></tr></thead>
          <tbody><tr v-for="(m, i) in stats.topMoviesByWishCount" :key="m.movieId"><td>{{ i + 1 }}</td><td>{{ m.title }}</td><td>{{ m.wishCount }}</td></tr></tbody>
        </table></div>
      </div>
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 1px 8px rgba(0,0,0,0.06);
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #1976d2;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.chart-container {
  width: 100%;
  height: 400px;
  margin-top: 20px;
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 8px rgba(0,0,0,0.06);
}

.table-wrap {
  background: #fff;
  border-radius: 10px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
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

.empty {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
