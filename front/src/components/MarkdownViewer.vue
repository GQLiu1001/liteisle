<template>
  <div class="markdown-viewer w-full h-full flex flex-col bg-white rounded-2xl overflow-hidden">
    <!-- 顶部工具栏 -->
    <div class="flex-shrink-0 border-b p-4 flex items-center justify-between bg-gray-50">
      <div class="flex items-center space-x-4">
        <button
          @click="handleClose"
          class="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <span>返回</span>
        </button>
        
        <div class="flex items-center space-x-2">
          <div class="text-lg font-semibold text-gray-800">{{ title || 'Markdown 编辑器' }}</div>
          </div>
      </div>
      
      <div class="flex items-center space-x-2">
        <!-- 缩放指示器 -->
        <div class="flex items-center space-x-2 px-3 py-2 text-gray-600 bg-gray-100 rounded-lg">
          <span class="text-sm">{{ Math.round(zoomLevel * 100) }}%</span>
          <button 
            @click="resetZoom"
            class="text-xs hover:text-gray-800 transition-colors"
            title="重置缩放 (Ctrl+0)"
          >
            重置
          </button>
        </div>

        <!-- 快捷键提示按钮 -->
        <button 
          @click="showShortcuts = !showShortcuts"
          class="flex items-center space-x-1 px-3 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
          title="快捷键提示"
        >
          <span class="text-sm">?</span>
          <span>快捷键</span>
        </button>

        <!-- 隐藏大纲按钮 -->
        <button 
          @click="toggleOutline"
          class="flex items-center space-x-1 px-3 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
          :title="showOutline ? '隐藏大纲' : '显示大纲'"
        >
          <span>{{ showOutline ? '隐藏大纲' : '显示大纲' }}</span>
        </button>

        <!-- 保存按钮 -->
        <button
          @click="saveContent"
          class="flex items-center space-x-1 px-3 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors"
          title="保存 (Ctrl+S)"
        >
          <CheckIcon class="w-4 h-4" />
          <span>保存</span>
        </button>


      </div>
    </div>

    <!-- 快捷键提示弹窗 -->
    <div v-if="showShortcuts" class="absolute top-16 right-4 bg-white border border-gray-300 rounded-lg shadow-lg p-4 w-80 z-50">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold text-gray-800">快捷键</h3>
        <button @click="showShortcuts = false" class="text-gray-500 hover:text-gray-700">
          <span class="text-xl">×</span>
        </button>
      </div>
      
      <div class="space-y-2 text-sm">
        <div class="grid grid-cols-2 gap-2">
          <div class="font-medium text-gray-700">标题:</div>
          <div class="text-gray-600">Ctrl+1-6 (切换标题/文本)</div>
          
          <div class="font-medium text-gray-700">粗体:</div>
          <div class="text-gray-600">Ctrl+B</div>
          
          <div class="font-medium text-gray-700">斜体:</div>
          <div class="text-gray-600">Ctrl+I</div>
          
          <div class="font-medium text-gray-700">下划线:</div>
          <div class="text-gray-600">Ctrl+U</div>
          
          <div class="font-medium text-gray-700">行内代码:</div>
          <div class="text-gray-600">Ctrl+`</div>
          
          <div class="font-medium text-gray-700">代码块:</div>
          <div class="text-gray-600">Ctrl+Shift+`</div>
          
          <div class="font-medium text-gray-700">链接:</div>
          <div class="text-gray-600">Ctrl+K</div>
          
          <div class="font-medium text-gray-700">列表:</div>
          <div class="text-gray-600">Ctrl+L</div>
          
          <div class="font-medium text-gray-700">表格:</div>
          <div class="text-gray-600">Ctrl+E</div>
          
          <div class="font-medium text-gray-700">分割线:</div>
          <div class="text-gray-600">Ctrl+D</div>
          
          <div class="font-medium text-gray-700">引用:</div>
          <div class="text-gray-600">Ctrl+Q</div>
          
          <div class="font-medium text-gray-700">保存:</div>
          <div class="text-gray-600">Ctrl+S</div>
          
          <div class="font-medium text-gray-700">缩放:</div>
          <div class="text-gray-600">Ctrl+滚轮</div>
          
          <div class="font-medium text-gray-700">重置缩放:</div>
          <div class="text-gray-600">Ctrl+0</div>
          
          <div class="font-medium text-gray-700">图片粘贴:</div>
          <div class="text-gray-600">Ctrl+V (需启用PicGo)</div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="flex-1 flex overflow-hidden" @contextmenu="handleContextMenu">
      <!-- 编辑器容器 -->
      <div class="flex-1 overflow-hidden relative" ref="scaledElement">
        <div ref="vditorElement" class="h-full w-full"></div>
        
        <!-- PicGo上传状态提示 -->
        <div 
          v-if="isUploadingImage" 
          class="absolute top-4 right-4 bg-white border border-gray-300 rounded-lg shadow-lg p-4 z-50 min-w-[250px]"
        >
          <div class="flex items-center gap-3">
            <div class="w-4 h-4 border-2 border-teal-600 border-t-transparent rounded-full animate-spin"></div>
            <div class="flex-1">
              <div class="text-sm font-medium text-gray-800">PicGo图片上传</div>
              <div class="text-xs text-gray-600">{{ uploadProgress }}</div>
            </div>
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
          @click.stop="translateText"
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
// Icon imports
import CheckIcon from 'lucide-vue-next/dist/esm/icons/check'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { useVditorStore } from '@/store/VditorStore'
import { useSettingsStore } from '@/store/SettingsStore'
import { uploadClipboardImageToPicGo } from '@/utils/picgo'
import { API } from '@/utils/api'

// 组件属性
interface Props {
  content: string
  title?: string
  filePath?: string
}

const props = withDefaults(defineProps<Props>(), {
  content: '',
  title: '',
  filePath: ''
})

const emit = defineEmits<{
  close: []
  save: [content: string]
  'update:content': [content: string]
}>()

// 路由实例
const router = useRouter()
let removeRouterGuard: (() => void) | null = null

// 状态管理
const currentContent = ref(props.content || '')
const currentVersion = ref(0) // 修复：初始化为0，与服务器保持一致
const zoomLevel = ref(1) // 添加缩放级别状态
const showShortcuts = ref(false) // 添加快捷键提示状态
const showOutline = ref(true) // 大纲显示状态

