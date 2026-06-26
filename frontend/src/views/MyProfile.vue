<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { useOrderStore } from '../stores/orders.js'
import { apiUpdateProfile, apiChangePassword, apiUploadImage, apiRecharge } from '../services/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const orderStore = useOrderStore()

const loading = ref(true)
const activeSection = ref('orders')
const activeTab = ref('all')
const detailOrder = ref(null)
const detailLoading = ref(false)

// 头像上传
const avatarInput = ref(null)

// 昵称修改
const showNicknameDialog = ref(false)
const nicknameForm = ref({ username: '' })
const nicknameLoading = ref(false)

// 密码修改
const showPasswordDialog = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordLoading = ref(false)

// 头像预览
const showAvatarPreview = ref(false)

// 充值
const showRechargeDialog = ref(false)
const rechargeForm = ref({ amount: '', password: '' })
const rechargeLoading = ref(false)

onMounted(async () => {
  try {
    await Promise.all([
      orderStore.fetchOrders(),
      auth.refreshProfile(),
    ])
  } catch {} finally {
    loading.value = false
  }
})

const statusLabels = { pending: '待支付', paid: '已支付', processing: '处理中', cancelled: '已取消', failed: '支付失败', refunded: '已退款' }
const statusClasses = { pending: 'status-pending', paid: 'status-paid', processing: 'status-processing', cancelled: 'status-cancelled', failed: 'status-failed', refunded: 'status-refunded' }

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'paid', label: '已支付' },
  { key: 'pending', label: '待支付' },
  { key: 'processing', label: '处理中' },
  { key: 'cancelled', label: '已取消' },
]

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orderStore.orders
  return orderStore.orders.filter(o => o.status === activeTab.value)
})

function fmtTime(t) {
  if (!t) return ''
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

function fmtAmount(v) {
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
}

async function handleCancel(orderNo) {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？取消后座位将被释放，余额将退回。', '取消订单', { confirmButtonText: '确认取消', cancelButtonText: '暂不取消', type: 'warning' })
    const res = await orderStore.cancelOrder(orderNo)
    ElMessage.success('订单已取消')
    if (res?.walletBalance !== undefined) auth.refreshProfile()
    await orderStore.fetchOrders()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '取消失败')
  }
}

