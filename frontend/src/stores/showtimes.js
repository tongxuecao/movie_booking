import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apiGetShowtimes } from '../services/api.js'

export const useShowtimeStore = defineStore('showtimes', () => {
  const cinemaGroups = ref([])
  const loading = ref(false)

  async function fetchShowtimes(params = {}) {
    loading.value = true
    try {
      const data = await apiGetShowtimes(params)
      cinemaGroups.value = Array.isArray(data) ? data : []
    } finally {
      loading.value = false
    }
  }

  function getById(id) {
    for (const group of cinemaGroups.value) {
      const found = group.showtimes?.find(s => s.id === id)
      if (found) return { ...found, cinemaId: group.cinemaId, cinemaName: group.cinemaName }
    }
    return null
  }

  return { cinemaGroups, loading, fetchShowtimes, getById }
})
