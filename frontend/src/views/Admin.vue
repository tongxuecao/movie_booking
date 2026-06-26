<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useMovieStore } from '../stores/movies.js'
import { apiCreateMovie, apiUpdateMovie, apiDeleteMovie, apiCreateShowtime, apiGetAdminShowtimes, apiDeleteShowtime, apiGetAdminCinemas, apiGetCinemaHalls, apiCreateCinema, apiCreateHall, apiGetAdminOrders, apiGetStatistics, apiUploadImage, getToken } from '../services/api.js'
import { API_BASE, resolveImageUrl } from '../config.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const router = useRouter()
const auth = useAuthStore()
const movieStore = useMovieStore()

if (!auth.isAdmin) { router.replace('/login?redirect=/admin') }

const activeMenu = ref('movies')

// ==================== MOVIES ====================
const mSearchKW = ref('')
const mPage = ref(1)
const PAGE_SIZE = 10

onMounted(() => movieStore.fetchMovies({ size: 100 }))

const filteredMovies = computed(() => {
  if (!mSearchKW.value) return movieStore.movies
  const kw = mSearchKW.value.toLowerCase()
  return movieStore.movies.filter(m => m.title?.toLowerCase().includes(kw) || m.director?.toLowerCase().includes(kw))
})
const mTotalPages = computed(() => Math.max(1, Math.ceil(filteredMovies.value.length / PAGE_SIZE)))
const pagedMovies = computed(() => { const s = (mPage.value - 1) * PAGE_SIZE; return filteredMovies.value.slice(s, s + PAGE_SIZE) })

const showMovieForm = ref(false)
const editingMovieId = ref(null)
const movieForm = reactive({ title: '', poster: '', genre: '', duration: 120, rating: null, director: '', actors: '', description: '', releaseDate: '', status: 'showing' })

function openMovieAdd() {
  editingMovieId.value = null
  Object.assign(movieForm, { title: '', poster: '', genre: '', duration: 120, rating: null, director: '', actors: '', description: '', releaseDate: '', status: 'showing' })
  showMovieForm.value = true
}
function openMovieEdit(m) {
  editingMovieId.value = m.id
  Object.assign(movieForm, { title: m.title, poster: m.poster, genre: m.genre, duration: m.duration, rating: m.rating, director: m.director, actors: m.actors, description: m.description, releaseDate: m.releaseDate, status: m.status })
  showMovieForm.value = true
}
function closeMovieForm() { showMovieForm.value = false }

