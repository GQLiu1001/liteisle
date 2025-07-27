import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { API } from '@/utils/api'
import { onTransferLogUpdated, onFileStatusUpdated, onShareSaveCompleted, connectWebSocket } from '@/utils/websocket'
import { useToast } from 'vue-toastification'
import { TransferTypeEnum, TransferStatusEnum } from '@/types/api'
import { useMusicStore } from '@/store/MusicStore' // 导入MusicStore
import { useDocsStore } from '@/store/DocsStore' // 导入DocsStore
import { useSettingsStore } from '@/store/SettingsStore' // 导入SettingsStore
import type {
  TransferSummaryResp,
  TransferLogPageResp,
  TransferLogItem,
  TransferStatusUpdateReq,
  FileUploadAsyncResp,
  DownloadSessionResp,
  DownloadFileItem,
  ItemsSelectionReq
} from '@/types/api'

// 扩展的传输任务类型，包含UI所需的额外信息
export interface ExtendedTransferItem extends TransferLogItem {
  progress?: number           // 进度百分比
  speed?: string             // 传输速度
  error_message?: string     // 错误信息
  file_path?: string         // 本地文件路径（下载任务）
  transfer_status?: TransferStatusEnum // 任务状态
}

export const useTransferStore = defineStore('transfer', () => {
  const toast = useToast()
  const musicStore = useMusicStore()
  const docsStore = useDocsStore()
  const settingsStore = useSettingsStore()
  
  // 防抖定时器
  let musicRefreshTimer: number | null = null
  let docsRefreshTimer: number | null = null
  
  // === 传输统计状态 ===
  const uploadCount = ref(0)
  const downloadCount = ref(0)
  const isLoadingSummary = ref(false)
  
  // === 传输历史记录 ===
  const processingTasks = ref<ExtendedTransferItem[]>([])    // 进行中的任务
  const completedTasks = ref<ExtendedTransferItem[]>([])     // 已完成的任务
  const isLoadingTasks = ref(false)
  
  // === 分页信息 ===
  const processingPagination = ref({
    total: 0,
    current_page: 1,
    page_size: 20,
    hasMore: true
  })
  
  const completedPagination = ref({
    total: 0,
    current_page: 1,
    page_size: 20,
    hasMore: true
  })
  
  // === 当前活动的传输任务 ===
  const activeUploads = ref<Map<number, { file: File; progress: number; xhr?: XMLHttpRequest }>>(new Map())
  const activeDownloads = ref<Map<number, { progress: number; controller?: AbortController; startTime?: number }>>(new Map())
  
  // === 计算属性 ===
  const totalProcessingTasks = computed(() => processingTasks.value.length)
  const totalCompletedTasks = computed(() => completedTasks.value.length)
  const totalActiveTasks = computed(() => activeUploads.value.size + activeDownloads.value.size)
  
  const processingUploadTasks = computed(() => 
    processingTasks.value.filter(task => task.transfer_type === TransferTypeEnum.UPLOAD)
  )
  
  const processingDownloadTasks = computed(() => 
    processingTasks.value.filter(task => task.transfer_type === TransferTypeEnum.DOWNLOAD)
  )
  
  const hasTasks = computed(() => totalProcessingTasks.value > 0 || totalCompletedTasks.value > 0)
  
  /**
   * 设置WebSocket监听
   */
  const setupWebSocketListeners = () => {
    onTransferLogUpdated((payload) => {
      console.log('📋 收到传输日志更新:', payload)
      if (payload && payload.logId) {
        updateTransferTaskStatus(payload.logId, payload.log_status, payload.error_message)
      }
    })

    onFileStatusUpdated((payload: any) => {
      console.log('📁 收到文件状态更新:', payload)
      // 根据实际消息格式，字段名是 logId, transferStatus, errorMessage, progress
      if (payload && payload.logId) {
        // 如果有进度信息，先更新进度
        if (payload.progress !== undefined) {
          updateTaskProgress(payload.logId, payload.progress)
        }

        // 如果有状态信息，更新状态
        if (payload.transferStatus) {
          console.log(`🔄 更新任务状态: logId=${payload.logId}, status=${payload.transferStatus}`)
          updateTransferTaskStatus(payload.logId, payload.transferStatus, payload.errorMessage)
          
          // 如果任务完成（成功或失败），确保清理活跃任务状态
          if (payload.transferStatus === 'success' || payload.transferStatus === 'failed') {
            console.log(`🧹 WebSocket完成清理: logId=${payload.logId}, status=${payload.transferStatus}`)
            activeUploads.value.delete(payload.logId)
            activeDownloads.value.delete(payload.logId)
          }
        }
      }
    })

    // 监听转存完成事件
    onShareSaveCompleted((payload) => {
      console.log('📋 收到转存完成通知:', payload)

      if (payload && payload.totalFiles > 0) {
        // 更新所有相关任务的状态
        if (payload.successCount > 0) {
          // 有成功的文件，刷新相关库
          setTimeout(() => {
            const musicStore = useMusicStore()
            const docsStore = useDocsStore()

            console.log('🎵 转存完成，刷新音乐库')
            musicStore.loadPlaylistsFromDrive()

            console.log('📄 转存完成，刷新文档库')
            docsStore.loadCategoriesFromDrive()
          }, 500)

          toast.success(`转存完成！成功转存 ${payload.successCount} 个文件`)
        }

        if (payload.failedCount > 0) {
          toast.error(`转存部分失败，${payload.failedCount} 个文件转存失败`)
        }
      }
    })
  }

  // 初始化时设置WebSocket监听
  setupWebSocketListeners()
  
  /**
   * 加载传输统计摘要
   */
  const loadTransferSummary = async (): Promise<void> => {
    try {
      isLoadingSummary.value = true
      const response = await API.transfer.getSummary()
      
      if (response.data) {
        uploadCount.value = response.data.upload_count
        downloadCount.value = response.data.download_count
      }
    } catch (error) {
      console.error('加载传输摘要失败:', error)
    } finally {
      isLoadingSummary.value = false
    }
  }
  
  /**
   * 加载传输历史记录
   */
  const loadTransferHistory = async (
    status: 'processing' | 'success',
    reset = false
  ): Promise<void> => {
    try {
      isLoadingTasks.value = true
      
      const pagination = status === 'processing' ? processingPagination.value : completedPagination.value
      const page = reset ? 1 : pagination.current_page
      
      const response = await API.transfer.getHistory(status, page, pagination.page_size)

      console.log('传输历史API响应:', response.data)

      if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
        const transferData = (response.data as any).data
        const records = transferData.records || []

        const tasks = records.map((task: any) => ({
          ...task,
          progress: 100 // 历史记录默认为完成状态
        }))
        
        if (status === 'processing') {
          if (reset) {
            processingTasks.value = tasks
            processingPagination.value.current_page = 1
          } else {
            processingTasks.value.push(...tasks)
          }
          processingPagination.value.total = transferData.total || 0
          processingPagination.value.hasMore = processingTasks.value.length < (transferData.total || 0)
        } else {
          if (reset) {
            completedTasks.value = tasks
            completedPagination.value.current_page = 1
          } else {
            completedTasks.value.push(...tasks)
          }
          completedPagination.value.total = transferData.total || 0
          completedPagination.value.hasMore = completedTasks.value.length < (transferData.total || 0)
        }
      }
    } catch (error) {
      console.error('加载传输历史失败:', error)
      toast.error('加载传输历史失败')
    } finally {
      isLoadingTasks.value = false
    }
  }
  
  /**
   * 加载更多传输记录
   */
  const loadMoreTasks = async (status: 'processing' | 'success'): Promise<void> => {
    const pagination = status === 'processing' ? processingPagination.value : completedPagination.value

    if (!pagination.hasMore || isLoadingTasks.value) {
      return
    }

    pagination.current_page++
    await loadTransferHistory(status)
  }
  
  /**
   * 上传文件
   */
  const uploadFile = async (
    file: File,
    folderId: number,
    onProgress?: (progress: number) => void
  ): Promise<FileUploadAsyncResp | null> => {
    try {
      const response = await API.upload.uploadFile(file, folderId, (progress) => {
        // 更新本地进度状态
        if (onProgress) {
          onProgress(progress)
        }
      })

      console.log('上传文件API响应:', response)

      // 根据API文档，上传接口返回 202 状态码表示文件已接收，正在后台处理
      if (response.data && ((response.data as any).code === 200 || (response.data as any).code === 202) && (response.data as any).data) {
        const uploadData = (response.data as any).data

        // 添加到处理中的任务列表

        const baseTask: ExtendedTransferItem = {
          log_id: uploadData.log_id,
          item_name: file.name,
          item_size: file.size,
          transfer_type: TransferTypeEnum.UPLOAD,
          create_time: new Date().toISOString(),
          progress: uploadData.log_status === 'success' ? 100 : 0
        }

        if (uploadData.log_status === 'success') {
          // 秒传直接完成
          completedTasks.value.unshift(baseTask)
          console.log('⚡ 秒传完成，直接标记任务成功')
        } else {
          // 正常上传流程
          processingTasks.value.unshift(baseTask)
          activeUploads.value.set(uploadData.log_id, { file, progress: 0 })
          console.log(`📤 上传任务添加: ${file.name}, logId=${uploadData.log_id}, 活跃任务数=${totalActiveTasks.value}`)
        }

        uploadCount.value++

        // 如果有 initial_file_data，可以在这里处理文件的初始状态
        if (uploadData.initial_file_data) {
          console.log('文件初始数据:', uploadData.initial_file_data)
          
          // 检查文件是否直接变成 available 状态（如文档文件）
          const fileData = uploadData.initial_file_data
          if (fileData.file_status === 'available') {
            console.log('📄 文件上传后直接可用，立即检查是否需要刷新页面')
            
            // 检查是否为音乐文件
            const isMusicFile = fileData.name?.endsWith('.mp3') || 
                                fileData.name?.endsWith('.flac') || 
                                fileData.name?.endsWith('.wav') ||
                                fileData.name?.endsWith('.m4a') ||
                                fileData.name?.endsWith('.aac');
            
            // 检查是否为文档文件
            const isDocumentFile = fileData.name?.endsWith('.pdf') ||
                                    fileData.name?.endsWith('.doc') ||
                                    fileData.name?.endsWith('.docx') ||
                                    fileData.name?.endsWith('.txt') ||
                                    fileData.name?.endsWith('.md') ||
                                    fileData.name?.endsWith('.xls') ||
                                    fileData.name?.endsWith('.xlsx') ||
                                    fileData.name?.endsWith('.ppt') ||
                                    fileData.name?.endsWith('.pptx');
            
            console.log(`🎵 是否为音乐文件: ${isMusicFile}`);
            console.log(`📄 是否为文档文件: ${isDocumentFile}`);
            
            if (isMusicFile) {
              console.log('🎵 音乐文件直接可用，正在安排刷新音乐库...');
              // 使用防抖机制，避免频繁刷新
              if (musicRefreshTimer) {
                clearTimeout(musicRefreshTimer)
              }
              musicRefreshTimer = setTimeout(() => {
                console.log('🎵 执行音乐库刷新');
                musicStore.loadPlaylistsFromDrive();
                musicRefreshTimer = null
              }, 500) // 500ms防抖延迟
            }
            
            if (isDocumentFile) {
              console.log('📄 文档文件直接可用，正在安排刷新文档库...');
              // 使用防抖机制，避免频繁刷新
              if (docsRefreshTimer) {
                clearTimeout(docsRefreshTimer)
              }
              docsRefreshTimer = setTimeout(() => {
                console.log('📄 执行文档库刷新');
                docsStore.loadCategoriesFromDrive();
                docsRefreshTimer = null
              }, 500) // 500ms防抖延迟
            }
            
            toast.success(`文件 "${fileData.name}" 上传成功`)
          } else {
            toast.success('文件已接收，正在后台处理')
          }
        } else {
          toast.success('文件已接收，正在后台处理')
        }

        return uploadData
      } else {
        console.warn('上传文件API响应格式错误:', response.data)
        const errorMessage = (response.data as any)?.message || '上传文件失败'

        // 特殊处理上传文件夹不存在的错误
        if (errorMessage.includes('上传文件夹不存在') || errorMessage.includes('文件夹不存在')) {
          toast.error('上传文件夹不存在，请确保您的账户已正确初始化系统文件夹')
          console.error('系统文件夹缺失，建议检查用户账户初始化状态')
        } else {
          toast.error(errorMessage)
        }
        return null
      }
    } catch (error) {
      console.error('上传文件失败:', error)
      toast.error('网络错误，上传文件失败')
      return null
    }
  }

  /**
   * 批量上传文件
   */
  const uploadFiles = async (
    files: File[],
    targetPath: string,
    onProgress?: (fileIndex: number, progress: number) => void
  ): Promise<FileUploadAsyncResp[]> => {
    if (!files || files.length === 0) {
      toast.warning('请选择要上传的文件')
      return []
    }

    // 在上传前确保WebSocket连接
    const token = localStorage.getItem('access_token')
    if (token) {
      connectWebSocket(token)
    }

    try {
      let folderId: number

      // 特殊处理：如果是传输页面的上传任务（路径为 '/上传'）
      if (targetPath === '/上传') {
        // 直接使用 folder_id: 0，让后端自动处理
        // 根据后端代码，当 folderId 为 0 时会自动查找用户的上传系统文件夹
        folderId = 0
        console.log('传输页面上传任务，使用 folder_id: 0，后端将自动查找上传系统文件夹')
      } else {
        // 其他情况需要根据路径获取文件夹ID
        const resolvedFolderId = await getFolderIdByPath(targetPath)
        if (!resolvedFolderId) {
          toast.error('无法确定上传目标文件夹')
          return []
        }
        folderId = resolvedFolderId
      }

      const results: FileUploadAsyncResp[] = []
      let successCount = 0
      let failCount = 0

      // 逐个上传文件
      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        const logMessage = folderId === 0
          ? `开始上传文件 ${i + 1}/${files.length}: ${file.name} (传输页面上传任务)`
          : `开始上传文件 ${i + 1}/${files.length}: ${file.name}`
        console.log(logMessage)

        const result = await uploadFile(file, folderId, (progress) => {
          if (onProgress) {
            onProgress(i, progress)
          }
        })

        if (result) {
          results.push(result)
          successCount++
        } else {
          failCount++
        }
      }

      // 显示上传结果
      if (successCount > 0) {
        toast.success(`成功开始上传 ${successCount} 个文件`)
      }
      if (failCount > 0) {
        toast.error(`${failCount} 个文件上传失败`)
      }

      return results
    } catch (error) {
      console.error('批量上传文件失败:', error)
      toast.error('批量上传文件失败')
      return []
    }
  }



  /**
   * 根据路径获取文件夹ID（如果不存在则创建）
   */
  const getFolderIdByPath = async (path: string): Promise<number | null> => {
    try {
      console.log('获取文件夹ID，路径:', path)

      // 获取文件夹层级信息
      const response = await API.folder.getFolderHierarchy()

      if (!response.data || (response.data as any).code !== 200 || !(response.data as any).data) {
        console.error('获取文件夹层级失败:', response.data)
        return null
      }

      const folderHierarchy = (response.data as any).data as any[]
      console.log('文件夹层级:', folderHierarchy)

      // 解析路径，例如 "/音乐/我的歌单" -> ["音乐", "我的歌单"]
      const pathParts = path.split('/').filter(part => part.trim() !== '')
      console.log('路径部分:', pathParts)

      if (pathParts.length === 0) {
        console.error('路径为空')
        return null
      }

      // 查找对应的文件夹
      let currentFolder: any = null

      // 首先查找第一级文件夹（系统文件夹）
      const firstLevelName = pathParts[0]
      currentFolder = folderHierarchy.find((folder: any) =>
        folder.folder_name === firstLevelName && folder.folder_type === 'system'
      )

      if (!currentFolder) {
        console.error(`未找到系统文件夹: ${firstLevelName}`)
        return null
      }

      console.log(`找到系统文件夹: ${firstLevelName}, ID: ${currentFolder.id}`)

      // 如果只有一级路径，直接返回系统文件夹ID
      if (pathParts.length === 1) {
        return currentFolder.id
      }

      // 查找第二级文件夹（用户创建的歌单/书单）
      const secondLevelName = pathParts[1]

      // 获取系统文件夹的内容
      const folderContentResponse = await API.folder.getFolderContent(currentFolder.id)

      if (!folderContentResponse.data || (folderContentResponse.data as any).code !== 200 || !(folderContentResponse.data as any).data) {
        console.error('获取文件夹内容失败:', folderContentResponse.data)
        return null
      }

      const folderContent = (folderContentResponse.data as any).data
      const targetFolder = folderContent.folders.find((folder: any) => folder.folder_name === secondLevelName)

      if (targetFolder) {
        console.log(`找到目标文件夹: ${secondLevelName}, ID: ${targetFolder.id}`)
        return targetFolder.id
      }

      // 如果找不到目标文件夹，尝试创建
      console.log(`未找到文件夹 ${secondLevelName}，尝试创建`)

      // 根据系统文件夹类型确定要创建的文件夹类型
      let folderType: 'playlist' | 'booklist'
      if (firstLevelName === '歌单') {
        folderType = 'playlist'
      } else if (firstLevelName === '书单' || firstLevelName === '文档') {
        folderType = 'booklist'
      } else {
        console.error(`不支持在 ${firstLevelName} 文件夹下创建子文件夹`)
        return null
      }

      // 创建文件夹
      const createResponse = await API.folder.createFolder({
        name: secondLevelName,
        parent_id: currentFolder.id,
        folder_type: folderType as any
      })

      if (!createResponse.data || (createResponse.data as any).code !== 200) {
        console.error('创建文件夹失败:', createResponse.data)
        return null
      }

      // 重新获取文件夹内容以获取新创建的文件夹ID
      const newFolderContentResponse = await API.folder.getFolderContent(currentFolder.id)

      if (!newFolderContentResponse.data || (newFolderContentResponse.data as any).code !== 200 || !(newFolderContentResponse.data as any).data) {
        console.error('获取新文件夹内容失败:', newFolderContentResponse.data)
        return null
      }

      const newFolderContent = (newFolderContentResponse.data as any).data
      const newFolder = newFolderContent.folders.find((folder: any) => folder.folder_name === secondLevelName)

      if (newFolder) {
        console.log(`成功创建文件夹: ${secondLevelName}, ID: ${newFolder.id}`)
        toast.success(`文件夹 "${secondLevelName}" 创建成功`)
        return newFolder.id
      }

      console.error('创建文件夹后未能找到新文件夹')
      return null

    } catch (error) {
      console.error('获取文件夹ID失败:', error)
      return null
    }
  }
  
  /**
   * 创建下载会话
   */
  const createDownloadSession = async (
    selection: ItemsSelectionReq
  ): Promise<DownloadSessionResp | null> => {
    try {
      const response = await API.download.createDownloadSession(selection)
      console.log('📥 下载会话API响应:', response)
      
      if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
        const downloadData = (response.data as any).data
        console.log('📥 下载会话数据:', downloadData)
        
        // 为每个文件创建下载任务记录
        if (downloadData.files_d && Array.isArray(downloadData.files_d)) {
          downloadData.files_d.forEach((fileItem: any) => {
            const newTask: ExtendedTransferItem = {
              log_id: fileItem.log_id,
              item_name: fileItem.file_name,
              item_size: fileItem.size,
              transfer_type: TransferTypeEnum.DOWNLOAD,
              create_time: new Date().toISOString(),
              progress: 0,
              file_path: fileItem.relative_path,
              speed: '0 KB/s' // 初始化速度
            }
            
            processingTasks.value.unshift(newTask)
          })
          
          // 更新统计
          downloadCount.value += downloadData.files_d.length
          
          toast.success(`创建了 ${downloadData.files_d.length} 个下载任务`)
          return downloadData
        } else {
          console.warn('下载会话响应中没有files_d字段或不是数组:', downloadData)
          toast.error('下载会话响应格式错误')
          return null
        }
      } else {
        console.warn('下载会话API响应格式错误:', response.data)
        toast.error((response.data as any)?.message || '创建下载会话失败')
        return null
      }
    } catch (error) {
      console.error('创建下载会话失败:', error)
      toast.error('创建下载会话失败')
      return null
    }
  }
  
  /**
   * 下载单个文件
   */
  const downloadFile = async (
    downloadItem: DownloadFileItem,
    savePath?: string
  ): Promise<boolean> => {
    try {
      const controller = new AbortController()
      
      // 记录下载状态和开始时间
      const startTime = Date.now()
      activeDownloads.value.set(downloadItem.log_id, {
        progress: 0,
        controller,
        startTime
      })
      
      const response = await fetch(downloadItem.download_url, {
        signal: controller.signal
      })
      
      if (!response.ok) {
        throw new Error(`下载失败: ${response.statusText}`)
      }
      
      const reader = response.body?.getReader()
      const contentLength = parseInt(response.headers.get('Content-Length') || '0')
      
      let receivedLength = 0
      const chunks: Uint8Array[] = []
      
      while (reader) {
        const { done, value } = await reader.read()
        
        if (done) break
        
        chunks.push(value)
        receivedLength += value.length
        
        // 更新进度和速度
        const progress = contentLength > 0 ? (receivedLength / contentLength) * 100 : 0
        const downloadState = activeDownloads.value.get(downloadItem.log_id)
        if (downloadState) {
          downloadState.progress = progress
          
          // 计算下载速度
          if (downloadState.startTime) {
            const elapsedTime = (Date.now() - downloadState.startTime) / 1000 // 秒
            const speed = elapsedTime > 0 ? receivedLength / elapsedTime : 0 // 字节/秒
            const speedText = formatTransferSpeed(speed)
            
            // 更新任务中的速度信息
            const taskIndex = processingTasks.value.findIndex(task => task.log_id === downloadItem.log_id)
            if (taskIndex > -1) {
              processingTasks.value[taskIndex].speed = speedText
            }
          }
        }
        
        // 更新任务列表中的进度
        updateTaskProgress(downloadItem.log_id, progress)
        
        // 移除延迟，提升下载速度
      }
      
                   // 合并数据
      const blob = new Blob(chunks as BlobPart[])
      
      // 使用用户配置的下载目录
      const downloadDirectory = settingsStore.settings.downloadDirectory || 'C:\\Users\\Public\\Downloads'
      console.log(`📁 下载到目录: ${downloadDirectory}`)
      
      // 检查是否在Electron环境中
      if (window.electronAPI && window.electronAPI.saveFileToDirectory) {
        // Electron环境：保存到指定目录
        try {
          const arrayBuffer = await blob.arrayBuffer()
          const result = await window.electronAPI.saveFileToDirectory({
            fileName: downloadItem.file_name,
            arrayBuffer,
            downloadDirectory
          })
          
          if (result.success) {
            console.log(`✅ 文件已保存到: ${result.filePath}`)
            toast.success(`文件已下载到: ${downloadDirectory}`)
          } else {
            throw new Error(result.error || '保存文件失败')
          }
        } catch (electronError) {
          console.error('Electron下载失败，回退到浏览器下载:', electronError)
          // 回退到浏览器下载
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = downloadItem.file_name
          document.body.appendChild(a)
          a.click()
          document.body.removeChild(a)
          URL.revokeObjectURL(url)
          toast.success(`文件已下载到浏览器默认目录`)
        }
      } else {
        // 浏览器环境：使用默认下载
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = downloadItem.file_name
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
        toast.success(`文件已下载到浏览器默认目录`)
      }
      
      // 更新任务状态为成功
      await updateTransferStatus(downloadItem.log_id, {
        log_status: TransferStatusEnum.SUCCESS,
        transfer_duration_ms: Date.now()
      })
      
      // 清理下载状态
      activeDownloads.value.delete(downloadItem.log_id)
      
      // 延迟后自动移除已完成的下载任务
      setTimeout(() => {
        const taskIndex = processingTasks.value.findIndex(task => task.log_id === downloadItem.log_id)
        if (taskIndex > -1) {
          processingTasks.value.splice(taskIndex, 1)
          console.log(`🗑️ 已自动清除下载记录: ${downloadItem.file_name}`)
        }
      }, 3000) // 3秒后清除
      
      toast.success(`"${downloadItem.file_name}" 下载完成`)
      return true
    } catch (error) {
      console.error('下载文件失败:', error)
      
      // 更新任务状态为失败
      await updateTransferStatus(downloadItem.log_id, {
        log_status: TransferStatusEnum.FAILED,
        error_message: error instanceof Error ? error.message : '下载失败'
      })
      
      // 清理下载状态
      activeDownloads.value.delete(downloadItem.log_id)
      
      // 延迟后自动移除失败的下载任务
      setTimeout(() => {
        const taskIndex = processingTasks.value.findIndex(task => task.log_id === downloadItem.log_id)
        if (taskIndex > -1) {
          processingTasks.value.splice(taskIndex, 1)
          console.log(`🗑️ 已自动清除失败的下载记录: ${downloadItem.file_name}`)
        }
      }, 5000) // 失败的任务5秒后清除，让用户有时间看到错误
      
      toast.error(`"${downloadItem.file_name}" 下载失败`)
      return false
    }
  }
  
  /**
   * 取消上传任务
   */
  const cancelUpload = async (logId: number): Promise<boolean> => {
    try {
      await API.transfer.cancelUpload(logId)
      
      // 清理本地状态
      activeUploads.value.delete(logId)
      
      // 更新任务状态
      updateTransferTaskStatus(logId, TransferStatusEnum.CANCELED)
      
      toast.success('上传任务已取消')
      return true
    } catch (error) {
      console.error('取消上传失败:', error)
      toast.error('取消上传失败')
      return false
    }
  }
  
  /**
   * 取消下载任务
   */
  const cancelDownload = async (logId: number): Promise<boolean> => {
    const downloadState = activeDownloads.value.get(logId)
    if (downloadState?.controller) {
      downloadState.controller.abort()
      activeDownloads.value.delete(logId)

      // 通知后端更新状态
      try {
        await updateTransferStatus(logId, {
          log_status: TransferStatusEnum.CANCELED,
          error_message: '用户取消下载'
        })
      } catch (error) {
        console.error('通知后端下载取消失败:', error)
      }

      // 更新本地任务状态
      updateTransferTaskStatus(logId, TransferStatusEnum.CANCELED)

      toast.success('下载任务已取消')
      return true
    }

    return false
  }
  
  /**
   * 更新传输状态
   */
  const updateTransferStatus = async (
    logId: number,
    statusUpdate: TransferStatusUpdateReq
  ): Promise<boolean> => {
    try {
      await API.transfer.updateStatus(logId, statusUpdate)
      return true
    } catch (error) {
      console.error('更新传输状态失败:', error)
      return false
    }
  }
  
  /**
   * 删除传输记录
   */
  const deleteTransferRecord = async (
    logId: number,
    deleteFile = false
  ): Promise<boolean> => {
    try {
      await API.transfer.deleteRecord(logId, deleteFile)
      
      // 从本地列表中移除
      const processingIndex = processingTasks.value.findIndex(task => task.log_id === logId)
      if (processingIndex > -1) {
        processingTasks.value.splice(processingIndex, 1)
        processingPagination.value.total--
      }
      
      const completedIndex = completedTasks.value.findIndex(task => task.log_id === logId)
      if (completedIndex > -1) {
        completedTasks.value.splice(completedIndex, 1)
        completedPagination.value.total--
      }
      
      toast.success('传输记录已删除')
      return true
    } catch (error) {
      console.error('删除传输记录失败:', error)
      toast.error('删除传输记录失败')
      return false
    }
  }
  
  /**
   * 清空已完成的传输记录
   */
  const clearCompletedTasks = async (deleteFile = false, category?: string): Promise<boolean> => {
    try {
      await API.transfer.clearCompleted(deleteFile)
      
      let tasksToRemove = completedTasks.value
      
      // 如果指定了分类，只清空该分类的任务
      if (category) {
        tasksToRemove = completedTasks.value.filter(task => {
          const taskType = task.transfer_type?.toLowerCase()
          return taskType === category
        })
        
        // 从已完成任务中移除指定分类的任务
        completedTasks.value = completedTasks.value.filter(task => {
          const taskType = task.transfer_type?.toLowerCase()
          return taskType !== category
        })
      } else {
        // 清空所有已完成任务
        completedTasks.value = []
      }
      
      const count = tasksToRemove.length
      completedPagination.value.total = Math.max(0, completedPagination.value.total - count)
      
      const categoryText = category ? (category === 'download' ? '下载' : '上传') : '已完成'
      // 不显示toast，让调用者处理
      return true
    } catch (error) {
      console.error('清空已完成记录失败:', error)
      return false
    }
  }
  
  /**
   * 更新任务进度
   */
  const updateTaskProgress = (logId: number, progress: number): void => {
    let task = processingTasks.value.find(t => t.log_id === logId);
    
    if (!task) {
      task = completedTasks.value.find(t => t.log_id === logId);
    }

    if (task) {
      task.progress = progress
      console.log(`📊 更新任务进度: ${task.item_name} -> ${progress}%`)

      // 如果进度达到100%，任务即将完成
      if (progress === 100) {
        console.log(`✅ 任务即将完成: ${task.item_name}`)
      }
    } else {
      console.warn(`⚠️ 未找到任务: logId=${logId}`)
    }
  }
  
  /**
   * 更新传输任务状态（WebSocket回调）
   */
  const updateTransferTaskStatus = (
    logId: number,
    status: TransferStatusEnum,
    errorMessage?: string
  ): void => {
    const musicStore = useMusicStore() // 获取MusicStore实例
    const docsStore = useDocsStore() // 获取DocsStore实例
    const taskIndex = processingTasks.value.findIndex(task => task.log_id === logId)
    
    if (taskIndex > -1) {
      const task = processingTasks.value[taskIndex]
      
      if (status === TransferStatusEnum.SUCCESS || 
          status === TransferStatusEnum.FAILED || 
          status === TransferStatusEnum.CANCELED) {
        
        // 1. 同步地将任务从"进行中"移动到"已完成"
        processingTasks.value.splice(taskIndex, 1)
        processingPagination.value.total--
        
        const completedTask: ExtendedTransferItem = {
          ...task,
          transfer_status: status,
          progress: status === TransferStatusEnum.SUCCESS ? 100 : task.progress || 0,
          error_message: errorMessage
        }
        completedTasks.value.unshift(completedTask)
        completedPagination.value.total++
        
        // 2. 清理活动的传输状态
        const wasActiveUpload = activeUploads.value.has(logId)
        const wasActiveDownload = activeDownloads.value.has(logId)
        activeUploads.value.delete(logId)
        activeDownloads.value.delete(logId)
        
        console.log(`🧹 任务状态更新清理: logId=${logId}, 上传任务=${wasActiveUpload}, 下载任务=${wasActiveDownload}, 剩余活跃任务=${totalActiveTasks.value}`)

        // 3. 如果是成功的上传任务，根据文件类型刷新对应的页面
        if (status === TransferStatusEnum.SUCCESS && task.transfer_type === TransferTypeEnum.UPLOAD) {
          console.log(`🔍 检查上传文件: ${task.item_name}, 类型: ${task.transfer_type}, 状态: ${status}`);
          
          // 检查是否为音乐文件
          const isMusicFile = task.item_name?.endsWith('.mp3') || 
                              task.item_name?.endsWith('.flac') || 
                              task.item_name?.endsWith('.wav') ||
                              task.item_name?.endsWith('.m4a') ||
                              task.item_name?.endsWith('.aac');
          
          // 检查是否为文档文件
          const isDocumentFile = task.item_name?.endsWith('.pdf') ||
                                  task.item_name?.endsWith('.doc') ||
                                  task.item_name?.endsWith('.docx') ||
                                  task.item_name?.endsWith('.txt') ||
                                  task.item_name?.endsWith('.md') ||
                                  task.item_name?.endsWith('.xls') ||
                                  task.item_name?.endsWith('.xlsx') ||
                                  task.item_name?.endsWith('.ppt') ||
                                  task.item_name?.endsWith('.pptx');
          
          console.log(`🎵 是否为音乐文件: ${isMusicFile}`);
          console.log(`📄 是否为文档文件: ${isDocumentFile}`);
          
          if (isMusicFile) {
            console.log('🎵 音乐文件上传成功，正在安排刷新音乐库...');
            // 使用防抖机制，避免频繁刷新
            if (musicRefreshTimer) {
              clearTimeout(musicRefreshTimer)
            }
            musicRefreshTimer = setTimeout(() => {
              console.log('🎵 执行音乐库刷新');
              musicStore.loadPlaylistsFromDrive();
              musicRefreshTimer = null
            }, 500) // 500ms防抖延迟
          }
          
          if (isDocumentFile) {
            console.log('📄 文档文件上传成功，正在安排刷新文档库...');
            // 使用防抖机制，避免频繁刷新
            if (docsRefreshTimer) {
              clearTimeout(docsRefreshTimer)
            }
            docsRefreshTimer = setTimeout(() => {
              console.log('📄 执行文档库刷新');
              docsStore.loadCategoriesFromDrive();
              docsRefreshTimer = null
            }, 500) // 500ms防抖延迟
          }
        }
      } else {
        // 仅更新进行中任务的状态
        task.transfer_status = status
        if (errorMessage) {
          task.error_message = errorMessage
        }
      }
    }
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
   * 格式化传输速度
   */
  const formatTransferSpeed = (bytesPerSecond: number): string => {
    return formatFileSize(bytesPerSecond) + '/s'
  }
  
  /**
   * 重置状态
   */
  const reset = (): void => {
    uploadCount.value = 0
    downloadCount.value = 0
    processingTasks.value = []
    completedTasks.value = []
    
    // 清理分页信息
    processingPagination.value = {
      total: 0,
      current_page: 1,
      page_size: 20,
      hasMore: true
    }
    
    completedPagination.value = {
      total: 0,
      current_page: 1,
      page_size: 20,
      hasMore: true
    }
    
    // 取消所有活动任务
    activeUploads.value.clear()
    activeDownloads.value.forEach(download => {
      download.controller?.abort()
    })
    activeDownloads.value.clear()
  }
  
  // 初始化
  setupWebSocketListeners()
  
  return {
    // === 统计状态 ===
    uploadCount,
    downloadCount,
    isLoadingSummary,
    
    // === 任务列表 ===
    processingTasks,
    completedTasks,
    isLoadingTasks,
    processingPagination,
    completedPagination,
    
    // === 活动任务 ===
    activeUploads,
    activeDownloads,
    
    // === 计算属性 ===
    totalProcessingTasks,
    totalCompletedTasks,
    totalActiveTasks,
    processingUploadTasks,
    processingDownloadTasks,
    hasTasks,
    
    // === 数据加载方法 ===
    loadTransferSummary,
    loadTransferHistory,
    loadMoreTasks,
    
    // === 传输操作方法 ===
    uploadFile,
    uploadFiles,
    createDownloadSession,
    downloadFile,
    cancelUpload,
    cancelDownload,
    
    // === 任务管理方法 ===
    updateTransferStatus,
    deleteTransferRecord,
    clearCompletedTasks,
    updateTaskProgress,
    updateTransferTaskStatus,
    
    // === 工具方法 ===
    formatFileSize,
    formatTransferSpeed,
    reset,
    
    // === 获取统计信息 ===
    getTransferStats: () => {
      return {
        totalProcessing: totalProcessingTasks.value,
        totalCompleted: totalCompletedTasks.value,
        totalActive: totalActiveTasks.value,
        hasAnyTasks: hasTasks.value
      }
    },
    
    // === 内部清理方法 ===
    clearRefreshTimers: () => {
      if (musicRefreshTimer) {
        clearTimeout(musicRefreshTimer)
        musicRefreshTimer = null
      }
      if (docsRefreshTimer) {
        clearTimeout(docsRefreshTimer)
        docsRefreshTimer = null
      }
    },
    
    // === 调试方法 ===
    debugClearAllActiveTasks: () => {
      console.log(`🚨 手动清理所有活跃任务: 上传=${activeUploads.value.size}, 下载=${activeDownloads.value.size}`)
      activeUploads.value.clear()
      activeDownloads.value.clear()
      console.log(`✅ 清理完成, 剩余活跃任务=${totalActiveTasks.value}`)
    }
  }
}) 