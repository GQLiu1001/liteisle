import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { API } from '@/utils/api'
import { useToast } from 'vue-toastification'

export const useIslandStore = defineStore('island', () => {
  const toast = useToast()
  
  // 响应式状态
  const userIslands = ref<string[]>([])
  const isLoading = ref(false)
  const lastUpdated = ref<Date | null>(null)
  
  // 计算属性
  const islandCount = computed(() => userIslands.value.length)
  const hasIslands = computed(() => userIslands.value.length > 0)
  
  // 岛屿只有图片URL，没有名字和描述
  // 这些图片URL通过后端专注奖励系统返回
  
  /**
   * 加载用户岛屿收集情况
   */
  const loadUserIslands = async (): Promise<void> => {
    try {
      isLoading.value = true
      const response = await API.island.getUserIslands()

      console.log('🏝️ 岛屿API响应:', response)

      // 处理嵌套的API响应结构
      if (response.data) {
        const apiResponse = response.data
        console.log('🏝️ API响应数据:', apiResponse)

        // 检查是否是标准的ApiResponse格式
        if (apiResponse.code === 200 && apiResponse.data) {
          userIslands.value = Array.isArray(apiResponse.data) ? apiResponse.data : []
          console.log('🏝️ 设置岛屿数据:', userIslands.value)
        } else if (Array.isArray(apiResponse)) {
          // 如果直接返回数组数据
          userIslands.value = apiResponse
          console.log('🏝️ 设置岛屿数据(直接):', userIslands.value)
        } else {
          userIslands.value = []
        }
        lastUpdated.value = new Date()
      }
    } catch (error) {
      console.error('加载岛屿收集情况失败:', error)
      toast.error('加载岛屿信息失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 添加新岛屿（通过专注奖励获得）
   */
  const addIsland = (islandUrl: string): void => {
    if (!userIslands.value.includes(islandUrl)) {
      userIslands.value.push(islandUrl)
      
      toast.success('🏝️ 恭喜获得新岛屿！', {
        timeout: 5000
      })
    }
  }
  
  /**
   * 检查是否拥有特定岛屿
   */
  const hasIsland = (islandUrl: string): boolean => {
    return userIslands.value.includes(islandUrl)
  }
  
  /**
   * 格式化岛屿收集进度
   */
  const getProgress = computed(() => {
    const collected = islandCount.value
    
    return {
      collected,
      // 由于不知道总数，只显示已收集的数量
      total: collected,
      percentage: 100 // 始终显示100%，因为我们不知道总共有多少岛屿
    }
  })
  
  /**
   * 重置状态
   */
  const reset = (): void => {
    userIslands.value = []
    isLoading.value = false
    lastUpdated.value = null
  }
  
  return {
    // 状态
    userIslands,
    isLoading,
    lastUpdated,
    
    // 计算属性
    islandCount,
    hasIslands,
    getProgress,
    
    // 方法
    loadUserIslands,
    addIsland,
    hasIsland,
    reset
  }
}) 