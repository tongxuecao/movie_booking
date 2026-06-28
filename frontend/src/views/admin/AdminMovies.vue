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

const GENRE_OPTIONS = [
  '动作', '喜剧', '科幻', '爱情', '恐怖', '悬疑', '奇幻',
  '动画', '剧情', '战争', '犯罪', '纪录片', '冒险', '古装',
  '家庭', '音乐', '历史', '传记', '武侠', '灾难'
]

const showMovieForm = ref(false)
const editingMovieId = ref(null)
const selectedGenres = ref([])
const posterList = ref([])
const movieForm = reactive({ title: '', poster: '', duration: 120, director: '', actors: '', description: '', releaseDate: '', status: 'showing' })

function openMovieAdd() {
  editingMovieId.value = null
  selectedGenres.value = []
  posterList.value = []
  Object.assign(movieForm, { title: '', poster: '', duration: 120, director: '', actors: '', description: '', releaseDate: '', status: 'showing' })
  showMovieForm.value = true
}
async function openMovieEdit(m) {
  editingMovieId.value = m.id
  selectedGenres.value = m.genre ? m.genre.split(' / ').filter(Boolean) : []
  Object.assign(movieForm, { title: m.title, poster: m.poster, duration: m.duration, director: m.director, actors: m.actors, description: m.description, releaseDate: m.releaseDate, status: m.status })
  try {
    const detail = await movieStore.fetchMovie(m.id)
    const imgs = detail.images?.length ? detail.images : (m.poster ? [m.poster] : [])
    posterList.value = imgs.map(u => u.replace(/^\/api(?=\/uploads)/, ''))
  } catch { posterList.value = m.poster ? [m.poster.replace(/^\/api(?=\/uploads)/, '')] : [] }
  showMovieForm.value = true
}
function closeMovieForm() { showMovieForm.value = false }

