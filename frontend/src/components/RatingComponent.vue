<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { apiGetReviewStatus, apiCreateReview, apiGetReviewList } from '../services/api.js'
import { resolveImageUrl } from '../config.js'
import { ElMessage } from 'element-plus'

const props = defineProps({
  movieId: { type: Number, required: true }
})

const emit = defineEmits(['need-login'])

const auth = useAuthStore()
const hasReviewed = ref(false)
const userRating = ref(0)
const averageRating = ref(null)
const ratingCount = ref(0)
const selectedRating = ref(0)
const commentText = ref('')
const showRatingDialog = ref(false)
const loading = ref(false)

// 评论列表
const reviews = ref([])
const reviewPage = ref(1)
const reviewTotal = ref(0)
const reviewLoading = ref(false)
const pageSize = 10

onMounted(async () => {
  await Promise.all([loadReviewStatus(), loadReviews()])
})

async function loadReviewStatus() {
  try {
    const status = await apiGetReviewStatus(props.movieId)
    hasReviewed.value = status.hasReviewed
    userRating.value = status.userRating || 0
    averageRating.value = status.averageRating || 0
    ratingCount.value = status.ratingCount || 0
    selectedRating.value = userRating.value
  } catch (error) {
    console.error('获取评分状态失败:', error)
  }
}

async function loadReviews() {
  reviewLoading.value = true
  try {
    const res = await apiGetReviewList(props.movieId, reviewPage.value, pageSize)
    reviews.value = res.list || res.content || []
    reviewTotal.value = res.total || 0
  } catch {
    // ignore
  } finally {
    reviewLoading.value = false
  }
}

function openRatingDialog() {
  if (!auth.isLoggedIn) {
    emit('need-login')
    return
  }
  selectedRating.value = userRating.value || 0
  commentText.value = ''
  // 如果已评分，加载已有评论内容
  const existing = reviews.value.find(r => r.userId === auth.currentUser?.id)
  if (existing) {
    commentText.value = existing.content || ''
  }
  showRatingDialog.value = true
}

async function submitRating() {
  if (selectedRating.value < 1 || selectedRating.value > 10) {
    ElMessage.warning('请选择1-10分')
    return
  }

  loading.value = true
  try {
    await apiCreateReview({
      movieId: props.movieId,
      rating: selectedRating.value,
      content: commentText.value.trim() || null
    })
    ElMessage.success(hasReviewed.value ? '评分已更新' : '评分成功')
    showRatingDialog.value = false
    await Promise.all([loadReviewStatus(), loadReviews()])
  } catch (error) {
    if (error.message.includes('请先登录')) {
      emit('need-login')
    } else {
      ElMessage.error(error.message || '评分失败')
    }
  } finally {
    loading.value = false
  }
}

function getRatingText(rating) {
  if (rating >= 9) return '绝佳'
  if (rating >= 8) return '很好'
  if (rating >= 7) return '好'
  if (rating >= 6) return '还行'
  if (rating >= 5) return '一般'
  if (rating >= 4) return '较差'
  if (rating >= 3) return '差'
  if (rating >= 2) return '很差'
  return '极差'
}

