import { defineStore } from 'pinia'
import { ref, computed, shallowRef } from 'vue'
import { Settings, User, FileText, Clock, Share2, Image } from 'lucide-vue-next'
import { API } from '@/utils/api'
import { useToast } from 'vue-toastification'
import type { ShareRecordItem, FocusRecordItem } from '@/types/api'

// 设置接口定义
interface AppSettings {
  // 通用设置
  launchAtStartup: boolean
  downloadDirectory: string
  language: string
  defaultPage: string
  
  // 专注设置
  focusTime: number
  breakTime: number
  soundEnabled: boolean
  soundType: string
  autoNextRound: boolean
  
  // 外观设置
  themeMode: 'light' | 'dark' | 'system'
  accentColor: string
  windowOpacity: number
  
  // 账户设置
  username: string
  avatar: string
  storageUsed: number
  storageTotal: number
  
  // PicGo设置
  picgoEnabled: boolean
  picgoPath: string
}

export interface SettingCategory {
  id: string
  name: string
  description: string
  icon: any
}

export interface ShareItem {
  id: number
  name: string
  type: 'file' | 'folder'
  shareToken: string // 分享令牌，而非完整URL
  sharePassword: string // 访问密码
  expiryDate: string
  status: 'active' | 'expired' | 'disabled'
  createdAt: string
  accessCount: number
}

export interface ShareRecord {
  id: number
  name: string
  type: 'file' | 'folder'
  shareToken: string
  sharePassword: string
  expiryDate: string
  status: 'active' | 'expired' | 'disabled'
  createdAt: string
  accessCount: number
}

export interface FocusRecord {
  id: number
  date: string
  focusTime: number
  breakTime: number
  totalTime: number
  createdAt: string
}