async function handleMovieSave() {
  if (!movieForm.title.trim()) { ElMessage.warning('请输入电影名称'); return }
  if (selectedGenres.value.length === 0) { ElMessage.warning('请选择电影类型'); return }
  movieForm.poster = posterList.value[0] || ''
  const data = { ...movieForm, title: movieForm.title.trim(), duration: Number(movieForm.duration) || 120, genre: selectedGenres.value.join(' / '), images: posterList.value }
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
    posterList.value.push(res.data?.url || '')
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function removeImage(index) {
  posterList.value.splice(index, 1)
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
        <thead><tr><th>海报</th><th>片名</th><th>导演</th><th>类型</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="m in pagedMovies" :key="m.id">
            <td><img v-if="m.poster" :src="m.poster" class="thumb" /><span v-else class="no-thumb">无</span></td>
            <td class="title-cell">{{ m.title }}</td>
            <td>{{ m.director }}</td>
            <td>{{ m.genre }}</td>
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
            <div class="form-row">
              <div class="form-group flex-2"><label>电影名称 *</label><input v-model="movieForm.title" placeholder="请输入电影名称" /></div>
              <div class="form-group flex-1"><label>状态</label><select v-model="movieForm.status"><option value="showing">热映中</option><option value="upcoming">即将上映</option></select></div>
            </div>
            <div class="form-row">
              <div class="form-group flex-1"><label>导演</label><input v-model="movieForm.director" placeholder="请输入导演" /></div>
              <div class="form-group flex-1"><label>类型</label><el-select v-model="selectedGenres" multiple placeholder="请选择类型" style="width:100%"><el-option v-for="g in GENRE_OPTIONS" :key="g" :label="g" :value="g" /></el-select></div>
            </div>
            <div class="form-row">
              <div class="form-group flex-1"><label>片长（分钟）</label><input v-model.number="movieForm.duration" type="number" min="1" placeholder="120" /></div>
              <div class="form-group flex-1"><label>上映日期</label><input v-model="movieForm.releaseDate" type="date" /></div>
            </div>
            <div class="form-group"><label>主演</label><input v-model="movieForm.actors" placeholder="演员1 / 演员2" /></div>
            <div class="form-group"><label>剧情简介</label><textarea v-model="movieForm.description" rows="3" /></div>
            <div class="form-group">
              <label>海报（可上传多张，第一张为封面）</label>
              <div class="poster-list">
                <div class="poster-item" v-for="(url, idx) in posterList" :key="idx">
                  <img :src="resolveImageUrl(url)" class="poster-thumb" />
                  <span class="cover-badge" v-if="idx === 0">封面</span>
                  <button class="thumb-del" @click="removeImage(idx)">&times;</button>
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
                    <div class="upload-trigger">
                      <span class="upload-icon">+</span>
                      <span class="upload-text">上传</span>
                    </div>
                  </el-upload>
                </div>
              </div>
              <div class="poster-row" style="margin-top:10px">
                <input v-model="movieForm.poster" placeholder="或直接输入海报URL（封面）" />
                <button type="button" class="btn-add-url" @click="movieForm.poster && posterList.push(movieForm.poster)">添加</button>
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
  background: #ffebee;
  color: #d32f2f;
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
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(2px);
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 32px 16px;
  overflow-y: auto;
}

.modal {
  background: #fff;
  border-radius: 14px;
  width: 680px;
  max-width: 100%;
  box-shadow: 0 12px 48px rgba(0,0,0,0.18);
  overflow: hidden;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.modal-head h3 {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.btn-close {
  width: 32px;
  height: 32px;
  font-size: 22px;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.btn-close:hover {
  color: #333;
  background: #eee;
}

.modal-body {
  padding: 28px;
  max-height: 62vh;
  overflow-y: auto;
}

.form-row {
  display: flex;
  gap: 18px;
  margin-bottom: 4px;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #444;
  margin-bottom: 6px;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 9px 13px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.form-group input:hover,
.form-group select:hover,
.form-group textarea:hover {
  border-color: #bbb;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #d32f2f;
  box-shadow: 0 0 0 3px rgba(211, 47, 47, 0.1);
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #bbb;
}

.form-group textarea {
  resize: vertical;
  min-height: 70px;
}

.flex-1 { flex: 1; min-width: 0; }
.flex-2 { flex: 2; min-width: 0; }

.poster-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.poster-row input {
  flex: 1;
}

.poster-upload-area {
  display: inline-block;
}

.poster-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: flex-start;
}

.poster-item {
  position: relative;
  width: 130px;
  height: 173px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #eee;
}

.poster-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  font-size: 11px;
  padding: 2px 8px;
  background: #d32f2f;
  color: #fff;
  border-radius: 4px;
  font-weight: 500;
}

.thumb-del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border: none;
  background: rgba(0,0,0,0.55);
  color: #fff;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.thumb-del:hover {
  background: rgba(0,0,0,0.8);
}

.btn-add-url {
  padding: 9px 16px;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}

.btn-add-url:hover {
  background: #e0e0e0;
}

.upload-trigger {
  width: 130px;
  height: 173px;
  border: 2px dashed #ddd;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.upload-trigger:hover {
  border-color: #d32f2f;
  background: #fef0f0;
}

.upload-icon {
  font-size: 36px;
  color: #ccc;
  line-height: 1;
}

.upload-text {
  font-size: 12px;
  color: #aaa;
  margin-top: 6px;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 28px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.btn-cancel {
  padding: 10px 24px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cancel:hover {
  background: #f5f5f5;
  border-color: #ccc;
}

.btn-save {
  padding: 10px 28px;
  background: #d32f2f;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-save:hover {
  background: #b71c1c;
  box-shadow: 0 2px 8px rgba(211, 47, 47, 0.3);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .modal-overlay { padding: 12px; align-items: flex-start; }
  .modal { width: 100%; border-radius: 12px; }
  .modal-head { padding: 14px 18px; }
  .modal-head h3 { font-size: 16px; }
  .modal-body { padding: 18px; max-height: 55vh; }
  .modal-foot { padding: 12px 18px; }
  .form-row { flex-direction: column; gap: 0; }
  .poster-row { flex-direction: column; }
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
  .content-section { padding: 16px; }
  .toolbar { flex-direction: column; }
  .toolbar .el-input { max-width: 100% !important; }
  td, th { padding: 8px 10px; font-size: 13px; }
}

@media (max-width: 480px) {
  .content-section { padding: 12px; border-radius: 8px; }
  .section-header h3 { font-size: 16px; }
  .btn-add { width: 100%; text-align: center; }
}

</style>
