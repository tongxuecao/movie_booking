<script setup>
import { ref, reactive, onMounted } from 'vue'
import { apiGetAdminCinemas, apiGetCinemaHalls, apiCreateCinema, apiCreateHall, apiDeleteCinema, apiDeleteHall, apiUpdateCinema, apiUpdateHall } from '../../services/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const adminCinemas = ref([])
const cinemasLoading = ref(false)
const cinemaPage = ref(1)
const cinemaTotal = ref(0)
const CINEMA_PAGE_SIZE = 10
const editingCinemaId = ref(null)
const showCinemaForm = ref(false)
const cinemaForm = reactive({ name: '', address: '', phone: '', businessHours: '' })
const editingHallId = ref(null)
const showHallForm = ref(false)
const selectedCinemaId = ref(null)
const hallForm = reactive({ cinemaId: '', name: '', seatRows: 10, seatCols: 10, hallType: 'normal' })
const cinemaHalls = ref([])
const showHallsDialog = ref(false)
const selectedCinemaName = ref('')

onMounted(() => loadCinemas())

async function loadCinemas() {
  cinemasLoading.value = true
  try {
    const data = await apiGetAdminCinemas({ page: cinemaPage.value, size: CINEMA_PAGE_SIZE })
    adminCinemas.value = data.list || []
    cinemaTotal.value = data.total || 0
  } catch {} finally { cinemasLoading.value = false }
}

function openCinemaAdd() {
  editingCinemaId.value = null
  Object.assign(cinemaForm, { name: '', address: '', phone: '', businessHours: '' })
  showCinemaForm.value = true
}

function openCinemaEdit(cinema) {
  editingCinemaId.value = cinema.id
  Object.assign(cinemaForm, { name: cinema.name, address: cinema.address || '', phone: cinema.phone || '', businessHours: cinema.businessHours || '' })
  showCinemaForm.value = true
}

async function handleCinemaSave() {
  if (!cinemaForm.name.trim()) { ElMessage.warning('请输入影院名称'); return }
  try {
    if (editingCinemaId.value) {
      await apiUpdateCinema(editingCinemaId.value, cinemaForm)
      ElMessage.success('修改成功')
    } else {
      await apiCreateCinema(cinemaForm)
      ElMessage.success('添加成功')
    }
    showCinemaForm.value = false
    loadCinemas()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
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
  editingHallId.value = null
  Object.assign(hallForm, { cinemaId: selectedCinemaId.value, name: '', seatRows: 10, seatCols: 10, hallType: 'normal' })
  showHallForm.value = true
}

function openHallEdit(hall) {
  editingHallId.value = hall.id
  Object.assign(hallForm, { cinemaId: hall.cinemaId || selectedCinemaId.value, name: hall.name, seatRows: hall.seatRows, seatCols: hall.seatCols, hallType: hall.hallType })
  showHallForm.value = true
}

async function handleHallSave() {
  if (!hallForm.name.trim()) { ElMessage.warning('请输入影厅名称'); return }
  try {
    if (editingHallId.value) {
      await apiUpdateHall(editingHallId.value, hallForm)
      ElMessage.success('修改成功')
    } else {
      await apiCreateHall(hallForm)
      ElMessage.success('添加成功')
    }
    showHallForm.value = false
    const data = await apiGetCinemaHalls(selectedCinemaId.value)
    cinemaHalls.value = data || []
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleHallDelete(hall) {
  try {
    await ElMessageBox.confirm(`确定要删除影厅「${hall.name}」吗？将级联删除其下所有座位、排片及订单数据。`, '确认删除', { type: 'warning', confirmButtonText: '确认删除' })
    await apiDeleteHall(hall.id)
    ElMessage.success('删除成功')
    const data = await apiGetCinemaHalls(selectedCinemaId.value)
    cinemaHalls.value = data || []
  } catch {}
}

async function handleCinemaDelete(cinema) {
  try {
    await ElMessageBox.confirm(`确定要删除影院「${cinema.name}」吗？将级联删除其下所有影厅、座位、排片及订单数据，不可恢复。`, '确认删除', { type: 'warning', confirmButtonText: '确认删除' })
    await apiDeleteCinema(cinema.id)
    ElMessage.success('删除成功')
    loadCinemas()
  } catch {}
}
</script>

<template>
  <div class="content-section">
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
            <td><button class="btn-edit" @click="openCinemaEdit(c)">编辑</button><button class="btn-edit" @click="openHalls(c)">影厅管理</button><button class="btn-del" @click="handleCinemaDelete(c)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="empty">暂无影院数据</div>
    <div class="pagination-wrap" v-if="cinemaTotal > CINEMA_PAGE_SIZE">
      <el-pagination v-model:current-page="cinemaPage" :page-size="CINEMA_PAGE_SIZE" :total="cinemaTotal" layout="prev,pager,next,total" background @current-change="loadCinemas" />
    </div>

    <!-- CINEMA FORM MODAL -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showCinemaForm" class="modal-overlay">
        <div class="modal">
          <div class="modal-head"><h3>{{ editingCinemaId ? '编辑影院' : '添加影院' }}</h3><button class="btn-close" @click="showCinemaForm = false">&times;</button></div>
          <div class="modal-body">
            <div class="form-group"><label>影院名称*</label><input v-model="cinemaForm.name" /></div>
            <div class="form-group"><label>地址</label><input v-model="cinemaForm.address" /></div>
            <div class="form-row">
              <div class="form-group flex-1"><label>电话</label><input v-model="cinemaForm.phone" /></div>
              <div class="form-group flex-1"><label>营业时间</label><input v-model="cinemaForm.businessHours" placeholder="09:00-22:00" /></div>
            </div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showCinemaForm = false">取消</button><button class="btn-save" @click="handleCinemaSave">{{ editingCinemaId ? '保存' : '添加' }}</button></div>
        </div>
      </div>
    </Transition></Teleport>

    <!-- HALLS DIALOG -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showHallsDialog" class="modal-overlay">
        <div class="modal modal-lg">
          <div class="modal-head"><h3>{{ selectedCinemaName }} - 影厅列表</h3><button class="btn-close" @click="showHallsDialog = false">&times;</button></div>
          <div class="modal-body">
            <div class="toolbar" style="margin-bottom: 16px;">
              <button class="btn-add" @click="openHallAdd">+ 添加影厅</button>
            </div>
            <div class="table-wrap" v-if="cinemaHalls.length">
              <table>
                <thead><tr><th>影厅名称</th><th>座位数</th><th>类型</th><th>操作</th></tr></thead>
                <tbody>
                  <tr v-for="h in cinemaHalls" :key="h.id">
                    <td>{{ h.name }}</td>
                    <td>{{ h.seatCount }}</td>
                    <td>{{ h.hallType }}</td>
                    <td><button class="btn-edit" @click="openHallEdit(h)">编辑</button><button class="btn-del" @click="handleHallDelete(h)">删除</button></td>
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
      <div v-if="showHallForm" class="modal-overlay">
        <div class="modal">
          <div class="modal-head"><h3>{{ editingHallId ? '编辑影厅' : '添加影厅' }}</h3><button class="btn-close" @click="showHallForm = false">&times;</button></div>
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
                <option value="couple">情侣厅</option>
                <option value="threeD">3D</option>
              </select>
            </div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showHallForm = false">取消</button><button class="btn-save" @click="handleHallSave">{{ editingHallId ? '保存' : '添加' }}</button></div>
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

.title-cell {
  font-weight: 600;
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
  margin-left: 6px;
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
.form-group select {
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
