import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { API } from '@/utils/api'
import { onFileStatusUpdated, onTransferLogUpdated } from '@/utils/websocket'
import { useToast } from 'vue-toastification'
import {
  FolderTypeEnum,
  FileTypeEnum,
  FileStatusEnum
} from '@/types/api'
import type {
  FolderContentResp,
  FolderInfo,
  FileInfo,
  BreadcrumbItem,
  FolderCreateReq,
  ItemsRenameReq,
  ItemsOperationReq,
  ItemsDeleteReq,
  SetOrderReq,
  ItemDetailResp,
  FolderHierarchyResp,
  RecycleBinContentResp,
  RecycleBinReq
} from '@/types/api'

export const useDriveStore = defineStore('drive', () => {
  const toast = useToast()
  
  // 响应式状态
  const currentFolderId = ref<number>(0) // 0表示根目录
  const breadcrumb = ref<BreadcrumbItem[]>([])
  const folders = ref<FolderInfo[]>([])
  const files = ref<FileInfo[]>([])
  const isLoading = ref(false)
  const searchQuery = ref('')
  const sortBy = ref<string>('sorted_order')
  const sortOrder = ref<'ASC' | 'DESC'>('DESC')
  
  // 回收站状态
  const isInRecycleBin = ref(false)
  const recycleBinFolders = ref<any[]>([])
  const recycleBinFiles = ref<any[]>([])
  
  // 文件夹层级缓存（用于移动对话框）
  const folderHierarchy = ref<FolderHierarchyResp[]>([])
  
  // 计算属性
  const currentPath = computed(() => {
    if (isInRecycleBin.value) {
      return '/回收站'
    }
    if (breadcrumb.value.length === 0) {
      return '/云盘'
    }
    return '/云盘/' + breadcrumb.value.map(item => item.name).join('/')
  })
  
  const totalItems = computed(() => folders.value.length + files.value.length)
  const hasItems = computed(() => totalItems.value > 0)
  
  const systemFolders = computed(() => 
    folders.value.filter(folder => folder.folder_type === FolderTypeEnum.SYSTEM)
  )
  
  const customFolders = computed(() =>
    folders.value.filter(folder => folder.folder_type !== FolderTypeEnum.SYSTEM)
  )

  const recycleBinItems = computed(() => {
    // 将回收站文件夹转换为 DriveItem 格式
    const folderItems = recycleBinFolders.value.map(folder => ({
      ...folder,
      id: folder.original_id,
      name: folder.original_name,
      type: 'folder' as const,
      size: 0,
      itemCount: folder.sub_count || 0,
      createdAt: new Date(folder.delete_time),
      modifiedAt: new Date(folder.delete_time),
      deletedAt: new Date(folder.delete_time),
      expireAt: new Date(folder.expire_time)
    }))

    // 将回收站文件转换为 DriveItem 格式
    const fileItems = recycleBinFiles.value.map(file => ({
      ...file,
      id: file.original_id,
      name: file.original_name,
      type: file.original_type === FileTypeEnum.MUSIC ? 'audio' : 'document' as const,
      size: file.file_size || 0,
      createdAt: new Date(file.delete_time),
      modifiedAt: new Date(file.delete_time),
      deletedAt: new Date(file.delete_time),
      expireAt: new Date(file.expire_time)
    }))

    return [...folderItems, ...fileItems]
  })

  const driveItems = computed(() => {
    // 将 FolderInfo 转换为 DriveItem 格式
    const folderItems = folders.value.map(folder => {
      const createdAt = new Date(folder.create_time)
      const modifiedAt = new Date(folder.update_time)

      return {
        ...folder,
        name: folder.folder_name,
        type: 'folder' as const,
        size: 0,
        itemCount: folder.sub_count || 0,
        createdAt: createdAt,
        modifiedAt: modifiedAt,
        // 系统级文件夹（根目录下的文件夹）不能被重命名、移动或删除
        isLocked: folder.folder_type === FolderTypeEnum.SYSTEM,
        // 添加path属性，用于移动操作
        path: `${currentPath.value}/${folder.folder_name}`.replace(/\/+/g, '/').replace(/^\//, '') || '/'
      }
    })

    // 将 FileInfo 转换为 DriveItem 格式
    const fileItems = files.value.map(file => {
      const createdAt = new Date(file.create_time)
      const modifiedAt = new Date(file.update_time)

      return {
        ...file,
        name: file.file_name,
        type: file.file_type === FileTypeEnum.MUSIC ? 'audio' : 'document' as const,
        size: file.file_size || 0,
        createdAt: createdAt,
        modifiedAt: modifiedAt,
        // 文件通常不会被锁定
        isLocked: false,
        // 添加path属性，用于移动操作
        path: `${currentPath.value}/${file.file_name}`.replace(/\/+/g, '/').replace(/^\//, '') || '/'
      }
    })

    return [...folderItems, ...fileItems]
  })
  
  /**
   * 设置WebSocket事件监听
   */
  const setupWebSocketListeners = () => {
    // 监听文件状态更新
    onFileStatusUpdated((payload) => {
      const fileIndex = files.value.findIndex(file => file.id === payload.file_id)
      if (fileIndex > -1 && payload.file_data) {
        files.value[fileIndex] = payload.file_data
        
        if (payload.file_status === FileStatusEnum.AVAILABLE) {
          toast.success(`文件 "${payload.file_data.file_name}" 处理完成`)
        } else if (payload.file_status === FileStatusEnum.FAILED) {
          toast.error(`文件 "${payload.file_data.file_name}" 处理失败`)
        }
      }
    })
    
    // 监听传输日志更新
    onTransferLogUpdated((payload) => {
      // 可以在这里更新相关的UI状态
      console.log('传输日志更新:', payload)
    })
  }
  
  /**
   * 加载文件夹内容
   */
  const loadFolderContent = async (
    folderId: number = 0,
    params?: {
      sort_by?: string
      sort_order?: string
      content?: string
    }
  ): Promise<void> => {
    try {
      isLoading.value = true
      currentFolderId.value = folderId
      
      const requestParams = {
        sort_by: params?.sort_by || sortBy.value,
        sort_order: params?.sort_order || sortOrder.value,
        content: params?.content || searchQuery.value || undefined
      }
      
      const response = await API.folder.getFolderContent(folderId, requestParams)

      if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
        const folderData = (response.data as any).data
        breadcrumb.value = folderData.breadcrumb || []
        folders.value = folderData.folders || []
        files.value = folderData.files || []
      }
    } catch (error) {
      console.error('加载文件夹内容失败:', error)
      toast.error('加载文件夹内容失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 搜索文件和文件夹
   */
  const search = async (query: string): Promise<void> => {
    searchQuery.value = query
    await loadFolderContent(currentFolderId.value, {
      content: query,
      sort_by: sortBy.value,
      sort_order: sortOrder.value
    })
  }
  
  /**
   * 设置排序方式
   */
  const setSorting = async (newSortBy: string, newSortOrder: 'ASC' | 'DESC'): Promise<void> => {
    sortBy.value = newSortBy
    sortOrder.value = newSortOrder
    await loadFolderContent(currentFolderId.value, {
      sort_by: newSortBy,
      sort_order: newSortOrder,
      content: searchQuery.value || undefined
    })
  }
  
  /**
   * 创建文件夹
   */
  const createFolder = async (data: FolderCreateReq): Promise<boolean> => {
    try {
      isLoading.value = true
      
      // 验证创建规则 - 不允许在根目录下直接创建文件夹
      if (data.parent_id === 0) {
        toast.error('根目录下不允许创建文件夹')
        return false
      }
      
      // 检查父文件夹类型，确保遵循二级目录结构
      console.log('创建文件夹请求:', data)
      console.log('当前文件夹列表:', folders.value)
      console.log('文件夹层级:', folderHierarchy.value)

      // 首先尝试从当前文件夹列表中查找
      let parentFolder = folders.value.find(f => f.id === data.parent_id)
      console.log('从当前文件夹列表找到的父文件夹:', parentFolder)

      // 如果在当前文件夹列表中找不到，从文件夹层级中查找
      if (!parentFolder) {
        parentFolder = folderHierarchy.value.find(f => f.id === data.parent_id)
        console.log('从文件夹层级找到的父文件夹:', parentFolder)
      }

      if (parentFolder && parentFolder.folder_type === FolderTypeEnum.SYSTEM) {
        // 在系统文件夹下创建的只能是playlist或booklist
        if (parentFolder.folder_name === '歌单' && data.folder_type !== FolderTypeEnum.PLAYLIST) {
          toast.error('歌单文件夹下只能创建播放列表')
          return false
        }
        if ((parentFolder.folder_name === '文档' || parentFolder.folder_name === '书单') && data.folder_type !== FolderTypeEnum.BOOKLIST) {
          toast.error('文档文件夹下只能创建笔记本')
          return false
        }
      }
      
      await API.folder.createFolder(data)
      toast.success('文件夹创建成功')
      
      // 刷新当前目录
      await loadFolderContent(currentFolderId.value)
      return true
    } catch (error) {
      console.error('创建文件夹失败:', error)
      toast.error('创建文件夹失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 重命名项目
   */
  const renameItem = async (data: ItemsRenameReq): Promise<boolean> => {
    try {
      isLoading.value = true
      await API.item.rename(data)
      toast.success('重命名成功')
      
      // 更新本地状态
      if (data.file_id) {
        const fileIndex = files.value.findIndex(f => f.id === data.file_id)
        if (fileIndex > -1) {
          files.value[fileIndex].file_name = data.new_name
        }
      } else if (data.folder_id) {
        const folderIndex = folders.value.findIndex(f => f.id === data.folder_id)
        if (folderIndex > -1) {
          folders.value[folderIndex].folder_name = data.new_name
        }
      }
      
      return true
    } catch (error) {
      console.error('重命名失败:', error)
      toast.error('重命名失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 移动项目
   */
  const moveItems = async (data: ItemsOperationReq): Promise<boolean> => {
    try {
      isLoading.value = true

      console.log('moveItems - folderHierarchy.value:', folderHierarchy.value)
      console.log('moveItems - data:', data)

      // 验证移动规则
      // 处理folderHierarchy可能是API响应对象的情况
      let hierarchy: any[] = []
      if (Array.isArray(folderHierarchy.value)) {
        hierarchy = folderHierarchy.value
      } else if (folderHierarchy.value && (folderHierarchy.value as any).data) {
        hierarchy = Array.isArray((folderHierarchy.value as any).data) ? (folderHierarchy.value as any).data : []
      }

      console.log('moveItems - extracted hierarchy:', hierarchy)

      const targetFolder = hierarchy.find(f => f.id === data.target_folder_id)
      if (targetFolder) {
        // 检查移动规则：文件夹只能移动到根目录四大类，文件可以移动到二级目录
        if (data.folder_ids.length > 0 && targetFolder.parent_id !== 0) {
          toast.error('文件夹只能移动到根目录的四大类文件夹中')
          return false
        }
      }
      
      await API.item.move(data)
      toast.success('移动成功')
      
      // 刷新当前目录
      await loadFolderContent(currentFolderId.value)
      return true
    } catch (error) {
      console.error('移动失败:', error)
      toast.error('移动失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 复制项目
   */
  const copyItems = async (data: ItemsOperationReq): Promise<boolean> => {
    try {
      isLoading.value = true
      await API.item.copy(data)
      toast.success('复制成功')
      
      // 刷新当前目录
      await loadFolderContent(currentFolderId.value)
      return true
    } catch (error) {
      console.error('复制失败:', error)
      toast.error('复制失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 删除项目（移入回收站）
   */
  const deleteItems = async (data: ItemsDeleteReq): Promise<boolean> => {
    try {
      isLoading.value = true
      await API.item.delete(data)
      
      const totalItems = data.file_ids.length + data.folder_ids.length
      toast.success(`已将 ${totalItems} 个项目移入回收站`)
      
      // 从本地状态中移除
      if (data.file_ids.length > 0) {
        files.value = files.value.filter(file => !data.file_ids.includes(file.id))
      }
      if (data.folder_ids.length > 0) {
        folders.value = folders.value.filter(folder => !data.folder_ids.includes(folder.id))
      }
      
      return true
    } catch (error) {
      console.error('删除失败:', error)
      toast.error('删除失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 设置项目排序
   */
  const setItemOrder = async (
    itemId: number,
    itemType: 'file' | 'folder',
    data: SetOrderReq
  ): Promise<boolean> => {
    try {
      await API.item.setOrder(itemId, itemType, data)
      
      // 刷新当前目录以更新排序
      await loadFolderContent(currentFolderId.value)
      return true
    } catch (error) {
      console.error('设置排序失败:', error)
      toast.error('设置排序失败')
      return false
    }
  }
  
  /**
   * 获取项目详情
   */
  const getItemDetail = async (
    itemId: number,
    itemType: 'file' | 'folder'
  ): Promise<ItemDetailResp | null> => {
    try {
      const response = await API.item.getDetail(itemId, itemType)

      if (response.data && response.data.code === 200 && response.data.data) {
        return response.data.data
      } else {
        console.warn('获取项目详情API响应格式错误:', response.data)
        toast.error(response.data?.message || '获取项目详情失败')
        return null
      }
    } catch (error) {
      console.error('获取项目详情失败:', error)
      toast.error('获取项目详情失败')
      return null
    }
  }
  
  /**
   * 加载文件夹层级（用于移动对话框）
   */
  const loadFolderHierarchy = async (): Promise<void> => {
    try {
      const response = await API.folder.getFolderHierarchy()
      console.log('loadFolderHierarchy response:', response)

      if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
        const hierarchyData = (response.data as any).data
        folderHierarchy.value = Array.isArray(hierarchyData) ? hierarchyData : []
        console.log('folderHierarchy updated:', folderHierarchy.value)
      } else {
        folderHierarchy.value = []
      }
    } catch (error) {
      console.error('加载文件夹层级失败:', error)
      folderHierarchy.value = []
    }
  }
  
  /**
   * 进入回收站
   */
  const enterRecycleBin = async (): Promise<void> => {
    try {
      isLoading.value = true
      isInRecycleBin.value = true
      
      const response = await API.recycleBin.getContent(searchQuery.value || undefined)
      if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
        const recycleBinData = (response.data as any).data
        recycleBinFolders.value = recycleBinData.folders || []
        recycleBinFiles.value = recycleBinData.files || []
      }
    } catch (error) {
      console.error('加载回收站失败:', error)
      toast.error('加载回收站失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 退出回收站
   */
  const exitRecycleBin = async (): Promise<void> => {
    isInRecycleBin.value = false
    await loadFolderContent(0) // 回到根目录
  }
  
  /**
   * 还原回收站项目
   */
  const restoreRecycleBinItems = async (data: RecycleBinReq): Promise<boolean> => {
    try {
      isLoading.value = true
      await API.recycleBin.restore(data)
      
      const totalItems = data.file_ids.length + data.folder_ids.length
      toast.success(`已成功还原 ${totalItems} 个项目`)
      
      // 刷新回收站
      await enterRecycleBin()
      return true
    } catch (error) {
      console.error('还原失败:', error)
      toast.error('还原失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 彻底删除回收站项目
   */
  const deleteRecycleBinItems = async (data: RecycleBinReq): Promise<boolean> => {
    try {
      isLoading.value = true
      await API.recycleBin.deleteItems(data)
      
      const totalItems = data.file_ids.length + data.folder_ids.length
      toast.success(`已彻底删除 ${totalItems} 个项目`)
      
      // 刷新回收站
      await enterRecycleBin()
      return true
    } catch (error) {
      console.error('彻底删除失败:', error)
      toast.error('彻底删除失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 清空回收站
   */
  const emptyRecycleBin = async (): Promise<boolean> => {
    try {
      isLoading.value = true
      await API.recycleBin.empty()
      toast.success('回收站已清空')
      
      // 清空本地状态
      recycleBinFolders.value = []
      recycleBinFiles.value = []
      return true
    } catch (error) {
      console.error('清空回收站失败:', error)
      toast.error('清空回收站失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 导航到指定文件夹
   */
  const navigateToFolder = async (folderId: number): Promise<void> => {
    if (isInRecycleBin.value) {
      await exitRecycleBin()
    }
    await loadFolderContent(folderId)
  }
  
  /**
   * 导航到根目录
   */
  const navigateToRoot = async (): Promise<void> => {
    if (isInRecycleBin.value) {
      await exitRecycleBin()
    }
    await loadFolderContent(0)
  }
  
  /**
   * 刷新当前目录
   */
  const refresh = async (): Promise<void> => {
    if (isInRecycleBin.value) {
      await enterRecycleBin()
    } else {
      await loadFolderContent(currentFolderId.value)
    }
  }
  
  /**
   * 重置状态
   */
  const reset = (): void => {
    currentFolderId.value = 0
    breadcrumb.value = []
    folders.value = []
    files.value = []
    isLoading.value = false
    searchQuery.value = ''
    isInRecycleBin.value = false
    recycleBinFolders.value = []
    recycleBinFiles.value = []
    folderHierarchy.value = []
  }
  
  /**
   * 设置当前路径（用于导航）
   */
  const setCurrentPath = async (path: string): Promise<void> => {
    if (path === '/云盘' || path === '/') {
      await navigateToRoot()
    } else if (path.startsWith('/云盘/')) {
      // 解析路径并导航到对应文件夹
      const pathParts = path.replace('/云盘/', '').split('/')
      // 这里可以根据需要实现更复杂的路径解析逻辑
      // 目前简单地导航到根目录
      await navigateToRoot()
    }
  }
  
  /**
   * 打开回收站
   */
  const openRecycleBin = async (): Promise<void> => {
    await enterRecycleBin()
  }
  
  /**
   * 设置搜索查询
   */
  const setSearchQuery = (query: string): void => {
    searchQuery.value = query
  }
  
  // 初始化WebSocket监听
  setupWebSocketListeners()
  
  return {
    // 状态
    currentFolderId,
    breadcrumb,
    folders,
    files,
    isLoading,
    searchQuery,
    sortBy,
    sortOrder,
    isInRecycleBin,
    recycleBinFolders,
    recycleBinFiles,
    folderHierarchy,
    
    // 计算属性
    currentPath,
    totalItems,
    hasItems,
    systemFolders,
    customFolders,
    recycleBinItems,
    driveItems,
    
    // 方法
    loadFolderContent,
    search,
    setSorting,
    createFolder,
    renameItem,
    moveItems,
    copyItems,
    deleteItems,
    setItemOrder,
    getItemDetail,
    loadFolderHierarchy,
    enterRecycleBin,
    exitRecycleBin,
    restoreRecycleBinItems,
    deleteRecycleBinItems,
    emptyRecycleBin,
    navigateToFolder,
    navigateToRoot,
    refresh,
    reset,
    setCurrentPath,
    openRecycleBin,
    setSearchQuery
  }
}) 