// 缓存机制
const documentCache = new Map<string, { content: string, version: number, timestamp: number }>()
const CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存

// DOM 引用
const vditorElement = ref<HTMLElement>()
const scaledElement = ref<HTMLElement>()

// Vditor 实例和Store
let vditor: Vditor | null = null
const vditorStore = useVditorStore()
const settingsStore = useSettingsStore()

// 右键菜单相关状态
const showContextMenu = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const selectedText = ref('')
const translatedText = ref('')
const isTranslating = ref(false)

// 图片上传相关状态
const isUploadingImage = ref(false)
const uploadProgress = ref('')

// 快捷键状态跟踪（保留以备将来使用）
// const lastShortcutKey = ref<string | null>(null)
// const lastShortcutTime = ref<number>(0)

// 注意：图片粘贴上传现在通过Vditor的upload配置处理，不再需要单独的粘贴事件处理函数

// 加载文档内容和版本信息
const loadDocument = async () => {
  if (!props.filePath) return

  try {
    const fileId = parseInt(props.filePath)
    if (isNaN(fileId)) {
      console.error('无效的文件ID:', props.filePath)
      return
    }

    const cacheKey = props.filePath
    const now = Date.now()

    // 检查缓存
    const cached = documentCache.get(cacheKey)
    if (cached && (now - cached.timestamp) < CACHE_DURATION) {
      console.log('使用缓存的文档内容')
      currentContent.value = cached.content
      currentVersion.value = cached.version

      // 优化：只在 vditor 存在且内容不同时才更新
      if (vditor && vditor.getValue() !== cached.content) {
        console.time('缓存内容渲染')
        try {
          vditor.setValue(cached.content)
          console.timeEnd('缓存内容渲染')
        } catch (err) {
          console.error('设置Vditor内容失败:', err)
        }
      }
      return
    }

    console.time('API请求')
    const response: any = await API.document.getMarkdownContent(fileId)
    console.timeEnd('API请求')

    const apiRes = response.data
    if (apiRes && apiRes.code === 200 && apiRes.data) {
      const mdData = apiRes.data
      currentContent.value = mdData.content
      currentVersion.value = mdData.version

      // 更新缓存
      documentCache.set(cacheKey, {
        content: mdData.content,
        version: mdData.version,
        timestamp: now
      })

      // 优化：只在 vditor 存在且内容不同时才更新
      if (vditor && vditor.getValue() !== mdData.content) {
        console.time('内容渲染')
        try {
          vditor.setValue(mdData.content)



          console.timeEnd('内容渲染')
        } catch (err) {
          console.error('设置Vditor内容失败:', err)
        }
      }

      console.log('Markdown文档加载成功，版本:', currentVersion.value)
    } else {
      console.error('Markdown文档加载失败:', apiRes?.message || '未知错误')
    }
  } catch (error) {
    console.error('Markdown文档加载失败:', error)
  }
}

