import { useSettingsStore } from '@/store/SettingsStore'

// 将图片文件转换为base64
const fileToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      // 移除data:image/xxx;base64,前缀
      const base64 = result.split(',')[1]
      resolve(base64)
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

// 通过PicGo HTTP API上传剪贴板中的图片
export const uploadClipboardImageToPicGo = async (): Promise<string> => {
  const settingsStore = useSettingsStore()
  
  if (!settingsStore.settings.picgoEnabled) {
    throw new Error('PicGo上传功能未启用')
  }
  
  if (!settingsStore.settings.picgoPath) {
    throw new Error('PicGo路径未设置')
  }
  
  try {
    const port = 36677 // PicGo默认端口
    
    // 根据PicGo官方文档，上传剪贴板图片应该发送空的POST请求
    // PicGo会自动从系统剪贴板读取图片
    const response = await fetch(`http://127.0.0.1:${port}/upload`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      // 不发送body，让PicGo从剪贴板读取
      signal: AbortSignal.timeout(30000) // 30秒超时
    })
    
    if (!response.ok) {
      throw new Error(`PicGo服务器响应错误: ${response.status}`)
    }
    
    const result = await response.json()
    
    if (!result.success) {
      // 如果PicGo返回了错误信息，使用它
      const errorMsg = result.message || result.error || 'PicGo上传失败'
      
      // 提供更详细的错误信息和解决建议
      let detailedError = errorMsg
      
      if (errorMsg.includes('image not found in clipboard')) {
        detailedError = '❌ 剪贴板中未找到图片\n\n💡 可能原因：\n• 图片复制到剪贴板失败\n• 剪贴板被其他程序清空\n• 时序问题，请重试'
      } else if (errorMsg.includes('status code 468')) {
        detailedError = '❌ Gitee图床上传失败 (错误码: 468)\n\n🔧 解决方案：\n• 检查Gitee Personal Access Token是否有效\n• 确认Token具有仓库读写权限\n• 检查目标仓库是否存在且可访问\n• 确认图片文件名不与现有文件冲突\n• 图片大小建议控制在1MB以内\n• 在PicGo中重新测试Gitee配置'
      } else if (errorMsg.includes('status code')) {
        const statusMatch = errorMsg.match(/status code (\d+)/)
        const statusCode = statusMatch ? statusMatch[1] : '未知'
        detailedError = `❌ 图床服务器错误 (状态码: ${statusCode})\n\n🔧 建议：\n• 检查网络连接\n• 确认图床服务可用\n• 检查PicGo配置\n• 稍后重试`
      } else if (errorMsg.includes('upload error')) {
        detailedError = '❌ PicGo上传失败\n\n🔧 解决方案：\n• 查看PicGo应用中的错误日志\n• 检查图床配置是否正确\n• 确认网络连接正常\n• 尝试重新配置图床'
      }
      
      throw new Error(detailedError)
    }
    
    if (!result.result || result.result.length === 0) {
      throw new Error('PicGo上传结果为空，请检查剪贴板中是否有图片')
    }
    
    const imageUrl = result.result[0]
    
    // 验证返回的URL
    if (!imageUrl || typeof imageUrl !== 'string') {
      throw new Error('PicGo返回的URL格式无效')
    }
    
    if (imageUrl.startsWith('data:') || imageUrl.includes('base64')) {
      throw new Error('PicGo返回的是base64数据而不是图片URL，请检查PicGo图床配置')
    }
    
    // 返回上传后的图片URL
    return imageUrl
    
  } catch (error) {
    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        throw new Error('上传超时，请检查网络连接')
      }
      throw error
    }
    throw new Error('上传过程中发生未知错误')
  }
}

// 检查剪贴板中是否有图片
export const hasImageInClipboard = async (): Promise<boolean> => {
  try {
    const clipboardItems = await navigator.clipboard.read()
    for (const item of clipboardItems) {
      for (const type of item.types) {
        if (type.startsWith('image/')) {
          return true
        }
      }
    }
    return false
  } catch (error) {
    console.warn('无法访问剪贴板:', error)
    return false
  }
}

// 从剪贴板获取图片文件
export const getImageFromClipboard = async (): Promise<File | null> => {
  try {
    const clipboardItems = await navigator.clipboard.read()
    for (const item of clipboardItems) {
      for (const type of item.types) {
        if (type.startsWith('image/')) {
          const blob = await item.getType(type)
          // 生成一个唯一的文件名
          const timestamp = new Date().getTime()
          const extension = type.split('/')[1] || 'png'
          const fileName = `clipboard-${timestamp}.${extension}`
          return new File([blob], fileName, { type })
        }
      }
    }
    return null
  } catch (error) {
    console.error('从剪贴板获取图片失败:', error)
    return null
  }
} 