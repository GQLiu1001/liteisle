<template>
  <div class="card h-full flex flex-col min-h-0 overflow-hidden w-full p-3 lg:p-4">
    <!-- 顶部区域：专注记录标题，占15%高度 -->
    <div class="flex items-center justify-between flex-shrink-0 h-[15%] min-h-[40px] max-h-[60px]">
      <h3 class="text-base lg:text-lg font-semibold text-morandi-900">专注记录</h3>
      <span class="text-xs lg:text-sm text-morandi-600">{{ currentMonthName }}</span>
    </div>
    
    <!-- 中间日历区域，占70%高度 -->
    <div class="flex-1 min-h-0 overflow-hidden h-[70%]">
      <!-- 星期标题 -->
      <div class="grid grid-cols-7 gap-0.5 mb-1 flex-shrink-0">
        <div v-for="day in weekDays" :key="day" 
             class="text-xs text-morandi-500 text-center py-1">
          {{ day }}
        </div>
      </div>
      
      <!-- 日期网格 - 更小的方格 -->
      <div class="grid grid-cols-7 gap-0.5 h-[calc(100%-2rem)]">
        <div v-for="(day, index) in calendarDays" :key="index"
             :class="[
               'aspect-square flex items-center justify-center rounded-md text-xs font-medium relative',
               'min-h-[20px] max-h-[32px] w-full',
               day.date ? 'cursor-pointer transition-colors' : '',
               day.date && day.isToday ? 'ring-1 ring-blue-400' : '',
               day.date && day.hasFocus ? 'bg-blue-400 text-white' : day.date ? 'bg-morandi-100 text-morandi-700 hover:bg-morandi-200' : 'bg-transparent'
             ]">
          <span v-if="day.date" class="relative z-10 text-xs">{{ day.date }}</span>
          <!-- 专注时长提示 - 更小的指示器 -->
          <div v-if="day.date && day.focusMinutes > 0" 
               class="absolute bottom-0.5 right-0.5">
            <div class="w-1 h-1 bg-green-400 rounded-full" 
                 :title="`专注${day.focusMinutes}分钟`"></div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 底部统计区域，占15%高度 -->
    <div class="flex-shrink-0 h-[15%] min-h-[40px] max-h-[80px] pt-2 border-t border-morandi-200">
      <div class="flex items-center justify-between text-xs text-morandi-600 mb-1">
        <span>本月签到: {{ monthlyStats.checkedDays }}天</span>
        <span>总专注: {{ Math.round(monthlyStats.totalMinutes/60) }}小时</span>
      </div>
      <!-- 图例 - 更紧凑 -->
      <div class="flex items-center justify-center gap-3 text-xs text-morandi-500">
        <div class="flex items-center gap-1">
          <div class="w-2 h-2 bg-morandi-100 rounded-sm border"></div>
          <span>未签到</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="w-2 h-2 bg-blue-400 rounded-sm"></div>
          <span>已签到</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useFocusStore } from '@/store/FocusStore'

const focusStore = useFocusStore()

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// 当前月份名称
const currentMonthName = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月`
})

// 生成当前月份的日历数据
const calendarDays = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  
  // 本月第一天
  const firstDay = new Date(year, month, 1)
  // 本月最后一天
  const lastDay = new Date(year, month + 1, 0)
  // 第一天是星期几（0=周日）
  const firstDayOfWeek = firstDay.getDay()
  // 本月有多少天
  const daysInMonth = lastDay.getDate()
  
  const days = []
  
  // 添加前面的空白天数
  for (let i = 0; i < firstDayOfWeek; i++) {
    days.push({ date: null, hasFocus: false, focusMinutes: 0, isToday: false })
  }
  
  // 添加本月的所有天数
  for (let date = 1; date <= daysInMonth; date++) {
    const isToday = date === now.getDate()
    
    // 检查这一天是否有专注记录
    const hasFocus = focusStore.calendarData?.check_in_days?.includes(date) || false
    
    days.push({
      date,
      hasFocus,
      focusMinutes: hasFocus ? 30 : 0, // 简化：有记录就显示30分钟
      isToday
    })
  }
  
  return days
})

// 组件挂载时加载日历数据
onMounted(async () => {
  try {
    await focusStore.loadFocusCalendar()
  } catch (error) {
    console.warn('加载专注日历失败:', error)
  }
})

// 本月统计
const monthlyStats = computed(() => {
  if (!focusStore.calendarData || !focusStore.calendarData.check_in_days) {
    return {
      checkedDays: 0,
      totalMinutes: 0
    }
  }
  
  const checkedDays = focusStore.calendarData.check_in_days.length
  const totalMinutes = focusStore.calendarData.total_focus_minutes
  
  return {
    checkedDays,
    totalMinutes
  }
})
</script>

<style scoped>
/* 确保日历网格紧凑对齐 */
.grid {
  gap: 0.125rem; /* 更小的间隙 */
}

/* 确保网格均匀分布 */
.grid-cols-7 {
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .grid {
    gap: 0.25rem;
  }
}

/* 确保在最小窗口尺寸下的适配 */
@media (min-width: 1200px) and (min-height: 800px) {
  .aspect-square {
    min-height: 24px;
    max-height: 36px;
  }
}

/* 小屏幕优化 */
@media (max-width: 768px) {
  .aspect-square {
    min-height: 18px;
    max-height: 28px;
  }
}
</style>

 