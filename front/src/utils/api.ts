import { http } from './http'
import type { AxiosResponse } from 'axios'
import type {
  ApiResponse,
  // 认证相关
  AuthLoginReq,
  AuthRegisterReq,
  AuthForgotPasswordReq,
  AuthResetPasswordReq,
  AuthInfoResp,
  AuthCurrentUserResp,
  // 文件夹相关
  FolderContentResp,
  FolderCreateReq,
  FolderHierarchyResp,
  // 文件相关
  ItemsRenameReq,
  ItemsOperationReq,
  ItemsDeleteReq,
  SetOrderReq,
  ItemDetailResp,
  // 音乐相关
  MusicViewResp,
  // 文档相关
  DocumentViewResp,
  MarkdownContentResp,
  MarkdownUpdateReq,
  MarkdownCreateReq,
  // 回收站相关
  RecycleBinContentResp,
  RecycleBinReq,
  // 上传下载相关
  FileUploadAsyncResp,
  ItemsSelectionReq,
  DownloadSessionResp,
  // 传输相关
  TransferSummaryResp,
  TransferLogPageResp,
  TransferStatusUpdateReq,
  // 分享相关
  ShareCreateReq,
  ShareCreateResp,
  ShareRecordPageResp,
  ShareVerifyReq,
  ShareInfoResp,
  ShareSaveReq,
  ShareSaveAsyncResp,
  // 专注相关
  FocusStatsPageResp,
  FocusCalendarResp,
  // 翻译相关
  TranslateReq,
  TranslateResp
} from '@/types/api'