export const useSettingsStore = defineStore('settings', () => {
  const toast = useToast()
  // 状态
  const currentCategoryId = ref('general')
  
  const settings = ref<AppSettings>({
    launchAtStartup: false,
    downloadDirectory: 'C:\\Users\\Public\\Downloads',
    language: 'zh-CN',
    defaultPage: 'home',
    focusTime: 25,
    breakTime: 5,
    soundEnabled: true,
    soundType: 'bell',
    autoNextRound: false,
    themeMode: 'light',
    accentColor: '#14B8A6',
    windowOpacity: 100,
    username: 'User',
    avatar: '',
    storageUsed: 2.1 * 1024 * 1024 * 1024, // 2.1GB
    storageTotal: 5 * 1024 * 1024 * 1024,   // 5GB
    picgoEnabled: false,
    picgoPath: ''
  })

  // 云盘信息
  const cloudStorage = ref({
    used: 2.1,  // 已使用 2.1GB
    total: 5    // 总容量 5GB
  })

  // === 分享管理状态 ===
  const shareRecords = ref<ShareRecordItem[]>([])
  const shareCurrentPage = ref(1)
  const sharePageSize = ref(10)
  const shareTotal = ref(0)
  const shareHasMore = ref(true)
  const isLoadingShare = ref(false)

  // === 专注记录状态 ===  
  const focusRecords = ref<FocusRecordItem[]>([])
  const focusCurrentPage = ref(1)
  const focusPageSize = ref(10)
  const focusTotal = ref(0)
  const hasMoreRecords = ref(true)
  const isLoadingFocus = ref(false)

  // 设置分类
  const categories = ref<SettingCategory[]>([
    {
      id: 'general',
      name: '通用设置',
      description: '基本功能设置',
      icon: shallowRef(Settings)
    },
    {
      id: 'account',
      name: '账户与云盘',
      description: '账户信息和云盘管理',
      icon: shallowRef(User)
    },
    {
      id: 'picgo',
      name: '图片上传',
      description: 'PicGo图片上传设置',
      icon: shallowRef(Image)
    },
    {
      id: 'shares',
      name: '我的分享',
      description: '分享文件管理',
      icon: shallowRef(Share2)
    },
    {
      id: 'focus',
      name: '专注记录',
      description: '专注历史和统计',
      icon: shallowRef(Clock)
    },
    {
      id: 'about',
      name: '关于',
      description: '应用信息和版本',
      icon: shallowRef(FileText)
    }
  ])

  // 计算属性
  const currentCategory = computed(() => {
    return categories.value.find(cat => cat.id === currentCategoryId.value) || categories.value[0]
  })

  const storageInfo = computed(() => {
    if (cloudStorage.value.total === 0) {
      return { percentage: 0, text: 'N/A' }
    }
    const percentage = (cloudStorage.value.used / cloudStorage.value.total) * 100
    const text = `已使用 ${cloudStorage.value.used.toFixed(1)} GB / ${cloudStorage.value.total} GB`
    return { percentage, text }
  })

  // 方法
  const setCurrentCategoryId = (categoryId: string) => {
    currentCategoryId.value = categoryId
  }

  const updateSetting = <K extends keyof AppSettings>(key: K, value: AppSettings[K]) => {
    settings.value[key] = value
  }

  const saveSettings = () => {
    try {
      localStorage.setItem('liteisle-settings', JSON.stringify(settings.value))
    } catch (error) {
      console.error('Failed to save settings:', error)
    }
  }

  const getDefaultPicGoPath = async () => {
    try {
      // 使用 Electron API 获取系统用户名
      const username = await (window as any).electronAPI?.getUsername?.() || 'YourUsername'
      return `C:\\Users\\${username}\\AppData\\Local\\Programs\\PicGo\\PicGo.exe`
    } catch (error) {
      console.error('获取用户名失败:', error)
      return 'C:\\Users\\YourUsername\\AppData\\Local\\Programs\\PicGo\\PicGo.exe'
    }
  }

  const loadSettings = async () => {
    try {
      const savedSettings = localStorage.getItem('liteisle-settings')
      if (savedSettings) {
        const parsedSettings = JSON.parse(savedSettings)
        Object.assign(settings.value, parsedSettings)
        
        // 如果 picgoPath 为空，设置默认路径
        if (!settings.value.picgoPath) {
          settings.value.picgoPath = await getDefaultPicGoPath()
        }
      } else {
        // 首次加载时设置默认路径
        settings.value.picgoPath = await getDefaultPicGoPath()
      }
    } catch (error) {
      console.error('Failed to load settings:', error)
      // 即使出错也设置默认路径
      if (!settings.value.picgoPath) {
        settings.value.picgoPath = await getDefaultPicGoPath()
      }
    }
  }

  const logout = () => {
    // 清除用户登录状态
    localStorage.removeItem('liteisle-user')
    localStorage.removeItem('liteisle-settings')
    
    // 重定向到登录页
    window.location.href = '#/login'
  }

  // 检查更新（调用真实API）
  const checkForUpdates = async (): Promise<boolean> => {
    try {
      // 调用真实的更新检查API
      console.log('检查更新...')
      // const response = await API.system.checkUpdate()
      // return response.data?.hasUpdate || false
      return false // 暂时返回false，等待API实现
    } catch (error) {
      console.error('检查更新失败:', error)
      return false
    }
  }

  // 修改密码（调用真实API）
  const changePassword = async (currentPassword: string, newPassword: string): Promise<boolean> => {
    try {
      const response = await API.auth.changePassword({
        current_password: currentPassword,
        new_password: newPassword
      })
      toast.success('密码修改成功')
      return true
    } catch (error) {
      console.error('密码修改失败:', error)
      toast.error('密码修改失败')
      return false
    }
  }

  // 取消分享（调用真实API）
  const cancelShare = async (shareId: number): Promise<boolean> => {
    try {
      await API.share.cancel(shareId)
      toast.success('分享已取消')
      
      // 从本地列表中移除
      const index = shareRecords.value.findIndex(item => item.id === shareId)
      if (index > -1) {
        shareRecords.value.splice(index, 1)
        shareTotal.value = Math.max(0, shareTotal.value - 1)
      }
      
      return true
    } catch (error) {
      console.error('取消分享失败:', error)
      toast.error('取消分享失败')
      return false
    }
  }

  /**
   * 加载分享记录（从API获取）
   */
  const loadShareRecords = async (page: number = 1): Promise<void> => {
    try {
      isLoadingShare.value = true
      console.log('开始加载分享记录，页码:', page)
      
      const response = await API.share.getMyShares(page, sharePageSize.value)
      console.log('分享记录API响应:', response)
      
      if (response.data && response.data.data) {
        const apiData = response.data.data

        if (page === 1) {
          shareRecords.value = apiData.records || []
        } else {
          shareRecords.value.push(...(apiData.records || []))
        }

        shareCurrentPage.value = page
        shareTotal.value = apiData.total || 0
        shareHasMore.value = shareRecords.value.length < (apiData.total || 0)
      }
    } catch (error) {
      console.error('加载分享记录失败:', error)
      toast.error('加载分享记录失败')
    } finally {
      isLoadingShare.value = false
    }
  }

  /**
   * 加载专注记录（从API获取）
   */
  const loadFocusRecords = async (page: number = 1): Promise<void> => {
    try {
      isLoadingFocus.value = true
      console.log('开始加载专注记录，页码:', page)

      const response = await API.focus.getRecords(page, focusPageSize.value)
      console.log('专注记录API响应:', response)

      if (response.data && response.data.code === 200 && response.data.data) {
        const recordsData = response.data.data
        console.log('专注记录数据:', recordsData)

        if (page === 1) {
          focusRecords.value = recordsData.records || []
        } else {
          focusRecords.value.push(...(recordsData.records || []))
        }

        focusCurrentPage.value = page
        focusTotal.value = recordsData.total || 0
        hasMoreRecords.value = focusRecords.value.length < (recordsData.total || 0)
      } else {
        console.warn('专注记录API响应格式错误:', response.data)
        toast.error(response.data?.message || '加载专注记录失败')
      }
    } catch (error) {
      console.error('加载专注记录失败:', error)
      toast.error('加载专注记录失败')
    } finally {
      isLoadingFocus.value = false
    }
  }

  // 加载更多分享数据
  const loadMoreShareItems = async () => {
    if (!shareHasMore.value || isLoadingShare.value) return
    await loadShareRecords(shareCurrentPage.value + 1)
  }

  // 刷新分享数据
  const refreshShareItems = async () => {
    await loadShareRecords(1)
  }

  // 生成分享链接的方法
  const generateShareLink = (shareToken: string) => {
    return `https://liteisle.com/share/${shareToken}`
  }

  // 复制分享信息（链接+密码）
  const copyShareInfo = async (shareToken: string, sharePassword: string) => {
    const shareLink = generateShareLink(shareToken)
    const shareInfo = `分享链接: ${shareLink}\n访问密码: ${sharePassword}`
    
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(shareInfo)
        return true
      } else {
        // 降级方案
        const textArea = document.createElement('textarea')
        textArea.value = shareInfo
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
        return true
      }
    } catch (error) {
      console.error('复制失败:', error)
      return false
    }
  }

  return {
    // 状态
    currentCategoryId,
    settings,
    cloudStorage,
    categories,
    shareRecords,
    isLoadingShare,
    shareCurrentPage,
    shareTotal,
    shareHasMore,
    focusRecords,
    isLoadingFocus,
    focusCurrentPage,
    focusTotal,
    hasMoreRecords,
    
    // 计算属性
    currentCategory,
    storageInfo,
    
    // 方法
    setCurrentCategoryId,
    updateSetting,
    saveSettings,
    loadSettings,
    logout,
    checkForUpdates,
    changePassword,
    loadShareRecords,
    loadFocusRecords,
    loadMoreShareItems,
    refreshShareItems,
    cancelShare,
    generateShareLink,
    copyShareInfo
  }
}) 