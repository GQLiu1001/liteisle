<template>
  <div class="pdf-viewer h-full flex flex-col bg-white rounded-2xl overflow-hidden">
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
          <div v-if="isLoading" class="text-xs text-gray-500">加载中...</div>
          <div v-else-if="error" class="text-xs text-red-500">{{ error }}</div>
        </div>
      </div>

      <!-- 页面控制 -->
      <div class="flex items-center gap-3">
        <button
          @click="previousPage"
          :disabled="currentPage <= 1 || isLoading"
          class="p-2 rounded hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronLeft :size="20" />
        </button>

        <span class="text-sm text-gray-600">
          {{ currentPage }} / {{ totalPages || '?' }}
        </span>

        <button
          @click="nextPage"
          :disabled="currentPage >= totalPages || isLoading"
          class="p-2 rounded hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronRight :size="20" />
        </button>

        <!-- 缩放控制 -->
        <div class="border-l border-gray-300 pl-3 ml-3 flex items-center gap-2">
                  <button @click="() => zoomOut()" class="p-2 rounded hover:bg-gray-200" :disabled="isLoading">
          <Minus :size="16" />
        </button>

        <span class="text-sm text-gray-600 min-w-[4rem] text-center">
          {{ Math.round(scale * 100) }}%
        </span>

        <button @click="() => zoomIn()" class="p-2 rounded hover:bg-gray-200" :disabled="isLoading">
          <Plus :size="16" />
        </button>
        </div>
      </div>
    </div>
    
    <!-- PDF内容区域 -->
    <div 
      class="flex-1 bg-gray-100 p-4" 
      ref="pdfContainer"
      :style="{
        overflow: 'auto',
        position: 'relative'
      }"
      @mousedown="handleMouseDown"
      @mousemove="handleMouseMove"
      @mouseup="handleMouseUp"
      @mouseleave="handleMouseUp"
    >
      <!-- 加载状态 -->
      <div v-if="isLoading" class="flex items-center justify-center h-full">
        <div class="text-center">
          <div class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p class="text-gray-600">正在加载PDF文档...</p>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="flex items-center justify-center h-full">
        <div class="text-center">
          <div class="text-red-500 text-6xl mb-4">⚠️</div>
          <h3 class="text-lg font-medium text-gray-900 mb-2">加载失败</h3>
          <p class="text-gray-600 mb-4">{{ error }}</p>
          <button @click="loadPDF" class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">
            重试
          </button>
        </div>
      </div>

      <!-- PDF渲染区域 -->
      <div v-else 
        class="w-full h-full"
        :style="{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minWidth: scale > 1 ? `${scale * 100}%` : '100%',
          minHeight: scale > 1 ? `${scale * 100}%` : '100%'
        }"
      >
        <div
          ref="scaledElement"
          class="bg-white shadow-lg rounded-xl relative"
          :class="{ 'cursor-grab': scale > 1 && !isDragging, 'cursor-grabbing': isDragging }"
          :style="{ 
            transform: `scale(${scale})`,
            transformOrigin: '0 0',
            willChange: 'transform'
          }"
        >
          <!-- PDF页面画布 -->
          <canvas
            ref="pdfCanvas"
            class="block rounded-xl"
          ></canvas>
          
          <!-- 文本选择层 -->
          <div
            ref="textLayer"
            class="textLayer absolute top-0 left-0 rounded-xl"
            @mouseup="handleTextSelection"
            @contextmenu="handleContextMenu"
          ></div>
        </div>
      </div>
    </div>


    <!-- 右键菜单 -->
    <div
      v-if="showContextMenu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      class="context-menu fixed bg-white border border-gray-200 rounded-lg shadow-lg py-2 z-50 min-w-[150px] max-w-[300px]"
    >
      <button
        @click.stop="copyText"
        class="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
      >
        📋 复制{{ translatedText ? '译文' : '' }}
      </button>
      <button
        @click.stop="translateSelection"
        :disabled="isTranslating"
        class="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2 disabled:opacity-50"
      >
        🌐 翻译
      </button>
      
      <!-- 翻译结果区域 -->
      <div v-if="isTranslating || translatedText" class="border-t border-gray-200 mt-2">
        <div v-if="isTranslating" class="px-4 py-3 text-xs text-gray-500">
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 border border-gray-400 border-t-transparent rounded-full animate-spin"></div>
            翻译中...
          </div>
        </div>
        <div v-else-if="translatedText" class="px-4 py-3">
          <div class="text-xs text-gray-500 mb-1">译文:</div>
          <div class="text-sm text-gray-800 leading-relaxed">{{ translatedText }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ChevronLeft, ChevronRight, Minus, Plus } from 'lucide-vue-next'
