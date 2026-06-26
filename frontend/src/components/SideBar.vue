<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiGetTodayBoxOffice, apiGetMostExpectedMovies } from '../services/api.js'

const router = useRouter()
const boxOffice = ref(null)
const mostExpected = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [boxOfficeData, expectedData] = await Promise.all([
      apiGetTodayBoxOffice(),
      apiGetMostExpectedMovies(5)
    ])
    boxOffice.value = boxOfficeData
    mostExpected.value = expectedData
  } catch (error) {
    console.error('加载侧边栏数据失败:', error)
  } finally {
    loading.value = false
  }
})

function formatMoney(amount) {
  if (!amount) return '0.00'
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}
</script>

<template>
  <div class="sidebar">
    <!-- 今日票房 -->
    <div class="card box-office-card">
      <h3 class="card-title">
        <span class="icon">🎬</span>
        今日票房
      </h3>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="boxOffice" class="box-office-content">
        <div class="total-revenue">
          <span class="label">总票房</span>
          <span class="amount">¥{{ formatMoney(boxOffice.totalRevenue) }}</span>
        </div>
        <div class="movie-list" v-if="boxOffice.movies && boxOffice.movies.length">
          <div class="movie-item clickable" v-for="(movie, index) in boxOffice.movies" :key="movie.movieId" @click="router.push(`/movie/${movie.movieId}`)">
            <span class="rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
            <span class="title">{{ movie.title }}</span>
            <span class="revenue">¥{{ formatMoney(movie.revenue) }}</span>
          </div>
        </div>
        <div v-else class="empty">暂无票房数据</div>
      </div>
    </div>

    <!-- 最受期待 -->
    <div class="card expected-card">
      <h3 class="card-title">
        <span class="icon">🔥</span>
        最受期待
      </h3>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="mostExpected.length" class="expected-content">
        <div class="movie-item clickable" v-for="(movie, index) in mostExpected" :key="movie.movieId" @click="router.push(`/movie/${movie.movieId}`)">
          <span class="rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
          <div class="movie-info">
            <span class="title">{{ movie.title }}</span>
            <span class="wish-count">{{ movie.wishCount }}人想看</span>
          </div>
        </div>
      </div>
      <div v-else class="empty">暂无期待数据</div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 300px;
  flex-shrink: 0;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon {
  font-size: 18px;
}

.loading {
  text-align: center;
  color: #999;
  padding: 20px 0;
}

.empty {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 14px;
}

/* 今日票房 */
.total-revenue {
  text-align: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.total-revenue .label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.total-revenue .amount {
  font-size: 28px;
  font-weight: 700;
  color: #ff6b6b;
}

.movie-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.movie-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.movie-item.clickable {
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.movie-item.clickable:hover {
  background: #f5f5f5;
}

.rank {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank.top3 {
  background: linear-gradient(135deg, #ff6b6b, #ff8e8e);
  color: #fff;
}

.movie-item .title {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-item .revenue {
  font-size: 13px;
  color: #ff6b6b;
  font-weight: 600;
  flex-shrink: 0;
}

/* 最受期待 */
.expected-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.expected-content .movie-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.movie-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.movie-info .title {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-info .wish-count {
  font-size: 12px;
  color: #ff9800;
}

@media (max-width: 1024px) {
  .sidebar {
    width: 100%;
    display: flex;
    gap: 20px;
  }

  .card {
    flex: 1;
  }
}

@media (max-width: 640px) {
  .sidebar {
    flex-direction: column;
  }
}
</style>
