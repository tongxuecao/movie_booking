<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="nf-code">404</div>
    <p class="nf-text">页面不存在</p>
    <p class="nf-desc">你访问的页面可能已被移除或地址有误</p>
    <button class="btn-home" @click="router.push('/')">返回首页</button>
  </div>
</template>

<style scoped>
.not-found { text-align: center; padding: 120px 20px; }
.nf-code { font-size: 96px; font-weight: 800; color: var(--border); line-height: 1; margin-bottom: 8px; }
.nf-text { font-size: 22px; font-weight: 600; color: #333; margin-bottom: 8px; }
.nf-desc { font-size: 14px; color: var(--text-light); margin-bottom: 28px; }
.btn-home { padding: 10px 36px; background: var(--primary); color: #fff; font-size: 15px; border-radius: 8px; transition: background 0.2s; }
.btn-home:hover { background: var(--primary-hover); }
</style>
