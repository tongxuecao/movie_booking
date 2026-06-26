import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiGetCinemas } from '../services/api.js'

export const useCinemaStore = defineStore('cinemas', () => {
  const cinemas = ref([])
  const total = ref(0)
  const currentPage = ref(1)

  async function fetchCinemas(params = {}) {
    const data = await apiGetCinemas({ page: currentPage.value, size: 20, ...params })
    cinemas.value = data.list || []
    total.value = data.total || 0
    return data
  }

  function getCinema(id) {
    return cinemas.value.find(c => c.id === id) || null
  }

  async function fetchCinema(id) {
    // 先从本地缓存找，没有则拉取列表
    let c = getCinema(id)
    if (!c) {
      await fetchCinemas({ size: 200 })
      c = getCinema(id)
    }
    return c
  }

  return { cinemas, total, currentPage, fetchCinemas, getCinema, fetchCinema }
})