async function handleMovieSave() {
  if (!movieForm.title.trim()) { ElMessage.warning('请输入电影名称'); return }
  const data = { ...movieForm, title: movieForm.title.trim(), duration: Number(movieForm.duration) || 120, rating: movieForm.rating ? Number(movieForm.rating) : null }
  try {
    if (editingMovieId.value) {
      await apiUpdateMovie(editingMovieId.value, data)
      ElMessage.success('修改成功')
    } else {
      await apiCreateMovie(data)
      ElMessage.success('添加成功')
    }
    await movieStore.fetchMovies({ size: 100 })
    closeMovieForm()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleMovieDelete(m) {
  try {
    await ElMessageBox.confirm(`确定要删除《${m.title}》吗？`, '确认删除', { type: 'warning' })
    await apiDeleteMovie(m.id)
    ElMessage.success('删除成功')
    await movieStore.fetchMovies({ size: 100 })
  } catch {}
}

const uploadHeaders = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

function handleUploadSuccess(res) {
  if (res.code === 200) {
    movieForm.poster = resolveImageUrl(res.data?.url || '')
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function beforePosterUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!isLt5M) { ElMessage.error('图片大小不能超过5MB'); return false }
  return true
}

// ==================== SHOWTIME ====================
const showStForm = ref(false)
const stForm = reactive({ movieId: '', hallId: '', showDate: '', showTime: '14:30', price: 49.9 })
const adminShowtimes = ref([])
const showtimesLoading = ref(false)
const stMovieFilter = ref('')
const stDateFilter = ref('')
const stPage = ref(1)
const stTotal = ref(0)
const ST_PAGE_SIZE = 10

async function loadShowtimes() {
  showtimesLoading.value = true
  try {
    const params = { page: stPage.value, size: ST_PAGE_SIZE }
    if (stMovieFilter.value) params.movieId = stMovieFilter.value
    if (stDateFilter.value) params.date = stDateFilter.value
    const data = await apiGetAdminShowtimes(params)
    adminShowtimes.value = data.list || []
    stTotal.value = data.total || 0
  } catch {} finally { showtimesLoading.value = false }
}

function openStAdd() {
  Object.assign(stForm, { movieId: movieStore.movies[0]?.id || '', hallId: '', showDate: '', showTime: '14:30', price: 49.9 })
  showStForm.value = true
}
async function handleStSave() {
  if (!stForm.movieId || !stForm.hallId || !stForm.showDate || !stForm.showTime) { ElMessage.warning('请填写完整信息'); return }
  try {
    await apiCreateShowtime({ movieId: Number(stForm.movieId), hallId: Number(stForm.hallId), showDate: stForm.showDate, showTime: stForm.showTime, price: Number(stForm.price) })
    ElMessage.success('排片成功')
    showStForm.value = false
    loadShowtimes()
  } catch (e) { ElMessage.error(e.message || '排片失败') }
}

async function handleShowtimeDelete(st) {
  try {
    await ElMessageBox.confirm(`确定要删除该排片吗？`, '确认删除', { type: 'warning' })
    await apiDeleteShowtime(st.id)
    ElMessage.success('删除成功')
    loadShowtimes()
  } catch {}
}

// ==================== CINEMAS ====================
const adminCinemas = ref([])
const cinemasLoading = ref(false)
const cinemaPage = ref(1)
const cinemaTotal = ref(0)
const CINEMA_PAGE_SIZE = 10
const showCinemaForm = ref(false)
const cinemaForm = reactive({ name: '', address: '', phone: '', businessHours: '' })
const showHallForm = ref(false)
const selectedCinemaId = ref(null)
const hallForm = reactive({ cinemaId: '', name: '', seatRows: 10, seatCols: 10, hallType: 'normal' })
const cinemaHalls = ref([])
const showHallsDialog = ref(false)
const selectedCinemaName = ref('')

async function loadCinemas() {
  cinemasLoading.value = true
  try {
    const data = await apiGetAdminCinemas({ page: cinemaPage.value, size: CINEMA_PAGE_SIZE })
    adminCinemas.value = data.list || []
    cinemaTotal.value = data.total || 0
  } catch {} finally { cinemasLoading.value = false }
}

function openCinemaAdd() {
  Object.assign(cinemaForm, { name: '', address: '', phone: '', businessHours: '' })
  showCinemaForm.value = true
}

async function handleCinemaSave() {
  if (!cinemaForm.name.trim()) { ElMessage.warning('请输入影院名称'); return }
  try {
    await apiCreateCinema(cinemaForm)
    ElMessage.success('添加成功')
    showCinemaForm.value = false
    loadCinemas()
  } catch (e) { ElMessage.error(e.message || '添加失败') }
}

async function openHalls(cinema) {
  selectedCinemaId.value = cinema.id
  selectedCinemaName.value = cinema.name
  try {
    const data = await apiGetCinemaHalls(cinema.id)
    cinemaHalls.value = data || []
    showHallsDialog.value = true
  } catch (e) { ElMessage.error('获取影厅列表失败') }
}

function openHallAdd() {
  Object.assign(hallForm, { cinemaId: selectedCinemaId.value, name: '', seatRows: 10, seatCols: 10, hallType: 'normal' })
  showHallForm.value = true
}

async function handleHallSave() {
  if (!hallForm.name.trim()) { ElMessage.warning('请输入影厅名称'); return }
  try {
    await apiCreateHall(hallForm)
    ElMessage.success('添加成功')
    showHallForm.value = false
    const data = await apiGetCinemaHalls(selectedCinemaId.value)
    cinemaHalls.value = data || []
  } catch (e) { ElMessage.error(e.message || '添加失败') }
}

// ==================== STATISTICS ====================
const stats = ref(null)
const statsLoading = ref(false)
const trendChartRef = ref(null)
const topMoviesChartRef = ref(null)
let trendChart = null
let topMoviesChart = null

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await apiGetStatistics()
    console.log('Stats loaded:', stats.value)
    // 使用nextTick和setTimeout确保DOM完全渲染
    await nextTick()
    setTimeout(() => {
      initCharts()
    }, 200)
  } catch (e) {
    console.error('Error loading stats:', e)
  } finally { statsLoading.value = false }
}

