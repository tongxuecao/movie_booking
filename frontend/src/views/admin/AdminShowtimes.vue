<script setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useMovieStore } from '../../stores/movies.js'
import { useCinemaStore } from '../../stores/cinemas.js'
import { apiCreateShowtime, apiGetAdminShowtimes, apiDeleteShowtime, apiGetCinemaHalls } from '../../services/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const movieStore = useMovieStore()
const cinemaStore = useCinemaStore()

const showStForm = ref(false)
const stForm = reactive({ movieId: '', cinemaId: '', hallId: '', showDate: '', startTime: '09:00', interval: 2.5, count: 3, price: 49.9 })
const selectedMovie = computed(() => movieStore.movies.find(m => m.id === stForm.movieId))
const suggestedInterval = computed(() => {
  const d = selectedMovie.value?.duration || 120
  return Math.ceil((d + 30) / 30) * 0.5
})
const generatedTimes = ref([])
const intervalPreset = ref(null)

function getInterval() {
  if (intervalPreset.value === 'custom') return stForm.interval
  if (intervalPreset.value != null) return intervalPreset.value
  return stForm.interval
}

function regenerateTimes() {
  const start = stForm.startTime
  const interval = getInterval()
  const count = stForm.count
  if (!start || !interval || !count || count < 1) { generatedTimes.value = []; return }
  const [h, m] = start.split(':').map(Number)
  let total = h * 60 + m
  const result = []
  for (let i = 0; i < count; i++) {
    const hh = Math.floor(total / 60) % 24
    const mm = total % 60
    result.push(String(hh).padStart(2, '0') + ':' + String(mm).padStart(2, '0'))
    total += Math.round(interval * 60)
  }
  generatedTimes.value = result
}

function removeTime(index) { generatedTimes.value.splice(index, 1) }

watch([() => stForm.startTime, () => stForm.interval, () => stForm.count, intervalPreset], () => {
  if (intervalPreset.value === null) stForm.interval = suggestedInterval.value
  regenerateTimes()
}, { deep: false })
watch(() => stForm.movieId, () => { if (stForm.movieId) { stForm.interval = suggestedInterval.value; intervalPreset.value = null } })

const halls = ref([])
const adminShowtimes = ref([])
const showtimesLoading = ref(false)
const stMovieFilter = ref('')
const stDateFilter = ref('')
const stPage = ref(1)
const stTotal = ref(0)
const ST_PAGE_SIZE = 10
const selectedIds = ref(new Set())
const allSelected = computed(() => adminShowtimes.value.length > 0 && adminShowtimes.value.every(s => selectedIds.value.has(s.id)))

function toggleSelect(id) {
  const s = new Set(selectedIds.value)
  s.has(id) ? s.delete(id) : s.add(id)
  selectedIds.value = s
}

function toggleSelectAll() {
  selectedIds.value = allSelected.value ? new Set() : new Set(adminShowtimes.value.map(s => s.id))
}

