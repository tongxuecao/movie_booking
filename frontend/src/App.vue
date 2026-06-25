<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth.js'

const router = useRouter()
const auth = useAuthStore()

function handleLogout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="app">
    <header class="app-header">
      <div class="header-inner">
        <router-link to="/" class="logo">MovieTicket</router-link>
        <nav class="nav">
          <router-link to="/" class="nav-link" active-class="active" exact>首页</router-link>
          <router-link to="/cinemas" class="nav-link" active-class="active">影院</router-link>
          <router-link to="/admin" v-if="auth.isAdmin" class="nav-link" active-class="active">管理</router-link>
        </nav>
        <div class="user-area">
          <template v-if="auth.isLoggedIn">
            <router-link to="/my" class="nav-link my-link" active-class="active">我的</router-link>
            <span class="user-name">{{ auth.currentUsername }}</span>
            <span v-if="auth.isAdmin" class="admin-badge">管理员</span>
            <button class="logout-btn" @click="handleLogout">退出</button>
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

    <footer class="app-footer">
      <div class="footer-inner">
        <div class="footer-section">
          <h4>MovieTicket</h4>
          <p>您的一站式在线电影票务平台</p>
        </div>
        <div class="footer-section">
          <h4>服务支持</h4>
          <p>帮助中心</p>
          <p>退票说明</p>
          <p>改签规则</p>
        </div>
        <div class="footer-section">
          <h4>关于我们</h4>
          <p>公司介绍</p>
          <p>联系方式</p>
          <p>加入我们</p>
        </div>
      </div>
      <div class="footer-disclaimer">
        <p>免责声明：本网站仅为演示项目，所有电影信息、影院数据及图片仅供学习参考，不构成任何商业用途。</p>
        <p>电影海报图片来源于互联网，若涉及版权问题请联系删除。票价、场次等信息以实际影院公布为准。</p>
        <p class="footer-copy">&copy; 2025 MovieTicket. All rights reserved.</p>
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

.my-link {
  font-size: 13px;
  padding: 4px 12px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.user-name {
  font-size: 14px;
  color: #333;
}

.admin-badge {
  font-size: 11px;
  padding: 1px 8px;
  background: #fff3e0;
  color: #ff9800;
  border-radius: 10px;
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

.logout-btn {
  padding: 5px 14px;
  background: none;
  font-size: 13px;
  color: var(--text-light);
  border-radius: 4px;
  transition: all 0.2s;
}

.logout-btn:hover { color: var(--primary); background: #fafafa; }

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
  transition: color 0.2s;
  cursor: default;
}

.footer-section p:hover { color: rgba(255,255,255,0.75); }

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
</style>
