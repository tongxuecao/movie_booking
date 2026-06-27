<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCinemaStore } from '../stores/cinemas.js'
import { useAuthStore } from '../stores/auth.js'
import { apiGetShowtimes, apiGetMovies } from '../services/api.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const cinemaStore = useCinemaStore()
const auth = useAuthStore()

const cinema = ref(null)
const showtimes = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    cinema.value = await cinemaStore.fetchCinema(id)
    if (!cinema.value) {
      ElMessage.error('影院不存在')
      router.replace('/cinemas')
      return
    }
    // 获取所有电影，然后逐个查排片
    const moviesData = await apiGetMovies({ size: 200 })
    const movies = moviesData.list || []
    const allShowtimes = []
    for (const movie of movies) {
      try {
        const groups = await apiGetShowtimes({ movieId: movie.id, cinemaId: id })
        if (Array.isArray(groups)) {
          for (const group of groups) {
            for (const s of (group.showtimes || [])) {
              allShowtimes.push({ ...s, movieTitle: movie.title, movieId: movie.id })
            }
          }
        }
      } catch { /* skip */ }
    }
    showtimes.value = allShowtimes
  } catch {
    ElMessage.error('获取影院信息失败')
    router.replace('/cinemas')
  } finally {
    loading.value = false
  }
})

function goSeatSelect(showtime) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=/cinema/${route.params.id}`)
    return
  }
  router.push(`/seat-select/${showtime.id}`)
}
</script>

<template>
  <div class="cinema-detail" v-if="!loading && cinema">
    <button class="back-btn" @click="router.push('/cinemas')">&larr; 返回影院列表</button>

    <div class="cinema-header">
      <h2>{{ cinema.name }}</h2>
      <p class="addr" v-if="cinema.address">{{ cinema.address }}</p>
      <p class="phone" v-if="cinema.phone"><a :href="'tel:' + cinema.phone">&#128222; {{ cinema.phone }}</a></p>
    </div>

    <h3 class="section-title">今日排片</h3>

    <div class="showtime-list" v-if="showtimes.length">
      <div v-for="s in showtimes" :key="s.id" class="showtime-card">
        <div class="st-info">
          <div class="st-movie">{{ s.movieTitle }}</div>
          <div class="st-meta">{{ s.showDate }} {{ s.showTime }} · {{ s.hallName }}</div>
          <div class="st-seats">余座 {{ s.availableSeats }}/{{ s.totalSeats }}</div>
        </div>
        <div class="st-action">
          <span class="st-price">&yen;{{ s.price }}</span>
          <button class="btn-buy" @click="goSeatSelect(s)">购票</button>
        </div>
      </div>
    </div>
    <div class="empty" v-else>暂无排片信息</div>
  </div>
  <div v-else-if="loading" class="loading">加载中...</div>
</template>

<style scoped>
.cinema-detail { max-width: 900px; margin: 0 auto; padding: 24px 20px 40px; }
.loading { text-align: center; padding: 80px 0; color: var(--text-light); }
.back-btn { background: none; font-size: 15px; color: var(--primary); margin-bottom: 20px; padding: 6px 0; }
.back-btn:hover { opacity: 0.8; }

.cinema-header { background: #fff; border-radius: 12px; padding: 24px; box-shadow: var(--shadow); margin-bottom: 24px; }
.cinema-header h2 { font-size: 22px; margin-bottom: 6px; }
.cinema-header .addr { font-size: 14px; color: var(--text-light); margin-bottom: 2px; }
.cinema-header .phone { font-size: 14px; color: #666; }
.cinema-header .phone a { color: #666; text-decoration: none; }
.cinema-header .phone a:hover { color: var(--primary); }

.section-title { font-size: 18px; margin-bottom: 16px; }

.showtime-list { display: flex; flex-direction: column; gap: 12px; }
.showtime-card { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 6px rgba(0,0,0,0.06); display: flex; justify-content: space-between; align-items: center; transition: box-shadow 0.2s; }
.showtime-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.st-movie { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.st-meta { font-size: 13px; color: var(--text-light); margin-bottom: 2px; }
.st-seats { font-size: 12px; color: #999; }
.st-action { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.st-price { font-size: 20px; font-weight: 700; color: var(--primary); }
.btn-buy { padding: 8px 24px; background: var(--primary); color: #fff; font-size: 14px; font-weight: 600; border-radius: 20px; transition: background 0.2s; }
.btn-buy:hover { background: var(--primary-hover); }
.empty { text-align: center; padding: 60px 0; color: var(--text-light); }

@media (max-width: 768px) {
  .cinema-detail { padding: 12px 12px 40px; }
  .cinema-header { padding: 16px; }
  .cinema-header h2 { font-size: 20px; }
  .showtime-card { padding: 14px 16px; }
  .st-movie { font-size: 15px; }
  .st-price { font-size: 18px; }
  .btn-buy { padding: 6px 18px; font-size: 13px; }
}

@media (max-width: 480px) {
  .showtime-card { flex-wrap: wrap; gap: 10px; }
  .st-info { flex: 1 1 100%; min-width: 0; }
  .st-movie { word-break: break-all; }
  .st-action { width: 100%; justify-content: space-between; }
}
</style>
