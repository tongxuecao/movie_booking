<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useMovieStore } from '../../stores/movies.js'
import { useCinemaStore } from '../../stores/cinemas.js'
import { apiCreateShowtime, apiGetAdminShowtimes, apiDeleteShowtime, apiGetCinemaHalls } from '../../services/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const movieStore = useMovieStore()
const cinemaStore = useCinemaStore()

const showStForm = ref(false)
const stForm = reactive({ movieId: '', cinemaId: '', hallId: '', showDate: '', showTime: '14:30', price: 49.9 })
const halls = ref([])
const adminShowtimes = ref([])
const showtimesLoading = ref(false)
const stMovieFilter = ref('')
const stDateFilter = ref('')
const stPage = ref(1)
const stTotal = ref(0)
const ST_PAGE_SIZE = 10

onMounted(() => {
  movieStore.fetchMovies({ size: 100 })
  cinemaStore.fetchCinemas({ size: 200 })
  loadShowtimes()
})

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
  Object.assign(stForm, { movieId: movieStore.movies[0]?.id || '', cinemaId: '', hallId: '', showDate: '', showTime: '14:30', price: 49.9 })
  halls.value = []
  showStForm.value = true
}

async function onCinemaChange() {
  stForm.hallId = ''
  halls.value = []
  if (stForm.cinemaId) {
    try {
      const data = await apiGetCinemaHalls(stForm.cinemaId)
      halls.value = data || []
    } catch {}
  }
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
</script>

<template>
  <div class="content-section">
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

    <!-- SHOWTIME FORM MODAL -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showStForm" class="modal-overlay" @click.self="showStForm = false">
        <div class="modal">
          <div class="modal-head"><h3>添加排片</h3><button class="btn-close" @click="showStForm = false">&times;</button></div>
          <div class="modal-body">
            <div class="form-row">
              <div class="form-group flex-1">
                <label>电影</label>
                <select v-model="stForm.movieId">
                  <option v-for="m in movieStore.movies" :key="m.id" :value="m.id">{{ m.title }}</option>
                </select>
              </div>
              <div class="form-group flex-1">
                <label>影院</label>
                <select v-model="stForm.cinemaId" @change="onCinemaChange">
                  <option value="">请选择影院</option>
                  <option v-for="c in cinemaStore.cinemas" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group flex-1">
                <label>影厅</label>
                <select v-model="stForm.hallId" :disabled="!stForm.cinemaId">
                  <option value="">请选择影厅</option>
                  <option v-for="h in halls" :key="h.id" :value="h.id">{{ h.name }}（{{ h.seatCount }}座 / {{ h.hallType }}）</option>
                </select>
              </div>
              <div class="form-group flex-1"><label>日期</label><input v-model="stForm.showDate" type="date" /></div>
              <div class="form-group flex-1"><label>时间</label><input v-model="stForm.showTime" type="time" /></div>
            </div>
            <div class="form-row">
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

/* Modal */
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

.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  background: #fff;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #1976d2;
}

.flex-1 {
  flex: 1;
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
</style>
