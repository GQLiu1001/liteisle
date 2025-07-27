<template>
  <div class="h-full bg-transparent flex flex-col overflow-hidden select-none">
    <!-- 使用与网盘页面相同的高度计算方式 -->
    <div class="flex-1 flex flex-col p-2 sm:p-4 lg:p-6 min-h-0">
      <div class="max-w-7xl mx-auto w-full h-[calc(100vh-10rem)] flex flex-col min-h-0">
        <!-- 顶部区域 - 问候语卡片和学习卡片响应式布局 -->
        <div class="flex flex-col lg:flex-row lg:items-stretch gap-4 lg:gap-6 mb-4 lg:mb-6 flex-shrink-0">
          <!-- 左侧 - 问候语卡片 -->
          <div class="flex-1 min-w-0 flex">
            <div class="card w-full flex flex-col justify-center relative overflow-hidden">
              <!-- 问候语和励志语 -->
              <div class="flex flex-col justify-center z-10 relative">
                <h2 class="text-2xl lg:text-3xl font-bold text-morandi-900 mb-4">{{ greeting }}！{{ userName }}</h2>
                <!-- 励志语区域 -->
                <div class="flex justify-start">
                  <div class="bg-transparent text-gray-900 text-2xl lg:text-3xl font-bold min-h-[120px] flex flex-col justify-center">
                    <div class="text-left leading-relaxed">
                      <div 
                        class="transition-opacity duration-1000 ease-in-out"
                        :class="{ 'opacity-0': isTransitioning, 'opacity-100': !isTransitioning }"
                      >
                        "{{ currentQuote.text }}"
                      </div>
                      <div 
                        class="text-right text-lg lg:text-xl font-medium text-gray-700 mt-2 transition-opacity duration-1000 ease-in-out"
                        :class="{ 'opacity-0': isTransitioning, 'opacity-100': !isTransitioning }"
                        v-if="currentQuote.author"
                      >
                        —— {{ currentQuote.author }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 右下角图片 -->
              <div class="absolute bottom-4 right-4 w-24 h-24 lg:w-32 lg:h-32 opacity-80">
                <img 
                  src="/cardpic/cardpic.png" 
                  alt="Card decoration" 
                  class="w-full h-full object-contain"
                />
              </div>
            </div>
          </div>
          
          <!-- 右侧 - 学习卡片 响应式宽度 -->
          <div class="w-full lg:w-[320px] flex-shrink-0 flex">
            <StudyCard />
          </div>
        </div>
        
        <!-- 主内容区域 - 弹性布局，防截断 -->
        <div class="flex flex-col lg:flex-row gap-4 lg:gap-6 flex-1 min-h-0 overflow-hidden">
          <!-- 岛屿卡片 - 弹性高度，不会被截断，增加最小宽度控制 -->
          <div class="flex-1 min-h-[200px] min-w-[320px] flex">
            <IsleCard />
          </div>
          
          <!-- 专注记录卡片 - 弹性高度，不会被截断，确保固定宽度 -->
          <div class="flex-1 lg:flex-none lg:w-[360px] min-h-[200px] min-w-[320px] flex">
            <ActivityGrid />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import StudyCard from '@/components/cards/StudyCard.vue'
import ActivityGrid from '@/components/cards/ActivityGrid.vue'
import IsleCard from '@/components/cards/IsleCard.vue'
import { useAuthStore } from '@/store/AuthStore'
import { useFocusStore } from '@/store/FocusStore'

const authStore = useAuthStore()
const focusStore = useFocusStore()

// 名言接口
interface Quote {
  text: string
  author?: string
}

const quotes: Quote[] = [
  // 原有励志语
  { text: '今天的努力，是明天的收获' },
  { text: '专注当下，成就未来' },
  { text: '每一次坚持，都是成长的印记' },
  { text: '静心致远，专注致胜' },
  { text: '小步前进，终达千里' },
  { text: '专注是最好的投资' },
  { text: '今日事，今日毕' },
  { text: '心无旁骛，方能致远' },
  { text: '持之以恒，水滴石穿' },
  { text: '专注的力量，无可限量' },
  { text: '每一分专注，都值得珍惜' },
  { text: '静下心来，世界更清晰' },
  { text: '专注是通往成功的捷径' },
  { text: '用心做事，必有收获' },
  { text: '专注当下，拥抱未来' },
  { text: '坚持不懈，梦想成真' },
  { text: '专注让平凡变得不凡' },
  { text: '一心一意，事半功倍' },
  { text: '专注是最美的姿态' },
  { text: '今天的专注，明天的骄傲' },


  // 谚语和格言
  { text: '宝剑锋从磨砺出，梅花香自苦寒来' },
  { text: '千里之行，始于足下' },
  { text: '滴水穿石，非一日之功' },
  { text: '冰冻三尺，非一日之寒' },
  { text: '不积跬步，无以至千里' },
  { text: '学如逆水行舟，不进则退' },
  { text: '一分耕耘，一分收获' },
  { text: '熟能生巧，勤能补拙' },
  { text: '世上无难事，只怕有心人' },
  { text: '功夫不负有心人' },
  { text: '山重水复疑无路，柳暗花明又一村' },
  { text: '海纳百川，有容乃大' },
  { text: '静以修身，俭以养德' },
  { text: '非学无以广才，非志无以成学' },
  { text: '学而不思则罔，思而不学则殆' }
]

// 当前显示的名言
const currentQuote = ref<Quote>({ text: '', author: '' })
const isTransitioning = ref(false)

// 切换间隔定时器
let quoteInterval: number | null = null

// 根据时间获取问候语
const getGreeting = () => {
  const hour = new Date().getHours()
  
  if (hour >= 5 && hour < 9) {
    return '早上好'
  } else if (hour >= 9 && hour < 12) {
    return '上午好'
  } else if (hour >= 12 && hour < 18) {
    return '下午好'
  } else {
    return '晚上好'
  }
}

// 问候语计算属性
const greeting = computed(() => getGreeting())

// 用户名计算属性
const userName = computed(() => {
  console.log('🔍 用户信息调试:', {
    user: authStore.user,
    username: authStore.user?.username,
    name: authStore.user?.name,
    user_name: authStore.user?.user_name,
    isAuthenticated: authStore.isAuthenticated
  })
  
  // 尝试多个可能的用户名字段
  return authStore.user?.username || 
         authStore.user?.name || 
         authStore.user?.user_name || 
         'Zen'
})

// 随机选择一句名言
const selectRandomQuote = () => {
  const randomIndex = Math.floor(Math.random() * quotes.length)
  return quotes[randomIndex]
}

// 切换名言的函数
const changeQuote = () => {
  isTransitioning.value = true
  
  setTimeout(() => {
    currentQuote.value = selectRandomQuote()
    isTransitioning.value = false
  }, 500) // 淡出完成后切换内容
}

// 组件挂载时初始化
onMounted(async () => {
  // 每次进入首页都刷新用户信息（获取最新的用户名、头像等）
  if (authStore.isAuthenticated) {
    await authStore.getCurrentUser()
  }
  
  // 加载今日名言
  currentQuote.value = selectRandomQuote()
  
  // 每30秒更换一次名言
  quoteInterval = setInterval(changeQuote, 30000)
  
  // 初始化专注数据
  try {
    await Promise.all([
      focusStore.loadTotalFocusCount(),
      focusStore.loadFocusCalendar()
    ])
    console.log('专注数据初始化完成')
  } catch (error) {
    console.warn('专注数据初始化失败:', error)
  }
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (quoteInterval) {
    clearInterval(quoteInterval)
  }
})
</script> 