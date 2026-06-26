<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { apiGetStatistics } from '../../services/api.js'
import * as echarts from 'echarts'

const stats = ref(null)
const statsLoading = ref(false)
const trendChartRef = ref(null)
const topMoviesChartRef = ref(null)
let trendChart = null
let topMoviesChart = null

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
  initTopMoviesChart()
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

function initTopMoviesChart() {
  const chartDom = topMoviesChartRef.value
  if (!chartDom) return

  if (topMoviesChart) topMoviesChart.dispose()
  topMoviesChart = echarts.init(chartDom)

  if (!stats.value.topMovies || stats.value.topMovies.length === 0) {
    topMoviesChart.setOption({ title: { text: '热门影片', left: 'center', subtext: '暂无数据' } })
    return
  }

  const titles = stats.value.topMovies.map(m => m.title)
  const orderCounts = stats.value.topMovies.map(m => m.orderCount)
  const revenues = stats.value.topMovies.map(m => m.revenue)

  topMoviesChart.setOption({
    title: { text: '热门影片', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '票房'], bottom: 0 },
    xAxis: { type: 'category', data: titles },
    yAxis: [
      { type: 'value', name: '订单数' },
      { type: 'value', name: '票房(元)' }
    ],
    series: [
      { name: '订单数', type: 'bar', data: orderCounts },
      { name: '票房', type: 'bar', yAxisIndex: 1, data: revenues }
    ]
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
        <div class="stat-card"><div class="stat-num">{{ stats.totalUsers }}</div><div class="stat-label">总用户数</div></div>
        <div class="stat-card"><div class="stat-num">{{ stats.totalMovies }}</div><div class="stat-label">总影片数</div></div>
      </div>
      <div class="chart-container" ref="trendChartRef" style="min-height: 400px;"></div>
      <div class="chart-container" ref="topMoviesChartRef" style="min-height: 400px;"></div>
      <div v-if="stats?.topMovies?.length" style="margin-top:20px">
        <h4 style="margin-bottom:12px">热门影片详情</h4>
        <div class="table-wrap"><table>
          <thead><tr><th>影片</th><th>订单数</th><th>票房</th></tr></thead>
          <tbody><tr v-for="m in stats.topMovies" :key="m.movieId"><td>{{ m.title }}</td><td>{{ m.orderCount }}</td><td>&yen;{{ m.revenue?.toFixed(1) }}</td></tr></tbody>
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
  grid-template-columns: repeat(4, 1fr);
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
