<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiGetBoxOffice, apiGetMostExpectedMovies } from '../services/api.js'

const router = useRouter()
const boxOfficeType = ref('today')
const boxOfficeData = ref(null)
const mostExpected = ref([])
const loading = ref(true)

async function fetchBoxOffice(type) {
  try {
    boxOfficeData.value = await apiGetBoxOffice(type)
  } catch (error) {
    console.error('加载票房数据失败:', error)
  }
}

watch(boxOfficeType, (type) => {
  fetchBoxOffice(type)
})

onMounted(async () => {
  try {
    const [result, expectedData] = await Promise.all([
      apiGetBoxOffice('today'),
      apiGetMostExpectedMovies(5)
    ])
    boxOfficeData.value = result
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
    <!-- 票房 -->
    <div class="card box-office-card">
      <h3 class="card-title">
        <span class="icon">🎬</span>
        票房
      </h3>
      <div class="tabs">
        <button
          class="tab"
          :class="{ active: boxOfficeType === 'today' }"
          @click="boxOfficeType = 'today'"
        >今日票房</button>
        <button
          class="tab"
          :class="{ active: boxOfficeType === 'cumulative' }"
          @click="boxOfficeType = 'cumulative'"
        >累计票房</button>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="boxOfficeData" class="box-office-content">
        <div class="movie-list" v-if="boxOfficeData.movies && boxOfficeData.movies.length">
          <div
            class="movie-item clickable"
            v-for="(movie, index) in boxOfficeData.movies"
            :key="movie.movieId"
            @click="router.push(`/movie/${movie.movieId}`)"
          >
            <span class="rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
            <img v-if="movie.poster" class="poster" :src="movie.poster" alt="" />
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

/* 标签切换 */
.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 3px;
}

.tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 7px 0;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.tab.active {
  background: #fff;
  color: #e53935;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

/* 电影列表 */
.movie-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.movie-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
}

.movie-item.clickable {
  cursor: pointer;
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

.poster {
  width: 36px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.title {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.revenue {
  font-size: 13px;
  color: #e53935;
  font-weight: 600;
  flex-shrink: 0;
  margin-left: 8px;
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

.expected-content .movie-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.expected-content .movie-info .title {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wish-count {
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
