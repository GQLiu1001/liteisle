<template>
  <div class="card h-full flex flex-col justify-center w-full">
    <div class="flex flex-col items-center text-center">
      <div class="w-16 h-16 lg:w-20 lg:h-20 bg-gradient-to-r from-teal-400 to-blue-500 rounded-3xl flex items-center justify-center mb-4 lg:mb-6">
        <Clock :size="28" class="text-white lg:hidden" />
        <Clock :size="36" class="text-white hidden lg:block" />
      </div>
      <div>
        <h3 class="text-lg lg:text-xl font-semibold text-morandi-900 mb-2 lg:mb-3">累计专注次数</h3>
        <div class="flex items-baseline justify-center mb-1">
          <span class="text-3xl lg:text-4xl font-bold text-morandi-800">{{ displayFocusCount }}</span>
          <span class="text-base lg:text-lg text-morandi-700 ml-1.5">次</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Clock } from 'lucide-vue-next'
import { useFocusStore } from '@/store/FocusStore'
import { storeToRefs } from 'pinia'

const focusStore = useFocusStore()
const { totalFocusCount } = storeToRefs(focusStore)

// 确保显示正确的数字，而不是JSON
const displayFocusCount = computed(() => {
  // 如果totalFocusCount是数字，直接返回
  if (typeof totalFocusCount.value === 'number') {
    return totalFocusCount.value
  }
  
  // 如果是字符串或其他类型，尝试解析
  if (typeof totalFocusCount.value === 'string') {
    const parsed = parseInt(totalFocusCount.value, 10)
    return isNaN(parsed) ? 0 : parsed
  }
  
  // 默认返回0
  return 0
})

// 今日学习进度（假设目标是2小时=120分钟）
const todayProgress = computed(() => {
  // 由于V5版本暂时没有当前学习时间，先返回0
  const todayTarget = 120 // 分钟
  const progress = 0 // 暂时设为0，等待V5版本实现
  return Math.round(progress)
})
</script> 