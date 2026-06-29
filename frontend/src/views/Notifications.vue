<script setup>
import { ref, onMounted } from 'vue'
import { apiGetNotifications, apiMarkRead } from '../services/api.js'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10

async function fetchNotifications() {
  loading.value = true
  try {
    const data = await apiGetNotifications({ page: page.value, size })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

async function handleRead(item) {
  if (item.status === 'read') return
  try {
    await apiMarkRead(item.id)
    item.status = 'read'
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function typeTag(type) {
  return type === 'system' ? '系统' : '订单'
}

onMounted(fetchNotifications)
</script>

<template>
  <div class="notif-page">
    <h2 class="page-title">消息通知</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">
      <span class="empty-icon">🔔</span>
      <p>暂无通知</p>
    </div>
    <div v-else class="notif-list">
      <div
        class="notif-item"
        :class="{ unread: item.status === 'unread' }"
        v-for="item in list"
        :key="item.id"
        @click="handleRead(item)"
      >
        <div class="notif-dot" v-if="item.status === 'unread'"></div>
        <div class="notif-body">
          <div class="notif-header">
            <span class="notif-type" :class="item.type">{{ typeTag(item.type) }}</span>
            <span class="notif-title">{{ item.title }}</span>
            <span class="notif-time">{{ formatTime(item.createdAt) }}</span>
          </div>
          <p class="notif-content" v-if="item.content">{{ item.content }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notif-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 30px 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin-bottom: 24px;
}

.loading, .empty {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}

.notif-item:hover {
  background: #fafafa;
}

.notif-item.unread {
  background: #fafbff;
}

.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1890ff;
  flex-shrink: 0;
  margin-top: 6px;
}

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.notif-type {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  flex-shrink: 0;
}

.notif-type.order {
  background: #e6f7ff;
  color: #1890ff;
}

.notif-type.system {
  background: #fff7e6;
  color: #fa8c16;
}

.notif-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.notif-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.notif-content {
  font-size: 13px;
  color: #666;
  margin: 4px 0 0;
  line-height: 1.6;
}
</style>
