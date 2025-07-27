<template>
  <div class="card h-full flex flex-col min-h-0 overflow-hidden w-full">
    <!-- 标题和全局切换按钮 -->
    <div 
      class="flex items-center mb-6 lg:mb-8 flex-shrink-0 justify-between"
    >
      <button 
        @click="prev"
        class="w-8 h-8 lg:w-10 lg:h-10 bg-morandi-100 hover:bg-morandi-200 rounded-full flex items-center justify-center transition-all duration-200"
      >
        <ChevronLeft :size="20" class="text-morandi-600" />
      </button>
      
      <h3 class="text-xl lg:text-2xl font-bold text-morandi-900">秘境岛屿</h3>
      
      <button 
        @click="next"
        class="w-8 h-8 lg:w-10 lg:h-10 bg-morandi-100 hover:bg-morandi-200 rounded-full flex items-center justify-center transition-all duration-200"
      >
        <ChevronRight :size="20" class="text-morandi-600" />
      </button>
    </div>
    
    <!-- 主内容区域: 三段式响应式布局 -->
    <div class="flex-1 flex items-center justify-center min-h-0 mb-4 lg:mb-6">

      <!-- 无岛屿状态 -->
      <div v-if="!hasIsles" class="flex flex-col items-center justify-center h-full text-center px-4">
        <div class="w-24 h-24 lg:w-32 lg:h-32 mb-6 flex items-center justify-center">
          <img src="/cardpic/cardpic.png" alt="暂无岛屿" class="max-w-full max-h-full object-contain opacity-60" />
        </div>
        <h4 class="text-lg lg:text-xl font-semibold text-morandi-700 mb-2">暂未探索到秘境岛屿</h4>
        <p class="text-sm text-morandi-500">专注学习有概率探索到秘境岛屿</p>
      </div>

      <!-- 有岛屿时显示 -->
      <template v-else>
        <!-- A. 超大屏 (>=1280px): 单岛屿轮播 -->
        <div class="w-full h-full items-center justify-center hidden lg:flex relative">
          <!-- 加载状态 -->
          <div 
            v-if="singleIsleLoading" 
            class="flex items-center justify-center h-full"
          >
            <div class="flex space-x-2">
              <div class="w-3 h-3 bg-morandi-400 rounded-full animate-bounce" style="animation-delay: 0ms"></div>
              <div class="w-3 h-3 bg-morandi-400 rounded-full animate-bounce" style="animation-delay: 150ms"></div>
              <div class="w-3 h-3 bg-morandi-400 rounded-full animate-bounce" style="animation-delay: 300ms"></div>
            </div>
          </div>
          <!-- 图片 -->
          <Transition name="fade" mode="out-in">
            <img
              v-if="!singleIsleLoading && singleIsleImage"
              :key="currentSingleIsleIndex"
              :src="singleIsleImage"
              :alt="`岛屿 ${currentSingleIsleIndex + 1}`"
              class="max-w-full max-h-full object-contain transition-all duration-300 rounded-lg"
            />
          </Transition>
        </div>

        <!-- B. 小屏幕 (<1024px): 网格视图 -->
        <div class="grid grid-cols-3 sm:grid-cols-4 lg:hidden gap-4 w-full h-full overflow-y-auto">
          <div v-for="isle in isles" :key="`grid-${isle.id}`" class="flex flex-col items-center gap-2 p-2 rounded-lg">
            <div class="w-20 h-20 md:w-24 md:h-24 bg-morandi-50 rounded-xl flex items-center justify-center p-2 relative">
              <!-- 网格项加载状态 -->
              <div 
                v-if="gridImageLoading[isle.id]" 
                class="absolute inset-0 flex items-center justify-center"
              >
                <div class="flex space-x-1">
                  <div class="w-2 h-2 bg-morandi-400 rounded-full animate-bounce" style="animation-delay: 0ms"></div>
                  <div class="w-2 h-2 bg-morandi-400 rounded-full animate-bounce" style="animation-delay: 150ms"></div>
                  <div class="w-2 h-2 bg-morandi-400 rounded-full animate-bounce" style="animation-delay: 300ms"></div>
                </div>
              </div>
              <!-- 网格项图片 -->
              <img
                v-show="!gridImageLoading[isle.id]"
                :src="isle.image_url"
                :alt="`岛屿 ${isle.id}`"
                class="max-w-full max-h-full object-contain"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>
      </template>
    </div>
    
    <!-- 数量显示 -->
    <div class="text-center flex-shrink-0">
      <p class="text-base lg:text-lg font-bold text-morandi-900">数量：{{ isles.length }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch, reactive } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useFocusStore } from '@/store/FocusStore'