import { API } from '@/utils/api'

// 1. 引入 pdfjs-dist 主模块
import * as pdfjsLib from 'pdfjs-dist'

// 2. 引入 worker
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url'

// 修复1: 引入 pdf.js 的文本层 CSS
import 'pdfjs-dist/web/pdf_viewer.css'

// 3. 在组件脚本加载时，全局设置一次 workerSrc
pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker

interface Props {
  filePath: string
  fileName: string

}

const props = defineProps<Props>()

defineEmits<{
  close: []
}>()

// 状态
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1)
const pdfContainer = ref<HTMLElement>()
const pdfCanvas = ref<HTMLCanvasElement>()
const textLayer = ref<HTMLElement>()
const selectedText = ref('')
const showContextMenu = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const translatedText = ref('')
const isTranslating = ref(false)
const isLoading = ref(true)
const error = ref('')

// 拖拽相关状态
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const scrollPosition = ref({ x: 0, y: 0 })

const scaledElement = ref<HTMLElement>()

// 缩放相关状态
const zoomOrigin = ref({ x: 50, y: 50 }) // 缩放原点百分比位置, 按钮缩放时使用

// PDF相关状态
let pdfDocument: any = null
let currentPageObject: any = null
let currentRenderTask: any = null

// PDF加载函数
const loadPDF = async () => {
  try {
    isLoading.value = true
    error.value = ''

    // 获取PDF文件的下载URL
    const response = await API.document.getViewUrl(parseInt(props.filePath))
    const apiResponse = response.data as any
    if (!apiResponse || apiResponse.code !== 200 || !apiResponse.data) {
      throw new Error(apiResponse?.message || '获取PDF文件URL失败')
    }

    const pdfUrl = apiResponse.data
    
    // 下载PDF文件到内存
    const pdfResponse = await fetch(pdfUrl)
    if (!pdfResponse.ok) {
      throw new Error('PDF文件下载失败')
    }
    
    const pdfArrayBuffer = await pdfResponse.arrayBuffer()
    
    // 使用ArrayBuffer加载PDF文档
    const loadingTask = pdfjsLib.getDocument({
      data: pdfArrayBuffer
    })
    
    pdfDocument = await loadingTask.promise
    totalPages.value = pdfDocument.numPages
    currentPage.value = 1

    // 设置加载状态为false，然后等待DOM更新
    isLoading.value = false
    await nextTick()
    await renderPage()

  } catch (err: any) {
    console.error('PDF加载失败:', err)
    error.value = err.message || 'PDF加载失败'
    isLoading.value = false
  }
}

// 渲染PDF页面
const renderPage = async () => {
  if (!pdfCanvas.value || !textLayer.value) {
    console.error('Canvas 或 TextLayer 元素未找到')
    return
  }

  if (!pdfDocument) {
    console.error('PDF文档未加载')
    return
  }

  const canvas = pdfCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    console.error('无法获取Canvas 2D上下文')
    return
  }

  try {
    // 取消之前的渲染任务
    if (currentRenderTask) {
      currentRenderTask.cancel()
    }

    const page = await pdfDocument.getPage(currentPage.value)
    currentPageObject = page
    
    const devicePixelRatio = window.devicePixelRatio || 1
    
    // 使用基础视窗来设置 CSS 大小和文本层  
    const viewport = page.getViewport({ scale: 1.0 }) // 使用固定比例 1.0
    
    // 使用高分辨率视窗来渲染 Canvas
    const scaledViewport = page.getViewport({ scale: 1.0 * devicePixelRatio })
    
    canvas.width = scaledViewport.width
    canvas.height = scaledViewport.height
    canvas.style.width = viewport.width + 'px'
    canvas.style.height = viewport.height + 'px'
    
    // 设置文本层容器的尺寸，与视窗匹配
    textLayer.value.style.width = viewport.width + 'px'
    textLayer.value.style.height = viewport.height + 'px'

    const renderContext = {
      canvasContext: ctx,
      viewport: scaledViewport
    }

    // 保存当前渲染任务的引用
    currentRenderTask = page.render(renderContext)
    await currentRenderTask.promise
    currentRenderTask = null

    await renderTextLayer(page, viewport)

  } catch (err: any) {
    if (err.name === 'RenderingCancelledException') {
      console.log('渲染被取消')
      return
    }
    console.error('PDF页面渲染失败:', err)
    error.value = 'PDF页面渲染失败'
  }
}

