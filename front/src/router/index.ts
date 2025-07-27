import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/store/AuthStore'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => {
      const authStore = useAuthStore()
      return authStore.isAuthenticated ? '/drive' : '/login'
    }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue')
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/pages/HomePage.vue')
  },
  {
    path: '/drive',
    name: 'drive',
    component: () => import('@/pages/DrivePage.vue')
  },
  {
    path: '/transfer',
    name: 'transfer',
    component: () => import('@/pages/TransferPage.vue')
  },
  {
    path: '/music',
    name: 'music',
    component: () => import('@/pages/MusicPage.vue')
  },
  {
    path: '/docs',
    name: 'docs',
    component: () => import('@/pages/DocsPage.vue')
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/pages/SettingsPage.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫：检查认证状态
router.beforeEach(async (to: any, from: any, next: any) => {
  const authStore = useAuthStore()
  
  // 初始化认证状态（只在第一次访问时）
  if (!authStore.token && localStorage.getItem('access_token')) {
    try {
      await authStore.initializeAuth()
    } catch (error) {
      console.warn('初始化认证状态失败:', error)
    }
  }
  
  const isAuthenticated = authStore.isAuthenticated
  
  // 如果没有登录且不是去登录页，重定向到登录页
  if (!isAuthenticated && to.name !== 'login') {
    next({ name: 'login' })
  } 
  // 如果已经登录且试图访问登录页，重定向到首页
  else if (isAuthenticated && to.name === 'login') {
    next({ name: 'home' })
  } 
  // 其他情况正常跳转
  else {
    next()
  }
})

export default router 