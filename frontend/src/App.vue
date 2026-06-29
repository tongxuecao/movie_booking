<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth.js'
import { getToken, apiGetNotifications, apiGetUnreadCount, apiMarkRead } from './services/api.js'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const isAdminPage = computed(() => route.path.startsWith('/admin') && auth.isAdmin)
const unreadCount = ref(0)
const showNotifPopup = ref(false)
const notifList = ref([])
const detailItem = ref(null)

const avatarLetter = computed(() => {
  const name = auth.currentUsername || ''
  return name.charAt(0).toUpperCase() || '?'
})

async function refreshUnread() {
  if (!auth.isLoggedIn) { unreadCount.value = 0; return }
  try {
    const data = await apiGetUnreadCount()
    unreadCount.value = typeof data === 'number' ? data : (data?.count || 0)
  } catch { /* 静默 */ }
}

async function openNotifPopup() {
  showNotifPopup.value = !showNotifPopup.value
  if (showNotifPopup.value) {
    try {
      const data = await apiGetNotifications({ page: 1, size: 20 })
      notifList.value = data.list || []
    } catch { notifList.value = [] }
  } else {
    detailItem.value = null
  }
}

async function openDetail(item) {
  detailItem.value = item
  if (item.status === 'unread') {
    try {
      await apiMarkRead(item.id)
      item.status = 'read'
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch { /* 静默 */ }
  }
}

function closeDetail() { detailItem.value = null }

function formatNotifTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function closePopup() { showNotifPopup.value = false; detailItem.value = null }

watch(() => auth.isLoggedIn, (val) => { if (val) refreshUnread() })

onMounted(async () => {
  if (getToken()) {
    const ok = await auth.restoreSession()
    if (ok && auth.isAdmin && !route.path.startsWith('/admin')) {
      router.replace('/admin')
    }
    if (ok) refreshUnread()
  }
})
</script>

<template>
  <div class="app">
    <!-- 管理员页面不显示顶部导航栏和页脚 -->
    <header class="app-header" v-if="!isAdminPage">
      <div class="header-inner">
        <router-link to="/" class="logo">MovieTicket</router-link>
        <nav class="nav">
          <router-link to="/" class="nav-link" active-class="active">首页</router-link>
          <router-link to="/cinemas" class="nav-link" active-class="active">影院</router-link>
        </nav>
        <div class="user-area">
          <template v-if="auth.isLoggedIn">
            <router-link v-if="auth.isAdmin" to="/admin" class="admin-entry">后台管理</router-link>
            <!-- 通知铃铛 -->
            <div class="bell-wrapper">
              <button class="bell-btn" @click="openNotifPopup" title="消息通知">
                <span class="bell-icon">{{ unreadCount > 0 ? '🔔' : '🔕' }}</span>
                <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </button>
              <!-- 通知弹窗 -->
              <div v-if="showNotifPopup" class="notif-popup" @click.stop>
                <div class="notif-popup-header">
                  <span>消息通知</span>
                  <router-link to="/notifications" class="view-all" @click="showNotifPopup = false">查看全部 →</router-link>
                </div>
                <div class="notif-popup-body" v-if="notifList.length">
                  <div
                    class="notif-popup-item"
                    :class="{ unread: item.status === 'unread' }"
                    v-for="item in notifList"
                    :key="item.id"
                    @click="openDetail(item)"
                  >
                    <div class="notif-popup-row">
                      <span class="notif-popup-dot" v-if="item.status === 'unread'"></span>
                      <span class="notif-popup-type" :class="item.type">{{ item.type === 'system' ? '系统' : '订单' }}</span>
                      <span class="notif-popup-title">{{ item.title }}</span>
                      <span class="notif-popup-time">{{ formatNotifTime(item.createdAt) }}</span>
                    </div>
                  </div>
                </div>
                <div class="notif-popup-empty" v-else>暂无通知</div>
              </div>
            </div>
            <!-- 点击空白关闭弹窗 -->
            <!-- 通知详情弹窗 -->
            <Transition name="modal">
              <div v-if="detailItem" class="notif-detail-mask" @click.self="closeDetail">
                <div class="notif-detail-card" :class="detailItem.type">
                  <div class="detail-accent-bar"></div>
                  <button class="detail-close-btn" @click="closeDetail">
                    <span class="close-icon"></span>
                  </button>
                  <div class="detail-icon-wrap">
                    <span v-if="detailItem.type === 'order'" class="detail-icon">🎫</span>
                    <span v-else class="detail-icon">📢</span>
                  </div>
                  <span class="detail-badge" :class="detailItem.type">{{ detailItem.type === 'system' ? '系统通知' : '订单通知' }}</span>
                  <h3 class="detail-title">{{ detailItem.title }}</h3>
                  <span class="detail-time">{{ formatNotifTime(detailItem.createdAt) }}</span>
                  <div class="detail-divider"></div>
                  <p class="detail-content">{{ detailItem.content }}</p>
                </div>
              </div>
            </Transition>

            <div v-if="showNotifPopup" class="notif-overlay" @click="closePopup"></div>

            <router-link to="/my" class="user-info">
              <span class="header-avatar">
                <img v-if="auth.avatar" :src="auth.avatar" alt="" />
                <span v-else>{{ avatarLetter }}</span>
              </span>
              <span class="header-username">{{ auth.currentUsername }}</span>
            </router-link>
          </template>
          <template v-else>
            <router-link to="/login" class="login-btn">登录</router-link>
            <router-link to="/register" class="register-link">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <footer class="app-footer" v-if="!isAdminPage">
      <div class="footer-inner">
        <div class="footer-section">
          <h4>MovieTicket</h4>
          <p>您的一站式在线电影票务平台</p>
        </div>
        <div class="footer-section">
          <h4>服务支持</h4>
          <p class="dead-link">帮助中心</p>
          <p class="dead-link">退票说明</p>
          <p class="dead-link">改签规则</p>
        </div>
        <div class="footer-section">
          <h4>关于我们</h4>
          <p class="dead-link">公司介绍</p>
          <p class="dead-link">联系方式</p>
          <p class="dead-link">加入我们</p>
        </div>
      </div>
      <div class="footer-disclaimer">
        <p>免责声明：本网站仅为演示项目，所有电影信息、影院数据及图片仅供学习参考，不构成任何商业用途。</p>
        <p>电影海报图片来源于互联网，若涉及版权问题请联系删除。票价、场次等信息以实际影院公布为准。</p>
        <p class="footer-copy">&copy; 2026 MovieTicket. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: var(--primary);
  white-space: nowrap;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 15px;
  color: var(--text);
  transition: all 0.2s;
}

