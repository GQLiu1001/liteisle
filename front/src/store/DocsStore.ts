import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { API } from '@/utils/api'
import { onFileStatusUpdated } from '@/utils/websocket'
import { useToast } from 'vue-toastification'
import { FileStatusEnum } from '@/types/api'
import type {
  DocumentViewResp,
  FolderInfo,
  FileInfo,
  MarkdownContentResp,
  MarkdownUpdateReq,
  MarkdownCreateReq,
  TranslateReq,
  TranslateResp
} from '@/types/api'

export const useDocsStore = defineStore('docs', () => {
  const toast = useToast()
  
  // === 文档数据状态 ===
  const booklists = ref<FolderInfo[]>([])     // 笔记本列表（文件夹）
  const allDocuments = ref<FileInfo[]>([])    // 所有文档文件
  const isLoading = ref(false)
  const searchQuery = ref('')
  const lastUpdated = ref<Date | null>(null)
  
  // === 当前选择状态 ===
  const currentBooklist = ref<FolderInfo | null>(null)  // 当前选择的笔记本
  const selectedDocument = ref<FileInfo | null>(null)   // 当前选择的文档
  
  // === Markdown编辑状态 ===
  const currentMarkdownContent = ref('')
  const currentMarkdownVersion = ref(0)
  const isMarkdownMode = ref(false)
  const hasUnsavedChanges = ref(false)
  const isSaving = ref(false)
  
  // === 翻译功能状态 ===
  const isTranslating = ref(false)
  const lastTranslation = ref<TranslateResp | null>(null)
  
  // === 计算属性 ===
  const sortedBooklists = computed(() => {
    return [...booklists.value].sort((a, b) => b.sorted_order - a.sorted_order) // 按sorted_order降序排列
  })
  
  const currentBooklistDocuments = computed(() => {
    if (!currentBooklist.value) return []
    return allDocuments.value.filter(file =>
      file.folder_id === currentBooklist.value!.id
      // 移除file_status过滤，因为API返回的数据可能不包含此字段
      // && file.file_status === FileStatusEnum.AVAILABLE
    ).sort((a, b) => b.sorted_order - a.sorted_order) // 按sorted_order降序排列
  })
  
  const filteredDocuments = computed(() => {
    const documents = currentBooklistDocuments.value
    if (!searchQuery.value) return documents
    
    const query = searchQuery.value.toLowerCase()
    return documents.filter(doc => 
      doc.file_name.toLowerCase().includes(query)
    )
  })
  
  const canSaveMarkdown = computed(() => {
    return isMarkdownMode.value && hasUnsavedChanges.value && !isSaving.value
  })
  
  const isMarkdownDocument = computed(() => {
    return selectedDocument.value?.file_name.toLowerCase().endsWith('.md') || false
  })

  const currentCategoryData = computed(() => {
    return currentBooklist.value
  })
  
  /**
   * 设置WebSocket监听
   */
  const setupWebSocketListeners = () => {
    onFileStatusUpdated((payload) => {
      // 更新对应文档文件的状态
      const fileIndex = allDocuments.value.findIndex(file => file.id === payload.file_id)
      if (fileIndex > -1 && payload.file_data) {
        allDocuments.value[fileIndex] = payload.file_data
        
        if (payload.file_status === FileStatusEnum.AVAILABLE) {
          toast.success(`文档 "${payload.file_data.file_name}" 处理完成`)
        } else if (payload.file_status === FileStatusEnum.FAILED) {
          toast.error(`文档 "${payload.file_data.file_name}" 处理失败`)
        }
      }
    })
  }
  
  /**
   * 加载文档页面数据
   */
  const loadDocumentsData = async (content?: string): Promise<void> => {
    try {
      isLoading.value = true
      const response = await API.document.getDocumentView(content)
      
      if (response.data) {
        // 检查数据结构，适配不同的响应格式
        const actualData = (response.data as any).data || response.data
        console.log('Documents API response:', response)
        console.log('实际数据结构:', actualData)
        console.log('API返回的booklists:', actualData.booklists)
        console.log('API返回的files:', actualData.files)
        
        booklists.value = actualData.booklists || []
        allDocuments.value = actualData.files || []
        lastUpdated.value = new Date()
      }
    } catch (error) {
      console.error('加载文档数据失败:', error)
      toast.error('加载文档数据失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 搜索文档
   */
  const searchDocuments = async (query: string): Promise<void> => {
    searchQuery.value = query
    await loadDocumentsData(query)
  }
  
  /**
   * 选择笔记本
   */
  const selectBooklist = (booklist: FolderInfo): void => {
    console.log('选择书单:', booklist)
    console.log('所有文档:', allDocuments.value)
    console.log('书单ID匹配的文档:', allDocuments.value.filter(file => file.folder_id === booklist.id))

    currentBooklist.value = booklist

    // 如果当前选择的文档不在新选择的笔记本中，清除选择
    if (selectedDocument.value) {
      const booklistDocuments = allDocuments.value.filter(file => file.folder_id === booklist.id)
      if (!booklistDocuments.find(doc => doc.id === selectedDocument.value!.id)) {
        closeDocument()
      }
    }
  }
  
  /**
   * 选择文档
   */
  const selectDocument = async (document: FileInfo): Promise<void> => {
    console.log('选择文档:', document)

    // 如果有未保存的更改，提示用户
    if (hasUnsavedChanges.value) {
      const confirmed = confirm('您有未保存的更改，是否要放弃更改？')
      if (!confirmed) {
        return
      }
    }

    selectedDocument.value = document
    console.log('已设置selectedDocument:', selectedDocument.value)

    if (isMarkdownDocument.value) {
      console.log('这是Markdown文档，加载内容')
      await loadMarkdownContent(document.id)
    } else {
      console.log('这不是Markdown文档，文件类型:', document.file_name)
      // 对于非Markdown文档，退出编辑模式
      exitMarkdownMode()
    }
  }
  
  /**
   * 关闭文档
   */
  const closeDocument = (): void => {
    if (hasUnsavedChanges.value) {
      const confirmed = confirm('您有未保存的更改，是否要放弃更改？')
      if (!confirmed) {
        return
      }
    }
    
    selectedDocument.value = null
    exitMarkdownMode()
  }
  
  /**
   * 获取文档预览/下载链接
   */
  const getDocumentViewUrl = async (fileId: number): Promise<string | null> => {
    try {
      const response = await API.document.getViewUrl(fileId)

      console.log('获取文档链接API响应:', response)

      if (response.data && response.data.code === 200 && response.data.data) {
        return response.data.data
      } else {
        console.warn('获取文档链接API响应格式错误:', response.data)
        toast.error(response.data?.message || '获取文档链接失败')
        return null
      }
    } catch (error) {
      console.error('获取文档链接失败:', error)
      toast.error('获取文档链接失败')
      return null
    }
  }
  
  /**
   * 加载Markdown文档内容
   */
  const loadMarkdownContent = async (fileId: number): Promise<void> => {
    try {
      isLoading.value = true
      const response = await API.document.getMarkdownContent(fileId)

      console.log('加载Markdown内容API响应:', response)

      if (response.data && response.data.code === 200 && response.data.data) {
        const markdownData = response.data.data
        currentMarkdownContent.value = markdownData.content
        currentMarkdownVersion.value = markdownData.version
        isMarkdownMode.value = true
        hasUnsavedChanges.value = false
      } else {
        console.warn('加载Markdown内容API响应格式错误:', response.data)
        toast.error(response.data?.message || '加载Markdown内容失败')
      }
    } catch (error) {
      console.error('加载Markdown内容失败:', error)
      toast.error('加载Markdown内容失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 更新Markdown文档内容
   */
  const updateMarkdownContent = (content: string): void => {
    if (content !== currentMarkdownContent.value) {
      currentMarkdownContent.value = content
      hasUnsavedChanges.value = true
    }
  }
  
  /**
   * 保存Markdown文档
   */
  const saveMarkdownDocument = async (): Promise<boolean> => {
    if (!selectedDocument.value || !canSaveMarkdown.value) {
      return false
    }
    
    try {
      isSaving.value = true
      
      const updateData: MarkdownUpdateReq = {
        content: currentMarkdownContent.value,
        version: currentMarkdownVersion.value
      }
      
      const response = await API.document.updateMarkdownContent(selectedDocument.value.id, updateData)

      console.log('保存Markdown文档API响应:', response)

      if (response.data && response.data.code === 200) {
        hasUnsavedChanges.value = false
        currentMarkdownVersion.value++ // 增加版本号
        toast.success('文档保存成功')
        return true
      } else {
        console.warn('保存Markdown文档API响应格式错误:', response.data)
        toast.error(response.data?.message || '保存文档失败')
        return false
      }
    } catch (error) {
      console.error('保存Markdown文档失败:', error)
      toast.error('保存文档失败')
      return false
    } finally {
      isSaving.value = false
    }
  }
  
  /**
   * 创建新的Markdown文档
   */
  const createMarkdownDocument = async (name: string, folderId: number): Promise<boolean> => {
    try {
      isLoading.value = true
      
      const createData: MarkdownCreateReq = {
        name: name.endsWith('.md') ? name : `${name}.md`,
        folder_id: folderId
      }
      
      const response = await API.document.createMarkdown(createData)

      console.log('创建Markdown文档API响应:', response)

      if (response.data && response.data.code === 200 && response.data.data) {
        const newDocumentId = response.data.data
        toast.success('Markdown文档创建成功')

        // 刷新文档列表
        await loadDocumentsData()

        // 如果当前在对应的笔记本中，自动选择新创建的文档
        const newDocument = allDocuments.value.find(doc => doc.id === newDocumentId)
        if (newDocument && currentBooklist.value?.id === folderId) {
          await selectDocument(newDocument)
        }

        return true
      } else {
        console.warn('创建Markdown文档API响应格式错误:', response.data)
        toast.error(response.data?.message || '创建文档失败')
        return false
      }
      
      return false
    } catch (error) {
      console.error('创建Markdown文档失败:', error)
      toast.error('创建文档失败')
      return false
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 进入Markdown编辑模式
   */
  const enterMarkdownMode = (): void => {
    if (isMarkdownDocument.value && selectedDocument.value) {
      loadMarkdownContent(selectedDocument.value.id)
    }
  }
  
  /**
   * 退出Markdown编辑模式
   */
  const exitMarkdownMode = (): void => {
    if (hasUnsavedChanges.value) {
      const confirmed = confirm('您有未保存的更改，是否要放弃更改？')
      if (!confirmed) {
        return
      }
    }
    
    isMarkdownMode.value = false
    currentMarkdownContent.value = ''
    currentMarkdownVersion.value = 0
    hasUnsavedChanges.value = false
  }
  
  /**
   * 划词翻译
   */
  const translateText = async (
    text: string, 
    targetLang = 'zh-CN'
  ): Promise<TranslateResp | null> => {
    if (!selectedDocument.value) {
      toast.error('请先选择一个文档')
      return null
    }
    
    try {
      isTranslating.value = true
      
      const translateData: TranslateReq = {
        text: text.trim(),
        target_lang: targetLang,
        file_name: selectedDocument.value.file_name
      }
      
      const response = await API.translate.translate(translateData)

      console.log('翻译API响应:', response)

      if (response.data && response.data.code === 200 && response.data.data) {
        lastTranslation.value = response.data.data
        return response.data.data
      } else {
        console.warn('翻译API响应格式错误:', response.data)
        toast.error(response.data?.message || '翻译失败')
        return null
      }
    } catch (error) {
      console.error('翻译失败:', error)
      toast.error('翻译失败')
      return null
    } finally {
      isTranslating.value = false
    }
  }
  
  /**
   * 获取笔记本中的文档数量
   */
  const getBooklistDocumentCount = (booklistId: number): number => {
    return allDocuments.value.filter(file => 
      file.folder_id === booklistId && 
      file.file_status === FileStatusEnum.AVAILABLE
    ).length
  }
  
  /**
   * 检查文档类型
   */
  const getDocumentType = (fileName: string): string => {
    const extension = fileName.split('.').pop()?.toLowerCase() || ''
    
    switch (extension) {
      case 'md':
        return 'markdown'
      case 'pdf':
        return 'pdf'
      case 'doc':
      case 'docx':
        return 'word'
      case 'ppt':
      case 'pptx':
        return 'powerpoint'
      case 'xls':
      case 'xlsx':
        return 'excel'
      case 'txt':
        return 'text'
      default:
        return 'unknown'
    }
  }
  
  /**
   * 键盘快捷键处理
   */
  const handleKeyboardShortcut = async (event: KeyboardEvent): Promise<void> => {
    // Ctrl+S 保存
    if (event.ctrlKey && event.key === 's') {
      event.preventDefault()
      if (canSaveMarkdown.value) {
        await saveMarkdownDocument()
      }
    }
    
    // Esc 退出编辑模式
    if (event.key === 'Escape' && isMarkdownMode.value) {
      exitMarkdownMode()
    }
  }
  
  /**
   * 重置状态
   */
  const reset = (): void => {
    booklists.value = []
    allDocuments.value = []
    currentBooklist.value = null
    selectedDocument.value = null
    currentMarkdownContent.value = ''
    currentMarkdownVersion.value = 0
    isMarkdownMode.value = false
    hasUnsavedChanges.value = false
    isSaving.value = false
    isTranslating.value = false
    lastTranslation.value = null
    searchQuery.value = ''
    lastUpdated.value = null
  }

  /**
   * 重命名文档
   */
  const renameDocument = async (fileId: number, newName: string): Promise<boolean> => {
    try {
      await API.item.rename({
        file_id: fileId,
        folder_id: null,
        new_name: newName
      })
      
      // 更新本地状态中的文档名称
      const docIndex = allDocuments.value.findIndex(doc => doc.id === fileId)
      if (docIndex > -1) {
        allDocuments.value[docIndex].file_name = newName
      }
      
      // 如果是当前选中的文档，也要更新
      if (selectedDocument.value && selectedDocument.value.id === fileId) {
        selectedDocument.value.file_name = newName
      }
      
      toast.success('文档重命名成功')
      return true
    } catch (error) {
      console.error('文档重命名失败:', error)
      toast.error('文档重命名失败')
      return false
    }
  }

  /**
   * 删除文档
   */
  const deleteDocument = async (fileId: number): Promise<boolean> => {
    try {
      await API.item.delete({
        file_ids: [fileId],
        folder_ids: []
      })
      
      // 从本地状态中移除被删除的文档
      allDocuments.value = allDocuments.value.filter(doc => doc.id !== fileId)
      
      // 如果删除的是当前选中的文档，清除选择
      if (selectedDocument.value && selectedDocument.value.id === fileId) {
        selectedDocument.value = null
        currentMarkdownContent.value = ''
        currentMarkdownVersion.value = 0
        isMarkdownMode.value = false
        hasUnsavedChanges.value = false
      }
      
      toast.success('文档删除成功')
      return true
    } catch (error) {
      console.error('删除文档失败:', error)
      toast.error('删除文档失败')
      return false
    }
  }
  
  /**
   * 从云盘加载分类（笔记本）
   */
  const loadCategoriesFromDrive = async (): Promise<void> => {
    try {
      console.log('📄 开始刷新文档库数据...')
      isLoading.value = true
      const response = await API.document.getDocumentView()
      console.log('📄 文档库API响应:', response)

      if (response.data) {
        // 检查数据结构，适配不同的响应格式
        const actualData = (response.data as any).data || response.data
        console.log('Documents API response:', response)
        console.log('实际数据结构:', actualData)
        console.log('API返回的booklists:', actualData.booklists)
        console.log('API返回的files:', actualData.files)
        
        booklists.value = actualData.booklists || []
        allDocuments.value = actualData.files || []
        lastUpdated.value = new Date()
        console.log('📄 文档库数据刷新完成')
      }
    } catch (error) {
      console.error('加载文档数据失败:', error)
      toast.error('加载文档数据失败')
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 根据路径加载文档
   */
  const loadDocumentByPath = async (path: string): Promise<void> => {
    try {
      console.log('尝试根据路径加载文档:', path)
      console.log('当前所有文档:', allDocuments.value)

      // 首先确保文档数据已加载
      if (allDocuments.value.length === 0) {
        console.log('文档数据未加载，先加载文档数据')
        await loadCategoriesFromDrive()
      }

      // 根据路径找到对应的文档
      // 支持多种匹配方式：完整路径、文件名、或路径包含文件名
      const document = allDocuments.value.find(doc => {
        const fileName = doc.file_name
        return fileName === path ||
               path.endsWith(fileName) ||
               path.includes(fileName)
      })

      console.log('找到的文档:', document)

      if (document) {
        console.log('设置当前文档:', document)
        selectedDocument.value = document

        // 如果是Markdown文档，加载内容
        if (isMarkdownDocument.value) {
          await loadMarkdownContent(document.id)
        }
      } else {
        console.warn('未找到匹配的文档:', path)
        toast.warning(`未找到文档: ${path}`)
      }
    } catch (error) {
      console.error('加载文档失败:', error)
      toast.error('加载文档失败')
    }
  }
  
  /**
   * 设置当前文档
   */
  const setCurrentDocument = (doc: FileInfo | null): void => {
    selectedDocument.value = doc
    if (!doc) {
      currentMarkdownContent.value = ''
      currentMarkdownVersion.value = 0
      isMarkdownMode.value = false
      hasUnsavedChanges.value = false
    }
  }

  // 防止重复排序的标志
  let isReorderingCategories = false
  
  /**
   * 重新排序分类（笔记本）
   */
  const reorderCategories = async (oldIndex: number, newIndex: number): Promise<void> => {
    // 防止重复调用
    if (isReorderingCategories) {
      console.log('正在排序分类中，忽略重复请求')
      return
    }
    
    try {
      isReorderingCategories = true
      
      // 使用排序后的数组进行拖拽排序计算
      const sortedCategoriesArray = [...sortedBooklists.value]
      if (oldIndex < 0 || oldIndex >= sortedCategoriesArray.length || newIndex < 0 || newIndex >= sortedCategoriesArray.length) {
        console.error('排序索引超出范围')
        return
      }

      const movedCategory = sortedCategoriesArray[oldIndex]
      console.log(`正在移动分类: ${movedCategory.folder_name} (ID: ${movedCategory.id}) 从位置 ${oldIndex} 到位置 ${newIndex}`)

      // 计算before_id和after_id
      let beforeId: number | null = null
      let afterId: number | null = null

      if (newIndex > 0) {
        beforeId = sortedCategoriesArray[newIndex - (newIndex > oldIndex ? 0 : 1)].id
      }
      if (newIndex < sortedCategoriesArray.length - 1) {
        afterId = sortedCategoriesArray[newIndex + (newIndex > oldIndex ? 1 : 0)].id
      }

      console.log(`排序参数: before_id=${beforeId}, after_id=${afterId}`)

      // 调用API设置排序
      await API.item.setOrder(movedCategory.id, 'folder', {
        before_id: beforeId,
        after_id: afterId
      })

      // 重新加载数据以获取最新排序
      await loadCategoriesFromDrive()
      toast.success('分类排序已更新')
    } catch (error) {
      console.error('重新排序分类失败:', error)
      toast.error('重新排序分类失败')
    } finally {
      isReorderingCategories = false
    }
  }

  /**
   * 重新排序当前分类中的文档
   */
  const reorderDocumentsInCurrentCategory = async (oldIndex: number, newIndex: number): Promise<void> => {
    try {
      if (!currentBooklist.value) {
        console.error('没有选择当前分类')
        return
      }

      const documents = [...currentBooklistDocuments.value]
      if (oldIndex < 0 || oldIndex >= documents.length || newIndex < 0 || newIndex >= documents.length) {
        console.error('排序索引超出范围')
        return
      }

      const movedDocument = documents[oldIndex]

      // 计算before_id和after_id
      let beforeId: number | null = null
      let afterId: number | null = null

      if (newIndex > 0) {
        beforeId = documents[newIndex - (newIndex > oldIndex ? 0 : 1)].id
      }
      if (newIndex < documents.length - 1) {
        afterId = documents[newIndex + (newIndex > oldIndex ? 1 : 0)].id
      }

      // 调用API设置排序
      await API.item.setOrder(movedDocument.id, 'file', {
        before_id: beforeId,
        after_id: afterId
      })

      // 重新加载数据以获取最新排序
      await loadCategoriesFromDrive()
      toast.success('文档排序已更新')
    } catch (error) {
      console.error('重新排序文档失败:', error)
      toast.error('重新排序文档失败')
    }
  }

  // 初始化WebSocket监听
  setupWebSocketListeners()
  
  return {
    // 状态
    booklists,
    allDocuments,
    isLoading,
    searchQuery,
    lastUpdated,
    currentBooklist,
    selectedDocument,
    currentMarkdownContent,
    currentMarkdownVersion,
    isMarkdownMode,
    hasUnsavedChanges,
    isSaving,
    isTranslating,
    lastTranslation,
    
    // 计算属性
    sortedBooklists,
    currentBooklistDocuments,
    filteredDocuments,
    currentCategoryData,
    
    // 方法
    loadDocumentsData,
    searchDocuments,
    selectBooklist,
    selectDocument,
    closeDocument,
    getDocumentViewUrl,
    getBooklistDocumentCount,
    getDocumentType,
    loadMarkdownContent,
    updateMarkdownContent,
    saveMarkdownDocument,
    createMarkdownDocument,
    enterMarkdownMode,
    exitMarkdownMode,
    translateText,
    handleKeyboardShortcut,
    reset,
    loadCategoriesFromDrive,
    loadDocumentByPath,
    setCurrentDocument,
    reorderCategories,
    reorderDocumentsInCurrentCategory,
    renameDocument,
    deleteDocument
  }
}) 