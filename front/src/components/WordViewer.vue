<template>
  <div class="word-viewer h-full flex flex-col bg-white rounded-2xl overflow-hidden">
    <!-- 工具栏 -->
    <div class="flex-shrink-0 bg-gray-50 border-b border-gray-200 p-3 flex items-center justify-between">
      <div class="flex items-center gap-4 flex-1 min-w-0">
        <button @click="$emit('close')" class="flex items-center gap-2 text-gray-600 hover:text-gray-800 flex-shrink-0">
          <ChevronLeft :size="20" />
          <span>返回列表</span>
        </button>
        
        <!-- 文档信息 -->
        <div class="flex-1 min-w-0 ml-4 border-l border-gray-300 pl-4">
          <h3 class="font-medium text-gray-900 truncate">{{ fileName }}</h3>
          <div v-if="isLoading" class="text-xs text-gray-500">正在加载Word文档...</div>
          <div v-else-if="error" class="text-xs text-red-500">{{ error }}</div>
          <div v-else class="text-xs text-gray-500">Microsoft Word文档</div>
        </div>
      </div>
      
      <!-- 控制按钮 -->
      <div class="flex items-center gap-3">
        <!-- 下载按钮 -->
        <button 
          v-if="previewUrl" 
          @click="downloadFile"
          class="flex items-center gap-2 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded text-sm"
        >
          <Download :size="16" />
          下载
        </button>
        
        <!-- 全屏按钮 -->
        <button @click="toggleFullscreen" class="p-2 rounded hover:bg-gray-200 border-l border-gray-300 pl-3 ml-3">
          <Maximize :size="16" />
        </button>
      </div>
    </div>
    
    <!-- Word内容区域 -->
    <div 
      class="flex-1 bg-gray-100 p-4 overflow-hidden" 
      ref="wordContainer"
    >
      <!-- 加载状态 -->
      <div v-if="isLoading" class="flex items-center justify-center h-full">
        <div class="text-center">
          <div class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p class="text-gray-600">正在加载Word文档...</p>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="flex items-center justify-center h-full">
        <div class="text-center max-w-md">
          <div class="text-red-500 text-6xl mb-4">⚠️</div>
          <h3 class="text-lg font-medium text-gray-900 mb-2">加载失败</h3>
          <p class="text-gray-600 mb-6 text-sm leading-relaxed">{{ error }}</p>
          <div class="space-y-3">
            <button 
              v-if="previewUrl"
              @click="downloadFile" 
              class="block w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded flex items-center justify-center gap-2"
            >
              <Download :size="16" />
              下载原文件
            </button>
            <button 
              @click="loadDocument" 
              class="block w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded"
            >
              重试
            </button>
          </div>
        </div>
      </div>

      <!-- Word文档预览 -->
      <div v-else-if="previewUrl" class="w-full h-full">
        <!-- 使用Office Online嵌入式查看器 -->
        <iframe
          :src="getOfficeViewerUrl(previewUrl)"
          class="w-full h-full border-0 rounded-xl bg-white shadow-lg"
          frameborder="0"
          allowfullscreen
          sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-top-navigation"
          referrerpolicy="strict-origin-when-cross-origin"
          loading="lazy"
          @load="onIframeLoad"
          @error="onIframeError"
        ></iframe>
      </div>

      <!-- 错误状态 -->
      <div v-else class="flex items-center justify-center h-full">
        <div class="text-center">
          <div class="text-gray-400 text-6xl mb-4">📄</div>
          <h3 class="text-lg font-medium text-gray-700 mb-2">
            {{ error || '无法获取文档内容' }}
          </h3>
          <p class="text-gray-500 text-sm mb-4">
            {{ showDownloadOption ? '您可以下载文档到本地查看' : '请稍后重试' }}
          </p>

          <!-- 重试和下载按钮 -->
          <div class="flex gap-2 justify-center">
            <button
              v-if="!showDownloadOption && retryCount < maxRetries"
              @click="retryLoad"
              class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
            >
              重试加载
            </button>

            <button
              v-if="showDownloadOption && previewUrl"
              @click="downloadFile"
              class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition-colors"
            >
              下载文档
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ChevronLeft, Maximize, Download } from 'lucide-vue-next'
import { API } from '@/utils/api'

interface Props {
  filePath: string
  fileName: string
}

const props = defineProps<Props>()

defineEmits<{
  close: []
}>()

// 状态
const wordContainer = ref<HTMLElement>()
const isLoading = ref(true)
const error = ref('')
const previewUrl = ref('')
const retryCount = ref(0)
const maxRetries = 3
const showDownloadOption = ref(false)

// 文档加载函数
const loadDocument = async () => {
  try {
    isLoading.value = true
    error.value = ''

    // 获取Word文件的预览URL
    const response = await API.document.getViewUrl(parseInt(props.filePath))
    if (!response.data || response.data.code !== 200) {
      throw new Error('获取Word文档链接失败')
    }

    previewUrl.value = response.data.data

  } catch (err: any) {
    console.error('Word文档加载失败:', err)
    error.value = err.message || 'Word文档加载失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

// 生成Office在线查看器URL
const getOfficeViewerUrl = (url: string): string => {
  // 如果URL已经是可以直接嵌入的预览URL，直接使用
  if (url.includes('embed') || url.includes('preview') || url.includes('view')) {
    return url
  }

  // 使用 embed.aspx 格式，专为嵌入设计，更稳定
  const baseUrl = 'https://view.officeapps.live.com/op/embed.aspx'
  const params = new URLSearchParams({
    src: url,
    wdStartOn: '1',
    wdEmbedCode: '0'
  })

  return `${baseUrl}?${params.toString()}`
}

// 下载文件
const downloadFile = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 手动重试加载
const retryLoad = async () => {
  retryCount.value++
  error.value = ''
  isLoading.value = true
  showDownloadOption.value = false

  // 重新加载文档
  await loadDocument()
}



// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    wordContainer.value?.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}



// iframe事件处理
const onIframeLoad = () => {
  console.log('Word文档加载完成')
  retryCount.value = 0 // 重置重试计数
  isLoading.value = false
  error.value = ''
}

const onIframeError = () => {
  console.error('Word文档iframe加载失败，重试次数:', retryCount.value)

  if (retryCount.value < maxRetries) {
    retryCount.value++
    setTimeout(() => {
      console.log(`正在重试加载Word文档 (${retryCount.value}/${maxRetries})`)
      // 强制重新加载iframe
      const iframe = document.querySelector('iframe')
      if (iframe && previewUrl.value) {
        iframe.src = getOfficeViewerUrl(previewUrl.value)
      }
    }, 2000 * retryCount.value) // 递增延迟
  } else {
    error.value = 'Word文档在线预览失败'
    showDownloadOption.value = true
    isLoading.value = false
  }
}

// 键盘快捷键
const handleKeydown = (event: KeyboardEvent) => {
  switch (event.key) {
    case 'f':
    case 'F':
      event.preventDefault()
      toggleFullscreen()
      break
  }
}

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  
  // 加载Word文档
  await loadDocument()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.word-viewer {
  font-family: 'Times New Roman', Times, serif;
}

/* 滚动条样式 */
.overflow-auto::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.overflow-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.overflow-auto::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.overflow-auto::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>