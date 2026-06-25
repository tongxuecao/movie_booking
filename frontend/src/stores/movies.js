import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiGetMovies, apiGetMovie } from '../services/api.js'

export const useMovieStore = defineStore('movies', () => {
  const movies = ref([])
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(20)

  async function fetchMovies(params = {}) {
    const data = await apiGetMovies({ page: currentPage.value, size: pageSize.value, ...params })
    movies.value = data.list || []
    total.value = data.total || 0
    return data
  }

  async function fetchMovie(id) {
    return await apiGetMovie(id)
  }

  return { movies, total, currentPage, pageSize, fetchMovies, fetchMovie }
})
