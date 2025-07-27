import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { API } from '@/utils/api'
import { useToast } from 'vue-toastification'
import type {
  ShareCreateReq,
  ShareCreateResp,
  ShareRecordItem,
  ShareRecordPageResp,
  ShareVerifyReq,
  ShareInfoResp,
  ShareSaveReq,
  ShareSaveAsyncResp
} from '@/types/api'

export const useShareStore = defineStore('share', () => {
  const toast = useToast()
  
  // 响应式状态
  const isLoading = ref(false)
  const myShares = ref<ShareRecordItem[]>([])
  const sharesPagination = ref({
    total: 0,
    currentPage: 1,
    pageSize: 10,
    hasMore: true
  })
  
  // 当前分享验证信息
  const verifiedShareInfo = ref<ShareInfoResp | null>(null)
  const isVerifying = ref(false)
  
  // 计算属性
  const totalShares = computed(() => sharesPagination.value.total)
  const hasShares = computed(() => myShares.value.length > 0)
  
  /**
   * 创建分享链接
   */
  const createShare = async (data: ShareCreateReq): Promise<ShareCreateResp | null> => {
    try {
      isLoading.value = true
      const response = await API.share.create(data)
      
      if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
        const shareData = (response.data as any).data
        toast.success('分享链接创建成功')
        
        // 复制到剪贴板
        const shareText = data.is_encrypted 
          ? `${shareData.share_token}&${shareData.share_password}`
          : shareData.share_token
        
        try {
          await navigator.clipboard.writeText(shareText)
          toast.info('分享信息已复制到剪贴板')
        } catch (error) {
          console.warn('复制到剪贴板失败:', error)
        }
        
        // 刷新我的分享列表
        await loadMyShares(true)
        
        return shareData
      }
      
      return null
    } catch (error) {
      console.error('创建分享失败:', error)
      toast.error('创建分享失败')
      return null
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 加载我的分享记录
   */
  const loadMyShares = async (reset = false): Promise<void> => {
    try {
      isLoading.value = true
      
      const page = reset ? 1 : sharesPagination.value.currentPage
      const response = await API.share.getMyShares(page, sharesPagination.value.pageSize)
      
      if (response.data) {
        if (reset) {
          myShares.value = response.data.records || []
          sharesPagination.value.currentPage = 1
        } else {
          myShares.value.push(...(response.data.records || []))
        }
        
        sharesPagination.value.total = response.data.total || 0
        sharesPagination.value.currentPage = page
        sharesPagination.value.hasMore = myShares.value.length < (response.data.total || 0)
      }
    } catch (error) {
      console.error('加载分享记录失败:', error)
      toast.error('加载分享记录失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 加载更多分享记录
   */
  const loadMoreShares = async (): Promise<void> => {
    if (!sharesPagination.value.hasMore || isLoading.value) {
      return
    }
    
    sharesPagination.value.currentPage++
    await loadMyShares()
  }
  
  /**
   * 取消分享
   */
  const cancelShare = async (shareId: number): Promise<boolean> => {
    try {
      isLoading.value = true
      await API.share.cancel(shareId)
      
      // 从列表中移除
      const index = myShares.value.findIndex(share => share.id === shareId)
      if (index > -1) {
        myShares.value.splice(index, 1)
        sharesPagination.value.total--
      }
      
      toast.success('分享已取消')
      return true
    } catch (error) {
      console.error('取消分享失败:', error)
      toast.error('取消分享失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 验证分享链接
   */
  const verifyShare = async (data: ShareVerifyReq): Promise<ShareInfoResp | null> => {
    try {
      isVerifying.value = true
      const response = await API.share.verify(data)

      if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
        const shareData = (response.data as any).data
        verifiedShareInfo.value = shareData
        return shareData
      }

      return null
    } catch (error) {
      console.error('验证分享链接失败:', error)
      toast.error('分享链接无效或已过期')
      verifiedShareInfo.value = null
      return null
    } finally {
      isVerifying.value = false
    }
  }
  
  /**
   * 保存分享内容到我的网盘
   */
  const saveShare = async (data: ShareSaveReq): Promise<(ShareSaveAsyncResp & { code: number }) | null> => {
    try {
      isLoading.value = true
      const response = await API.share.save(data)

      if (response.data && ((response.data as any).code === 200 || (response.data as any).code === 202) && (response.data as any).data) {
        // 返回包含code的完整数据，让调用方判断是否为秒传
        return {
          ...(response.data as any).data,
          code: (response.data as any).code
        }
      }

      return null
    } catch (error) {
      console.error('保存分享内容失败:', error)
      toast.error('保存分享内容失败')
      return null
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 复制分享链接到剪贴板
   */
  const copyShareLink = async (share: ShareRecordItem): Promise<void> => {
    try {
      const shareText = share.share_password 
        ? `${share.share_token} & ${share.share_password}`
        : share.share_token
      
      await navigator.clipboard.writeText(shareText)
      toast.success('分享链接已复制到剪贴板')
    } catch (error) {
      console.error('复制分享链接失败:', error)
      toast.error('复制失败')
    }
  }
  
  /**
   * 解析分享链接文本
   */
  const parseShareLink = (shareText: string): { token: string; password?: string } | null => {
    const trimmed = shareText.trim()

    // 检查是否包含密码（新格式：token&password）
    if (trimmed.includes('&')) {
      const parts = trimmed.split('&')
      if (parts.length === 2) {
        return {
          token: parts[0].trim(),
          password: parts[1].trim()
        }
      }
    }

    // 兼容旧格式（token & password，带空格）
    if (trimmed.includes(' & ')) {
      const parts = trimmed.split(' & ')
      if (parts.length === 2) {
        return {
          token: parts[0].trim(),
          password: parts[1].trim()
        }
      }
    }

    // 只有token
    if (trimmed.length > 0) {
      return {
        token: trimmed
      }
    }

    return null
  }
  
  /**
   * 格式化分享过期时间
   */
  const formatExpireTime = (expireTime?: string | null): string => {
    if (!expireTime) {
      return '永久有效'
    }
    
    const expire = new Date(expireTime)
    const now = new Date()
    const diffMs = expire.getTime() - now.getTime()
    
    if (diffMs <= 0) {
      return '已过期'
    }
    
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24))
    
    if (diffDays <= 1) {
      const diffHours = Math.ceil(diffMs / (1000 * 60 * 60))
      return `${diffHours}小时后过期`
    }
    
    return `${diffDays}天后过期`
  }
  
  /**
   * 格式化文件大小
   */
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }
  
  /**
   * 清空验证信息
   */
  const clearVerifiedInfo = (): void => {
    verifiedShareInfo.value = null
  }
  
  /**
   * 重置状态
   */
  const reset = (): void => {
    myShares.value = []
    sharesPagination.value = {
      total: 0,
      currentPage: 1,
      pageSize: 10,
      hasMore: true
    }
    verifiedShareInfo.value = null
    isLoading.value = false
    isVerifying.value = false
  }
  
  return {
    // 状态
    isLoading,
    myShares,
    sharesPagination,
    verifiedShareInfo,
    isVerifying,
    
    // 计算属性
    totalShares,
    hasShares,
    
    // 方法
    createShare,
    loadMyShares,
    loadMoreShares,
    cancelShare,
    verifyShare,
    saveShare,
    copyShareLink,
    parseShareLink,
    formatExpireTime,
    formatFileSize,
    clearVerifiedInfo,
    reset
  }
}) 