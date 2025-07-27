<template>
  <div class="excel-viewer h-full flex flex-col bg-white rounded-2xl overflow-hidden">
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
          <div v-if="isLoading" class="text-xs text-gray-500">正在加载Excel电子表格...</div>
          <div v-else-if="error" class="text-xs text-red-500">{{ error }}</div>
          <div v-else class="text-xs text-gray-500">Microsoft Excel电子表格</div>
        </div>
      </div>
      
      <!-- 控制按钮 -->
      <div class="flex items-center gap-3">
        <!-- 下载按钮 -->
        <button 
          v-if="previewUrl" 
          @click="downloadFile"
          class="flex items-center gap-2 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded text-sm"
        >
          <Download :size="16" />
          下载
        </button>
        
        <!-- 缩放控制 -->
        <div class="border-l border-gray-300 pl-3 ml-3 flex items-center gap-2">
          <button @click="zoomOut" class="p-2 rounded hover:bg-gray-200" :disabled="isLoading">
            <Minus :size="16" />
          </button>
          
          <span class="text-sm text-gray-600 min-w-[4rem] text-center">
            {{ Math.round(scale * 100) }}%
          </span>
          
          <button @click="zoomIn" class="p-2 rounded hover:bg-gray-200" :disabled="isLoading">
            <Plus :size="16" />
          </button>
          
          <!-- 适合窗口 -->
          <button @click="fitToWindow" class="p-2 rounded hover:bg-gray-200 ml-2" :disabled="isLoading">
            <RefreshCcw :size="16" />
          </button>
          
          <!-- 全屏按钮 -->
          <button @click="toggleFullscreen" class="p-2 rounded hover:bg-gray-200">
            <Maximize :size="16" />
          </button>
        </div>
      </div>
    </div>
    
    <!-- Excel内容区域 -->
    <div 
      class="flex-1 bg-gray-100 p-4 overflow-auto" 
      ref="excelContainer"
      @wheel="handleWheel"
    >
      <!-- 加载状态 -->
      <div v-if="isLoading" class="flex items-center justify-center h-full">
        <div class="text-center">
          <div class="w-8 h-8 border-4 border-green-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p class="text-gray-600">正在加载Excel文档...</p>
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
              class="block w-full px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded flex items-center justify-center gap-2"
            >
              <Download :size="16" />
              下载原文件
            </button>
            <button 
              @click="loadDocument" 
              class="block w-full px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded"
            >
              重试
            </button>
          </div>
        </div>
      </div>

      <!-- Excel文档预览 -->
      <div v-else-if="previewUrl" class="w-full h-full flex items-center justify-center">
        <div
          ref="scaledElement"
          class="bg-white shadow-lg rounded-xl relative"
          :style="{ 
            transform: `scale(${scale})`,
            transformOrigin: '0 0',
            willChange: 'transform',
            width: '100%',
            height: '100%',
            minWidth: '800px',
            minHeight: '600px'
          }"
        >
          <!-- 使用Office Online嵌入式查看器 -->
          <iframe
            :src="getOfficeViewerUrl(previewUrl)"
            class="w-full h-full border-0 rounded-xl"
            frameborder="0"
            allowfullscreen
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-top-navigation"
            referrerpolicy="strict-origin-when-cross-origin"
            loading="lazy"
            @load="onIframeLoad"
            @error="onIframeError"
          ></iframe>
          
          <!-- 预览层覆盖（用于捕获用户交互） -->
          <div
            v-if="scale !== 1"
            class="absolute inset-0 pointer-events-none"
            style="background: transparent;"
          ></div>
        </div>
      </div>

      <!-- 无内容状态 -->
      <div v-else class="flex items-center justify-center h-full">
        <div class="text-center">
          <div class="text-gray-400 text-6xl mb-4">📊</div>
          <h3 class="text-lg font-medium text-gray-700 mb-2">无法获取文档内容</h3>
          <p class="text-gray-500 text-sm">请稍后重试</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ChevronLeft, Maximize, Download, Minus, Plus, RefreshCcw } from 'lucide-vue-next'
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
const excelContainer = ref<HTMLElement>()
const scaledElement = ref<HTMLElement>()
const isLoading = ref(true)
const error = ref('')
const previewUrl = ref('')
const scale = ref(1)
const retryCount = ref(0)
const maxRetries = 3

