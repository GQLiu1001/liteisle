// API响应通用格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 枚举类型定义
export enum FolderTypeEnum {
  SYSTEM = 'system',
  PLAYLIST = 'playlist',
  BOOKLIST = 'booklist'
}

export enum FileTypeEnum {
  MUSIC = 'music',
  DOCUMENT = 'document'
}

export enum FileStatusEnum {
  PROCESSING = 'processing',
  AVAILABLE = 'available',
  FAILED = 'failed'
}

export enum TransferTypeEnum {
  UPLOAD = 'upload',
  DOWNLOAD = 'download'
}

export enum TransferStatusEnum {
  PROCESSING = 'processing',
  PAUSED = 'paused',
  SUCCESS = 'success',
  FAILED = 'failed',
  CANCELED = 'canceled'
}

export enum ItemTypeEnum {
  FILE = 'file',
  FOLDER = 'folder'
}

// 认证相关类型
export interface AuthLoginReq {
  username: string
  password: string
}

export interface AuthRegisterReq {
  username: string
  email: string
  password: string
  vcode: string
}

export interface AuthForgotPasswordReq {
  username: string
  email: string
  vcode: string
  new_password: string
  confirm_password: string
}

export interface AuthResetPasswordReq {
  old_password: string
  new_password: string
  confirm_password: string
}

export interface AuthInfoResp {
  username: string
  email: string
  avatar: string
  token: string
}

export interface AuthCurrentUserResp {
  username: string
  email: string
  avatar: string
  storage_used: number
  storage_quota: number
}

// 文件夹相关类型
export interface FolderInfo {
  id: number
  folder_name: string
  folder_type: FolderTypeEnum
  sub_count: number
  sorted_order: number
  create_time: string
  update_time: string
}

export interface FileInfo {
  id: number
  folder_id: number
  file_name: string
  file_type: FileTypeEnum
  file_size: number
  file_status: FileStatusEnum
  sorted_order: number
  create_time: string
  update_time: string
}

export interface BreadcrumbItem {
  id: number
  name: string
}

export interface FolderContentResp {
  breadcrumb: BreadcrumbItem[]
  folders: FolderInfo[]
  files: FileInfo[]
}

export interface FolderCreateReq {
  name: string
  parent_id: number
  folder_type: FolderTypeEnum
}

export interface ItemsRenameReq {
  file_id?: number | null
  folder_id?: number | null
  new_name: string
}

export interface ItemsOperationReq {
  file_ids: number[]
  folder_ids: number[]
  target_folder_id: number
}

export interface ItemsDeleteReq {
  file_ids: number[]
  folder_ids: number[]
}

export interface SetOrderReq {
  before_id?: number | null
  after_id?: number | null
}

export interface ItemDetailResp {
  id: number
  name: string
  item_type: ItemTypeEnum
  size: number
  path: string
  create_time: string
  update_time: string
}

export interface FolderHierarchyResp {
  id: number
  folder_name: string
  folder_type: FolderTypeEnum
  parent_id: number
}

// 音乐相关类型
export interface MusicFileInfo extends FileInfo {
  folder_id: number
  artist?: string
  album?: string
  duration: number
  cover_art_url?: string
}

export interface MusicViewResp {
  playlists: FolderInfo[]
  files: MusicFileInfo[]
}

// 文档相关类型
export interface DocumentViewResp {
  booklists: FolderInfo[]
  files: FileInfo[]
}

export interface MarkdownContentResp {
  content: string
  version: number
}

export interface MarkdownUpdateReq {
  content: string
  version: number
}

export interface MarkdownCreateReq {
  name: string
  folder_id: number
}

// 回收站相关类型
export interface RecycleBinFolderInfo {
  original_id: number
  original_name: string
  original_type: FolderTypeEnum
  sub_count: number
  delete_time: string
  expire_time: string
}

export interface RecycleBinFileInfo {
  original_id: number
  original_name: string
  original_type: FileTypeEnum
  file_size: number
  delete_time: string
  expire_time: string
}

export interface RecycleBinContentResp {
  folders: RecycleBinFolderInfo[]
  files: RecycleBinFileInfo[]
}

export interface RecycleBinReq {
  file_ids: number[]
  folder_ids: number[]
}

// 上传相关类型
export interface FileUploadAsyncResp {
  log_id: number
  file_id: number
  log_status: TransferStatusEnum
  initial_file_data: FileInfo
}

// 下载相关类型
export interface ItemsSelectionReq {
  file_ids?: number[]
  folder_id?: number | null
}

export interface DownloadFileItem {
  log_id: number
  file_id: number
  file_name: string
  relative_path: string
  size: number
  download_url: string
}

export interface DownloadFolderItem {
  folder_id: number
  folder_name: string
  relative_path: string
}

export interface DownloadSessionResp {
  total_size: number
  total_files: number
  folder_d: DownloadFolderItem | null
  files_d: DownloadFileItem[]
}

// 传输相关类型
export interface TransferSummaryResp {
  upload_count: number
  download_count: number
}

export interface TransferLogItem {
  log_id: number
  item_name: string
  item_size: number
  transfer_type: TransferTypeEnum
  create_time: string
}

export interface TransferLogPageResp {
  total: number
  current_page: number
  page_size: number
  records: TransferLogItem[]
}

export interface TransferStatusUpdateReq {
  log_status: TransferStatusEnum
  error_message?: string | null
  transfer_duration_ms?: number | null
}

// 分享相关类型
export interface ShareCreateReq {
  file_id?: number | null
  folder_id?: number | null
  is_encrypted: boolean
  expires_in_days: number
}

export interface ShareCreateResp {
  share_token: string
  share_password?: string
}

export interface ShareRecordItem {
  id: number
  file_id?: number | null
  folder_id?: number | null
  share_item_name: string
  share_token: string
  share_password?: string
  create_time: string
  expire_time?: string | null
}

export interface ShareRecordPageResp {
  total: number
  current_page: number
  page_size: number
  records: ShareRecordItem[]
}

export interface ShareVerifyReq {
  share_token: string
  share_password?: string
}

export interface ShareInfoResp {
  item_type: ItemTypeEnum
  item_name: string
  item_size: number
  total_files?: number
}

export interface ShareSaveReq {
  share_token: string
  share_password?: string
  target_folder_id: number
}

export interface ShareSaveAsyncResp {
  total_files_to_save: number
  initial_file_data_list: FileInfo[]
}

// 专注相关类型
export interface FocusRecordItem {
  id: number
  focus_minutes: number
  create_time: string
}

export interface FocusStatsPageResp {
  total: number
  current_page: number
  page_size: number
  records: FocusRecordItem[]
}

export interface FocusCalendarResp {
  year_month: string
  check_in_days: number[]
  total_check_in_count: number
  total_focus_minutes: number
}

// 翻译相关类型
export interface TranslateReq {
  text: string
  target_lang?: string
  file_name: string
}

export interface TranslateResp {
  original_text: string
  translated_text: string
}

// WebSocket事件类型
export interface WebSocketEvent {
  event: string
  payload: any
}

export interface FileStatusUpdatedEvent {
  file_id: number
  file_status: FileStatusEnum
  file_data?: FileInfo
}

export interface TransferLogUpdatedEvent {
  log_id: number
  logId?: number // 兼容camelCase
  log_status: TransferStatusEnum
  error_message?: string
}

export interface NotificationEvent {
  type: 'info' | 'success' | 'warning' | 'error'
  title: string
  message: string
} 