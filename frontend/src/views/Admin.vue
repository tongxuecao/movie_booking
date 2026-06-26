<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useMovieStore } from '../stores/movies.js'
import { useOrderStore } from '../stores/orders.js'
import { apiCreateMovie, apiUpdateMovie, apiDeleteMovie, apiCreateShowtime, apiGetAdminOrders, apiGetStatistics, apiUploadImage, getToken } from '../services/api.js'
import { API_BASE, resolveImageUrl } from '../config.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const movieStore = useMovieStore()
const orderStore = useOrderStore()

if (!auth.isAdmin) { router.replace('/login?redirect=/admin') }

const activeTab = ref('movies')

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

async function handlePosterUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const data = await apiUploadImage(file)
    movieForm.poster = data.url
    ElMessage.success('上传成功')
  } catch (e) { ElMessage.error(e.message || '上传失败') }
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
  } catch (e) { ElMessage.error(e.message || '排片失败') }
}

// ==================== STATISTICS ====================
const stats = ref(null)
const statsLoading = ref(false)

async function loadStats() {
  statsLoading.value = true
  try { stats.value = await apiGetStatistics() } catch {} finally { statsLoading.value = false }
}

// ==================== ORDERS ====================
const adminOrders = ref([])
const ordersLoading = ref(false)

async function loadOrders() {
  ordersLoading.value = true
  try {
    const data = await apiGetAdminOrders({ size: 50 })
    adminOrders.value = data.list || []
  } catch {} finally { ordersLoading.value = false }
}

// Load data when switching tabs
function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'statistics' && !stats.value) loadStats()
  if (tab === 'orders' && !adminOrders.value.length) loadOrders()
}
</script>

<template>
  <div class="admin" v-if="auth.isAdmin">
    <div class="admin-header"><h2>管理后台</h2></div>
    <div class="admin-tabs">
      <button :class="{ active: activeTab === 'movies' }" @click="switchTab('movies')">电影管理</button>
      <button :class="{ active: activeTab === 'showtimes' }" @click="switchTab('showtimes')">排片管理</button>
      <button :class="{ active: activeTab === 'statistics' }" @click="switchTab('statistics')">数据统计</button>
      <button :class="{ active: activeTab === 'orders' }" @click="switchTab('orders')">订单管理</button>
    </div>

    <!-- MOVIES -->
    <div v-if="activeTab === 'movies'">
      <div class="toolbar">
        <el-input v-model="mSearchKW" placeholder="搜索电影…" clearable style="max-width:300px" @input="mPage = 1" />
        <button class="btn-add" @click="openMovieAdd">+ 添加电影</button>
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
    <div v-if="activeTab === 'showtimes'">
      <div class="toolbar">
        <p class="hint-text">通过填写电影ID、影厅ID和时间来创建排片</p>
        <button class="btn-add" @click="openStAdd">+ 添加排片</button>
      </div>
    </div>

    <!-- STATISTICS -->
    <div v-if="activeTab === 'statistics'">
      <div v-if="statsLoading" class="empty">加载中...</div>
      <div v-else-if="stats" class="stats-grid">
        <div class="stat-card"><div class="stat-num">{{ stats.todayOrderCount }}</div><div class="stat-label">今日订单</div></div>
        <div class="stat-card"><div class="stat-num">&yen;{{ stats.todayRevenue?.toFixed(1) }}</div><div class="stat-label">今日收入</div></div>
        <div class="stat-card"><div class="stat-num">{{ stats.totalUsers }}</div><div class="stat-label">总用户数</div></div>
        <div class="stat-card"><div class="stat-num">{{ stats.totalMovies }}</div><div class="stat-label">总影片数</div></div>
      </div>
      <div v-if="stats?.topMovies?.length" style="margin-top:20px">
        <h4 style="margin-bottom:12px">热门影片</h4>
        <div class="table-wrap"><table>
          <thead><tr><th>影片</th><th>订单数</th><th>票房</th></tr></thead>
          <tbody><tr v-for="m in stats.topMovies" :key="m.movieId"><td>{{ m.title }}</td><td>{{ m.orderCount }}</td><td>&yen;{{ m.revenue?.toFixed(1) }}</td></tr></tbody>
        </table></div>
      </div>
    </div>

    <!-- ORDERS -->
    <div v-if="activeTab === 'orders'">
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
    </div>

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
  </div>
</template>

