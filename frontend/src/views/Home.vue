<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMovieStore } from '../stores/movies.js'
import MovieCard from '../components/MovieCard.vue'
import MovieSearch from '../components/MovieSearch.vue'
import BuyTicket from '../components/BuyTicket.vue'

const router = useRouter()
const movieStore = useMovieStore()

const activeTab = ref('showing')
const searchKeyword = ref('')
const previewMovie = ref(null)
const buyMovie = ref(null)

async function loadMovies() {
  await movieStore.fetchMovies({ status: activeTab.value, keyword: searchKeyword.value })
}

onMounted(loadMovies)
watch([activeTab, searchKeyword], () => { movieStore.currentPage = 1; loadMovies() })

function switchTab(tab) { activeTab.value = tab }
function onSearch(kw) { searchKeyword.value = kw }
async function showPreview(movie) {
  try {
    const detail = await movieStore.fetchMovie(movie.id)
    previewMovie.value = { ...movie, ...detail }
  } catch {
    previewMovie.value = movie
  }
}
function hidePreview() { previewMovie.value = null }
function openBuy(movie) { previewMovie.value = null; buyMovie.value = movie }

function handlePageChange(page) {
  movieStore.currentPage = page
  loadMovies()
}
</script>

<template>
  <div class="home">
    <section class="banner">
      <div class="banner-content">
        <h2>热门影片 在线选座</h2>
        <p>最新大片随时看，一键购票更便捷</p>
      </div>
    </section>

    <section class="main-content">
      <div class="tabs">
        <button :class="{ active: activeTab === 'showing' }" @click="switchTab('showing')">正在热映</button>
        <button :class="{ active: activeTab === 'coming' }" @click="switchTab('coming')">即将上映</button>
      </div>

      <MovieSearch @search="onSearch" />

      <div class="result-info" v-if="searchKeyword">
        搜索「{{ searchKeyword }}」找到 {{ movieStore.total }} 部影片
      </div>

      <div class="movie-grid" v-if="movieStore.movies.length">
        <MovieCard v-for="movie in movieStore.movies" :key="movie.id" :movie="movie" @preview="showPreview" />
      </div>
      <div class="empty" v-else>
        <p>{{ searchKeyword ? '未找到匹配的影片' : '暂无影片数据' }}</p>
      </div>

      <div class="pagination-wrap" v-if="movieStore.total > movieStore.pageSize">
        <el-pagination
          :current-page="movieStore.currentPage"
          :page-size="movieStore.pageSize"
          :total="movieStore.total"
          layout="prev, pager, next, total"
          background
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <!-- Preview Overlay -->
    <Teleport to="body"><Transition name="fade">
      <div v-if="previewMovie" class="preview-overlay" @click.self="hidePreview">
        <div class="preview-box">
          <button class="close-btn" @click="hidePreview">&times;</button>
          <div class="preview-layout">
            <div class="preview-poster">
              <img v-if="previewMovie.poster" :src="previewMovie.poster" :alt="previewMovie.title" />
              <div v-else class="no-poster">暂无海报</div>
            </div>
            <div class="preview-info">
              <h2 class="preview-title">{{ previewMovie.title }}</h2>
              <div class="preview-tags">
                <span v-if="previewMovie.rating" class="tag rating">★ {{ previewMovie.rating }}</span>
                <span class="tag">{{ previewMovie.genre }}</span>
                <span class="tag">{{ previewMovie.duration }}分钟</span>
              </div>
              <div class="preview-field"><label>导演</label><span>{{ previewMovie.director }}</span></div>
              <div class="preview-field"><label>主演</label><span>{{ previewMovie.actors }}</span></div>
              <div class="preview-field"><label>上映日期</label><span>{{ previewMovie.releaseDate }}</span></div>
              <p class="preview-synopsis">{{ previewMovie.description }}</p>
              <div class="preview-actions">
                <router-link :to="`/movie/${previewMovie.id}`" class="btn-detail" @click="hidePreview">查看详情</router-link>
                <button class="btn-buy" @click="openBuy(previewMovie)">选座购票</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition></Teleport>

    <BuyTicket v-if="buyMovie" :movie="buyMovie" @close="buyMovie = null" @need-login="router.push('/login?redirect=/')" />
  </div>
</template>

<style scoped>
.home { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.banner { height: 280px; border-radius: var(--radius); background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%); display: flex; align-items: center; justify-content: center; margin: 20px 0 32px; }
.banner-content { text-align: center; color: #fff; }
.banner-content h2 { font-size: 32px; margin-bottom: 8px; }
.banner-content p { font-size: 16px; opacity: 0.7; }
.tabs { display: flex; gap: 0; border-bottom: 2px solid var(--border); margin-bottom: 20px; }
.tabs button { padding: 10px 24px; font-size: 16px; background: none; color: var(--text-light); border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.tabs button.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }
.result-info { font-size: 13px; color: var(--miku); margin-bottom: 14px; }
.movie-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 20px; }
.empty { text-align: center; padding: 60px 0; color: var(--text-light); }
.pagination-wrap { display: flex; justify-content: center; margin-top: 32px; }

@media (max-width: 1024px) { .movie-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px) { .movie-grid { grid-template-columns: repeat(2, 1fr); } .banner { height: 180px; } .banner-content h2 { font-size: 22px; } }

.preview-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 40px; }
.preview-box { background: #fff; border-radius: 12px; max-width: 780px; width: 100%; max-height: 85vh; overflow-y: auto; position: relative; }
.close-btn { position: absolute; top: 12px; right: 16px; font-size: 28px; background: none; color: #999; z-index: 1; }
.close-btn:hover { color: #333; }
.preview-layout { display: flex; gap: 28px; padding: 32px; }
.preview-poster { flex: 0 0 240px; border-radius: 8px; overflow: hidden; background: #eee; }
.preview-poster img { width: 100%; display: block; }
.no-poster { aspect-ratio: 2/3; display: flex; align-items: center; justify-content: center; color: #ccc; font-size: 14px; }
.preview-info { flex: 1; }
.preview-title { font-size: 24px; margin-bottom: 14px; color: #111; }
.preview-tags { display: flex; gap: 8px; margin-bottom: 18px; flex-wrap: wrap; }
.tag { padding: 3px 10px; border-radius: 4px; font-size: 12px; background: #f0f0f0; color: #666; }
.tag.rating { background: #fff3e0; color: #ff9800; font-weight: 700; }
.preview-field { display: flex; gap: 8px; margin-bottom: 8px; font-size: 14px; }
.preview-field label { color: var(--text-light); min-width: 56px; }
.preview-synopsis { margin-top: 14px; font-size: 14px; line-height: 1.7; color: #555; }
.preview-actions { display: flex; gap: 12px; margin-top: 22px; }
.btn-detail { padding: 10px 28px; border: 1px solid #ccc; border-radius: 6px; font-size: 14px; color: #333; transition: all 0.2s; }
.btn-detail:hover { border-color: #999; background: #fafafa; }
.btn-buy { padding: 10px 28px; background: var(--primary); color: #fff; font-size: 14px; border-radius: 6px; transition: background 0.2s; }
.btn-buy:hover { background: var(--primary-hover); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
