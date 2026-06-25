import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiLogin, apiRegister, apiAdminLogin, apiGetProfile, getToken, removeToken, getStoredUser } from '../services/api.js'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref(getStoredUser())

  const isLoggedIn = computed(() => currentUser.value !== null)
  const isAdmin = computed(() => currentUser.value?.role === 'admin')
  const currentUsername = computed(() => currentUser.value?.username || '')

  async function login(username, password) {
    const data = await apiLogin(username, password)
    currentUser.value = data.userInfo
    return { ok: true }
  }

  async function adminLogin(username, password) {
    const data = await apiAdminLogin(username, password)
    currentUser.value = data.userInfo
    return { ok: true }
  }

  async function register(username, password, phone) {
    await apiRegister(username, password, phone)
    return { ok: true }
  }

  function logout() {
    currentUser.value = null
    removeToken()
  }

  async function restoreSession() {
    if (!getToken()) return false
    try {
      const profile = await apiGetProfile()
      currentUser.value = profile
      return true
    } catch {
      removeToken()
      currentUser.value = null
      return false
    }
  }

  return { currentUser, isLoggedIn, isAdmin, currentUsername, login, adminLogin, register, logout, restoreSession }
})