import { useIslandStore } from '@/store/IslandStore'
import { storeToRefs } from 'pinia'

// 岛屿数据类型
interface Island {
  id: number
  image_url: string
  rarity: 'common' | 'rare' | 'epic' | 'legendary'
  obtain_probability: number
  min_focus_minutes: number
}

const focusStore = useFocusStore()
const islandStore = useIslandStore()

const { islandProgress } = storeToRefs(islandStore)

// 图片加载状态管理
const singleIsleLoading = ref(true)
const gridImageLoading = reactive<Record<number, boolean>>({})
const initialLoadComplete = ref(false)
const allImagesLoaded = ref(false)

// 用户已收集的岛屿（从后端获取）
const isles = computed((): Island[] => {
  // 如果有真实数据，使用真实数据
  if (islandStore.hasIslands) {
    return islandStore.userIslands.map((imageUrl: string, index: number) => ({
      id: index + 1,
      image_url: imageUrl,
      rarity: 'common' as const,
      obtain_probability: 0.1,
      min_focus_minutes: 30
    }))
  }
  
  // 如果没有数据，返回空数组
  return []
})

// 是否有岛屿
const hasIsles = computed(() => isles.value.length > 0)

// --- 统一状态和控制逻辑 ---
const currentScreenMode = ref<'single' | 'three' | 'grid'>('single')
const currentSingleIsleIndex = ref(0)
const threeIsleCurrentPage = ref(0)

// A. 单岛屿轮播
const singleIsleImage = computed(() => isles.value[currentSingleIsleIndex.value]?.image_url || '')
const nextSingleIsle = () => { 
  if (isles.value.length > 0) {
    currentSingleIsleIndex.value = (currentSingleIsleIndex.value + 1) % isles.value.length 
  }
}
const prevSingleIsle = () => { 
  if (isles.value.length > 0) {
    currentSingleIsleIndex.value = (currentSingleIsleIndex.value - 1 + isles.value.length) % isles.value.length 
  }
}

// 预加载所有图片
const preloadAllImages = async (): Promise<void> => {
  if (isles.value.length === 0) return

  const imagePromises = isles.value.map((isle) => {
    return new Promise<void>((resolve) => {
      const img = new Image()
      img.onload = () => resolve()
      img.onerror = () => resolve() // 即使加载失败也继续
      img.src = isle.image_url
    })
  })

  try {
    await Promise.all(imagePromises)
    console.log('所有岛屿图片预加载完成')
    allImagesLoaded.value = true
    singleIsleLoading.value = false

    // 设置所有网格图片为已加载状态
    isles.value.forEach(isle => {
      gridImageLoading[isle.id] = false
    })
  } catch (error) {
    console.warn('图片预加载失败:', error)
    singleIsleLoading.value = false

    // 即使预加载失败，也设置网格图片为已加载状态
    isles.value.forEach(isle => {
      gridImageLoading[isle.id] = false
    })
  }
}

// 监听岛屿数据变化，启动自动轮播和预加载
watch(() => isles.value, async (newIsles) => {
  if (newIsles.length > 0) {
    // 初始化网格加载状态
    newIsles.forEach(isle => {
      gridImageLoading[isle.id] = true
    })

    // 预加载所有图片
    await preloadAllImages()

    // 如果有多张图片，启动自动轮播
    if (newIsles.length > 1) {
      startAutoPlay()
    }
  } else {
    stopAutoPlay()
  }
}, { immediate: true })

