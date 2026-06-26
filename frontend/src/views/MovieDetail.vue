<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMovieStore } from '../stores/movies.js'
import BuyTicket from '../components/BuyTicket.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const movieStore = useMovieStore()

const movie = ref(null)
const loading = ref(true)
const buyMovie = ref(null)

onMounted(async () => {
  try {
    movie.value = await movieStore.fetchMovie(Number(route.params.id))
  } catch {
    ElMessage.error('影片不存在')
    router.replace('/')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="detail" v-if="!loading && movie">
    <button class="back-btn" @click="router.back()">&larr; 返回</button>

    <div class="detail-layout">
      <div class="poster-section">
        <img v-if="movie.poster" :src="movie.poster" :alt="movie.title" class="poster-large" />
        <div v-else class="poster-placeholder">暂无海报</div>
      </div>

      <div class="info-section">
        <h1 class="movie-title">{{ movie.title }}</h1>

        <div class="tags">
          <span v-if="movie.rating" class="tag rating">★ {{ movie.rating }}</span>
          <span class="tag">{{ movie.genre }}</span>
          <span class="tag">{{ movie.duration }}分钟</span>
          <span class="tag" :class="movie.status === 'showing' ? 'showing' : 'upcoming'">
            {{ movie.status === 'showing' ? '热映中' : '即将上映' }}
          </span>
        </div>

        <div class="info-block">
          <div class="field"><label>导演</label><span>{{ movie.director }}</span></div>
          <div class="field"><label>主演</label><span>{{ movie.actors }}</span></div>
          <div class="field"><label>类型</label><span>{{ movie.genre }}</span></div>
          <div class="field"><label>片长</label><span>{{ movie.duration }}分钟</span></div>
          <div class="field"><label>上映日期</label><span>{{ movie.releaseDate }}</span></div>
        </div>

        <div class="synopsis-block">
          <h3>剧情简介</h3>
          <p>{{ movie.description }}</p>
        </div>

        <button class="buy-btn" @click="buyMovie = movie">选座购票</button>
      </div>
    </div>

    <BuyTicket v-if="buyMovie" :movie="buyMovie" @close="buyMovie = null" @need-login="router.push('/login?redirect=' + route.fullPath)" />
  </div>
  <div v-else-if="loading" class="loading">加载中...</div>
</template>

<style scoped>
.detail { max-width: 1100px; margin: 0 auto; padding: 24px 20px 40px; }
.loading { text-align: center; padding: 80px 0; color: var(--text-light); }
.back-btn { background: none; font-size: 15px; color: var(--primary); margin-bottom: 24px; padding: 6px 0; }
.back-btn:hover { opacity: 0.8; }
.detail-layout { display: flex; gap: 40px; }
.poster-section { flex: 0 0 340px; }
.poster-large { width: 100%; border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.poster-placeholder { aspect-ratio: 2/3; background: #eee; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #ccc; font-size: 16px; }
.info-section { flex: 1; }
.movie-title { font-size: 30px; color: #111; margin-bottom: 16px; }
.tags { display: flex; gap: 8px; margin-bottom: 24px; flex-wrap: wrap; }
.tag { padding: 4px 12px; border-radius: 4px; font-size: 13px; background: #f0f0f0; color: #666; }
.tag.rating { background: #fff3e0; color: #ff9800; font-weight: 700; }
.tag.showing { background: #e8f5e9; color: #4caf50; }
.tag.upcoming { background: #e3f2fd; color: #2196f3; }
.info-block { background: #fafafa; border-radius: 8px; padding: 20px 24px; margin-bottom: 24px; }
.field { display: flex; gap: 12px; padding: 8px 0; font-size: 15px; }
.field label { color: var(--text-light); min-width: 72px; }
.synopsis-block { margin-bottom: 28px; }
.synopsis-block h3 { font-size: 16px; margin-bottom: 10px; color: #333; }
.synopsis-block p { font-size: 15px; line-height: 1.8; color: #555; }
.buy-btn { padding: 12px 48px; background: var(--primary); color: #fff; font-size: 16px; border-radius: 8px; transition: background 0.2s; }
.buy-btn:hover { background: var(--primary-hover); }
@media (max-width: 768px) { .detail-layout { flex-direction: column; } .poster-section { flex: none; max-width: 260px; } }
</style>