async function handleBatchDelete() {
  const ids = [...selectedIds.value]
  if (!ids.length) { ElMessage.warning('请选择排片'); return }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 场排片吗？`, '批量删除', { type: 'warning', confirmButtonText: '确认删除' })
  } catch { return }
  let ok = 0, fail = 0
  for (const id of ids) {
    try {
      await apiDeleteShowtime(id)
      ok++
    } catch (e) {
      fail++
      ElMessage.error(e.message || '删除失败')
      break
    }
  }
  if (fail === 0) ElMessage.success(`成功删除 ${ok} 场排片`)
  else if (ok > 0) ElMessage.warning(`成功 ${ok} 场，失败 ${fail} 场`)
  selectedIds.value = new Set()
  loadShowtimes()
}

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
  Object.assign(stForm, { movieId: movieStore.movies[0]?.id || '', cinemaId: '', hallId: '', showDate: '', startTime: '09:00', interval: suggestedInterval.value, count: 3, price: 49.9 })
  intervalPreset.value = null
  halls.value = []
  regenerateTimes()
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
  if (!stForm.movieId || !stForm.hallId || !stForm.showDate) { ElMessage.warning('请填写完整信息'); return }
  if (selectedMovie.value?.releaseDate && stForm.showDate < selectedMovie.value.releaseDate) {
    ElMessage.warning('拍片日期不能早于电影上映日期（' + selectedMovie.value.releaseDate + '）')
    return
  }
  if (generatedTimes.value.length === 0) { ElMessage.warning('请至少添加一个场次时间'); return }
  let ok = 0, fail = 0
  for (const time of generatedTimes.value) {
    try {
      await apiCreateShowtime({ movieId: Number(stForm.movieId), hallId: Number(stForm.hallId), showDate: stForm.showDate, showTime: time, price: Number(stForm.price) })
      ok++
    } catch (e) {
      fail++
      ElMessage.error(time + ' ' + (e.message || '添加失败'))
      break
    }
  }
  if (fail === 0) ElMessage.success(`成功添加 ${ok} 场排片`)
  else if (ok > 0) ElMessage.warning(`成功 ${ok} 场，失败 ${fail} 场`)
  showStForm.value = false
  loadShowtimes()
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
        <div class="toolbar-actions">
          <button v-if="selectedIds.size" class="btn-batch-del" @click="handleBatchDelete">删除选中（{{ selectedIds.size }}）</button>
          <button class="btn-add" @click="openStAdd">+ 添加排片</button>
        </div>
      </div>
    </div>
    <div v-if="showtimesLoading" class="empty">加载中...</div>
    <div v-else-if="adminShowtimes.length" class="table-wrap">
      <table>
        <thead><tr><th style="width:40px"><input type="checkbox" :checked="allSelected" @change="toggleSelectAll" /></th><th>电影</th><th>影院</th><th>影厅</th><th>日期</th><th>时间</th><th>票价</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="st in adminShowtimes" :key="st.id">
            <td><input type="checkbox" :checked="selectedIds.has(st.id)" @change="toggleSelect(st.id)" /></td>
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
      <div v-if="showStForm" class="modal-overlay">
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
              <div class="form-group flex-1"><label>日期</label><input v-model="stForm.showDate" type="date" :min="selectedMovie?.releaseDate || ''" /></div>
              <div class="form-group flex-1"><label>票价(¥)</label><input v-model.number="stForm.price" type="number" step="0.1" /></div>
            </div>
            <div class="form-row">
              <div class="form-group" style="width:120px"><label>首场时间</label><input v-model="stForm.startTime" type="time" /></div>
              <div class="form-group" style="width:130px">
                <label>间隔</label>
                <select v-model="intervalPreset">
                  <option :value="null">智能({{ suggestedInterval }}h)</option>
                  <option :value="1.5">1.5 小时</option>
                  <option :value="2">2 小时</option>
                  <option :value="2.5">2.5 小时</option>
                  <option :value="3">3 小时</option>
                  <option value="custom">自定义</option>
                </select>
                <input v-if="intervalPreset === 'custom'" v-model.number="stForm.interval" type="number" step="0.5" min="0.5" max="8" style="margin-top:4px" />
              </div>
              <div class="form-group" style="width:90px"><label>场次数</label><input v-model.number="stForm.count" type="number" min="1" max="20" /></div>
            </div>
            <div v-if="generatedTimes.length" class="form-row" style="flex-wrap:wrap">
              <div class="form-group" style="width:100%">
                <label>场次列表（{{ generatedTimes.length }}场，点击移除）</label>
                <div class="time-tags">
                  <span v-for="(t, i) in generatedTimes" :key="i" class="time-tag" @click="removeTime(i)" :title="'点击移除 ' + t">{{ t }} ✕</span>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showStForm = false">取消</button><button class="btn-save" @click="handleStSave">批量添加（{{ generatedTimes.length }}场）</button></div>
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

.toolbar-actions { display: flex; gap: 10px; align-items: center; }
.btn-add {
  padding: 8px 20px;
  background: #d32f2f;
  color: #fff;
  font-size: 13px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-add:hover {
  background: #b71c1c;
}

.btn-batch-del {
  padding: 8px 16px;
  background: #fff;
  color: #e53935;
  font-size: 13px;
  border: 1px solid #e53935;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-batch-del:hover { background: #fce4ec; }

th input[type="checkbox"], td input[type="checkbox"] {
  width: 16px; height: 16px; cursor: pointer; accent-color: #d32f2f;
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
  border-color: #d32f2f;
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
  background: #d32f2f;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-save:hover {
  background: #b71c1c;
}

.time-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.time-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 12px; background: #fce4ec; color: #d32f2f;
  border-radius: 20px; font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.time-tag:hover { background: #f8bbd0; transform: scale(1.05); }

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