function fmtTime(t) {
  if (!t) return ''
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

function getAvatarUrl(avatar) {
  if (!avatar) return null
  return resolveImageUrl(avatar)
}

const avatarLetter = (name) => (name || '?').charAt(0).toUpperCase()

function handlePageChange(page) {
  reviewPage.value = page
  loadReviews()
}
</script>

<template>
  <div class="rating-section">
    <!-- 评分概览 -->
    <div class="rating-overview">
      <div class="rating-display">
        <div class="average-rating">
          <span class="score">{{ averageRating != null ? Number(averageRating).toFixed(1) : '暂无' }}</span>
          <span class="label">平均评分</span>
        </div>
        <div class="rating-count">
          <span class="count">{{ ratingCount }}</span>
          <span class="label">人评分</span>
        </div>
      </div>

      <div v-if="auth.isLoggedIn" class="user-rating">
        <button class="rating-btn" @click="openRatingDialog">
          {{ hasReviewed ? '修改评分' : '立即评分' }}
        </button>
        <span v-if="hasReviewed" class="user-score">我的评分: {{ userRating }}分</span>
      </div>
      <div v-else class="cannot-review">
        <span>登录后即可评分和评论</span>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="reviews-card">
      <h3 class="reviews-title">观众评论 ({{ reviewTotal }})</h3>

      <div v-if="reviewLoading" class="reviews-loading">加载中...</div>

      <template v-else>
        <div v-if="reviews.length" class="reviews-list">
          <div v-for="r in reviews" :key="r.id" class="review-item">
            <div class="review-avatar">
              <img v-if="getAvatarUrl(r.avatar)" :src="getAvatarUrl(r.avatar)" alt="avatar" />
              <span v-else class="avatar-fallback">{{ avatarLetter(r.username) }}</span>
            </div>
            <div class="review-body">
              <div class="review-header">
                <span class="review-user">{{ r.username }}</span>
                <span class="review-rating">
                  <span class="stars-mini">{{ '★'.repeat(Math.round(r.rating / 2)) }}{{ '☆'.repeat(5 - Math.round(r.rating / 2)) }}</span>
                  <span class="rating-num">{{ r.rating }}分</span>
                </span>
                <span class="review-time">{{ fmtTime(r.createdAt) }}</span>
              </div>
              <div v-if="r.content" class="review-content">{{ r.content }}</div>
            </div>
          </div>

          <div v-if="reviewTotal > pageSize" class="reviews-pagination">
            <el-pagination
              small
              layout="prev, pager, next"
              :total="reviewTotal"
              :page-size="pageSize"
              :current-page="reviewPage"
              @current-change="handlePageChange"
            />
          </div>
        </div>

        <div v-else class="reviews-empty">
          <div class="empty-icon">&#128172;</div>
          <p>暂无评论</p>
          <p class="empty-hint">登录后即可评分和评论</p>
        </div>
      </template>
    </div>

    <!-- 评分弹窗 -->
    <div v-if="showRatingDialog" class="rating-dialog-overlay" @click.self="showRatingDialog = false">
      <div class="rating-dialog">
        <h3>{{ hasReviewed ? '修改评分和评论' : '为电影评分' }}</h3>
        <div class="rating-selector">
          <div class="rating-stars">
            <span
              v-for="i in 10"
              :key="i"
              class="star"
              :class="{ 'active': i <= selectedRating }"
              @click="selectedRating = i"
            >{{ i <= selectedRating ? '★' : '☆' }}</span>
          </div>
          <div class="rating-value">
            <span class="value">{{ selectedRating }}</span>
            <span class="text">{{ getRatingText(selectedRating) }}</span>
          </div>
        </div>
        <div class="comment-input">
          <textarea v-model="commentText" placeholder="写下你的观影感受（可选）" rows="3" maxlength="500"></textarea>
          <span class="char-count">{{ commentText.length }}/500</span>
        </div>
        <div class="dialog-buttons">
          <button class="cancel-btn" @click="showRatingDialog = false">取消</button>
          <button class="submit-btn" :disabled="loading || selectedRating < 1" @click="submitRating">
            {{ loading ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rating-section {
  margin-top: 32px;
  padding-top: 0;
}

/* 评分概览 */
.rating-overview {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 0;
  flex-wrap: wrap;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.rating-display {
  display: flex;
  gap: 20px;
}

.average-rating,
.rating-count {
  text-align: center;
}

.average-rating .score,
.rating-count .count {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #ff9800;
}

.average-rating .label,
.rating-count .label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.user-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-btn {
  padding: 8px 20px;
  background: linear-gradient(135deg, #ff9800, #ff5722);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.rating-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.3);
}

.user-score {
  font-size: 14px;
  color: #ff9800;
  font-weight: 600;
}

.cannot-review {
  font-size: 13px;
  color: #999;
}

/* 评论列表 */
.reviews-card {
  margin-top: 24px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.reviews-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.reviews-loading {
  text-align: center;
  padding: 24px 0;
  color: #999;
}

.reviews-empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.reviews-empty .empty-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.reviews-empty p {
  font-size: 14px;
  margin: 4px 0;
}

.reviews-empty .empty-hint {
  font-size: 13px;
  color: #bbb;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.review-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}

.review-item:last-child {
  border-bottom: none;
}

.review-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
}

.review-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.review-body {
  flex: 1;
  min-width: 0;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.review-user {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.review-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stars-mini {
  font-size: 13px;
  color: #ff9800;
  letter-spacing: 1px;
}

.rating-num {
  font-size: 12px;
  color: #ff9800;
  font-weight: 600;
}

.review-time {
  font-size: 12px;
  color: #ccc;
  margin-left: auto;
}

.review-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
}

.reviews-pagination {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

/* 评分弹窗 */
.rating-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.rating-dialog {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  width: 420px;
  max-width: 90vw;
}

.rating-dialog h3 {
  font-size: 18px;
  margin-bottom: 20px;
  text-align: center;
}

.rating-selector {
  text-align: center;
  margin-bottom: 20px;
}

.rating-stars {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-bottom: 12px;
}

.star {
  font-size: 28px;
  cursor: pointer;
  color: #ddd;
  transition: color 0.2s;
}

.star.active {
  color: #ff9800;
}

.star:hover {
  color: #ffb74d;
}

.rating-value {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.rating-value .value {
  font-size: 32px;
  font-weight: 700;
  color: #ff9800;
}

.rating-value .text {
  font-size: 14px;
  color: #666;
}

.comment-input {
  position: relative;
  margin-bottom: 20px;
}

.comment-input textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.comment-input textarea:focus {
  border-color: var(--primary);
}

.char-count {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 12px;
  color: #ccc;
}

.dialog-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.cancel-btn,
.submit-btn {
  padding: 10px 28px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
}

.cancel-btn:hover {
  background: #eee;
}

.submit-btn {
  background: linear-gradient(135deg, #ff9800, #ff5722);
  color: #fff;
  border: none;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 152, 0, 0.3);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