// 初始化 Vditor
const initVditor = async () => {
  if (!vditorElement.value) return

  try {
    console.time('Vditor初始化')

    // 使用全局VditorStore创建实例，确保依赖已预加载
    vditor = await vditorStore.createVditorInstance(vditorElement.value, {
      height: '100%',
      mode: 'ir', // 即时渲染模式 - 配合CSS强制显示空白行
      value: currentContent.value,
      placeholder: '开始编写 Markdown...',
      theme: 'classic',
      lang: 'en_US', // 使用英文避免加载中文语言包
      typewriterMode: false, // 打字机模式，可选启用
      undoDelay: 300, // 撤销延迟，以毫秒为单位，控制撤销粒度
      preview: {
        theme: {
          current: 'light',
          // 使用本地路径避免CDN延迟
          path: '/node_modules/vditor/dist/css/content-theme'
        },
        hljs: {
          enable: true,
          style: 'github',
          lineNumber: false
        },
        math: {
          engine: 'KaTeX',
          inlineDigit: false
        },
        markdown: {
          codeBlockPreview: false,
          mathBlockPreview: true,
          autoSpace: false,
          fixTermTypo: false,
          toc: true,
          footnotes: true,
          paragraphBeginningSpace: false,
          listStyle: true,
          linkBase: '',
          linkPrefix: '',
          mark: true
        }
      },
      toolbar: [], // 完全隐藏工具栏以获得纯净的 IR 体验
      counter: {
        enable: false
      },
      cache: {
        enable: false
      },
      outline: {
        enable: showOutline.value, // 启用大纲以增强文档结构感
        position: 'left'
      },
      tab: '\t', // 设置 Tab 键行为
      
            // 配置上传功能来处理图片粘贴 - 完全被动模式
      upload: {
        accept: 'image/*',
        multiple: false,
        fieldName: 'file',
        async handler(files: File[]) {
          // 检查是否启用了PicGo功能
          if (!settingsStore.settings.picgoEnabled) {
            return null
          }
          
          if (!files || files.length === 0) {
            return null
          }
          
          const file = files[0]
          
          try {
            // 显示上传状态
            isUploadingImage.value = true
            uploadProgress.value = '正在通过PicGo上传图片...'
            
            // 将图片复制到剪贴板，然后让PicGo从剪贴板读取
            // 使用Clipboard API将图片复制到剪贴板
            const canvas = document.createElement('canvas')
            const ctx = canvas.getContext('2d')
            const img = new Image()
            
            // 创建一个Promise来处理图片加载
            const imageLoadPromise = new Promise<void>((resolve, reject) => {
              img.onload = () => {
                canvas.width = img.width
                canvas.height = img.height
                ctx?.drawImage(img, 0, 0)
                
                canvas.toBlob(async (blob) => {
                  if (blob) {
                    try {
                      await navigator.clipboard.write([
                        new ClipboardItem({ [blob.type]: blob })
                      ])
                      resolve()
                    } catch (clipboardError) {
                      reject(clipboardError)
                    }
                  } else {
                    reject(new Error('无法生成图片blob'))
                  }
                }, file.type)
              }
              img.onerror = () => {
                reject(new Error('图片加载失败'))
              }
            })
            
            // 加载图片
            const fileURL = URL.createObjectURL(file)
            img.src = fileURL
            
            // 等待图片处理完成
            await imageLoadPromise
            
            // 释放URL
            URL.revokeObjectURL(fileURL)
            
            // 等待更长时间确保剪贴板状态稳定
            await new Promise(resolve => setTimeout(resolve, 1000))
            
            uploadProgress.value = '图片已复制到剪贴板，正在上传...'
            
            // 现在让PicGo从剪贴板上传，带重试机制
            let imageUrl: string = ''
            let uploadSuccess = false
            const maxRetries = 2
            
            for (let attempt = 1; attempt <= maxRetries; attempt++) {
              try {
                uploadProgress.value = `第${attempt}次尝试上传...`
                
                imageUrl = await uploadClipboardImageToPicGo()
                uploadSuccess = true
                break
                
              } catch (uploadError) {
                if (attempt < maxRetries) {
                  // 如果是剪贴板问题且还有重试机会，等待后重试
                  const errorMsg = uploadError instanceof Error ? uploadError.message : '未知错误'
                  if (errorMsg.includes('image not found in clipboard')) {
                    uploadProgress.value = `第${attempt}次失败，等待重试...`
                    await new Promise(resolve => setTimeout(resolve, 2000))
                    continue
                  }
                }
                
                // 最后一次尝试失败或不是剪贴板问题，抛出错误
                throw uploadError
              }
            }
            
            if (!uploadSuccess || !imageUrl) {
              throw new Error('所有上传尝试都失败了')
            }
            
            // 验证URL
            if (!imageUrl || imageUrl.startsWith('data:') || imageUrl.includes('base64')) {
              throw new Error('PicGo返回的不是有效的图片URL')
            }
            
            uploadProgress.value = '上传成功！'

            // 3秒后清除状态提示
            setTimeout(() => {
              isUploadingImage.value = false
              uploadProgress.value = ''
            }, 3000)

            // 手动插入图片的 Markdown 语法到编辑器
            if (vditor) {
              const imageMarkdown = `![${file.name}](${imageUrl})\n`
              vditor.insertValue(imageMarkdown)
            }

            // 返回 null 表示我们已经手动处理了插入
            return null
            
          } catch (error) {
            uploadProgress.value = `上传失败: ${error instanceof Error ? error.message : '未知错误'}`
            
            // 上传失败时，生成base64作为备选
            try {
              uploadProgress.value = 'PicGo上传失败，使用本地预览...'
              
              const reader = new FileReader()
              const readPromise = new Promise<string>((resolve) => {
                reader.onload = (e) => {
                  const base64 = e.target?.result as string
                  resolve(base64)
                }
              })
              
              reader.readAsDataURL(file)
              const base64URL = await readPromise
              
              uploadProgress.value = '已插入本地预览（建议修复PicGo配置）'

              // 10秒后清除提示
              setTimeout(() => {
                isUploadingImage.value = false
                uploadProgress.value = ''
              }, 10000)

              // 手动插入base64图片的 Markdown 语法到编辑器
              if (vditor) {
                const imageMarkdown = `![${file.name}](${base64URL})\n`
                vditor.insertValue(imageMarkdown)
              }

              // 返回 null 表示我们已经手动处理了插入
              return null
              
            } catch (fallbackError) {
              uploadProgress.value = `图片处理失败: ${error instanceof Error ? error.message : '未知错误'}`

              // 10秒后清除错误提示
              setTimeout(() => {
                isUploadingImage.value = false
                uploadProgress.value = ''
              }, 10000)

              // 返回 null 表示上传失败，Vditor 不会插入任何内容
              return null
            }
          }
        }
      },
      
      // 禁用提示功能以避免剪贴板访问
      hint: {
        delay: 0,
        emoji: false,
        emojiPath: '',
        parse: false,
        extend: []
      },
      // 自定义快捷键
      keydown: (event: KeyboardEvent) => {
        // 检查是否在表格中按回车
        if (event.key === 'Enter' && !event.shiftKey) {
          const selection = window.getSelection()
          if (selection && selection.rangeCount > 0) {
            const range = selection.getRangeAt(0)
            const element = range.startContainer.nodeType === Node.TEXT_NODE 
              ? range.startContainer.parentElement 
              : range.startContainer as Element
            
            // 查找是否在表格单元格内
            const cell = element?.closest('td, th')
            if (cell) {
              const table = cell.closest('table')
              if (table) {
                event.preventDefault()
                
                // 找到当前行
                const currentRow = cell.closest('tr')
                const allRows = Array.from(table.querySelectorAll('tr'))
                const currentRowIndex = allRows.indexOf(currentRow as HTMLTableRowElement)
                
                // 如果是最后一行，添加新行
                if (currentRowIndex === allRows.length - 1) {
                  const newRow = (currentRow as HTMLTableRowElement).cloneNode(true) as HTMLTableRowElement
                  // 清空新行的内容
                  newRow.querySelectorAll('td, th').forEach(cell => {
                    cell.textContent = ''
                  })
                  table.appendChild(newRow)
                  
                  // 将光标移动到新行的第一个单元格
                  const firstCell = newRow.querySelector('td, th') as HTMLElement
                  if (firstCell) {
                    firstCell.focus()
                    const range = document.createRange()
                    range.selectNodeContents(firstCell)
                    range.collapse(true)
                    selection.removeAllRanges()
                    selection.addRange(range)
                  }
                  return false
                }
              }
            }
          }
        }
        return true
      },
      input: (value: string) => {
        currentContent.value = value
        emit('update:content', value)
      },
      after: () => {
        console.timeEnd('Vditor初始化')
        console.time('文档加载')

        // 简化背景设置，只执行一次
        const setWhiteBackground = () => {
          const elements = document.querySelectorAll('.vditor-content, .vditor-ir, .vditor-ir .vditor-reset')
          elements.forEach((el: any) => {
            if (el instanceof HTMLElement) {
              el.style.backgroundColor = 'white'
              el.style.setProperty('background-color', 'white', 'important')
            }
          })
        }

        // 立即设置背景，不使用轮询
        setWhiteBackground()

        // 设置滚轮事件监听 - 只在编辑器容器上监听
        if (vditorElement.value) {
          vditorElement.value.addEventListener('wheel', handleZoom, { passive: false })
        }

        // 立即加载文档内容，不延迟
        loadDocument().then(() => {
          console.timeEnd('文档加载')

          // 延迟设置焦点，但减少延迟时间
          setTimeout(() => {
            try {
              vditor?.focus()
            } catch (error) {
              // 如果焦点设置失败，不影响正常使用
            }
          }, 100) // 减少延迟时间
        }).catch(err => console.error('加载文档失败:', err))
      }
    })
  } catch (error) {
    console.error('Vditor IR 模式初始化失败:', error)
  }
}