// B. 三岛屿静态分页
const maxThreeIslePage = computed(() => Math.max(0, Math.ceil(isles.value.length / 3) - 1))
const threeIslesForCurrentPage = computed(() => {
  const start = threeIsleCurrentPage.value * 3;
  const end = start + 3;
  return isles.value.slice(start, end);
})
const nextThreeIslePage = () => { 
  if (maxThreeIslePage.value > 0) {
    threeIsleCurrentPage.value = (threeIsleCurrentPage.value + 1) % (maxThreeIslePage.value + 1) 
  }
}
const prevThreeIslePage = () => { 
  if (maxThreeIslePage.value > 0) {
    threeIsleCurrentPage.value = (threeIsleCurrentPage.value - 1 + (maxThreeIslePage.value + 1)) % (maxThreeIslePage.value + 1) 
  }
}

// 统一的切换函数
const next = () => {
  if (currentScreenMode.value === 'single') nextSingleIsle()
  else if (currentScreenMode.value === 'three') nextThreeIslePage()
}
const prev = () => {
  if (currentScreenMode.value === 'single') prevSingleIsle()
  else if (currentScreenMode.value === 'three') prevThreeIslePage()
}

// 检测当前屏幕模式
const checkScreenMode = () => {
  if (typeof window === 'undefined') return
  if (window.innerWidth >= 1024) {
    currentScreenMode.value = 'single'
  } else {
    currentScreenMode.value = 'grid'
  }
}

// 图片加载错误处理
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120"%3E%3Ccircle cx="60" cy="60" r="50" fill="url(%23gradient1)" /%3E%3Cdefs%3E%3ClinearGradient id="gradient1" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" style="stop-color:%23FFB6C1;stop-opacity:1" /%3E%3Cstop offset="50%25" style="stop-color:%23DDA0DD;stop-opacity:1" /%3E%3Cstop offset="100%25" style="stop-color:%2387CEEB;stop-opacity:1" /%3E%3C/linearGradient%3E%3C/defs%3E%3Cellipse cx="60" cy="30" rx="35" ry="12" fill="%23FF6B6B" opacity="0.8" /%3E%3Cellipse cx="60" cy="85" rx="25" ry="12" fill="%237CB342" /%3E%3Crect x="59" y="72" width="2" height="15" fill="%238D6E63" /%3E%3Ccircle cx="60" cy="69" r="4" fill="%2366BB6A" /%3E%3Ctext x="60" y="100" text-anchor="middle" fill="%23666" font-size="8"%3E岛屿%3C/text%3E%3C/svg%3E'
  
  // 错误时也要取消加载状态
  const imgElement = event.target as HTMLImageElement
  const altText = imgElement.alt
  if (altText.includes('岛屿')) {
    // 从alt文本中提取岛屿ID或判断是单岛屿模式
    if (currentScreenMode.value === 'single') {
      singleIsleLoading.value = false
    } else {
      // 网格模式下通过src匹配找到对应的岛屿ID
      const matchedIsle = isles.value.find(isle => imgElement.src.includes(isle.image_url) || imgElement.src === isle.image_url)
      if (matchedIsle) {
        gridImageLoading[matchedIsle.id] = false
      }
    }
  }
}

// 自动轮播定时器
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

// 启动自动轮播
const startAutoPlay = () => {
  if (autoPlayTimer) clearInterval(autoPlayTimer)
  autoPlayTimer = setInterval(() => {
    if (isles.value.length > 1) {
      next()
    }
  }, 30000) // 30秒切换一次
}

// 停止自动轮播
const stopAutoPlay = () => {
  if (autoPlayTimer) {
    clearInterval(autoPlayTimer)
    autoPlayTimer = null
  }
}

// 组件挂载时加载岛屿数据
onMounted(async () => {
  try {
    await islandStore.loadUserIslands()
    console.log('岛屿数据加载完成')
  } catch (error) {
    console.warn('岛屿数据加载失败:', error)
  }

  // 检测屏幕模式并添加监听器
  checkScreenMode()
  window.addEventListener('resize', checkScreenMode)
})

// 在组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', checkScreenMode)
  stopAutoPlay()
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style> 