async function viewDetail(orderNo) {
  detailLoading.value = true
  try {
    detailOrder.value = await orderStore.getOrderDetail(orderNo)
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 支付密码验证
const showPayPasswordDialog = ref(false)
const payPasswordForm = ref({ orderNo: '', password: '', amount: 0, movieTitle: '' })
const payPasswordLoading = ref(false)

function openPayPasswordDialog(orderNo, amount, movieTitle) {
  payPasswordForm.value = { orderNo, password: '', amount, movieTitle }
  showPayPasswordDialog.value = true
}

async function handlePayWithPassword() {
  if (!payPasswordForm.value.password) {
    ElMessage.warning('请输入支付密码')
    return
  }

  payPasswordLoading.value = true
  try {
    const result = await orderStore.payOrder(payPasswordForm.value.orderNo, payPasswordForm.value.password)
    if (result.status === 'paid') {
      ElMessage.success('支付成功')
      showPayPasswordDialog.value = false
      await auth.refreshProfile()
      await orderStore.fetchOrders()
    } else {
      ElMessage.error(result.message || '支付失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '支付失败')
  } finally {
    payPasswordLoading.value = false
  }
}

function closeDetail() { detailOrder.value = null }

function handleLogout() {
  auth.logout()
  router.push('/')
}

// ==================== 充值 ====================
function openRechargeDialog() {
  rechargeForm.value = { amount: '', password: '' }
  showRechargeDialog.value = true
}

async function handleRecharge() {
  const { amount, password } = rechargeForm.value
  if (!amount || Number(amount) <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (!password) {
    ElMessage.warning('请输入密码')
    return
  }

  rechargeLoading.value = true
  try {
    await apiRecharge(Number(amount), password)
    await auth.refreshProfile()
    ElMessage.success('充值成功')
    showRechargeDialog.value = false
  } catch (e) {
    ElMessage.error(e.message || '充值失败')
  } finally {
    rechargeLoading.value = false
  }
}

// ==================== 头像 ====================
function triggerAvatarInput() {
  avatarInput.value?.click()
}

async function handleAvatarUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    e.target.value = ''
    return
  }
  try {
    const data = await apiUploadImage(file)
    await apiUpdateProfile({ avatar: data.url })
    await auth.refreshProfile()
    ElMessage.success('头像更新成功')
  } catch (err) {
    ElMessage.error(err.message || '上传失败')
  }
  e.target.value = ''
}

function openAvatarPreview() {
  if (auth.avatar) showAvatarPreview.value = true
}

const avatarLetter = computed(() => {
  const name = auth.currentUsername || ''
  return name.charAt(0).toUpperCase() || '?'
})

// ==================== 昵称修改 ====================
function openNicknameDialog() {
  nicknameForm.value.username = auth.currentUsername || ''
  showNicknameDialog.value = true
}

async function handleNicknameSave() {
  const name = nicknameForm.value.username.trim()
  if (!name) { ElMessage.warning('请输入用户名'); return }
  if (name === auth.currentUsername) { showNicknameDialog.value = false; return }
  nicknameLoading.value = true
  try {
    await apiUpdateProfile({ username: name })
    await auth.refreshProfile()
    ElMessage.success('昵称修改成功')
    showNicknameDialog.value = false
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    nicknameLoading.value = false
  }
}

// ==================== 密码修改 ====================
function openPasswordDialog() {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  showPasswordDialog.value = true
}

async function handlePasswordSave() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword) { ElMessage.warning('请输入旧密码'); return }
  if (!newPassword) { ElMessage.warning('请输入新密码'); return }
  if (newPassword.length < 6) { ElMessage.warning('新密码长度不能少于6位'); return }
  if (newPassword === oldPassword) { ElMessage.warning('新旧密码不能相同'); return }
  if (newPassword !== confirmPassword) { ElMessage.warning('两次输入的密码不一致'); return }
  passwordLoading.value = true
  try {
    await apiChangePassword(oldPassword, newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    showPasswordDialog.value = false
    auth.logout()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    passwordLoading.value = false
  }
}

const menuItems = [
  { key: 'profile', label: '个人资料', icon: '👤' },
  { key: 'orders', label: '购票记录', icon: '🎫' },
  { key: 'wallet', label: '我的钱包', icon: '💰' },
]
</script>

<template>
  <div class="profile-page" v-if="auth.isLoggedIn">
    <div class="profile-layout">
      <!-- 侧边栏 -->
      <aside class="sidebar">
        <div class="sidebar-user">
          <div class="sidebar-avatar" @click="triggerAvatarInput">
            <img v-if="auth.avatar" :src="auth.avatar" alt="avatar" />
            <span v-else>{{ avatarLetter }}</span>
            <div class="avatar-overlay">更换头像</div>
          </div>
          <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarUpload" />
          <div class="sidebar-username">
            {{ auth.currentUsername }}
            <button class="btn-icon" @click="openNicknameDialog" title="修改昵称">✏️</button>
          </div>
          <div class="sidebar-role">{{ auth.isAdmin ? '管理员' : '普通用户' }}</div>
          <button v-if="auth.avatar" class="btn-preview-avatar" @click="openAvatarPreview">查看头像</button>
        </div>

        <nav class="sidebar-nav">
          <button v-for="item in menuItems" :key="item.key" :class="{ active: activeSection === item.key }" @click="activeSection = item.key">
            <span class="nav-icon">{{ item.icon }}</span>
            {{ item.label }}
          </button>
          <button @click="openPasswordDialog">
            <span class="nav-icon">🔒</span>
            修改密码
          </button>
        </nav>

        <button class="sidebar-logout" @click="handleLogout">退出登录</button>
      </aside>

      <!-- 主内容 -->
      <div class="main-content">
        <!-- 个人资料 -->
        <div v-if="activeSection === 'profile'" class="section">
          <h3 class="section-title">个人资料</h3>
          <div class="profile-card">
            <div class="info-row">
              <label>用户名</label>
              <span>{{ auth.currentUsername }}</span>
              <button class="btn-text" @click="openNicknameDialog">修改</button>
            </div>
            <div class="info-row"><label>手机号</label><span>{{ auth.currentUser?.phone || '未绑定' }}</span></div>
            <div class="info-row"><label>角色</label><span>{{ auth.isAdmin ? '管理员' : '普通用户' }}</span></div>
            <div class="info-row"><label>注册时间</label><span>{{ fmtTime(auth.currentUser?.createdAt) }}</span></div>
          </div>
        </div>

        <!-- 购票记录 -->
        <div v-if="activeSection === 'orders'" class="section">
          <h3 class="section-title">购票记录</h3>

          <div class="order-tabs">
            <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
          </div>

          <div v-if="!loading && filteredOrders.length" class="order-list">
            <div v-for="o in filteredOrders" :key="o.orderNo" class="order-card" @click="viewDetail(o.orderNo)">
              <div class="order-left">
                <div class="order-movie">{{ o.movieTitle }}</div>
                <div class="order-meta">{{ o.showDate }} {{ o.showTime }}</div>
                <div class="order-meta">{{ o.cinemaName }} · {{ o.hallName }}</div>
                <div class="order-meta">座位：{{ o.seatCount }} 个</div>
              </div>
              <div class="order-right">
                <div class="order-price">&yen;{{ fmtAmount(o.totalAmount) }}</div>
                <div class="order-status" :class="statusClasses[o.status]">{{ statusLabels[o.status] || o.status }}</div>
                <div class="order-time">{{ fmtTime(o.createdAt) }}</div>
                <div class="order-actions">
                  <button v-if="o.status === 'pending'" class="btn-pay-order" @click.stop="openPayPasswordDialog(o.orderNo, o.totalAmount, o.movieTitle)">继续支付</button>
                  <button v-if="o.status === 'paid'" class="btn-cancel" @click.stop="handleCancel(o.orderNo)">取消订单</button>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!loading" class="empty-state">
            <div class="empty-icon">&#127915;</div>
            <p>{{ activeTab === 'all' ? '暂无购票记录' : '暂无' + (tabs.find(t => t.key === activeTab)?.label || '') + '订单' }}</p>
            <router-link to="/" class="btn-go">去逛逛</router-link>
          </div>
          <div v-else class="loading">加载中...</div>
        </div>

        <!-- 我的钱包 -->
        <div v-if="activeSection === 'wallet'" class="section">
          <h3 class="section-title">我的钱包</h3>
          <div class="wallet-card">
            <div class="wallet-label">账户余额</div>
            <div class="wallet-balance">&yen;{{ fmtAmount(auth.walletBalance) }}</div>
            <button class="btn-recharge" @click="openRechargeDialog">充值</button>
            <p class="wallet-hint">注册即送 1000 元虚拟余额，购票自动扣款</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="detailOrder" class="detail-overlay" @click.self="closeDetail">
          <div class="detail-modal">
            <button class="detail-close" @click="closeDetail">&times;</button>
            <h3>订单详情</h3>
            <div class="detail-body">
              <div class="detail-row"><label>订单号</label><span>{{ detailOrder.orderNo }}</span></div>
              <div class="detail-row"><label>状态</label><span :class="statusClasses[detailOrder.status]">{{ statusLabels[detailOrder.status] || detailOrder.status }}</span></div>
              <div class="detail-row"><label>电影</label><span>{{ detailOrder.movieTitle }}</span></div>
              <div class="detail-row"><label>影院</label><span>{{ detailOrder.cinemaName }}</span></div>
              <div class="detail-row"><label>影厅</label><span>{{ detailOrder.hallName }}</span></div>
              <div class="detail-row"><label>场次</label><span>{{ detailOrder.showDate }} {{ detailOrder.showTime }}</span></div>
              <div class="detail-row"><label>座位</label><span>{{ detailOrder.seats?.map(s => s.row + '排' + s.col + '座').join('、') || '-' }}</span></div>
              <div class="detail-row"><label>金额</label><span class="detail-price">&yen;{{ fmtAmount(detailOrder.totalAmount) }}</span></div>
              <div class="detail-row"><label>下单时间</label><span>{{ fmtTime(detailOrder.createdAt) }}</span></div>
              <div class="detail-row" v-if="detailOrder.paidAt"><label>支付时间</label><span>{{ fmtTime(detailOrder.paidAt) }}</span></div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 头像预览弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showAvatarPreview" class="avatar-preview-overlay" @click.self="showAvatarPreview = false">
          <div class="avatar-preview-container">
            <button class="avatar-preview-close" @click="showAvatarPreview = false">&times;</button>
            <img :src="auth.avatar" alt="avatar" class="avatar-preview-img" />
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 修改昵称弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showNicknameDialog" class="detail-overlay" @click.self="showNicknameDialog = false">
          <div class="detail-modal nickname-modal">
            <button class="detail-close" @click="showNicknameDialog = false">&times;</button>
            <h3>修改昵称</h3>
            <div class="dialog-body">
              <div class="input-group">
                <label>新昵称</label>
                <input v-model="nicknameForm.username" placeholder="请输入新昵称" maxlength="20" @keyup.enter="handleNicknameSave" />
              </div>
            </div>
            <div class="dialog-foot">
              <button class="btn-dialog-cancel" @click="showNicknameDialog = false">取消</button>
              <button class="btn-dialog-confirm" :disabled="nicknameLoading" @click="handleNicknameSave">
                {{ nicknameLoading ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 修改密码弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPasswordDialog" class="detail-overlay" @click.self="showPasswordDialog = false">
          <div class="detail-modal password-modal">
            <button class="detail-close" @click="showPasswordDialog = false">&times;</button>
            <h3>修改密码</h3>
            <div class="dialog-body">
              <div class="input-group">
                <label>旧密码</label>
                <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" />
              </div>
              <div class="input-group">
                <label>新密码</label>
                <input v-model="passwordForm.newPassword" type="password" placeholder="至少6位" />
              </div>
              <div class="input-group">
                <label>确认新密码</label>
                <input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" @keyup.enter="handlePasswordSave" />
              </div>
            </div>
            <div class="dialog-foot">
              <button class="btn-dialog-cancel" @click="showPasswordDialog = false">取消</button>
              <button class="btn-dialog-confirm" :disabled="passwordLoading" @click="handlePasswordSave">
                {{ passwordLoading ? '修改中...' : '确认修改' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 充值弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showRechargeDialog" class="detail-overlay" @click.self="showRechargeDialog = false">
          <div class="detail-modal recharge-modal">
            <button class="detail-close" @click="showRechargeDialog = false">&times;</button>
            <h3>钱包充值</h3>
            <div class="dialog-body">
              <div class="input-group">
                <label>充值金额（元）</label>
                <input v-model="rechargeForm.amount" type="number" placeholder="请输入充值金额" min="0.01" step="0.01" />
              </div>
              <div class="input-group">
                <label>登录密码</label>
                <input v-model="rechargeForm.password" type="password" placeholder="请输入登录密码" @keyup.enter="handleRecharge" />
              </div>
              <p class="recharge-hint">输入密码确认充值，以账号密码代替支付密码</p>
            </div>
            <div class="dialog-foot">
              <button class="btn-dialog-cancel" @click="showRechargeDialog = false">取消</button>
              <button class="btn-dialog-confirm" :disabled="rechargeLoading" @click="handleRecharge">
                {{ rechargeLoading ? '充值中...' : '确认充值' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 支付弹窗（模拟微信支付） -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPayPasswordDialog" class="detail-overlay">
          <div class="detail-modal wx-pay-modal">
            <div class="wx-pay">
              <div class="wx-pay-header">
                <button class="wx-close" @click="showPayPasswordDialog = false">&times;</button>
                <span class="wx-title">请输入支付密码</span>
              </div>
              <div class="wx-amount">
                <span class="wx-currency">&yen;</span>
                <span class="wx-price">{{ fmtAmount(payPasswordForm.amount) }}</span>
              </div>
              <div class="wx-info">
                <div class="wx-info-row"><label>商品</label><span>电影票 - {{ payPasswordForm.movieTitle }}</span></div>
                <div class="wx-info-row"><label>收款方</label><span>电影票预订系统</span></div>
              </div>
              <div class="wx-password">
                <el-input v-model="payPasswordForm.password" type="password" placeholder="请输入支付密码" show-password @keyup.enter="handlePayWithPassword" />
              </div>
              <button class="wx-pay-btn" :disabled="payPasswordLoading" @click="handlePayWithPassword">
                {{ payPasswordLoading ? '处理中...' : '确认支付' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.profile-page { max-width: 1100px; margin: 0 auto; padding: 24px 20px 40px; }
.loading { text-align: center; padding: 60px 0; color: var(--text-light); }

.profile-layout { display: flex; gap: 24px; }

/* 侧边栏 */
.sidebar { width: 220px; flex-shrink: 0; background: #fff; border-radius: 14px; box-shadow: var(--shadow); padding: 24px 16px; display: flex; flex-direction: column; align-items: center; position: sticky; top: 84px; align-self: flex-start; }
.sidebar-user { text-align: center; margin-bottom: 20px; width: 100%; }
.sidebar-avatar { width: 72px; height: 72px; border-radius: 50%; background: var(--accent); color: #fff; font-size: 30px; font-weight: 700; display: flex; align-items: center; justify-content: center; margin: 0 auto 10px; cursor: pointer; position: relative; overflow: hidden; }
.sidebar-avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.5); color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s; }
.sidebar-avatar:hover .avatar-overlay { opacity: 1; }
.sidebar-username { font-size: 16px; font-weight: 600; margin-bottom: 2px; display: flex; align-items: center; justify-content: center; gap: 6px; }
.btn-icon { background: none; font-size: 14px; cursor: pointer; opacity: 0.5; transition: opacity 0.2s; padding: 2px; }
.btn-icon:hover { opacity: 1; }
.sidebar-role { font-size: 12px; color: var(--text-light); }
.btn-preview-avatar { margin-top: 8px; font-size: 12px; color: var(--primary); background: none; padding: 4px 12px; border: 1px solid var(--primary); border-radius: 12px; transition: all 0.2s; }
.btn-preview-avatar:hover { background: var(--primary); color: #fff; }

.sidebar-nav { width: 100%; display: flex; flex-direction: column; gap: 4px; margin-bottom: auto; }
.sidebar-nav button { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: none; border-radius: 8px; font-size: 14px; color: var(--text); transition: all 0.2s; text-align: left; width: 100%; }
.sidebar-nav button:hover { background: #f5f5f5; }
.sidebar-nav button.active { background: rgba(231,76,60,0.08); color: var(--primary); font-weight: 600; }
.nav-icon { font-size: 16px; }

.sidebar-logout { margin-top: 16px; padding: 8px; width: 100%; background: none; border-top: 1px solid var(--border); font-size: 13px; color: var(--text-light); transition: color 0.2s; }
.sidebar-logout:hover { color: var(--primary); }

/* 主内容 */
.main-content { flex: 1; min-width: 0; }
.section { background: #fff; border-radius: 14px; padding: 24px; box-shadow: var(--shadow); }
.section-title { font-size: 18px; color: #111; margin-bottom: 16px; }

/* 个人资料 */
.profile-card { display: flex; flex-direction: column; gap: 14px; }
.info-row { display: flex; gap: 12px; font-size: 14px; align-items: center; }
.info-row label { color: var(--text-light); min-width: 72px; flex-shrink: 0; }
.btn-text { background: none; color: var(--primary); font-size: 12px; padding: 2px 8px; border: 1px solid var(--primary); border-radius: 4px; transition: all 0.2s; }
.btn-text:hover { background: var(--primary); color: #fff; }

/* 钱包 */
.wallet-card { text-align: center; padding: 32px 0; }
.wallet-label { font-size: 14px; color: var(--text-light); margin-bottom: 8px; }
.wallet-balance { font-size: 42px; font-weight: 800; color: var(--primary); margin-bottom: 16px; }
.btn-recharge { padding: 10px 36px; background: #ff9800; color: #fff; font-size: 15px; font-weight: 600; border-radius: 8px; transition: all 0.2s; margin-bottom: 12px; }
.btn-recharge:hover { background: #f57c00; }
.wallet-hint { font-size: 13px; color: var(--text-light); }

/* 订单 */
.order-tabs { display: flex; gap: 0; border-bottom: 2px solid var(--border); margin-bottom: 16px; }
.order-tabs button { padding: 8px 20px; font-size: 14px; background: none; color: var(--text-light); border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.order-tabs button.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }

.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card { background: #f9fafb; border-radius: 10px; padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; transition: box-shadow 0.2s; cursor: pointer; }
.order-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.order-left { flex: 1; }
.order-movie { font-size: 15px; font-weight: 600; color: #111; margin-bottom: 3px; }
.order-meta { font-size: 12px; color: var(--text-light); margin-bottom: 2px; }
.order-right { text-align: right; flex-shrink: 0; margin-left: 16px; }
.order-price { font-size: 18px; font-weight: 700; color: var(--primary); }
.order-status { font-size: 12px; margin: 2px 0; }
.status-pending { color: #ff9800; }
.status-paid { color: #4caf50; }
.status-processing { color: #ff9800; }
.status-cancelled { color: #999; }
.status-failed { color: #e53935; }
.status-refunded { color: #2196f3; }
.order-time { font-size: 11px; color: #ccc; }
.order-actions { display: flex; gap: 6px; margin-top: 6px; justify-content: flex-end; }
.btn-pay-order { padding: 4px 12px; background: #ff9800; color: #fff; font-size: 11px; border-radius: 4px; transition: all 0.2s; }
.btn-pay-order:hover { background: #f57c00; }
.btn-cancel { padding: 4px 12px; background: none; border: 1px solid #e53935; color: #e53935; font-size: 11px; border-radius: 4px; transition: all 0.2s; }
.btn-cancel:hover { background: #fce4ec; }

.empty-state { text-align: center; padding: 60px 0; color: var(--text-light); }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-state p { font-size: 14px; margin-bottom: 16px; }
.btn-go { display: inline-block; padding: 8px 28px; background: var(--primary); color: #fff; border-radius: 6px; font-size: 14px; transition: background 0.2s; }
.btn-go:hover { background: var(--primary-hover); }

/* 订单详情弹窗 */
.detail-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 20px; }
.detail-modal { background: #fff; border-radius: 14px; max-width: 500px; width: 100%; padding: 28px; position: relative; }
.detail-close { position: absolute; top: 12px; right: 16px; font-size: 28px; background: none; color: #999; }
.detail-close:hover { color: #333; }
.detail-modal h3 { font-size: 18px; margin-bottom: 18px; }
.detail-body { display: flex; flex-direction: column; gap: 12px; }
.detail-row { display: flex; gap: 12px; font-size: 14px; }
.detail-row label { color: var(--text-light); min-width: 72px; flex-shrink: 0; }
.detail-price { font-size: 18px; font-weight: 700; color: var(--primary); }

/* 头像预览 */
.avatar-preview-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.85); z-index: 1100; display: flex; align-items: center; justify-content: center; }
.avatar-preview-container { position: relative; max-width: 90vw; max-height: 90vh; }
.avatar-preview-img { max-width: 90vw; max-height: 90vh; object-fit: contain; border-radius: 8px; }
.avatar-preview-close { position: absolute; top: -40px; right: 0; font-size: 32px; color: #fff; background: none; }
.avatar-preview-close:hover { opacity: 0.7; }

/* 通用弹窗表单 */
.nickname-modal, .password-modal, .recharge-modal { max-width: 400px; }
.dialog-body { display: flex; flex-direction: column; gap: 16px; margin-bottom: 20px; }
.input-group { display: flex; flex-direction: column; gap: 6px; }
.input-group label { font-size: 13px; color: #666; }
.input-group input { padding: 10px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; outline: none; transition: border-color 0.2s; }
.input-group input:focus { border-color: var(--primary); }
.dialog-foot { display: flex; justify-content: flex-end; gap: 12px; }
.btn-dialog-cancel { padding: 9px 22px; background: #f0f0f0; border-radius: 8px; font-size: 14px; color: #666; transition: background 0.2s; }
.btn-dialog-cancel:hover { background: #e0e0e0; }
.btn-dialog-confirm { padding: 9px 22px; background: var(--primary); color: #fff; border-radius: 8px; font-size: 14px; transition: all 0.2s; }
.btn-dialog-confirm:hover:not(:disabled) { background: var(--primary-hover); }
.btn-dialog-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
.recharge-hint { font-size: 12px; color: var(--text-light); margin-top: 4px; }

/* 微信支付弹窗 */
.wx-pay-modal { max-width: 380px; padding: 0; overflow: hidden; }
.wx-pay { padding: 28px 24px 24px; }
.wx-pay-header { text-align: center; position: relative; margin-bottom: 20px; }
.wx-close { position: absolute; left: 0; top: -4px; background: none; border: none; font-size: 24px; color: #999; cursor: pointer; line-height: 1; }
.wx-close:hover { color: #333; }
.wx-title { font-size: 17px; font-weight: 600; color: #333; }
.wx-amount { text-align: center; margin-bottom: 24px; }
.wx-currency { font-size: 20px; font-weight: 600; color: #333; vertical-align: top; margin-right: 2px; }
.wx-price { font-size: 40px; font-weight: 700; color: #333; }
.wx-info { background: #f9f9f9; border-radius: 8px; padding: 14px 16px; margin-bottom: 20px; }
.wx-info-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; }
.wx-info-row label { color: #999; }
.wx-info-row span { color: #333; }
.wx-password { margin-bottom: 20px; }
.wx-pay-btn { display: block; width: 100%; padding: 13px; background: #07c160; color: #fff; font-size: 16px; font-weight: 600; border-radius: 8px; border: none; cursor: pointer; transition: all 0.2s; }
.wx-pay-btn:hover:not(:disabled) { background: #06ad56; }
.wx-pay-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .profile-layout { flex-direction: column; }
  .sidebar { width: 100%; position: static; flex-direction: row; flex-wrap: wrap; gap: 16px; padding: 16px; }
  .sidebar-user { margin-bottom: 0; }
  .sidebar-nav { flex-direction: row; flex-wrap: wrap; width: auto; }
  .sidebar-logout { margin-top: 0; border-top: none; width: auto; }
}
</style>