// 修复1: 重写 renderTextLayer，使用 pdf.js 官方辅助函数
const renderTextLayer = async (page: any, viewport: any) => {
  if (!textLayer.value) return

  // 清空现有的文本层
  textLayer.value.innerHTML = ''

  try {
    const textContent = await page.getTextContent()

    // 手动渲染文本层，但使用正确的坐标计算
    textContent.items.forEach((textItem: any) => {
      const span = document.createElement('span')
      span.textContent = textItem.str
      span.style.position = 'absolute'
      span.style.left = textItem.transform[4] + 'px'
      span.style.top = (viewport.height - textItem.transform[5]) + 'px'
      span.style.fontSize = textItem.transform[0] + 'px'
      span.style.fontFamily = textItem.fontName || 'sans-serif'
      span.style.color = 'transparent'
      span.style.userSelect = 'text'
      span.style.pointerEvents = 'auto'
      span.style.cursor = 'text'
      span.style.lineHeight = '1'
      span.style.whiteSpace = 'pre'
      
      textLayer.value!.appendChild(span)
    })

  } catch (err) {
    console.warn('无法渲染文本层:', err)
  }
}

// 页面导航
const previousPage = async () => {
  if (currentPage.value > 1) {
    currentPage.value--
    await renderPage()
  }
}

const nextPage = async () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    await renderPage()
  }
}

// 缩放控制
const zoomIn = () => {
  if (scale.value < 3) {
    const newScale = Math.min(3, scale.value + 0.25)
    centerZoom(newScale)
  }
}

const zoomOut = () => {
  if (scale.value > 0.25) {
    const newScale = Math.max(0.25, scale.value - 0.25)
    centerZoom(newScale)
  }
}

