/// <reference types="vite/client" />

interface ImportMetaEnv {
  // 应用基本信息
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
  readonly VITE_APP_ENV: 'development' | 'production' | 'electron'
  
  // API配置
  readonly VITE_API_BASE_URL: string
  readonly VITE_WS_BASE_URL: string
  
  // 调试配置
  readonly VITE_DEBUG: string
  
  // 上传配置
  readonly VITE_UPLOAD_MAX_SIZE: string
  readonly VITE_UPLOAD_ALLOWED_TYPES: string
  
  // 功能开关
  readonly VITE_ENABLE_WEBSOCKET: string
  readonly VITE_ENABLE_PICGO: string
  readonly VITE_ENABLE_FOCUS_MODE: string
  readonly VITE_ENABLE_DEV_TOOLS: string
  readonly VITE_MOCK_DATA: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// 全局环境变量
declare const __APP_ENV__: string
