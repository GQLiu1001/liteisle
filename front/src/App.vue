<template>
  <!-- 登录页面 - 全屏显示 -->
  <div v-if="route.path === '/login'" class="h-screen bg-liteisle-bg">
    <router-view />
  </div>
  
  <!-- 主应用界面 - 带侧边栏和顶栏 -->
  <div v-else class="h-screen bg-liteisle-bg flex overflow-hidden">
    <!-- 左侧导航栏 -->
    <Sidebar v-if="uiStore.isSidebarVisible" />
    
    <!-- 固定顶栏 -->
    <Topbar />
    
    <!-- 主内容区域 -->
    <main 
      :class="[
        'flex-1 pt-[50px] bg-liteisle-bg rounded-tr-[40px] rounded-br-[40px] overflow-hidden transition-all duration-300',
        uiStore.isSidebarVisible ? 'ml-[150px]' : 'ml-0'
      ]"
    >
      <router-view />
    </main>
    
    <!-- 底部音乐栏 - 独立浮动 -->
    <MusicBar />
    
    <!-- 全局右键菜单 -->
    <GlobalContextMenu />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { onMounted } from 'vue'
import Sidebar from '@/components/Sidebar.vue'
import Topbar from '@/components/Topbar.vue'
import MusicBar from '@/components/MusicBar.vue'
import GlobalContextMenu from '@/components/GlobalContextMenu.vue'
import { useUIStore } from '@/store/UIStore'
import { useAuthStore } from '@/store/AuthStore'
import { useVditorStore } from '@/store/VditorStore'
import { useSettingsStore } from '@/store/SettingsStore'

const route = useRoute()
const uiStore = useUIStore()
const authStore = useAuthStore()
const vditorStore = useVditorStore()
const settingsStore = useSettingsStore()

// 预加载 Office Online 资源
const preloadOfficeResources = () => {
  try {
    // 预加载 Office Online 主域名
    const link1 = document.createElement('link')
    link1.rel = 'dns-prefetch'
    link1.href = '//view.officeapps.live.com'
    document.head.appendChild(link1)

    // 预加载 Office CDN 资源
    const link2 = document.createElement('link')
    link2.rel = 'dns-prefetch'
    link2.href = '//res-1.cdn.office.net'
    document.head.appendChild(link2)

    console.log('Office Online 资源预加载完成')
  } catch (error) {
    console.warn('Office Online 资源预加载失败:', error)
  }
}

// 应用启动时的初始化
onMounted(async () => {
  // 初始化认证状态
  await authStore.initializeAuth()

  // 预加载 Vditor 依赖，提前初始化以减少后续加载时间
  try {
    console.time('Vditor预加载')
    await vditorStore.initializeVditor()
    console.timeEnd('Vditor预加载')
  } catch (error) {
    console.warn('Vditor预加载失败:', error)
  }

  // 预加载 Office Online 资源
  preloadOfficeResources()

  // 加载设置
  await settingsStore.loadSettings()
})
</script>

<style>
/* 全局字体和基础样式 */
body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji';
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style> 