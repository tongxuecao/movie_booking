<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useMovieStore } from '../../stores/movies.js'
import { apiCreateMovie, apiUpdateMovie, apiDeleteMovie, getToken } from '../../services/api.js'
import { API_BASE, resolveImageUrl } from '../../config.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const movieStore = useMovieStore()

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
</script>

<template>
  <div class="content-section">
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
</style>
