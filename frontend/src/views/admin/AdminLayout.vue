<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import AdminMovies from './AdminMovies.vue'
import AdminShowtimes from './AdminShowtimes.vue'
import AdminCinemas from './AdminCinemas.vue'
import AdminOrders from './AdminOrders.vue'
import AdminStatistics from './AdminStatistics.vue'

const router = useRouter()
const auth = useAuthStore()

if (!auth.isAdmin) { router.replace('/login?redirect=/admin') }

const menuComponents = {
  movies: AdminMovies,
  showtimes: AdminShowtimes,
  orders: AdminOrders,
  statistics: AdminStatistics,
  cinemas: AdminCinemas,
}

const saved = window.location.hash.replace('#', '')
const activeMenu = ref(menuComponents[saved] ? saved : 'movies')

function switchMenu(menu) {
  activeMenu.value = menu
  window.location.hash = menu
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
        <div class="menu-item" :class="{ active: activeMenu === 'movies' }" @click="switchMenu('movies')">
          <span class="menu-icon">🎬</span><span class="menu-text">电影管理</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'showtimes' }" @click="switchMenu('showtimes')">
          <span class="menu-icon">📅</span><span class="menu-text">排片管理</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'orders' }" @click="switchMenu('orders')">
          <span class="menu-icon">📋</span><span class="menu-text">订单管理</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'statistics' }" @click="switchMenu('statistics')">
          <span class="menu-icon">📊</span><span class="menu-text">数据统计</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'cinemas' }" @click="switchMenu('cinemas')">
          <span class="menu-icon">🏢</span><span class="menu-text">影院管理</span>
        </div>
      </nav>
      <div class="sidebar-footer">
        <button class="btn-logout" @click="auth.logout(); router.push('/login')">退出登录</button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="admin-content">
      <component :is="menuComponents[activeMenu]" />
    </main>
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

.sidebar-header h1 {
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

@media (max-width: 768px) {
  .admin-sidebar {
    width: 60px;
  }

  .menu-text {
    display: none;
  }

  .sidebar-header h1 {
    display: none;
  }

  .admin-content {
    margin-left: 60px;
  }
}
</style>