// 保存内容
const saveContent = async () => {
  if (vditor) {
    const content = vditor.getValue()
    currentContent.value = content
    emit('update:content', content)

         if (props.filePath) {
       try {
         const fileId = parseInt(props.filePath)
         if (isNaN(fileId)) {
           console.error('无效的文件ID:', props.filePath)
           return
         }

         const updateData = {
           content: content,
           version: currentVersion.value
         }

         const saveRes: any = await API.document.updateMarkdownContent(fileId, updateData)
         if (saveRes && saveRes.data && saveRes.data.code === 200) {
           console.log('Markdown文档保存成功')

           // 保存成功后获取最新版本号
           try {
             const versionResponse: any = await API.document.getMarkdownVersion(fileId)
             if (versionResponse && versionResponse.data && versionResponse.data.code === 200) {
               currentVersion.value = versionResponse.data.data
               console.log('版本号已更新为:', currentVersion.value)
             }
           } catch (versionError) {
             console.warn('获取最新版本号失败:', versionError)
           }

           // 重新加载文档内容以确保显示最新的保存结果
           try {
             console.log('🔄 重新加载文档内容以刷新显示...')
             await loadDocument()
           } catch (reloadError) {
             console.warn('重新加载文档失败:', reloadError)
           }
         } else {
           console.error('Markdown文档保存失败:', (saveRes.data?.message) || saveRes?.message || '未知错误')
         }
       } catch (error) {
         console.error('Markdown文档保存失败:', error)
       }
     } else {
       emit('save', content)
     }
  }
}

// 处理关闭事件（退出时自动保存）
const handleClose = async () => {
  try {
    // 直接调用保存，不做复杂检测
    await saveContent()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    // 无论保存是否成功，都执行关闭操作
    emit('close')
  }
}

// 页面卸载前的保存逻辑（针对 Electron 优化）
const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  console.log('🌐 beforeunload 事件触发')

  // 检查是否有未保存的内容
  if (vditor) {
    const currentEditorContent = vditor.getValue()
    console.log('📝 检查内容变化:', {
      current: currentEditorContent.length,
      saved: currentContent.value.length,
      changed: currentEditorContent !== currentContent.value
    })

    if (currentEditorContent !== currentContent.value) {
      console.log('🔍 检测到未保存内容，执行自动保存...')

      // 有未保存的内容，尝试保存
      saveContent().then(() => {
        console.log('✅ beforeunload 自动保存完成')
      }).catch((error) => {
        console.error('❌ beforeunload 自动保存失败:', error)
      })

      // 在 Electron 中，可能需要不同的处理方式
      if (typeof window !== 'undefined' && (window as any).electronAPI) {
        // Electron 环境
        console.log('🖥️ 检测到 Electron 环境')
        event.preventDefault()
        return false
      } else {
        // 浏览器环境
        console.log('🌐 检测到浏览器环境')
        event.preventDefault()
        const message = '您有未保存的更改，确定要离开吗？'
        event.returnValue = message
        return message
      }
    }
  }
}



// 切换大纲显示状态
const toggleOutline = () => {
  showOutline.value = !showOutline.value
  
  // 重新初始化 Vditor 以应用大纲设置
  if (vditor) {
    vditor.destroy()
    nextTick(() => {
      initVditor()
    })
  }
}

// 优化的缩放功能
const handleZoom = (event: WheelEvent) => {
  // 检查是否按下 Ctrl 键
  if (event.ctrlKey) {
    event.preventDefault()

    // 根据滚轮方向调整缩放级别，使用更平滑的步长
    const delta = event.deltaY > 0 ? -0.05 : 0.05
    const newZoomLevel = Math.max(0.3, Math.min(2.5, zoomLevel.value + delta))

    zoomLevel.value = newZoomLevel
  }
}

// 重置缩放
const resetZoom = () => {
  zoomLevel.value = 1
}

// 键盘快捷键
const handleGlobalKeydown = (e: KeyboardEvent) => {
  const isCtrlOrMeta = e.ctrlKey || e.metaKey

  // Ctrl/Cmd + S 保存
  if (isCtrlOrMeta && e.key.toLowerCase() === 's') {
    e.preventDefault()
    saveContent()
    return
  }
  
  // Ctrl/Cmd + 0 重置缩放
  if (isCtrlOrMeta && e.key === '0') {
    e.preventDefault()
    resetZoom()
    return
  }

  // 只有编辑器获得焦点时才处理 Markdown 格式化快捷键
  if (!vditor) return

  // Ctrl/Cmd + 1-6 设置标题
  if (isCtrlOrMeta && ['1', '2', '3', '4', '5', '6'].includes(e.key)) {
    e.preventDefault()
    const level = parseInt(e.key)
    insertHeading(level)
    return
  }

  // Ctrl/Cmd + B 粗体
  if (isCtrlOrMeta && e.key.toLowerCase() === 'b') {
    e.preventDefault()
    toggleBold()
    return
  }

  // Ctrl/Cmd + I 斜体
  if (isCtrlOrMeta && e.key.toLowerCase() === 'i') {
    e.preventDefault()
    toggleItalic()
    return
  }

  // Ctrl/Cmd + U 下划线
  if (isCtrlOrMeta && e.key.toLowerCase() === 'u') {
    e.preventDefault()
    toggleUnderline()
    return
  }

  // Ctrl/Cmd + K 插入链接
  if (isCtrlOrMeta && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    insertLink()
    return
  }

  // Ctrl/Cmd + ` 插入行内代码
  if (isCtrlOrMeta && e.key === '`') {
    e.preventDefault()
    toggleInlineCode()
    return
  }

  // Ctrl/Cmd + Shift + ` 插入代码块
  if (isCtrlOrMeta && e.shiftKey && e.key === '`') {
    e.preventDefault()
    insertCodeBlock()
    return
  }

  // Ctrl/Cmd + L 插入列表
  if (isCtrlOrMeta && e.key.toLowerCase() === 'l') {
    e.preventDefault()
    insertList()
    return
  }

  // Ctrl/Cmd + E 插入表格
  if (isCtrlOrMeta && e.key.toLowerCase() === 'e') {
    e.preventDefault()
    insertTable()
    return
  }

  // Ctrl/Cmd + D 插入分割线
  if (isCtrlOrMeta && e.key.toLowerCase() === 'd') {
    e.preventDefault()
    insertDivider()
    return
  }

  // Ctrl/Cmd + Q 插入引用
  if (isCtrlOrMeta && e.key.toLowerCase() === 'q') {
    e.preventDefault()
    insertQuote()
    return
  }
}