.nav-link:hover,
.nav-link.active {
  color: var(--primary);
  background: rgba(231, 76, 60, 0.06);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: 20px;
  transition: background 0.2s;
}

.user-info:hover { background: #f5f5f5; }

.header-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.header-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.header-username {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.admin-entry {
  padding: 6px 14px;
  background: #d32f2f;
  color: #fff;
  font-size: 13px;
  border-radius: 6px;
  transition: background 0.2s;
}

.admin-entry:hover { background: #b71c1c; }

/* 铃铛 */
.bell-wrapper { position: relative; }

.bell-btn {
  position: relative;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background 0.2s;
  line-height: 1;
}
.bell-btn:hover { background: #f5f5f5; }

.bell-badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 16px;
  height: 16px;
  background: #e53935;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* 通知弹窗 */
.notif-popup {
  position: absolute;
  top: 42px;
  right: 0;
  width: 380px;
  max-height: 460px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notif-popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.view-all {
  font-size: 12px;
  font-weight: 400;
  color: #999;
  text-decoration: none;
}
.view-all:hover { color: #1890ff; }

.notif-popup-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.notif-popup-empty {
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.notif-popup-item {
  border-bottom: 1px solid #f8f8f8;
}

.notif-popup-item.unread { background: #fafbff; }

.notif-popup-item { cursor: pointer; }

.notif-popup-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  transition: background 0.15s;
}
.notif-popup-row:hover { background: #f5f5f5; }

.notif-popup-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #1890ff;
  flex-shrink: 0;
}

.notif-popup-type {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
  flex-shrink: 0;
}
.notif-popup-type.order { background: #e6f7ff; color: #1890ff; }
.notif-popup-type.system { background: #fff7e6; color: #fa8c16; }

.notif-popup-title {
  flex: 1;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-popup-time {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
}

/* ====== 通知详情弹窗 ====== */
.notif-detail-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(2px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Vue Transition */
.modal-enter-active { transition: all 0.25s ease-out; }
.modal-leave-active { transition: all 0.15s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-enter-from .notif-detail-card { transform: scale(0.92) translateY(16px); opacity: 0; }
.modal-leave-to { opacity: 0; }
.modal-leave-to .notif-detail-card { transform: scale(0.95); opacity: 0; }

.notif-detail-card {
  position: relative;
  background: #fff;
  border-radius: 16px;
  width: 400px;
  max-width: 90vw;
  max-height: 75vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
  box-shadow:
    0 1px 2px rgba(0,0,0,0.06),
    0 8px 32px rgba(0,0,0,0.18);
  transition: transform 0.25s ease-out, opacity 0.25s ease-out;
}

/* 顶部彩色条 */
.detail-accent-bar {
  height: 4px;
  border-radius: 16px 16px 0 0;
}
.notif-detail-card.order .detail-accent-bar { background: linear-gradient(90deg, #1890ff, #40a9ff); }
.notif-detail-card.system .detail-accent-bar { background: linear-gradient(90deg, #fa8c16, #ffc069); }

/* 关闭按钮 */
.detail-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0,0,0,0.04);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.detail-close-btn:hover { background: rgba(0,0,0,0.1); transform: rotate(90deg); }
.close-icon {
  position: relative;
  width: 14px; height: 14px;
}
.close-icon::before, .close-icon::after {
  content: '';
  position: absolute;
  top: 50%; left: 50%;
  width: 14px; height: 1.5px;
  background: #666;
  border-radius: 1px;
}
.close-icon::before { transform: translate(-50%,-50%) rotate(45deg); }
.close-icon::after  { transform: translate(-50%,-50%) rotate(-45deg); }

/* 图标 */
.detail-icon-wrap {
  text-align: center;
  padding: 28px 0 12px;
}
.detail-icon { font-size: 40px; }

/* 标签 */
.detail-badge {
  display: table;
  margin: 0 auto 12px;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}
.detail-badge.order { background: #e6f7ff; color: #1890ff; }
.detail-badge.system { background: #fff7e6; color: #fa8c16; }

/* 标题 */
.detail-title {
  text-align: center;
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 28px 6px;
}

/* 时间 */
.detail-time {
  display: block;
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-bottom: 20px;
}

/* 分割线 */
.detail-divider {
  width: 40px;
  height: 2px;
  background: #eee;
  margin: 0 auto 18px;
  border-radius: 1px;
}

/* 内容 */
.detail-content {
  font-size: 14px;
  color: #555;
  line-height: 1.9;
  margin: 0;
  padding: 0 28px 28px;
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: pre-wrap;
}

/* 点击空白关闭 */
.notif-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

@media (max-width: 640px) {
  .notif-popup {
    width: calc(100vw - 32px);
    right: -80px;
  }
}

.login-btn {
  padding: 6px 18px;
  background: var(--primary);
  color: #fff;
  font-size: 13px;
  border-radius: 6px;
  transition: background 0.2s;
}

.login-btn:hover { background: var(--primary-hover); }

.register-link {
  font-size: 13px;
  color: var(--text-light);
}

.register-link:hover { color: var(--primary); }

.app-main {
  flex: 1;
  padding-bottom: 40px;
}

.app-footer {
  background: #1a1a2e;
  color: rgba(255,255,255,0.7);
  font-size: 13px;
  margin-top: auto;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 20px 20px;
  display: flex;
  gap: 60px;
}

.footer-section h4 {
  color: #fff;
  font-size: 15px;
  margin-bottom: 10px;
}

.footer-section p {
  margin-bottom: 6px;
  font-size: 12px;
  color: rgba(255,255,255,0.45);
}

.dead-link { cursor: default; }

.footer-disclaimer {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 20px 24px;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.footer-disclaimer p {
  font-size: 11px;
  color: rgba(255,255,255,0.3);
  line-height: 1.8;
  margin-bottom: 2px;
}

.footer-copy {
  margin-top: 10px !important;
  color: rgba(255,255,255,0.4) !important;
  font-size: 12px !important;
}

@media (max-width: 768px) {
  .header-inner {
    gap: 12px;
    padding: 0 12px;
    height: 52px;
  }
  .logo { font-size: 18px; }
  .nav-link { padding: 6px 10px; font-size: 13px; }
  .header-username { display: none; }
  .login-btn { padding: 5px 14px; font-size: 12px; }
  .register-link { font-size: 12px; }
  .footer-inner {
    flex-direction: column;
    gap: 24px;
    padding: 24px 16px 16px;
  }
  .footer-disclaimer { padding: 12px 16px 20px; }
}

@media (max-width: 480px) {
  .header-inner { gap: 8px; height: 48px; }
  .logo { font-size: 16px; }
  .nav-link { padding: 4px 8px; font-size: 12px; }
  .header-avatar { width: 28px; height: 28px; font-size: 12px; }
  .register-link { display: none; }
}
</style>
