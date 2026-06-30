<script setup>
import { ref, reactive, onMounted } from 'vue'
import { apiGetAdminUsers, apiUpdateAdminUser, apiToggleUserStatus } from '../../services/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const usersLoading = ref(false)
const searchKW = ref('')
const uPage = ref(1)
const uTotal = ref(0)
const PAGE_SIZE = 10

const showEditModal = ref(false)
const editingUser = reactive({ id: null, username: '', phone: '', role: 'user', walletBalance: 0, password: '' })

onMounted(() => loadUsers())

async function loadUsers() {
  usersLoading.value = true
  try {
    const data = await apiGetAdminUsers({
      keyword: searchKW.value || undefined,
      page: uPage.value,
      size: PAGE_SIZE
    })
    users.value = data.list || []
    uTotal.value = data.total || 0
  } catch {} finally { usersLoading.value = false }
}

function openEdit(user) {
  Object.assign(editingUser, {
    id: user.id,
    username: user.username,
    phone: (user.phone || '').replace(/\*/g, ''),
    role: user.role,
    walletBalance: Number(user.walletBalance) || 0,
    password: ''
  })
  showEditModal.value = true
}

async function handleSave() {
  if (!editingUser.username.trim()) { ElMessage.warning('请输入用户名'); return }
  const data = {
    username: editingUser.username.trim(),
    phone: editingUser.phone || null,
    role: editingUser.role,
    walletBalance: Number(editingUser.walletBalance)
  }
  if (editingUser.password) data.password = editingUser.password
  try {
    await apiUpdateAdminUser(editingUser.id, data)
    ElMessage.success('修改成功')
    showEditModal.value = false
    loadUsers()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function handleToggleStatus(user) {
  const action = user.status === 'disabled' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户「${user.username}」吗？`, `确认${action}`, { type: 'warning' })
    await apiToggleUserStatus(user.id)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch {}
}
</script>

<template>
  <div class="content-section">
    <div class="section-header">
      <h3>用户管理</h3>
      <div class="toolbar">
        <el-input v-model="searchKW" placeholder="搜索用户名/手机号…" clearable style="max-width:300px" @input="uPage = 1; loadUsers()" />
      </div>
    </div>
    <div v-if="usersLoading" class="empty">加载中...</div>
    <div v-else-if="users.length" class="table-wrap">
      <table>
        <thead><tr><th>ID</th><th>用户名</th><th>手机号</th><th>角色</th><th>状态</th><th>余额</th><th>注册时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td class="title-cell">{{ u.username }}</td>
            <td>{{ u.phone }}</td>
            <td><span class="role-badge" :class="u.role">{{ u.role === 'admin' ? '管理员' : '用户' }}</span></td>
            <td><span class="status-badge" :class="u.status">{{ u.status === 'disabled' ? '已禁用' : '正常' }}</span></td>
            <td>&yen;{{ Number(u.walletBalance).toFixed(1) }}</td>
            <td>{{ u.createdAt?.substring(0, 10) }}</td>
            <td>
              <button class="btn-edit" @click="openEdit(u)">编辑</button>
              <button class="btn-toggle" :class="u.status" @click="handleToggleStatus(u)">{{ u.status === 'disabled' ? '启用' : '禁用' }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="empty">暂无用户数据</div>
    <div class="pagination-wrap" v-if="uTotal > PAGE_SIZE">
      <el-pagination v-model:current-page="uPage" :page-size="PAGE_SIZE" :total="uTotal" layout="prev,pager,next,total" background @current-change="loadUsers" />
    </div>

    <!-- EDIT MODAL -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="showEditModal" class="modal-overlay">
        <div class="modal">
          <div class="modal-head"><h3>编辑用户 — {{ editingUser.username }}</h3><button class="btn-close" @click="showEditModal = false">&times;</button></div>
          <div class="modal-body">
            <div class="form-row">
              <div class="form-group flex-1"><label>用户名</label><input v-model="editingUser.username" /></div>
              <div class="form-group flex-1"><label>角色</label><select v-model="editingUser.role"><option value="user">用户</option><option value="admin">管理员</option></select></div>
            </div>
            <div class="form-row">
              <div class="form-group flex-1"><label>手机号</label><input v-model="editingUser.phone" /></div>
              <div class="form-group flex-1"><label>钱包余额</label><input v-model.number="editingUser.walletBalance" type="number" step="0.01" /></div>
            </div>
            <div class="form-group"><label>新密码（留空不修改）</label><input v-model="editingUser.password" type="password" placeholder="留空则不修改密码" /></div>
          </div>
          <div class="modal-foot"><button class="btn-cancel" @click="showEditModal = false">取消</button><button class="btn-save" @click="handleSave">保存</button></div>
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

.role-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.role-badge.admin {
  background: #ffebee;
  color: #d32f2f;
}

.role-badge.user {
  background: #e8f5e9;
  color: #4caf50;
}

.btn-edit {
  padding: 4px 14px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  background: #e3f2fd;
  color: #1976d2;
  cursor: pointer;
}

.btn-edit:hover {
  background: #bbdefb;
}

.status-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.status-badge.active {
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.disabled {
  background: #f5f5f5;
  color: #999;
}

.btn-toggle {
  padding: 4px 14px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  margin-left: 6px;
  cursor: pointer;
}

.btn-toggle.disabled {
  background: #e8f5e9;
  color: #4caf50;
}

.btn-toggle.disabled:hover {
  background: #c8e6c9;
}

.btn-toggle.active {
  background: #fff3e0;
  color: #ff9800;
}

.btn-toggle.active:hover {
  background: #ffe0b2;
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
  width: 560px;
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
.form-group select {
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
.form-group select:hover {
  border-color: #bbb;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #d32f2f;
  box-shadow: 0 0 0 3px rgba(211, 47, 47, 0.1);
}

.flex-1 { flex: 1; min-width: 0; }

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
  .modal-overlay { padding: 12px; }
  .modal { width: 100%; border-radius: 12px; }
  .modal-head { padding: 14px 18px; }
  .modal-body { padding: 18px; max-height: 55vh; }
  .modal-foot { padding: 12px 18px; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>