// 使用 Vditor 官方 API 的快捷键功能函数
const insertHeading = (level: number) => {
  if (!vditor) return
  
  // 获取当前选中的文本
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 如果有选中文本，处理选中文本
    const headerRegex = /^(#{1,6})\s*/
    const existingHeaderMatch = selectedText.match(headerRegex)
    
    if (existingHeaderMatch) {
      const existingLevel = existingHeaderMatch[1].length
      if (existingLevel === level) {
        // 如果已经是相同级别的标题，变回普通文本
        const cleanText = selectedText.replace(headerRegex, '').trim()
        ;(vditor as any).deleteValue()
        vditor.insertValue(cleanText)
      } else {
        // 如果是不同级别的标题，改为指定级别
        const cleanText = selectedText.replace(headerRegex, '').trim()
        const headingText = '#'.repeat(level) + ' ' + cleanText
        ;(vditor as any).deleteValue()
        vditor.insertValue(headingText)
      }
    } else {
      // 不是标题，设置为指定级别标题
      const cleanText = selectedText.trim() || '标题文本'
      const headingText = '#'.repeat(level) + ' ' + cleanText
      ;(vditor as any).deleteValue()
      vditor.insertValue(headingText)
    }
  } else {
    // 如果没有选中文本，操作当前行
    // 使用简单的标记方法来定位当前行
    const marker = '||CURSOR_MARKER||'
    
    // 在光标位置插入标记
    vditor.insertValue(marker)
    
    // 获取包含标记的内容
    const contentWithMarker = vditor.getValue()
    
    // 找到标记的位置
    const markerIndex = contentWithMarker.indexOf(marker)
    
    if (markerIndex !== -1) {
      // 分析标记所在的行
      const lines = contentWithMarker.split('\n')
      let currentLineIndex = 0
      let charCount = 0
      
      // 找到包含标记的行
      for (let i = 0; i < lines.length; i++) {
        if (charCount + lines[i].length >= markerIndex) {
          currentLineIndex = i
          break
        }
        charCount += lines[i].length + 1 // +1 for newline
      }
      
      // 获取当前行（移除标记）
      const currentLine = lines[currentLineIndex].replace(marker, '')
      
      // 检查当前行是否已经是标题
      const headerRegex = /^(#{1,6})\s*/
      const existingHeaderMatch = currentLine.match(headerRegex)
      
      if (existingHeaderMatch) {
        const existingLevel = existingHeaderMatch[1].length
        if (existingLevel === level) {
          // 如果已经是相同级别的标题，变回普通文本
          const cleanLine = currentLine.replace(headerRegex, '').trim()
          lines[currentLineIndex] = cleanLine
        } else {
          // 如果是不同级别的标题，改为指定级别
          const cleanLine = currentLine.replace(headerRegex, '').trim()
          const newHeaderText = '#'.repeat(level) + ' ' + cleanLine
          lines[currentLineIndex] = newHeaderText
        }
      } else {
        // 不是标题，设置为指定级别标题
        const cleanLine = currentLine.trim() || '标题文本'
        const newHeaderText = '#'.repeat(level) + ' ' + cleanLine
        lines[currentLineIndex] = newHeaderText
      }
      
      // 更新编辑器内容（移除标记）
      const newContent = lines.join('\n')
      vditor.setValue(newContent)
    } else {
      // 如果找不到标记，移除标记并使用备用方案
      const cleanContent = contentWithMarker.replace(marker, '')
      const headingText = '#'.repeat(level) + ' 标题文本'
      vditor.setValue(cleanContent)
      vditor.insertValue(headingText)
    }
    
    // 重新聚焦编辑器
    setTimeout(() => {
      vditor?.focus()
    }, 50)
  }
}

const toggleBold = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 检查是否已经是粗体格式
    if (selectedText.startsWith('**') && selectedText.endsWith('**')) {
      // 移除粗体格式
      const cleanText = selectedText.slice(2, -2)
      ;(vditor as any).deleteValue()
      vditor.insertValue(cleanText)
    } else {
      // 添加粗体格式
      const boldText = `**${selectedText}**`
      ;(vditor as any).deleteValue()
      vditor.insertValue(boldText)
    }
  } else {
    // 没有选中文本，插入粗体模板
    vditor.insertValue('**粗体文本**')
  }
}

const toggleItalic = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 检查是否已经是斜体格式
    if (selectedText.startsWith('*') && selectedText.endsWith('*') && !selectedText.startsWith('**')) {
      // 移除斜体格式
      const cleanText = selectedText.slice(1, -1)
      ;(vditor as any).deleteValue()
      vditor.insertValue(cleanText)
    } else {
      // 添加斜体格式
      const italicText = `*${selectedText}*`
      ;(vditor as any).deleteValue()
      vditor.insertValue(italicText)
    }
  } else {
    // 没有选中文本，插入斜体模板
    vditor.insertValue('*斜体文本*')
  }
}

const toggleUnderline = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 检查是否已经是下划线格式
    if (selectedText.startsWith('<u>') && selectedText.endsWith('</u>')) {
      // 移除下划线格式
      const cleanText = selectedText.slice(3, -4)
      ;(vditor as any).deleteValue()
      vditor.insertValue(cleanText)
    } else {
      // 添加下划线格式
      const underlineText = `<u>${selectedText}</u>`
      ;(vditor as any).deleteValue()
      vditor.insertValue(underlineText)
    }
  } else {
    // 没有选中文本，插入下划线模板
    vditor.insertValue('<u>下划线文本</u>')
  }
}