const centerZoom = (newScale: number) => {
  const container = pdfContainer.value
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


// 键盘快捷键
const handleKeydown = (event: KeyboardEvent) => {
  switch (event.key) {
    case 'ArrowLeft':
    case 'PageUp':
      event.preventDefault()
      previousPage()
      break
    case 'ArrowRight':
    case 'PageDown':
    case ' ':
      event.preventDefault()
      nextPage()
      break
    case 'Escape':
      event.preventDefault()
      // 可以通过ref调用parent的方法或emit事件
      break
    case '=':
    case '+':
      event.preventDefault()
      zoomIn()
      break
    case '-':
      event.preventDefault()
      zoomOut()
      break
  }
}

// Ctrl+滚轮缩放
const handleWheel = (event: WheelEvent) => {
  // 只有在按住Ctrl键时才进行缩放
  if (event.ctrlKey) {
    event.preventDefault();

    const container = pdfContainer.value;
    if (!container) return;

    // 1. 获取缩放前的信息
    const oldScale = scale.value;
    const rect = container.getBoundingClientRect();
    
    // 鼠标在容器内的坐标 (相对于视口)
    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    // 鼠标指向的内容在缩放前的绝对坐标 (考虑了当前的滚动)
    const pointX = (container.scrollLeft + mouseX) / oldScale;
    const pointY = (container.scrollTop + mouseY) / oldScale;

    // 2. 计算新的缩放比例
    const delta = event.deltaY < 0 ? 0.15 : -0.15; // 调整缩放步长
    const newScale = Math.max(0.25, Math.min(3, oldScale + delta));
    
    if (Math.abs(newScale - oldScale) < 0.001) return; // 缩放比例没有变化

    scale.value = newScale;

    // 3. 计算并设置新的滚动位置，以保持内容点在鼠标下
    nextTick(() => {
        // a. 内容点在缩放后的新绝对坐标
        const newPointX = pointX * newScale;
        const newPointY = pointY * newScale;

        // b. 计算新的 scrollLeft/scrollTop
        const newScrollLeft = newPointX - mouseX;
        const newScrollTop = newPointY - mouseY;
        
        container.scrollLeft = newScrollLeft;
        container.scrollTop = newScrollTop;
    });
  }
};

// 文本选择处理
const handleTextSelection = () => {
  const selection = window.getSelection()
  if (selection && selection.toString().trim()) {
    selectedText.value = selection.toString().trim()
  } else {
    selectedText.value = ''
    showContextMenu.value = false
    translatedText.value = ''
    isTranslating.value = false
  }
}

// 右键菜单处理
const handleContextMenu = (event: MouseEvent) => {
  const selection = window.getSelection()
  if (selection && selection.toString().trim()) {
    event.preventDefault()
    selectedText.value = selection.toString().trim()
    contextMenuPosition.value = { x: event.clientX, y: event.clientY }
    showContextMenu.value = true
  }
}

// 复制文本
const copyText = async () => {
  const textToCopy = translatedText.value || selectedText.value
  if (textToCopy) {
    try {
      await navigator.clipboard.writeText(textToCopy)
      // 复制后关闭菜单
      showContextMenu.value = false
      translatedText.value = ''
      isTranslating.value = false
    } catch (err) {
      console.error('复制失败:', err)
    }
  }
}

// 翻译文本
const translateSelection = async () => {
  if (!selectedText.value) return
  
  isTranslating.value = true
  try {
    const response = await API.translate.translate({
      file_name: 'selected_text.txt',
      text: selectedText.value,
      target_lang: 'zh-CN'
    }) as any; // 强制类型转换为any
    if (response.data && response.data.data && response.data.data.translated_text) {
      let text = response.data.data.translated_text;
      if (typeof text === 'string' && text.startsWith('"') && text.endsWith('"')) {
        try {
          text = JSON.parse(text);
        } catch (e) {
          // Not a valid JSON string, use as is.
        }
      }
      translatedText.value = text;
    } else {
      translatedText.value = response.data?.message || '未返回翻译结果';
    }
  } catch (error) {
    console.error('翻译失败:', error)
    translatedText.value = '翻译服务暂不可用'
  } finally {
    isTranslating.value = false
  }
}

// 关闭右键菜单
const closeContextMenu = () => {
  showContextMenu.value = false
  translatedText.value = ''
  isTranslating.value = false
}

// 拖拽功能
const handleMouseDown = (event: MouseEvent) => {
  // 检查是否点击的是文本层或其中的文本元素
  const target = event.target as HTMLElement
  if (target && (target.tagName === 'SPAN' || target.classList.contains('textLayer'))) {
    return // 如果点击的是文本相关元素，允许文本选择
  }
  
  if (scale.value > 1 && event.button === 0) { // 只在放大时启用拖拽，且只响应左键
    isDragging.value = true
    dragStart.value = {
      x: event.clientX + (pdfContainer.value?.scrollLeft || 0),
      y: event.clientY + (pdfContainer.value?.scrollTop || 0)
    }
    event.preventDefault()
  }
}

const handleMouseMove = (event: MouseEvent) => {
  if (isDragging.value && pdfContainer.value) {
    const deltaX = dragStart.value.x - event.clientX
    const deltaY = dragStart.value.y - event.clientY
    
    pdfContainer.value.scrollLeft = deltaX
    pdfContainer.value.scrollTop = deltaY
    event.preventDefault()
  }
}

const handleMouseUp = () => {
  isDragging.value = false
}

// 点击其他地方关闭右键菜单
const handleClickOutside = (event: MouseEvent) => {
  if (showContextMenu.value) {
    // 检查点击的元素是否在右键菜单内部
    const target = event.target as Element
    const contextMenu = document.querySelector('.context-menu')
    
    if (contextMenu && !contextMenu.contains(target)) {
      showContextMenu.value = false
      translatedText.value = ''
      isTranslating.value = false
    }
  }
}

onMounted(async () => {
  const viewerElement = document.querySelector('.pdf-viewer') as HTMLElement;
  if (viewerElement) {
    viewerElement.addEventListener('wheel', handleWheel as EventListener, { passive: false });
    viewerElement.setAttribute('tabindex', '-1'); // 使其可聚焦
    viewerElement.addEventListener('keydown', handleKeydown as EventListener);
    viewerElement.focus();
  }
  
  document.addEventListener('click', handleClickOutside)

  // 加载PDF文档
  await loadPDF()
})

onUnmounted(() => {
  const viewerElement = document.querySelector('.pdf-viewer');
  if (viewerElement) {
    viewerElement.removeEventListener('wheel', handleWheel as EventListener);
    viewerElement.removeEventListener('keydown', handleKeydown as EventListener);
  }
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.pdf-viewer {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
}

/* 文本选择样式 */
.select-text {
  user-select: text;
  -webkit-user-select: text;
  -moz-user-select: text;
  -ms-user-select: text;
}

.select-text::selection {
  background-color: #3b82f6;
  color: white;
}

.select-text::-moz-selection {
  background-color: #3b82f6;
  color: white;
}

/* 文本层选择样式 */
:deep(.textLayer span::selection) {
  background-color: #3b82f6;
  color: white;
}

:deep(.textLayer span::-moz-selection) {
  background-color: #3b82f6;
  color: white;
}

/* 拖拽光标 */
.cursor-grab {
  cursor: grab;
}

.cursor-grab .textLayer {
  cursor: text !important;
}

.cursor-grabbing {
  cursor: grabbing;
}

.cursor-grabbing .textLayer {
  cursor: grabbing;
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

/* 打印样式模拟 */
@media print {
  .pdf-viewer {
    background: white;
  }
}
</style> 