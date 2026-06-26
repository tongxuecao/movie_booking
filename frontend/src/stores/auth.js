import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiLogin, apiRegister, apiAdminLogin, apiGetProfile, getToken, removeToken, getStoredUser, setStoredUser } from '../services/api.js'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref(getStoredUser())

  const isLoggedIn = computed(() => currentUser.value !== null)
  const isAdmin = computed(() => currentUser.value?.role === 'admin')
  const currentUsername = computed(() => currentUser.value?.username || '')
  const walletBalance = computed(() => Number(currentUser.value?.walletBalance) || 0)
  const avatar = computed(() => currentUser.value?.avatar || '')

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
      setStoredUser(profile)
      return true
    } catch {
      removeToken()
      currentUser.value = null
      return false
    }
  }

  async function refreshProfile() {
    try {
      const profile = await apiGetProfile()
      currentUser.value = profile
      setStoredUser(profile)
    } catch { /* ignore */ }
  }

  return { currentUser, isLoggedIn, isAdmin, currentUsername, walletBalance, avatar, login, adminLogin, register, logout, restoreSession, refreshProfile }
})
