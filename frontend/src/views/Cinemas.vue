<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCinemaStore } from '../stores/cinemas.js'

const router = useRouter()
const cinemaStore = useCinemaStore()

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 10

onMounted(() => cinemaStore.fetchCinemas({ size: 100 }))

const filteredCinemas = computed(() => {
  if (!searchKeyword.value.trim()) return cinemaStore.cinemas
  const kw = searchKeyword.value.toLowerCase()
  return cinemaStore.cinemas.filter(c =>
    c.name?.toLowerCase().includes(kw) ||
    c.address?.toLowerCase().includes(kw)
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredCinemas.value.length / pageSize)))
const pagedCinemas = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredCinemas.value.slice(start, start + pageSize)
})

function goCinema(id) {
  router.push(`/cinema/${id}`)
}
</script>

<template>
  <div class="cinemas-page">
    <div class="page-header"><h2>影院中心</h2><p>查找附近影院，享受观影乐趣</p></div>

    <div class="search-bar">
      <el-input v-model="searchKeyword" placeholder="搜索影院名称或地址…" clearable @input="currentPage = 1">
        <template #prefix><span>&#128269;</span></template>
      </el-input>
    </div>

    <div class="cinema-list" v-if="pagedCinemas.length">
      <div v-for="cinema in pagedCinemas" :key="cinema.id" class="cinema-card" @click="goCinema(cinema.id)" role="link" tabindex="0" @keydown.enter="goCinema(cinema.id)">
        <div class="cinema-info">
          <h3>{{ cinema.name }}</h3>
          <p class="addr">{{ cinema.address }}</p>
          <p class="phone" v-if="cinema.phone"><a :href="'tel:' + cinema.phone">&#128222; {{ cinema.phone }}</a></p>
        </div>
        <span class="arrow">&rsaquo;</span>
      </div>
    </div>
    <div class="empty" v-else>暂无匹配的影院</div>

    <div class="pagination-wrap" v-if="totalPages > 1">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="filteredCinemas.length" layout="prev, pager, next, total" background />
    </div>
  </div>
</template>

<style scoped>
.cinemas-page { max-width: 1200px; margin: 0 auto; padding: 24px 20px 40px; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 24px; color: #111; margin-bottom: 4px; }
.page-header p { font-size: 14px; color: var(--text-light); }
.search-bar { margin-bottom: 20px; max-width: 480px; }
.cinema-list { display: flex; flex-direction: column; gap: 14px; }
.cinema-card { background: #fff; border-radius: var(--radius); box-shadow: var(--shadow); padding: 20px 24px; transition: box-shadow 0.2s; cursor: pointer; display: flex; justify-content: space-between; align-items: center; }
.cinema-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.12); }
.cinema-info h3 { font-size: 17px; color: #111; margin-bottom: 4px; }
.cinema-info .addr { font-size: 13px; color: var(--text-light); margin-bottom: 2px; }
.cinema-info .phone { font-size: 13px; color: #666; }
.cinema-info .phone a { color: #666; }
.cinema-info .phone a:hover { color: var(--primary); }
.arrow { font-size: 24px; color: #ccc; flex-shrink: 0; }
.empty { text-align: center; padding: 80px 0; color: var(--text-light); }
.pagination-wrap { display: flex; justify-content: center; margin-top: 32px; }
</style>