// 加载Excel文档
const loadDocument = async () => {
  try {
    isLoading.value = true
    error.value = ''

    // 获取Excel文档的预览URL
    const response = await API.document.getViewUrl(parseInt(props.filePath))
    if (!response.data || response.data.code !== 200) {
      throw new Error('获取Excel文档链接失败')
    }

    previewUrl.value = response.data.data
    
  } catch (err: any) {
    console.error('Excel文档加载失败:', err)
    error.value = err.message || 'Excel文档加载失败，请稍后重试'
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

// 缩放控制
const zoomIn = () => {
  if (scale.value < 2) {
    const newScale = Math.min(2, scale.value + 0.25)
    centerZoom(newScale)
  }
}

const zoomOut = () => {
  if (scale.value > 0.5) {
    const newScale = Math.max(0.5, scale.value - 0.25)
    centerZoom(newScale)
  }
}

const fitToWindow = () => {
  scale.value = 1
}

const centerZoom = (newScale: number) => {
  const container = excelContainer.value
  if (!container) return

  const oldScale = scale.value
  const rect = container.getBoundingClientRect()

  const containerCenterX = rect.width / 2
  const containerCenterY = rect.height / 2

  const pointX = (container.scrollLeft + containerCenterX) / oldScale
  const pointY = (container.scrollTop + containerCenterY) / oldScale

  scale.value = newScale

  nextTick(() => {
    const newPointX = pointX * newScale
    const newPointY = pointY * newScale
    container.scrollLeft = newPointX - containerCenterX
    container.scrollTop = newPointY - containerCenterY
  })
}

// 全屏切换
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    excelContainer.value?.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 鼠标滚轮缩放
const handleWheel = (event: WheelEvent) => {
  if (event.ctrlKey) {
    event.preventDefault()
    
    const container = excelContainer.value
    if (!container) return

    const oldScale = scale.value
    const rect = container.getBoundingClientRect()
    
    const mouseX = event.clientX - rect.left
    const mouseY = event.clientY - rect.top

    const pointX = (container.scrollLeft + mouseX) / oldScale
    const pointY = (container.scrollTop + mouseY) / oldScale

    const delta = event.deltaY < 0 ? 0.15 : -0.15
    const newScale = Math.max(0.5, Math.min(2, oldScale + delta))
    
    if (Math.abs(newScale - oldScale) < 0.001) return

    scale.value = newScale

    nextTick(() => {
      const newPointX = pointX * newScale
      const newPointY = pointY * newScale
      const newScrollLeft = newPointX - mouseX
      const newScrollTop = newPointY - mouseY
      
      container.scrollLeft = newScrollLeft
      container.scrollTop = newScrollTop
    })
  }
}

// iframe事件处理
const onIframeLoad = () => {
  console.log('Excel文档加载完成')
  retryCount.value = 0 // 重置重试计数
  isLoading.value = false
  error.value = ''
}

const onIframeError = () => {
  console.error('Excel文档iframe加载失败，重试次数:', retryCount.value)

  if (retryCount.value < maxRetries) {
    retryCount.value++
    setTimeout(() => {
      console.log(`正在重试加载Excel文档 (${retryCount.value}/${maxRetries})`)
      // 强制重新加载iframe
      const iframe = document.querySelector('iframe')
      if (iframe && previewUrl.value) {
        iframe.src = getOfficeViewerUrl(previewUrl.value)
      }
    }, 2000 * retryCount.value) // 递增延迟
  } else {
    error.value = 'Excel文档加载失败，请检查网络连接或稍后重试'
    isLoading.value = false
  }
}

// 键盘快捷键
const handleKeydown = (event: KeyboardEvent) => {
  switch (event.key) {
    case '=':
    case '+':
      event.preventDefault()
      zoomIn()
      break
    case '-':
      event.preventDefault()
      zoomOut()
      break
    case '0':
      event.preventDefault()
      fitToWindow()
      break
    case 'f':
    case 'F':
      event.preventDefault()
      toggleFullscreen()
      break
  }
}

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  
  // 加载Excel文档
  await loadDocument()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.excel-viewer {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
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