const toggleInlineCode = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 检查是否已经是行内代码格式
    if (selectedText.startsWith('`') && selectedText.endsWith('`')) {
      // 移除行内代码格式
      const cleanText = selectedText.slice(1, -1)
      ;(vditor as any).deleteValue()
      vditor.insertValue(cleanText)
    } else {
      // 添加行内代码格式
      const codeText = `\`${selectedText}\``
      ;(vditor as any).deleteValue()
      vditor.insertValue(codeText)
    }
  } else {
    // 没有选中文本，插入行内代码模板
    vditor.insertValue('`行内代码`')
  }
}

const insertCodeBlock = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 将选中文本包装为代码块
    const codeBlock = `\n\`\`\`\n${selectedText}\n\`\`\`\n`
    ;(vditor as any).deleteValue()
    vditor.insertValue(codeBlock)
  } else {
    // 没有选中文本，插入代码块模板
    vditor.insertValue('\n```\n代码块\n```\n')
  }
}

const insertLink = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 将选中文本作为链接文本
    const linkText = `[${selectedText}](URL)`
    ;(vditor as any).deleteValue()
    vditor.insertValue(linkText)
  } else {
    // 没有选中文本，插入链接模板
    vditor.insertValue('[链接文本](URL)')
  }
}

const insertList = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 将选中文本转为列表项
    const lines = selectedText.split('\n')
    const listText = lines.map((line: string) => line.trim() ? `- ${line.trim()}` : '').join('\n')
    ;(vditor as any).deleteValue()
    vditor.insertValue(listText)
  } else {
    // 没有选中文本，插入列表模板
    vditor.insertValue('\n- 列表项1\n- 列表项2\n- 列表项3\n')
  }
}

const insertTable = () => {
  if (!vditor) return
  
  const tableText = '\n| 标题1 | 标题2 | 标题3 |\n|-------|-------|-------|\n| 内容1 | 内容2 | 内容3 |\n| 内容4 | 内容5 | 内容6 |\n'
  vditor.insertValue(tableText)
}

const insertDivider = () => {
  if (!vditor) return
  
  const dividerText = '\n---\n'
  vditor.insertValue(dividerText)
}

const insertQuote = () => {
  if (!vditor) return
  
  const selectedText = (vditor as any).getSelection()
  
  if (selectedText) {
    // 将选中文本转为引用
    const lines = selectedText.split('\n')
    const quoteText = lines.map((line: string) => line.trim() ? `> ${line.trim()}` : '>').join('\n')
    ;(vditor as any).deleteValue()
    vditor.insertValue(quoteText)
  } else {
    // 没有选中文本，插入引用模板
    vditor.insertValue('> 引用文本')
  }
}



// 监听 props 变化
watch(() => props.content, (newContent) => {
  if (newContent && newContent !== currentContent.value) {
    currentContent.value = newContent
    
    if (vditor) {
      vditor.setValue(newContent)
    }
  }
}, { immediate: true })

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
      showContextMenu.value = false
      translatedText.value = ''
      isTranslating.value = false
    } catch (err) {
      console.error('复制失败:', err)
    }
  }
}

