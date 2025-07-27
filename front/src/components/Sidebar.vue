<template>
  <aside class="fixed left-0 top-0 w-[150px] h-full bg-liteisle-sidebar flex flex-col z-50 select-none">

    <!-- Navigation Items Container - 智能居中布局，确保不被截断 -->
    <div class="flex-1 flex flex-col justify-center min-h-0 py-4 lg:py-6">
      <!-- Navigation Items - 自适应居中布局 -->
      <div class="flex flex-col gap-3 lg:gap-4 px-4">
        <div v-for="item in navItems" :key="item.name" class="relative flex-shrink-0">
          <router-link
            :to="item.path"
            class="sidebar-item"
            :class="{ 
              active: route.name === item.name,
              'has-active-tasks': item.name === 'transfer' && hasActiveTasks
            }"
            draggable="false"
            @dragstart.prevent
            @selectstart.prevent
          >
            <component :is="item.icon" class="sidebar-icon" draggable="false" />
            <span class="sidebar-text" draggable="false">{{ item.label }}</span>
          </router-link>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Home, Cloud, Music, FileText, Settings, CloudLightning } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { useTransferStore } from '../store/TransferStore'

const route = useRoute()
const transferStore = useTransferStore()

// 检查是否有活跃的传输任务
const hasActiveTasks = computed(() => {
  return transferStore.totalActiveTasks > 0
})

const navItems = [
  {
    name: 'home',
    path: '/home',
    label: '首页',
    icon: Home
  },
  {
    name: 'drive',
    path: '/drive',
    label: '云盘',
    icon: Cloud
  },
  {
    name: 'transfer',
    path: '/transfer',
    label: '传输',
    icon: CloudLightning
  },
  {
    name: 'music',
    path: '/music',
    label: '音乐',
    icon: Music
  },
  {
    name: 'docs',
    path: '/docs',
    label: '文档',
    icon: FileText
  },
  {
    name: 'settings',
    path: '/settings',
    label: '设置',
    icon: Settings
  }
]
</script>

<style scoped>
.sidebar-item.has-active-tasks {
  @apply bg-orange-500 text-white;
}

.sidebar-item.has-active-tasks:hover {
  @apply bg-orange-600;
}

/* 防止拖拽相关样式 */
.sidebar-item {
  -webkit-user-drag: none;
  -khtml-user-drag: none;
  -moz-user-drag: none;
  -o-user-drag: none;
  user-drag: none;
  -webkit-touch-callout: none;
  -webkit-user-select: none;
  -khtml-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}

.sidebar-icon,
.sidebar-text {
  -webkit-user-drag: none;
  -khtml-user-drag: none;
  -moz-user-drag: none;
  -o-user-drag: none;
  user-drag: none;
  pointer-events: none; /* 防止子元素触发拖拽 */
}
</style> 