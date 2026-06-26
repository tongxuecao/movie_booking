<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    ElMessage.success('登录成功')

    // 确定跳转目标
    let redirect = route.query.redirect
    if (auth.isAdmin) {
      // 管理员默认跳转到管理后台
      redirect = redirect || '/admin'
    } else {
      // 普通用户跳转到首页或指定页面
      redirect = redirect || '/'
    }

    // 使用replace而不是push，避免登录页留在历史记录中
    router.replace(redirect)
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h2>用户登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>
      <p class="switch">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<style scoped>
.login-page { min-height: 60vh; display: flex; align-items: center; justify-content: center; padding: 40px 20px; }
.login-card { width: 380px; background: #fff; border-radius: 12px; padding: 36px 32px; box-shadow: 0 2px 16px rgba(0,0,0,0.08); }
.login-card h2 { text-align: center; font-size: 22px; margin-bottom: 28px; color: #111; }
.form-group { margin-bottom: 18px; }
.form-group label { display: block; font-size: 14px; margin-bottom: 6px; color: #555; }
.form-group input { width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; outline: none; transition: border-color 0.2s; box-sizing: border-box; }
.form-group input:focus { border-color: var(--primary); }
.submit-btn { width: 100%; padding: 11px 0; background: var(--primary); color: #fff; font-size: 15px; border-radius: 6px; margin-top: 6px; transition: background 0.2s; }
.submit-btn:hover:not(:disabled) { background: var(--primary-hover); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.switch { text-align: center; margin-top: 18px; font-size: 13px; color: var(--text-light); }
.switch a { color: var(--primary); }
</style>
