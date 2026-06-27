<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const phone = ref('')
const confirmPassword = ref('')
const loading = ref(false)

async function handleRegister() {
  if (!username.value || !password.value) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (username.value.length < 3) {
    ElMessage.warning('用户名至少3个字符')
    return
  }
  if (password.value.length < 6) {
    ElMessage.warning('密码至少6个字符')
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  loading.value = true
  try {
    await auth.register(username.value, password.value, phone.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <h2>用户注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="至少3个字符" autocomplete="username" />
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input v-model="phone" type="tel" placeholder="请输入手机号" autocomplete="tel" />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="至少6个字符" autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input v-model="confirmPassword" type="password" placeholder="请再次输入密码" autocomplete="new-password" />
        </div>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </form>
      <p class="switch">已有账号？<router-link to="/login">立即登录</router-link></p>
    </div>
  </div>
</template>

<style scoped>
.register-page { min-height: 60vh; display: flex; align-items: center; justify-content: center; padding: 40px 20px; }
.register-card { width: 100%; max-width: 380px; background: #fff; border-radius: 12px; padding: 36px 32px; box-shadow: 0 2px 16px rgba(0,0,0,0.08); }
.register-card h2 { text-align: center; font-size: 22px; margin-bottom: 28px; color: #111; }
.form-group { margin-bottom: 18px; }
.form-group label { display: block; font-size: 14px; margin-bottom: 6px; color: #555; }
.form-group input { width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; outline: none; transition: border-color 0.2s; box-sizing: border-box; }
.form-group input:focus { border-color: var(--primary); }
.submit-btn { width: 100%; padding: 11px 0; background: var(--primary); color: #fff; font-size: 15px; border-radius: 6px; margin-top: 6px; transition: background 0.2s; }
.submit-btn:hover:not(:disabled) { background: var(--primary-hover); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.switch { text-align: center; margin-top: 18px; font-size: 13px; color: var(--text-light); }
.switch a { color: var(--primary); }

@media (max-width: 480px) {
  .register-page { padding: 24px 12px; }
  .register-card { padding: 24px 18px; }
  .register-card h2 { font-size: 20px; margin-bottom: 20px; }
  .form-group { margin-bottom: 14px; }
  .form-group input { padding: 10px; font-size: 16px; }
}
</style>