function initCharts() {
  if (!stats.value) {
    console.log('stats is null')
    return
  }
  console.log('Initializing charts with data:', stats.value)
  console.log('recent7Days:', stats.value.recent7Days)
  console.log('topMovies:', stats.value.topMovies)
  initTrendChart()
  initTopMoviesChart()
}

function initTrendChart() {
  const chartDom = trendChartRef.value
  console.log('trendChartRef:', chartDom)
  if (!chartDom) {
    console.log('trendChartRef is null')
    return
  }

  // 销毁已有实例
  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(chartDom)

  // 检查数据是否存在
  if (!stats.value.recent7Days || stats.value.recent7Days.length === 0) {
    console.log('recent7Days is empty or null')
    trendChart.setOption({
      title: { text: '近7天趋势', left: 'center', subtext: '暂无数据' }
    })
    return
  }

  const dates = stats.value.recent7Days.map(d => d.date)
  const orderCounts = stats.value.recent7Days.map(d => d.orderCount)
  const revenues = stats.value.recent7Days.map(d => d.revenue)
  console.log('Trend chart data:', { dates, orderCounts, revenues })

  const option = {
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
  }

  trendChart.setOption(option)
}

function initTopMoviesChart() {
  const chartDom = topMoviesChartRef.value
  console.log('topMoviesChartRef:', chartDom)
  if (!chartDom) {
    console.log('topMoviesChartRef is null')
    return
  }

  // 销毁已有实例
  if (topMoviesChart) {
    topMoviesChart.dispose()
  }

  topMoviesChart = echarts.init(chartDom)

  // 检查数据是否存在
  if (!stats.value.topMovies || stats.value.topMovies.length === 0) {
    console.log('topMovies is empty or null')
    topMoviesChart.setOption({
      title: { text: '热门影片', left: 'center', subtext: '暂无数据' }
    })
    return
  }

  const titles = stats.value.topMovies.map(m => m.title)
  const orderCounts = stats.value.topMovies.map(m => m.orderCount)
  const revenues = stats.value.topMovies.map(m => m.revenue)
  console.log('Top movies chart data:', { titles, orderCounts, revenues })

  const option = {
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
  }

  topMoviesChart.setOption(option)
}

// ==================== ORDERS ====================
const adminOrders = ref([])
const ordersLoading = ref(false)
const orderStatusFilter = ref('')
const orderPage = ref(1)
const orderTotal = ref(0)
const ORDER_PAGE_SIZE = 10

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

// Load data when switching menu
function switchMenu(menu) {
  activeMenu.value = menu
  if (menu === 'statistics') {
    if (!stats.value) {
      loadStats()
    } else {
      // 数据已存在，重新初始化图表
      setTimeout(() => {
        initCharts()
      }, 100)
    }
  }
  if (menu === 'orders') loadOrders()
  if (menu === 'showtimes') loadShowtimes()
  if (menu === 'cinemas') loadCinemas()
}
</script>