// 翻译文本
const translateText = async () => {
  if (selectedText.value) {
    isTranslating.value = true
    translatedText.value = ''
    
    try {
      const response = await API.translate.translate({
        file_name: 'selected_text.txt',
        text: selectedText.value,
        target_lang: 'zh-CN'
      }) as any;
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
}

// 点击其他地方关闭右键菜单
const handleClickOutside = (event: MouseEvent) => {
  if (showContextMenu.value) {
    const target = event.target as Element
    const contextMenu = document.querySelector('.context-menu')
    
    if (contextMenu && !contextMenu.contains(target)) {
      showContextMenu.value = false
      translatedText.value = ''
      isTranslating.value = false
    }
  }
}

// 监听 filePath 变化，重新加载文档
watch(() => props.filePath, async (newFilePath, oldFilePath) => {
  if (newFilePath && newFilePath !== oldFilePath) {
    console.log('文档路径变化，重新加载:', newFilePath)
    await loadDocument()
  }
}, { immediate: false })

// 生命周期
onMounted(async () => {
  document.addEventListener('keydown', handleGlobalKeydown)
  await nextTick()

  // 先初始化编辑器，再加载文档内容
  await initVditor() // loadDocument 会在 after 钩子里触发

  document.addEventListener('click', handleClickOutside)

  // 添加页面卸载前的保存逻辑
  window.addEventListener('beforeunload', handleBeforeUnload)

  // 添加路由导航守卫（针对 Electron 环境）
  removeRouterGuard = router.beforeEach(async (to: any, from: any, next: any) => {
    console.log('🧭 路由导航守卫触发:', { from: from.path, to: to.path })

    // 检查是否有未保存的内容
    if (vditor) {
      const currentEditorContent = vditor.getValue()
      if (currentEditorContent !== currentContent.value) {
        console.log('🔍 路由切换时检测到未保存内容，执行自动保存...')

        try {
          await saveContent()
          console.log('✅ 路由切换前自动保存完成')
        } catch (error) {
          console.error('❌ 路由切换前自动保存失败:', error)
        }
      }
    }

    next()
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)

  // 清理 beforeunload 事件监听器
  window.removeEventListener('beforeunload', handleBeforeUnload)

  // 清理路由守卫
  if (typeof removeRouterGuard === 'function') {
    removeRouterGuard()
  }

  if (vditorElement.value) {
    vditorElement.value.removeEventListener('wheel', handleZoom)
  }

  vditor?.destroy()
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* 隐藏 Vditor 的提示和工具栏 */
:deep(.vditor-tip) {
  display: none !important;
}

:deep(.vditor-hint) {
  display: none !important;
}

:deep(.vditor-counter) {
  display: none !important;
}

/* 强制隐藏工具栏 */
:deep(.vditor-toolbar) {
  display: none !important;
}

:deep(.vditor-toolbar--hide) {
  display: none !important;
}

/* 隐藏代码块预览弹窗 */
:deep(.vditor-hint--emoji) {
  display: none !important;
}

:deep(.vditor-hint) {
  display: none !important;
}

:deep(.vditor-tooltipped) {
  display: none !important;
}

:deep(.vditor-panel) {
  display: none !important;
}

:deep(.vditor-panel--none) {
  display: none !important;
}

/* 确保编辑器内容区域占80%宽度，左右留空 */
:deep(.vditor) {
  background-color: white !important;
  border: none !important;
  width: 100% !important;
  max-width: none !important;
}

:deep(.vditor-content) {
  background-color: white !important;
  width: 100% !important;
  max-width: none !important;
}

:deep(.vditor-ir) {
  background-color: white !important;
  width: 100% !important;
  max-width: none !important;
  padding: 0 !important;
}

/* 编辑器内容区域 - 优化缩放 */
:deep(.vditor-ir .vditor-reset) {
  background-color: white !important;
  width: 80% !important;
  max-width: none !important;
  margin: 0 auto !important;
  padding: 2rem !important;
  border: none !important;
  transform-origin: top center !important;
  transform: scale(v-bind(zoomLevel)) !important;
  transition: transform 0.2s ease-out !important;
}

/* 工具栏样式 */
:deep(.vditor-toolbar) {
  border: none !important;
  background-color: #f9fafb !important;
}

/* 大纲栏边框 */
:deep(.vditor-outline) {
  border-right: 1px solid #e5e7eb !important;
  background-color: #f9fafb !important;
}

:deep(.vditor-content .vditor-reset) {
  width: 80% !important;
  max-width: none !important;
  margin: 0 auto !important;
  padding: 2rem !important;
  border: none !important;
}

/* 自定义滚动条样式 - 使用更强的选择器 */
.markdown-viewer :deep(.vditor-ir),
.markdown-viewer :deep(.vditor-content),
.markdown-viewer :deep(.vditor-preview),
.markdown-viewer :deep(.vditor-ir .vditor-reset),
.markdown-viewer :deep(.vditor-content .vditor-reset) {
  /* Firefox滚动条 */
  scrollbar-width: thin;
  scrollbar-color: rgba(156, 163, 175, 0.4) transparent;
}

/* WebKit浏览器滚动条 - 更强的选择器 */
.markdown-viewer :deep(.vditor-ir::-webkit-scrollbar),
.markdown-viewer :deep(.vditor-content::-webkit-scrollbar),
.markdown-viewer :deep(.vditor-preview::-webkit-scrollbar),
.markdown-viewer :deep(.vditor-ir .vditor-reset::-webkit-scrollbar),
.markdown-viewer :deep(.vditor-content .vditor-reset::-webkit-scrollbar) {
  width: 6px !important;
  height: 6px !important;
}

.markdown-viewer :deep(.vditor-ir::-webkit-scrollbar-track),
.markdown-viewer :deep(.vditor-content::-webkit-scrollbar-track),
.markdown-viewer :deep(.vditor-preview::-webkit-scrollbar-track),
.markdown-viewer :deep(.vditor-ir .vditor-reset::-webkit-scrollbar-track),
.markdown-viewer :deep(.vditor-content .vditor-reset::-webkit-scrollbar-track) {
  background: transparent !important;
  border-radius: 0 !important;
}

.markdown-viewer :deep(.vditor-ir::-webkit-scrollbar-thumb),
.markdown-viewer :deep(.vditor-content::-webkit-scrollbar-thumb),
.markdown-viewer :deep(.vditor-preview::-webkit-scrollbar-thumb),
.markdown-viewer :deep(.vditor-ir .vditor-reset::-webkit-scrollbar-thumb),
.markdown-viewer :deep(.vditor-content .vditor-reset::-webkit-scrollbar-thumb) {
  background: rgba(156, 163, 175, 0.3) !important;
  border-radius: 3px !important;
  border: 1px solid transparent !important;
  background-clip: padding-box !important;
  max-height: 30px !important;
}

.markdown-viewer :deep(.vditor-ir::-webkit-scrollbar-thumb:hover),
.markdown-viewer :deep(.vditor-content::-webkit-scrollbar-thumb:hover),
.markdown-viewer :deep(.vditor-preview::-webkit-scrollbar-thumb:hover),
.markdown-viewer :deep(.vditor-ir .vditor-reset::-webkit-scrollbar-thumb:hover),
.markdown-viewer :deep(.vditor-content .vditor-reset::-webkit-scrollbar-thumb:hover) {
  background: rgba(156, 163, 175, 0.6) !important;
  background-clip: padding-box !important;
}

.markdown-viewer :deep(.vditor-ir::-webkit-scrollbar-corner),
.markdown-viewer :deep(.vditor-content::-webkit-scrollbar-corner),
.markdown-viewer :deep(.vditor-preview::-webkit-scrollbar-corner),
.markdown-viewer :deep(.vditor-ir .vditor-reset::-webkit-scrollbar-corner),
.markdown-viewer :deep(.vditor-content .vditor-reset::-webkit-scrollbar-corner) {
  background: transparent !important;
}

/* 额外的滚动条样式覆盖 */
.markdown-viewer :deep(*::-webkit-scrollbar) {
  width: 6px !important;
  height: 6px !important;
}

.markdown-viewer :deep(*::-webkit-scrollbar-track) {
  background: transparent !important;
}

.markdown-viewer :deep(*::-webkit-scrollbar-thumb) {
  background: rgba(156, 163, 175, 0.3) !important;
  border-radius: 3px !important;
  border: 1px solid transparent !important;
  background-clip: padding-box !important;
  min-height: 20px !important;
}

.markdown-viewer :deep(*::-webkit-scrollbar-thumb:hover) {
  background: rgba(156, 163, 175, 0.6) !important;
  background-clip: padding-box !important;
}

.markdown-viewer :deep(*::-webkit-scrollbar-corner) {
  background: transparent !important;
}

/* 在不同屏幕尺寸下优化内容布局 */
@media (min-width: 768px) {
  :deep(.vditor-ir .vditor-reset) {
    padding: 2rem 3rem !important;
  }
  
  :deep(.vditor-content .vditor-reset) {
    padding: 2rem 3rem !important;
  }
}

@media (min-width: 1024px) {
  :deep(.vditor-ir .vditor-reset) {
    padding: 2rem 4rem !important;
  }
  
  :deep(.vditor-content .vditor-reset) {
    padding: 2rem 4rem !important;
  }
}

@media (min-width: 1280px) {
  :deep(.vditor-ir .vditor-reset) {
    padding: 2rem 5rem !important;
  }
  
  :deep(.vditor-content .vditor-reset) {
    padding: 2rem 5rem !important;
  }
}

/* 移除其他区域的边框 */
:deep(.vditor-content),
:deep(.vditor-ir),
:deep(.vditor-preview) {
  border: none !important;
}

/* 确保编辑器占满整个容器 */
.markdown-viewer :deep(.vditor) {
  height: 100% !important;
  width: 100% !important;
}

/* 自定义 div 样式 */
:deep(.info) {
  background: #e7f3ff;
  border-left: 4px solid #2196f3;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 4px;
}

:deep(.warning) {
  background: #fff8e1;
  border-left: 4px solid #ff9800;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 4px;
}

:deep(.error) {
  background: #ffebee;
  border-left: 4px solid #f44336;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 4px;
}

:deep(.success) {
  background: #e8f5e8;
  border-left: 4px solid #4caf50;
  padding: 12px 16px;
  margin: 16px 0;
  border-radius: 4px;
}

/* 代码块语言标签 */
:deep(.code-language-tag) {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-family: monospace;
  z-index: 10;
}

/* 代码块样式 */
:deep(pre) {
  position: relative;
  background: #f6f8fa !important;
  border: 1px solid #e1e8ed;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
}

:deep(pre code) {
  background: #f6f8fa !important;
  padding: 0 !important;
  border-radius: 0 !important;
  border: none !important;
}

/* 行内代码样式 */
:deep(code) {
  background: #f1f3f4 !important;
  padding: 2px 4px !important;
  border-radius: 3px !important;
  font-family: 'Fira Code', 'JetBrains Mono', 'Cascadia Code', 'SFMono-Regular', Consolas, monospace !important;
  font-size: 0.9em !important;
}

/* 确保代码块内的代码有背景 */
:deep(.vditor-ir .vditor-reset pre code),
:deep(.vditor-ir .vditor-reset code) {
  background: #f6f8fa !important;
}

:deep(.vditor-ir .vditor-reset code:not(pre code)) {
  background: #f1f3f4 !important;
  padding: 2px 4px !important;
  border-radius: 3px !important;
}

/* 优化大纲样式 */
:deep(.vditor-outline) {
  font-size: 14px !important;
  padding: 16px !important;
}

:deep(.vditor-outline .vditor-outline__title) {
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #374151 !important;
  margin-bottom: 12px !important;
  padding-bottom: 8px !important;
  border-bottom: 1px solid #e5e7eb !important;
}

:deep(.vditor-outline .vditor-outline__item) {
  font-size: 14px !important;
  line-height: 1.5 !important;
  margin: 4px 0 !important;
  padding: 4px 8px !important;
  border-radius: 4px !important;
  transition: all 0.2s ease !important;
  color: #6b7280 !important;
  text-decoration: none !important;
  display: block !important;
  position: relative !important;
}

:deep(.vditor-outline .vditor-outline__item:hover) {
  background-color: #f3f4f6 !important;
  color: #374151 !important;
}

:deep(.vditor-outline .vditor-outline__item.vditor-outline__item--current) {
  background-color: #dbeafe !important;
  color: #1d4ed8 !important;
  font-weight: 500 !important;
}

/* 移除大纲项目前面的点和缩进标记 */
:deep(.vditor-outline .vditor-outline__item::before) {
  display: none !important;
}

:deep(.vditor-outline .vditor-outline__item::after) {
  display: none !important;
}

/* 根据标题级别设置缩进，但不显示点 */
:deep(.vditor-outline .vditor-outline__item[data-level="1"]) {
  padding-left: 8px !important;
  font-weight: 600 !important;
}

:deep(.vditor-outline .vditor-outline__item[data-level="2"]) {
  padding-left: 20px !important;
  font-weight: 500 !important;
}

:deep(.vditor-outline .vditor-outline__item[data-level="3"]) {
  padding-left: 32px !important;
}

:deep(.vditor-outline .vditor-outline__item[data-level="4"]) {
  padding-left: 44px !important;
}

:deep(.vditor-outline .vditor-outline__item[data-level="5"]) {
  padding-left: 56px !important;
}

:deep(.vditor-outline .vditor-outline__item[data-level="6"]) {
  padding-left: 68px !important;
}

/* 列表样式 */
:deep(ul), :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
  list-style: inherit !important;
}

:deep(ul) {
  list-style-type: disc !important;
}

:deep(ol) {
  list-style-type: decimal !important;
}

:deep(li) {
  margin: 0.25em 0;
  display: list-item !important;
  list-style: inherit !important;
}

:deep(ul ul), :deep(ol ol), :deep(ul ol), :deep(ol ul) {
  margin: 0.25em 0;
}

:deep(ul ul) {
  list-style-type: circle !important;
}

:deep(ul ul ul) {
  list-style-type: square !important;
}

/* 确保Vditor编辑器中的列表样式正确显示 */
:deep(.vditor-ir .vditor-reset ul),
:deep(.vditor-content .vditor-reset ul) {
  list-style-type: disc !important;
  padding-left: 1.5em !important;
}

:deep(.vditor-ir .vditor-reset ol),
:deep(.vditor-content .vditor-reset ol) {
  list-style-type: decimal !important;
  padding-left: 1.5em !important;
}

:deep(.vditor-ir .vditor-reset li),
:deep(.vditor-content .vditor-reset li) {
  display: list-item !important;
  list-style: inherit !important;
  margin: 0.25em 0 !important;
}

:deep(.vditor-ir .vditor-reset ul ul),
:deep(.vditor-content .vditor-reset ul ul) {
  list-style-type: circle !important;
}

:deep(.vditor-ir .vditor-reset ul ul ul),
:deep(.vditor-content .vditor-reset ul ul ul) {
  list-style-type: square !important;
}




</style>