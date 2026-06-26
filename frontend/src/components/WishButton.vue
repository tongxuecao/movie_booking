<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { apiToggleWish, apiGetWishStatus, getToken } from '../services/api.js'
import { ElMessage } from 'element-plus'

const props = defineProps({
  movieId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['need-login'])
const route = useRoute()

const isWished = ref(false)
const wishCount = ref(0)
const loading = ref(false)

async function loadWishStatus() {
  try {
    const status = await apiGetWishStatus(props.movieId)
    isWished.value = status.isWished
    wishCount.value = status.wishCount
  } catch (error) {
    console.error('获取想看状态失败:', error)
  }
}

onMounted(loadWishStatus)

// 监听路由变化，重新加载状态（登录后返回时）
watch(() => route.path, () => {
  loadWishStatus()
})

async function toggleWish() {
  if (loading.value) return

  // 检查是否登录
  if (!getToken()) {
    emit('need-login')
    return
  }

  loading.value = true
  try {
    const result = await apiToggleWish(props.movieId)
    isWished.value = result.isWished
    wishCount.value = result.wishCount
    ElMessage.success(isWished.value ? '已想看' : '已取消想看')
  } catch (error) {
    if (error.message.includes('请先登录')) {
      emit('need-login')
    } else {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <button
    class="wish-btn"
    :class="{ 'wished': isWished }"
    :disabled="loading"
    @click="toggleWish"
  >
    <span class="icon">{{ isWished ? '❤️' : '🤍' }}</span>
    <span class="text">{{ isWished ? '已想看' : '想看' }}</span>
    <span class="count">{{ wishCount }}</span>
  </button>
</template>

<style scoped>
.wish-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.wish-btn:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.wish-btn.wished {
  border-color: #ff6b6b;
  background: #fff5f5;
  color: #ff6b6b;
}

.wish-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.icon {
  font-size: 16px;
}

.count {
  font-weight: 600;
  min-width: 20px;
  text-align: center;
}
</style>
