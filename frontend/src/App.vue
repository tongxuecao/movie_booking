<script setup>
import { computed } from 'vue'
import { useAuthStore } from './stores/auth.js'

const auth = useAuthStore()

const avatarLetter = computed(() => {
  const name = auth.currentUsername || ''
  return name.charAt(0).toUpperCase() || '?'
})
</script>

<template>
  <div class="app">
    <header class="app-header">
      <div class="header-inner">
        <router-link to="/" class="logo">MovieTicket</router-link>
        <nav class="nav">
          <router-link to="/" class="nav-link" active-class="active">首页</router-link>
          <router-link to="/cinemas" class="nav-link" active-class="active">影院</router-link>
          <router-link to="/admin" v-if="auth.isAdmin" class="nav-link" active-class="active">管理</router-link>
        </nav>
        <div class="user-area">
          <template v-if="auth.isLoggedIn">
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

    <footer class="app-footer">
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
</style>