<style scoped>
.admin { max-width: 1160px; margin: 0 auto; padding: 24px 20px 40px; }
.admin-header { margin-bottom: 20px; }
.admin-header h2 { font-size: 22px; color: #111; }
.admin-tabs { display: flex; border-bottom: 2px solid var(--border); margin-bottom: 20px; }
.admin-tabs button { padding: 10px 24px; font-size: 15px; background: none; color: var(--text-light); border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.admin-tabs button.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }

.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; gap: 16px; flex-wrap: wrap; }
.hint-text { font-size: 13px; color: var(--text-light); }
.btn-add { padding: 8px 20px; background: var(--primary); color: #fff; font-size: 13px; border-radius: 6px; transition: background 0.2s; }
.btn-add:hover { background: var(--primary-hover); }

.table-wrap { background: #fff; border-radius: 10px; overflow-x: auto; box-shadow: 0 1px 8px rgba(0,0,0,0.06); }
table { width: 100%; border-collapse: collapse; min-width: 600px; }
th { text-align: left; padding: 14px 16px; font-size: 13px; color: var(--text-light); background: #fafafa; border-bottom: 1px solid #eee; }
td { padding: 12px 16px; font-size: 14px; border-bottom: 1px solid #f5f5f5; }
.thumb { width: 48px; height: 64px; object-fit: cover; border-radius: 4px; }
.no-thumb { font-size: 12px; color: #ccc; }
.title-cell { font-weight: 600; }
.mono { font-family: monospace; font-size: 12px; }
.status-badge { padding: 2px 10px; border-radius: 12px; font-size: 12px; }
.status-badge.showing { background: #e8f5e9; color: #4caf50; }
.status-badge.upcoming { background: #e3f2fd; color: #2196f3; }
.status-badge.paid { background: #e8f5e9; color: #4caf50; }
.status-badge.processing { background: #fff3e0; color: #ff9800; }
.status-badge.cancelled { background: #f5f5f5; color: #999; }
.status-badge.failed { background: #fce4ec; color: #e53935; }
.btn-edit { padding: 4px 14px; font-size: 12px; border-radius: 4px; margin-right: 6px; background: #e3f2fd; color: #1976d2; }
.btn-edit:hover { background: #bbdefb; }
.btn-del { padding: 4px 14px; font-size: 12px; border-radius: 4px; background: #fce4ec; color: #e53935; }
.btn-del:hover { background: #f8bbd0; }
.empty { text-align: center; padding: 60px 0; color: var(--text-light); }
.pagination-wrap { display: flex; justify-content: center; margin-top: 24px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { background: #fff; border-radius: 10px; padding: 24px; text-align: center; box-shadow: 0 1px 8px rgba(0,0,0,0.06); }
.stat-num { font-size: 28px; font-weight: 700; color: var(--primary); }
.stat-label { font-size: 13px; color: var(--text-light); margin-top: 4px; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000; display: flex; align-items: flex-start; justify-content: center; padding: 40px 20px; overflow-y: auto; }
.modal { background: #fff; border-radius: 12px; width: 640px; max-width: 100%; box-shadow: 0 8px 40px rgba(0,0,0,0.2); }
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px; border-bottom: 1px solid #eee; }
.modal-head h3 { font-size: 18px; }
.btn-close { font-size: 26px; background: none; color: #999; }
.btn-close:hover { color: #333; }
.modal-body { padding: 24px; max-height: 65vh; overflow-y: auto; }
.form-row { display: flex; gap: 16px; margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 5px; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; outline: none; font-family: inherit; box-sizing: border-box; }
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: var(--primary); }
.form-group textarea { resize: vertical; }
.flex-1 { flex: 1; }
.flex-2 { flex: 2; }
.poster-row { display: flex; gap: 10px; margin-bottom: 12px; }
.poster-row input { flex: 1; }
.poster-upload-area { display: inline-block; }
.upload-trigger { width: 120px; height: 160px; border: 2px dashed #ddd; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; }
.upload-trigger:hover { border-color: var(--primary); }
.upload-icon { font-size: 32px; color: #ccc; line-height: 1; }
.upload-text { font-size: 12px; color: #999; margin-top: 4px; }
.poster-preview { width: 120px; height: 160px; object-fit: cover; border-radius: 8px; cursor: pointer; }
.modal-foot { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #eee; }
.btn-cancel { padding: 9px 22px; background: #f0f0f0; border-radius: 6px; font-size: 14px; color: #666; }
.btn-cancel:hover { background: #e0e0e0; }
.btn-save { padding: 9px 22px; background: var(--primary); color: #fff; border-radius: 6px; font-size: 14px; transition: background 0.2s; }
.btn-save:hover { background: var(--primary-hover); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 768px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