// 认证相关API
export const authAPI = {
  // 用户登录
  login: (data: AuthLoginReq): Promise<ApiResponse<AuthInfoResp>> => {
    return http.post('/auth/login', data)
  },

  // 用户注册
  register: (data: AuthRegisterReq): Promise<ApiResponse<AuthInfoResp>> => {
    return http.post('/auth/register', data)
  },

  // 发送验证码
  sendVcode: (email: string): Promise<ApiResponse<null>> => {
    return http.post('/auth/send-vcode', null, { params: { email } })
  },

  // 忘记密码
  forgotPassword: (data: AuthForgotPasswordReq): Promise<ApiResponse<null>> => {
    return http.post('/auth/forgot-password', data)
  },

  // 获取当前用户信息
  getCurrentUser: (): Promise<ApiResponse<AuthCurrentUserResp>> => {
    return http.get('/auth/me')
  },

  // 修改密码
  resetPassword: (data: AuthResetPasswordReq): Promise<ApiResponse<null>> => {
    return http.post('/auth/me/reset-password', data)
  },

  // 上传头像
  uploadAvatar: (file: File): Promise<ApiResponse<string>> => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post('/auth/me/picture', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 重置头像
  resetAvatar: (): Promise<ApiResponse<string>> => {
    return http.put('/auth/me/reset-picture')
  },

  // 退出登录
  logout: (): Promise<ApiResponse<null>> => {
    return http.post('/auth/logout')
  }
}

// 文件夹相关API
export const folderAPI = {
  // 获取指定文件夹内容
  getFolderContent: (
    folderId: number, 
    params?: {
      sort_by?: string
      sort_order?: string
      content?: string
    }
  ): Promise<ApiResponse<FolderContentResp>> => {
    return http.get(`/folders/${folderId}`, { params })
  },

  // 创建文件夹
  createFolder: (data: FolderCreateReq): Promise<ApiResponse<null>> => {
    return http.post('/folders', data)
  },

  // 获取所有文件夹层级
  getFolderHierarchy: (): Promise<ApiResponse<FolderHierarchyResp[]>> => {
    return http.get('/folders/hierarchy')
  }
}

// 项目操作相关API
export const itemAPI = {
  // 重命名项目
  rename: (data: ItemsRenameReq): Promise<ApiResponse<null>> => {
    return http.put('/items/rename', data)
  },

  // 移动项目
  move: (data: ItemsOperationReq): Promise<ApiResponse<null>> => {
    return http.put('/items/move', data)
  },

  // 复制项目
  copy: (data: ItemsOperationReq): Promise<ApiResponse<null>> => {
    return http.post('/items/copy', data)
  },

  // 删除项目（移入回收站）
  delete: (data: ItemsDeleteReq): Promise<ApiResponse<null>> => {
    return http.delete('/items', { data })
  },

  // 获取项目详情
  getDetail: (itemId: number, itemType: 'file' | 'folder'): Promise<ApiResponse<ItemDetailResp>> => {
    return http.get(`/items/${itemId}/detail`, { params: { item_type: itemType } })
  },

  // 设置排序
  setOrder: (itemId: number, itemType: 'file' | 'folder', data: SetOrderReq): Promise<ApiResponse<null>> => {
    return http.put(`/items/${itemId}/set-order`, data, { params: { item_type: itemType } })
  }
}

// 音乐相关API
export const musicAPI = {
  // 获取音乐页面信息
  getMusicView: (content?: string): Promise<ApiResponse<MusicViewResp>> => {
    return http.get('/music', { params: content ? { content } : undefined })
  },

  // 获取音乐播放链接
  getPlayUrl: (fileId: number): Promise<ApiResponse<string>> => {
    return http.get(`/music/${fileId}/play`)
  }
}

// 文档相关API
export const documentAPI = {
  // 获取文档页面信息
  getDocumentView: (content?: string): Promise<ApiResponse<DocumentViewResp>> => {
    return http.get('/documents', { params: content ? { content } : undefined })
  },

  // 获取非MD文档预览链接
  getViewUrl: (fileId: number): Promise<AxiosResponse<ApiResponse<string>>> => {
    return http.get(`/documents/${fileId}/view`)
  },

  // 获取MD文档内容
  getMarkdownContent: (fileId: number): Promise<ApiResponse<MarkdownContentResp>> => {
    return http.get(`/documents/md/${fileId}`)
  },

  // 更新MD文档内容
  updateMarkdownContent: (fileId: number, data: MarkdownUpdateReq): Promise<ApiResponse<null>> => {
    return http.put(`/documents/md/${fileId}`, data)
  },

  // 新建MD文档
  createMarkdown: (data: MarkdownCreateReq): Promise<ApiResponse<number>> => {
    return http.post('/documents/md', data)
  },

  // 获取MD文档版本号
  getMarkdownVersion: (fileId: number): Promise<ApiResponse<number>> => {
    return http.get(`/documents/md-version/${fileId}`)
  }
}

// 回收站相关API
export const recycleBinAPI = {
  // 获取回收站内容
  getContent: (content?: string): Promise<ApiResponse<RecycleBinContentResp>> => {
    return http.get('/recycle-bin', { params: content ? { content } : undefined })
  },

  // 还原项目
  restore: (data: RecycleBinReq): Promise<ApiResponse<null>> => {
    return http.post('/recycle-bin/restore', data)
  },

  // 彻底删除项目
  deleteItems: (data: RecycleBinReq): Promise<ApiResponse<null>> => {
    return http.delete('/recycle-bin/items', { data })
  },

  // 清空回收站
  empty: (): Promise<ApiResponse<null>> => {
    return http.delete('/recycle-bin/all')
  }
}

// 上传下载相关API
export const uploadAPI = {
  // 上传文件
  uploadFile: (
    file: File,
    folderId: number,
    onProgress?: (progress: number) => void
  ): Promise<ApiResponse<FileUploadAsyncResp>> => {
    const formData = new FormData()
    formData.append('file', file)

    return http.post('/upload', formData, {
      params: { folder_id: folderId },
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent: any) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
  }
}

export const downloadAPI = {
  // 创建下载会话
  createDownloadSession: (data: ItemsSelectionReq): Promise<ApiResponse<DownloadSessionResp>> => {
    return http.post('/download/create', data)
  }
}

// 传输相关API
export const transferAPI = {
  // 获取传输任务摘要
  getSummary: (): Promise<ApiResponse<TransferSummaryResp>> => {
    return http.get('/transfers/summary')
  },

  // 获取传输历史记录
  getHistory: (
    status: 'processing' | 'success',
    page = 1,
    size = 20
  ): Promise<ApiResponse<TransferLogPageResp>> => {
    return http.get('/transfers', { params: { status, page, size } })
  },

  // 删除传输记录
  deleteRecord: (logId: number, deleteFile = false): Promise<ApiResponse<null>> => {
    return http.delete(`/transfers/${logId}`, { params: { delete_file: deleteFile } })
  },

  // 清空已完成传输记录
  clearCompleted: (deleteFile = false): Promise<ApiResponse<null>> => {
    return http.delete('/transfers/completed', { params: { delete_file: deleteFile } })
  },

  // 取消上传任务
  cancelUpload: (logId: number): Promise<ApiResponse<null>> => {
    return http.post(`/transfers/upload/${logId}/cancel`)
  },

  // 更新下载任务状态
  updateStatus: (logId: number, data: TransferStatusUpdateReq): Promise<ApiResponse<null>> => {
    return http.put(`/transfers/${logId}/status`, data)
  }
}

// 分享相关API
export const shareAPI = {
  // 创建分享链接
  create: (data: ShareCreateReq): Promise<ApiResponse<ShareCreateResp>> => {
    return http.post('/shares', data)
  },

  // 获取我的分享记录
  getMyShares: (page = 1, size = 10): Promise<ApiResponse<ShareRecordPageResp>> => {
    return http.get('/shares/me', { params: { page, size } })
  },

  // 取消分享
  cancel: (shareId: number): Promise<ApiResponse<null>> => {
    return http.delete(`/shares/${shareId}`)
  },

  // 验证分享信息
  verify: (data: ShareVerifyReq): Promise<ApiResponse<ShareInfoResp>> => {
    return http.post('/shares/verify', data)
  },

  // 转存分享内容
  save: (data: ShareSaveReq): Promise<ApiResponse<ShareSaveAsyncResp>> => {
    return http.post('/shares/save', data)
  }
}

// 专注相关API
export const focusAPI = {
  // 记录专注时长
  recordFocus: (focusMinutes: number): Promise<ApiResponse<string | null>> => {
    return http.post('/focus/records', null, { params: { focus_minutes: focusMinutes } })
  },

  // 获取专注总次数
  getTotalCount: (): Promise<ApiResponse<number>> => {
    return http.get('/focus/stats/total-count')
  },

  // 获取专注记录
  getRecords: (page = 1, size = 10): Promise<ApiResponse<FocusStatsPageResp>> => {
    return http.get('/focus/stats/records', { params: { page, size } })
  },

  // 获取专注日历数据
  getCalendar: (year: number, month: number): Promise<ApiResponse<FocusCalendarResp>> => {
    return http.get('/focus/stats/calendar', { params: { year, month } })
  }
}

// 岛屿相关API
export const islandAPI = {
  // 获取用户岛屿收集情况
  getUserIslands: (): Promise<ApiResponse<string[]>> => {
    return http.get('/islands/me')
  }
}

// 翻译相关API
export const translateAPI = {
  // 文本翻译
  translate: (data: TranslateReq): Promise<ApiResponse<TranslateResp>> => {
    return http.post('/translate', data)
  }
}

// 统一导出所有API
export const API = {
  auth: authAPI,
  folder: folderAPI,
  item: itemAPI,
  music: musicAPI,
  document: documentAPI,
  recycleBin: recycleBinAPI,
  upload: uploadAPI,
  download: downloadAPI,
  transfer: transferAPI,
  share: shareAPI,
  focus: focusAPI,
  island: islandAPI,
  translate: translateAPI
}

export default API 