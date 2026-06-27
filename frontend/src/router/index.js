import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getStoredUser } from '../services/api.js'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
  },
  {
    path: '/cinemas',
    name: 'Cinemas',
    component: () => import('../views/Cinemas.vue'),
  },
  {
    path: '/cinema/:id',
    name: 'CinemaDetail',
    component: () => import('../views/CinemaDetail.vue'),
  },
  {
    path: '/seat-select/:showtimeId',
    name: 'SeatSelect',
    component: () => import('../views/SeatSelect.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/payment',
    name: 'Payment',
    component: () => import('../views/Payment.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/my',
    name: 'MyProfile',
    component: () => import('../views/MyProfile.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/movie/:id',
    name: 'MovieDetail',
    component: () => import('../views/MovieDetail.vue'),
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = getToken()
  const user = getStoredUser()

  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 管理员不能访问用户页面
  if (user?.role === 'admin' && !to.path.startsWith('/admin') && to.path !== '/login') {
    return { path: '/admin' }
  }

  if (to.meta.requiresAdmin) {
    if (!user || user.role !== 'admin') {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
})

export default router