<template>
  <div class="admin-layout" v-if="auth.isAdmin">
    <!-- 左侧菜单 -->
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <div style="margin-bottom: 20px;">
        <h1>后台管理</h1>
        </div>
        <div class="admin-info">
          <div class="admin-avatar">
            <img v-if="auth.avatar" :src="auth.avatar" alt="" />
            <span v-else>{{ auth.currentUsername?.charAt(0)?.toUpperCase() || 'A' }}</span>
          </div>
          <div class="admin-name">{{ auth.currentUsername || '管理员' }}</div>
        </div>
      </div>
      <nav class="sidebar-menu">
        <div
          class="menu-item"
          :class="{ active: activeMenu === 'movies' }"
          @click="switchMenu('movies')"
        >
          <span class="menu-icon">🎬</span>
          <span class="menu-text">电影管理</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: activeMenu === 'showtimes' }"
          @click="switchMenu('showtimes')"
        >
          <span class="menu-icon">📅</span>
          <span class="menu-text">排片管理</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: activeMenu === 'orders' }"
          @click="switchMenu('orders')"
        >
          <span class="menu-icon">📋</span>
          <span class="menu-text">订单管理</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: activeMenu === 'statistics' }"
          @click="switchMenu('statistics')"
        >
          <span class="menu-icon">📊</span>
          <span class="menu-text">数据统计</span>
        </div>
        <div
          class="menu-item"
          :class="{ active: activeMenu === 'cinemas' }"
          @click="switchMenu('cinemas')"
        >
          <span class="menu-icon">🏢</span>
          <span class="menu-text">影院管理</span>
        </div>
      </nav>
      <div class="sidebar-footer">
        <button class="btn-logout" @click="auth.logout(); router.push('/login')">退出登录</button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="admin-content">
      <!-- MOVIES -->
      <div v-if="activeMenu === 'movies'" class="content-section">
        <div class="section-header">
          <h3>电影管理</h3>
          <div class="toolbar">
            <el-input v-model="mSearchKW" placeholder="搜索电影…" clearable style="max-width:300px" @input="mPage = 1" />
            <button class="btn-add" @click="openMovieAdd">+ 添加电影</button>
          </div>
        </div>
        <div class="table-wrap" v-if="pagedMovies.length">
          <table>
            <thead><tr><th>海报</th><th>片名</th><th>导演</th><th>类型</th><th>评分</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="m in pagedMovies" :key="m.id">
                <td><img v-if="m.poster" :src="m.poster" class="thumb" /><span v-else class="no-thumb">无</span></td>
                <td class="title-cell">{{ m.title }}</td>
                <td>{{ m.director }}</td>
                <td>{{ m.genre }}</td>
                <td>{{ m.rating ?? '-' }}</td>
                <td><span class="status-badge" :class="m.status">{{ m.status === 'showing' ? '热映' : m.status === 'upcoming' ? '即将' : m.status }}</span></td>
                <td><button class="btn-edit" @click="openMovieEdit(m)">编辑</button><button class="btn-del" @click="handleMovieDelete(m)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty">暂无影片数据</div>
        <div class="pagination-wrap" v-if="mTotalPages > 1">
          <el-pagination v-model:current-page="mPage" :page-size="PAGE_SIZE" :total="filteredMovies.length" layout="prev,pager,next,total" background />
        </div>
      </div>

      <!-- SHOWTIMES -->
      <div v-if="activeMenu === 'showtimes'" class="content-section">
        <div class="section-header">
          <h3>排片管理</h3>
          <div class="toolbar">
            <div class="filter-group">
              <el-select v-model="stMovieFilter" placeholder="选择电影" clearable @change="stPage = 1; loadShowtimes()">
                <el-option label="全部电影" value="" />
                <el-option v-for="m in movieStore.movies" :key="m.id" :label="m.title" :value="m.id" />
              </el-select>
              <el-date-picker v-model="stDateFilter" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" clearable @change="stPage = 1; loadShowtimes()" />
            </div>
            <button class="btn-add" @click="openStAdd">+ 添加排片</button>
          </div>
        </div>
        <div v-if="showtimesLoading" class="empty">加载中...</div>
        <div v-else-if="adminShowtimes.length" class="table-wrap">
          <table>
            <thead><tr><th>电影</th><th>影院</th><th>影厅</th><th>日期</th><th>时间</th><th>票价</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="st in adminShowtimes" :key="st.id">
                <td>{{ st.movieTitle }}</td>
                <td>{{ st.cinemaName }}</td>
                <td>{{ st.hallName }}</td>
                <td>{{ st.showDate }}</td>
                <td>{{ st.showTime }}</td>
                <td>&yen;{{ st.price?.toFixed(1) }}</td>
                <td><button class="btn-del" @click="handleShowtimeDelete(st)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty">暂无排片数据</div>
        <div class="pagination-wrap" v-if="stTotal > ST_PAGE_SIZE">
          <el-pagination v-model:current-page="stPage" :page-size="ST_PAGE_SIZE" :total="stTotal" layout="prev,pager,next,total" background @current-change="loadShowtimes" />
        </div>
      </div>

      <!-- ORDERS -->
      <div v-if="activeMenu === 'orders'" class="content-section">
        <div class="section-header">
          <h3>订单管理</h3>
          <div class="toolbar">
            <el-select v-model="orderStatusFilter" placeholder="订单状态" clearable @change="orderPage = 1; loadOrders()">
              <el-option label="全部" value="" />
              <el-option label="待支付" value="processing" />
              <el-option label="已支付" value="paid" />
              <el-option label="已取消" value="cancelled" />
              <el-option label="已完成" value="completed" />
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

      <!-- STATISTICS -->
      <div v-if="activeMenu === 'statistics'" class="content-section">
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
          <!-- 近7天趋势折线图 -->
          <div class="chart-container" ref="trendChartRef" style="min-height: 400px;"></div>
          <!-- 热门影片柱状图 -->
          <div class="chart-container" ref="topMoviesChartRef" style="min-height: 400px;"></div>
          <!-- 热门影片表格 -->
          <div v-if="stats?.topMovies?.length" style="margin-top:20px">
            <h4 style="margin-bottom:12px">热门影片详情</h4>
            <div class="table-wrap"><table>
              <thead><tr><th>影片</th><th>订单数</th><th>票房</th></tr></thead>
              <tbody><tr v-for="m in stats.topMovies" :key="m.movieId"><td>{{ m.title }}</td><td>{{ m.orderCount }}</td><td>&yen;{{ m.revenue?.toFixed(1) }}</td></tr></tbody>
            </table></div>
          </div>
        </div>
      </div>

      <!-- CINEMAS -->
      <div v-if="activeMenu === 'cinemas'" class="content-section">
        <div class="section-header">
          <h3>影院管理</h3>
          <div class="toolbar">
            <button class="btn-add" @click="openCinemaAdd">+ 添加影院</button>
          </div>
        </div>
        <div v-if="cinemasLoading" class="empty">加载中...</div>
        <div v-else-if="adminCinemas.length" class="table-wrap">
          <table>
            <thead><tr><th>影院名称</th><th>地址</th><th>电话</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="c in adminCinemas" :key="c.id">
                <td class="title-cell">{{ c.name }}</td>
                <td>{{ c.address }}</td>
                <td>{{ c.phone }}</td>
                <td><button class="btn-edit" @click="openHalls(c)">查看影厅</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty">暂无影院数据</div>
        <div class="pagination-wrap" v-if="cinemaTotal > CINEMA_PAGE_SIZE">
          <el-pagination v-model:current-page="cinemaPage" :page-size="CINEMA_PAGE_SIZE" :total="cinemaTotal" layout="prev,pager,next,total" background @current-change="loadCinemas" />
        </div>
      </div>
    </main>

    <!-- MOVIE FORM MODAL -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showMovieForm" class="modal-overlay" @click.self="closeMovieForm">
        <div class="modal">
          <div class="modal-head"><h3>{{ editingMovieId ? '编辑电影' : '添加电影' }}</h3><button class="btn-close" @click="closeMovieForm">&times;</button></div>
          <div class="modal-body">
            <div class="form-row"><div class="form-group flex-2"><label>电影名称*</label><input v-model="movieForm.title" /></div><div class="form-group flex-1"><label>状态</label><select v-model="movieForm.status"><option value="showing">热映中</option><option value="upcoming">即将上映</option></select></div></div>
            <div class="form-row"><div class="form-group flex-1"><label>导演</label><input v-model="movieForm.director" /></div><div class="form-group flex-1"><label>类型</label><input v-model="movieForm.genre" placeholder="动作 / 冒险" /></div></div>
            <div class="form-row"><div class="form-group flex-1"><label>片长(分钟)</label><input v-model.number="movieForm.duration" type="number" /></div><div class="form-group flex-1"><label>评分</label><input v-model.number="movieForm.rating" type="number" step="0.1" /></div><div class="form-group flex-1"><label>上映日期</label><input v-model="movieForm.releaseDate" type="date" /></div></div>
            <div class="form-group"><label>主演</label><input v-model="movieForm.actors" placeholder="演员1 / 演员2" /></div>
            <div class="form-group"><label>剧情简介</label><textarea v-model="movieForm.description" rows="3" /></div>
            <div class="form-group">
              <label>海报</label>
              <div class="poster-row">
                <input v-model="movieForm.poster" placeholder="或直接输入图片URL" />
              </div>
              <div class="poster-upload-area">
                <el-upload
                  :action="API_BASE + '/upload/image'"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :before-upload="beforePosterUpload"
                  :on-success="handleUploadSuccess"
                  accept="image/*"
                >
                  <div class="upload-trigger" v-if="!movieForm.poster">
                    <span class="upload-icon">+</span>
                    <span class="upload-text">上传海报</span>
                  </div>
                  <img v-else :src="movieForm.poster" class="poster-preview" />
                </el-upload>
              </div>
            </div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="closeMovieForm">取消</button><button class="btn-save" @click="handleMovieSave">{{ editingMovieId ? '保存' : '添加' }}</button></div>
        </div>
      </div>
    </Transition></Teleport>

    <!-- SHOWTIME FORM MODAL -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showStForm" class="modal-overlay" @click.self="showStForm = false">
        <div class="modal">
          <div class="modal-head"><h3>添加排片</h3><button class="btn-close" @click="showStForm = false">&times;</button></div>
          <div class="modal-body">
            <div class="form-row">
              <div class="form-group flex-1"><label>电影ID</label><input v-model.number="stForm.movieId" type="number" /></div>
              <div class="form-group flex-1"><label>影厅ID</label><input v-model.number="stForm.hallId" type="number" /></div>
            </div>
            <div class="form-row">
              <div class="form-group flex-1"><label>日期</label><input v-model="stForm.showDate" type="date" /></div>
              <div class="form-group flex-1"><label>时间</label><input v-model="stForm.showTime" type="time" /></div>
              <div class="form-group flex-1"><label>票价(¥)</label><input v-model.number="stForm.price" type="number" step="0.1" /></div>
            </div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showStForm = false">取消</button><button class="btn-save" @click="handleStSave">添加</button></div>
        </div>
      </div>
    </Transition></Teleport>

    <!-- CINEMA FORM MODAL -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showCinemaForm" class="modal-overlay" @click.self="showCinemaForm = false">
        <div class="modal">
          <div class="modal-head"><h3>添加影院</h3><button class="btn-close" @click="showCinemaForm = false">&times;</button></div>
          <div class="modal-body">
            <div class="form-group"><label>影院名称*</label><input v-model="cinemaForm.name" /></div>
            <div class="form-group"><label>地址</label><input v-model="cinemaForm.address" /></div>
            <div class="form-row">
              <div class="form-group flex-1"><label>电话</label><input v-model="cinemaForm.phone" /></div>
              <div class="form-group flex-1"><label>营业时间</label><input v-model="cinemaForm.businessHours" placeholder="09:00-22:00" /></div>
            </div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showCinemaForm = false">取消</button><button class="btn-save" @click="handleCinemaSave">添加</button></div>
        </div>
      </div>
    </Transition></Teleport>

    <!-- HALLS DIALOG -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showHallsDialog" class="modal-overlay" @click.self="showHallsDialog = false">
        <div class="modal modal-lg">
          <div class="modal-head"><h3>{{ selectedCinemaName }} - 影厅列表</h3><button class="btn-close" @click="showHallsDialog = false">&times;</button></div>
          <div class="modal-body">
            <div class="toolbar" style="margin-bottom: 16px;">
              <button class="btn-add" @click="openHallAdd">+ 添加影厅</button>
            </div>
            <div class="table-wrap" v-if="cinemaHalls.length">
              <table>
                <thead><tr><th>影厅名称</th><th>座位数</th><th>类型</th></tr></thead>
                <tbody>
                  <tr v-for="h in cinemaHalls" :key="h.id">
                    <td>{{ h.name }}</td>
                    <td>{{ h.seatCount }}</td>
                    <td>{{ h.hallType }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty">暂无影厅</div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showHallsDialog = false">关闭</button></div>
        </div>
      </div>
    </Transition></Teleport>

    <!-- HALL FORM MODAL -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showHallForm" class="modal-overlay" @click.self="showHallForm = false">
        <div class="modal">
          <div class="modal-head"><h3>添加影厅</h3><button class="btn-close" @click="showHallForm = false">&times;</button></div>
          <div class="modal-body">
            <div class="form-group"><label>影厅名称*</label><input v-model="hallForm.name" /></div>
            <div class="form-row">
              <div class="form-group flex-1"><label>座位行数</label><input v-model.number="hallForm.seatRows" type="number" /></div>
              <div class="form-group flex-1"><label>座位列数</label><input v-model.number="hallForm.seatCols" type="number" /></div>
            </div>
            <div class="form-group"><label>影厅类型</label>
              <select v-model="hallForm.hallType">
                <option value="normal">普通厅</option>
                <option value="imax">IMAX</option>
                <option value="vip">VIP</option>
                <option value="threeD">3D</option>
              </select>
            </div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showHallForm = false">取消</button><button class="btn-save" @click="handleHallSave">添加</button></div>
        </div>
      </div>
    </Transition></Teleport>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

/* 左侧菜单 */
.admin-sidebar {
  width: 220px;
  background: #fff;
  box-shadow: 2px 0 8px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.sidebar-header h2 {
  font-size: 18px;
  color: #333;
  margin: 0 0 16px 0;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #1976d2;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.admin-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-menu {
  flex: 1;
  padding: 12px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}

.menu-item:hover {
  background: #f5f7fa;
  color: #333;
}

.menu-item.active {
  background: #e3f2fd;
  color: #1976d2;
  font-weight: 600;
}

.menu-icon {
  margin-right: 10px;
  font-size: 16px;
}

.menu-text {
  font-size: 14px;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

.btn-logout {
  width: 100%;
  padding: 10px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-logout:hover {
  background: #e0e0e0;
  color: #333;
}

/* 右侧内容区 */
.admin-content {
  flex: 1;
  margin-left: 220px;
  padding: 24px;
}

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

.hint-text {
  font-size: 13px;
  color: #999;
}

.btn-add {
  padding: 8px 20px;
  background: #1976d2;
  color: #fff;
  font-size: 13px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-add:hover {
  background: #1565c0;
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

.thumb {
  width: 48px;
  height: 64px;
  object-fit: cover;
  border-radius: 4px;
}

.no-thumb {
  font-size: 12px;
  color: #ccc;
}

.title-cell {
  font-weight: 600;
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

.status-badge.showing {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.upcoming {
  background: #e3f2fd;
  color: #2196f3;
}

.status-badge.paid {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.processing {
  background: #fff3e0;
  color: #ff9800;
}

.status-badge.cancelled {
  background: #f5f5f5;
  color: #999;
}

.status-badge.failed {
  background: #fce4ec;
  color: #e53935;
}

.btn-edit {
  padding: 4px 14px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  margin-right: 6px;
  background: #e3f2fd;
  color: #1976d2;
  cursor: pointer;
}

.btn-edit:hover {
  background: #bbdefb;
}

.btn-del {
  padding: 4px 14px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  background: #fce4ec;
  color: #e53935;
  cursor: pointer;
}

.btn-del:hover {
  background: #f8bbd0;
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

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 20px;
  overflow-y: auto;
}

.modal {
  background: #fff;
  border-radius: 12px;
  width: 640px;
  max-width: 100%;
  box-shadow: 0 8px 40px rgba(0,0,0,0.2);
}

.modal-lg {
  width: 800px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #eee;
}

.modal-head h3 {
  font-size: 18px;
  margin: 0;
}

.btn-close {
  font-size: 26px;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
}

.btn-close:hover {
  color: #333;
}

.modal-body {
  padding: 24px;
  max-height: 65vh;
  overflow-y: auto;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 5px;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #1976d2;
}

.form-group textarea {
  resize: vertical;
}

.flex-1 {
  flex: 1;
}

.flex-2 {
  flex: 2;
}

.poster-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.poster-row input {
  flex: 1;
}

.poster-upload-area {
  display: inline-block;
}

.upload-trigger {
  width: 120px;
  height: 160px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-trigger:hover {
  border-color: #1976d2;
}

.upload-icon {
  font-size: 32px;
  color: #ccc;
  line-height: 1;
}

.upload-text {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.poster-preview {
  width: 120px;
  height: 160px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #eee;
}

.btn-cancel {
  padding: 9px 22px;
  background: #f0f0f0;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.btn-save {
  padding: 9px 22px;
  background: #1976d2;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-save:hover {
  background: #1565c0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: 60px;
  }

  .menu-text {
    display: none;
  }

  .sidebar-header h2 {
    display: none;
  }

  .admin-content {
    margin-left: 60px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
