<script setup>
import { ref, watch, onMounted } from 'vue'
import { useMovieStore } from '../stores/movies.js'
import MovieCard from '../components/MovieCard.vue'
import MovieSearch from '../components/MovieSearch.vue'
import SideBar from '../components/SideBar.vue'

const movieStore = useMovieStore()

const activeTab = ref('showing')
const searchKeyword = ref('')
let debounceTimer = null

async function loadMovies() {
  await movieStore.fetchMovies({ status: activeTab.value, keyword: searchKeyword.value })
}

onMounted(loadMovies)

watch([activeTab], () => { movieStore.currentPage = 1; loadMovies() })
watch(searchKeyword, () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { movieStore.currentPage = 1; loadMovies() }, 300)
})

function switchTab(tab) { activeTab.value = tab }
function onSearch(kw) { searchKeyword.value = kw }

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
      <div class="content-layout">
        <div class="movie-section">
          <div class="tabs">
            <button :class="{ active: activeTab === 'showing' }" @click="switchTab('showing')">正在热映</button>
            <button :class="{ active: activeTab === 'upcoming' }" @click="switchTab('upcoming')">即将上映</button>
          </div>

          <MovieSearch @search="onSearch" />

          <div class="result-info" v-if="searchKeyword">
            搜索「{{ searchKeyword }}」找到 {{ movieStore.total }} 部影片
          </div>

          <div class="movie-grid" v-if="movieStore.movies.length">
            <MovieCard v-for="movie in movieStore.movies" :key="movie.id" :movie="movie" />
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
        </div>

        <SideBar />
      </div>
    </section>
  </div>
</template>

<style scoped>
.home { max-width: 1400px; margin: 0 auto; padding: 0 20px; }
.banner { height: 200px; border-radius: var(--radius); background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%); display: flex; align-items: center; justify-content: center; margin: 20px 0 28px; }
.banner-content { text-align: center; color: #fff; }
.banner-content h2 { font-size: 28px; margin-bottom: 8px; }
.banner-content p { font-size: 15px; opacity: 0.7; }
.content-layout { display: flex; gap: 30px; }
.movie-section { flex: 1; min-width: 0; }
.tabs { display: flex; gap: 0; border-bottom: 2px solid var(--border); margin-bottom: 20px; }
.tabs button { padding: 10px 24px; font-size: 16px; background: none; color: var(--text-light); border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.tabs button.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }
.result-info { font-size: 13px; color: var(--accent); margin-bottom: 14px; }
.movie-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.empty { text-align: center; padding: 60px 0; color: var(--text-light); }
.pagination-wrap { display: flex; justify-content: center; margin-top: 32px; }

@media (max-width: 1200px) { .movie-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 1024px) {
  .content-layout { flex-direction: column; }
  .movie-grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 640px) { .movie-grid { grid-template-columns: repeat(2, 1fr); } .banner { height: 140px; } .banner-content h2 { font-size: 20px; } }
</style>
