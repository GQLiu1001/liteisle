import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { API } from '@/utils/api'
import { useToast } from 'vue-toastification'
import { useIslandStore } from './IslandStore'
import type {
  FocusStatsPageResp,
  FocusCalendarResp,
  FocusRecordItem
} from '@/types/api'

export const useFocusStore = defineStore('focus', () => {
  const toast = useToast()
  const islandStore = useIslandStore()
  
  // === 专注统计状态 ===
  const totalFocusCount = ref(0)          // 总专注次数
  const isLoading = ref(false)
  const lastUpdated = ref<Date | null>(null)
  
  // === 专注记录 ===
  const focusRecords = ref<FocusRecordItem[]>([])
  const recordsPagination = ref({
    total: 0,
    current_page: 1,
    page_size: 10,
    hasMore: true
  })
  
  // === 专注日历数据 ===
  const calendarData = ref<FocusCalendarResp | null>(null)
  const currentCalendarDate = ref(new Date())
  
  // === 当前专注会话状态 ===
  const isFocusing = ref(false)
  const focusStartTime = ref<Date | null>(null)
  const focusElapsedTime = ref(0)           // 已专注时间（秒）
  const focusTargetTime = ref(25 * 60)      // 目标专注时间（秒），默认25分钟
  const focusTimer = ref<number | null>(null)
  
  // === 计算属性 ===
  const focusProgress = computed(() => {
    if (focusTargetTime.value === 0) return 0
    return Math.min((focusElapsedTime.value / focusTargetTime.value) * 100, 100)
  })
  
  const focusRemaining = computed(() => {
    return Math.max(focusTargetTime.value - focusElapsedTime.value, 0)
  })
  
  const focusElapsedMinutes = computed(() => {
    return Math.floor(focusElapsedTime.value / 60)
  })
  
  const focusTargetMinutes = computed(() => {
    return Math.floor(focusTargetTime.value / 60)
  })
  
  const hasRecords = computed(() => focusRecords.value.length > 0)
  
  const todayFocusTime = computed(() => {
    if (!calendarData.value) return 0
    
    const today = new Date()
    const todayDate = today.getDate()
    const currentMonth = calendarData.value.year_month
    const expectedMonth = `${today.getFullYear()}-${today.getMonth() + 1}`
    
    if (currentMonth === expectedMonth && calendarData.value.check_in_days && calendarData.value.check_in_days.includes(todayDate)) {
      // 如果今天有签到，返回今天的专注时间
      // 这里简化处理，实际应该从API获取今天的具体专注时间
      return Math.floor(calendarData.value.total_focus_minutes / (calendarData.value.check_in_days.length || 1))
    }
    
    return 0
  })
  
  /**
   * 加载专注总次数
   */
  const loadTotalFocusCount = async (): Promise<void> => {
    try {
      isLoading.value = true
      const response = await API.focus.getTotalCount()

      console.log('🔢 专注次数API响应:', response)

      // 处理嵌套的API响应结构
      if (response.data !== undefined) {
        const apiResponse = response.data
        console.log('🔢 API响应数据:', apiResponse)

        // 检查是否是标准的ApiResponse格式
        if (typeof apiResponse === 'object' && apiResponse.code === 200 && apiResponse.data !== undefined) {
          totalFocusCount.value = typeof apiResponse.data === 'number' ? apiResponse.data : 0
          console.log('🔢 设置专注次数:', apiResponse.data)
        } else if (typeof apiResponse === 'number') {
          // 如果直接返回数字
          totalFocusCount.value = apiResponse
          console.log('🔢 设置专注次数(直接):', apiResponse)
        } else {
          totalFocusCount.value = 0
        }
      }
    } catch (error) {
      console.error('加载专注总次数失败:', error)
      toast.error('加载专注统计失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 加载专注记录
   */
  const loadFocusRecords = async (reset = false): Promise<void> => {
    try {
      isLoading.value = true

      const page = reset ? 1 : recordsPagination.value.current_page
      const response = await API.focus.getRecords(page, recordsPagination.value.page_size)

      console.log('专注记录API响应:', response)

      if (response.data && response.data.code === 200 && response.data.data) {
        const recordsData = response.data.data
        console.log('专注记录数据:', recordsData)

        if (reset) {
          focusRecords.value = recordsData.records || []
          recordsPagination.value.current_page = 1
        } else {
          focusRecords.value.push(...(recordsData.records || []))
        }

        recordsPagination.value.total = recordsData.total || 0
        recordsPagination.value.hasMore = focusRecords.value.length < (recordsData.total || 0)
        lastUpdated.value = new Date()
      } else {
        console.warn('专注记录API响应格式错误:', response.data)
        toast.error(response.data?.message || '加载专注记录失败')
      }
    } catch (error) {
      console.error('加载专注记录失败:', error)
      toast.error('加载专注记录失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 加载更多专注记录
   */
  const loadMoreRecords = async (): Promise<void> => {
    if (!recordsPagination.value.hasMore || isLoading.value) {
      return
    }
    
    recordsPagination.value.current_page++
    await loadFocusRecords()
  }
  
  /**
   * 加载专注日历数据
   */
  const loadFocusCalendar = async (year?: number, month?: number): Promise<void> => {
    try {
      isLoading.value = true

      const targetDate = new Date(year || currentCalendarDate.value.getFullYear(),
                                  (month || currentCalendarDate.value.getMonth() + 1) - 1)

      const response = await API.focus.getCalendar(
        targetDate.getFullYear(),
        targetDate.getMonth() + 1
      )

      console.log('📅 专注日历API响应:', response)

      // 处理嵌套的API响应结构
      if (response.data) {
        const apiResponse = response.data
        console.log('📅 API响应数据:', apiResponse)

        // 检查是否是标准的ApiResponse格式
        if (apiResponse.code === 200 && apiResponse.data) {
          calendarData.value = apiResponse.data
          console.log('📅 设置日历数据:', apiResponse.data)
        } else if (apiResponse.year_month) {
          // 如果直接返回日历数据
          calendarData.value = apiResponse
          console.log('📅 设置日历数据(直接):', apiResponse)
        } else {
          calendarData.value = null
        }
        currentCalendarDate.value = targetDate
      }
    } catch (error) {
      console.error('加载专注日历失败:', error)
      toast.error('加载专注日历失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 开始专注
   */
  const startFocus = (targetMinutes = 25): void => {
    if (isFocusing.value) return
    
    focusTargetTime.value = targetMinutes * 60
    focusElapsedTime.value = 0
    focusStartTime.value = new Date()
    isFocusing.value = true
    
    // 开始计时器
    focusTimer.value = window.setInterval(() => {
      focusElapsedTime.value++
      
      // 检查是否完成
      if (focusElapsedTime.value >= focusTargetTime.value) {
        completeFocus()
      }
    }, 1000)
    
    toast.success('开始专注')
  }
  
  /**
   * 暂停专注
   */
  const pauseFocus = (): void => {
    if (!isFocusing.value) return
    
    isFocusing.value = false
    
    if (focusTimer.value) {
      clearInterval(focusTimer.value)
      focusTimer.value = null
    }
    
    toast.info('专注已暂停')
  }
  
  /**
   * 恢复专注
   */
  const resumeFocus = (): void => {
    if (isFocusing.value) return
    
    isFocusing.value = true
    
    // 重新开始计时器
    focusTimer.value = window.setInterval(() => {
      focusElapsedTime.value++
      
      // 检查是否完成
      if (focusElapsedTime.value >= focusTargetTime.value) {
        completeFocus()
      }
    }, 1000)
    
    toast.success('专注已恢复')
  }
  
  /**
   * 停止专注
   */
  const stopFocus = (): void => {
    if (!isFocusing.value) return
    
    isFocusing.value = false
    
    if (focusTimer.value) {
      clearInterval(focusTimer.value)
      focusTimer.value = null
    }
    
    // 重置状态
    focusElapsedTime.value = 0
    focusStartTime.value = null
    
    toast.info('专注已停止')
  }
  
  /**
   * 完成专注
   */
  const completeFocus = async (): Promise<void> => {
    if (!isFocusing.value) return
    
    const completedMinutes = focusElapsedMinutes.value
    
    // 停止计时器
    isFocusing.value = false
    if (focusTimer.value) {
      clearInterval(focusTimer.value)
      focusTimer.value = null
    }
    
    try {
      // 记录专注时长到后端
      const response = await API.focus.recordFocus(completedMinutes)

      console.log('记录专注API响应:', response)

      if (response.data && response.data.code === 200) {
        // 如果获得了新岛屿
        const islandUrl = response.data.data
        if (islandUrl) {
          islandStore.addIsland(islandUrl)
        }

        // 更新本地统计
        totalFocusCount.value++

        // 刷新数据
        await Promise.all([
          loadFocusRecords(true),
          loadFocusCalendar(),
          loadTotalFocusCount()
        ])

        toast.success(`🎉 专注完成！用时 ${completedMinutes} 分钟`)
      } else {
        console.warn('记录专注API响应格式错误:', response.data)
        toast.error(response.data?.message || '记录专注失败')
      }
    } catch (error) {
      console.error('记录专注时长失败:', error)
      toast.error('记录专注失败')
    }
    
    // 重置状态
    focusElapsedTime.value = 0
    focusStartTime.value = null
  }
  
  /**
   * 设置专注目标时间
   */
  const setFocusTarget = (minutes: number): void => {
    if (minutes <= 0) return
    
    focusTargetTime.value = minutes * 60
    
    // 如果正在专注中，需要重新计算进度
    if (isFocusing.value) {
      toast.info(`目标时间已调整为 ${minutes} 分钟`)
    }
  }
  
  /**
   * 格式化时间显示
   */
  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  
  /**
   * 格式化专注时长
   */
  const formatFocusTime = (minutes: number): string => {
    if (minutes < 60) {
      return `${minutes}分钟`
    }
    
    const hours = Math.floor(minutes / 60)
    const remainingMinutes = minutes % 60
    
    if (remainingMinutes === 0) {
      return `${hours}小时`
    }
    
    return `${hours}小时${remainingMinutes}分钟`
  }
  
  /**
   * 获取专注连续天数
   */
  const getFocusStreak = (): number => {
    if (!calendarData.value || calendarData.value.check_in_days.length === 0) {
      return 0
    }
    
    const today = new Date()
    const currentDay = today.getDate()
    const checkInDays = [...calendarData.value.check_in_days].sort((a, b) => b - a) // 降序排列
    
    let streak = 0
    let expectedDay = currentDay
    
    for (const day of checkInDays) {
      if (day === expectedDay) {
        streak++
        expectedDay--
      } else {
        break
      }
    }
    
    return streak
  }
  
  /**
   * 切换到上个月
   */
  const gotoPreviousMonth = async (): Promise<void> => {
    const prevMonth = new Date(currentCalendarDate.value)
    prevMonth.setMonth(prevMonth.getMonth() - 1)
    await loadFocusCalendar(prevMonth.getFullYear(), prevMonth.getMonth() + 1)
  }
  
  /**
   * 切换到下个月
   */
  const gotoNextMonth = async (): Promise<void> => {
    const nextMonth = new Date(currentCalendarDate.value)
    nextMonth.setMonth(nextMonth.getMonth() + 1)
    await loadFocusCalendar(nextMonth.getFullYear(), nextMonth.getMonth() + 1)
  }
  
  /**
   * 回到当前月
   */
  const gotoCurrentMonth = async (): Promise<void> => {
    const now = new Date()
    await loadFocusCalendar(now.getFullYear(), now.getMonth() + 1)
  }
  
  /**
   * 重置状态
   */
  const reset = (): void => {
    // 停止专注
    stopFocus()
    
    // 重置数据
    totalFocusCount.value = 0
    focusRecords.value = []
    calendarData.value = null
    lastUpdated.value = null
    
    // 重置分页
    recordsPagination.value = {
      total: 0,
      current_page: 1,
      page_size: 10,
      hasMore: true
    }
    
    // 重置日期
    currentCalendarDate.value = new Date()
  }
  
  return {
    // === 统计状态 ===
    totalFocusCount,
    isLoading,
    lastUpdated,
    
    // === 记录数据 ===
    focusRecords,
    recordsPagination,
    calendarData,
    currentCalendarDate,
    
    // === 专注会话状态 ===
    isFocusing,
    focusStartTime,
    focusElapsedTime,
    focusTargetTime,
    
    // === 计算属性 ===
    focusProgress,
    focusRemaining,
    focusElapsedMinutes,
    focusTargetMinutes,
    hasRecords,
    todayFocusTime,
    
    // === 数据加载方法 ===
    loadTotalFocusCount,
    loadFocusRecords,
    loadMoreRecords,
    loadFocusCalendar,
    
    // === 专注控制方法 ===
    startFocus,
    pauseFocus,
    resumeFocus,
    stopFocus,
    completeFocus,
    setFocusTarget,
    
    // === 日历导航方法 ===
    gotoPreviousMonth,
    gotoNextMonth,
    gotoCurrentMonth,
    
    // === 工具方法 ===
    formatTime,
    formatFocusTime,
    getFocusStreak,
    reset
  }
}) 