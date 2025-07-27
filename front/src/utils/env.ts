/**
 * 环境配置工具类
 */

// 环境类型
export type AppEnv = 'development' | 'production' | 'electron'

// 环境配置接口
export interface EnvConfig {
  // 应用信息
  appTitle: string
  appVersion: string
  appEnv: AppEnv
  
  // API配置
  apiBaseUrl: string
  wsBaseUrl: string
  
  // 调试配置
  debug: boolean
  
  // 上传配置
  uploadMaxSize: number // MB
  uploadAllowedTypes: string[]
  
  // 功能开关
  enableWebSocket: boolean
  enablePicGo: boolean
  enableFocusMode: boolean
  enableDevTools: boolean
  mockData: boolean
}

/**
 * 获取当前环境
 */
export function getAppEnv(): AppEnv {
  return (import.meta.env.VITE_APP_ENV || 'development') as AppEnv
}

/**
 * 是否为开发环境
 */
export function isDev(): boolean {
  return getAppEnv() === 'development'
}

/**
 * 是否为生产环境
 */
export function isProd(): boolean {
  return getAppEnv() === 'production'
}

/**
 * 是否为 Electron 环境
 */
export function isElectron(): boolean {
  return getAppEnv() === 'electron'
}

/**
 * 字符串转布尔值
 */
function toBool(value: string | undefined, defaultValue = false): boolean {
  if (!value) return defaultValue
  return value.toLowerCase() === 'true'
}

/**
 * 字符串转数字
 */
function toNumber(value: string | undefined, defaultValue = 0): number {
  if (!value) return defaultValue
  const num = parseInt(value, 10)
  return isNaN(num) ? defaultValue : num
}

/**
 * 字符串转数组
 */
function toArray(value: string | undefined, separator = ','): string[] {
  if (!value) return []
  return value.split(separator).map(item => item.trim()).filter(Boolean)
}

/**
 * 获取环境配置
 */
export function getEnvConfig(): EnvConfig {
  const env = import.meta.env
  
  return {
    // 应用信息
    appTitle: env.VITE_APP_TITLE || 'Liteisle Desktop',
    appVersion: env.VITE_APP_VERSION || '1.0.0',
    appEnv: getAppEnv(),
    
    // API配置
    apiBaseUrl: env.VITE_API_BASE_URL || 'http://localhost:8002',
    wsBaseUrl: env.VITE_WS_BASE_URL || 'ws://localhost:8002',
    
    // 调试配置
    debug: toBool(env.VITE_DEBUG, isDev()),
    
    // 上传配置
    uploadMaxSize: toNumber(env.VITE_UPLOAD_MAX_SIZE, 100),
    uploadAllowedTypes: toArray(env.VITE_UPLOAD_ALLOWED_TYPES, ','),
    
    // 功能开关
    enableWebSocket: toBool(env.VITE_ENABLE_WEBSOCKET, true),
    enablePicGo: toBool(env.VITE_ENABLE_PICGO, true),
    enableFocusMode: toBool(env.VITE_ENABLE_FOCUS_MODE, true),
    enableDevTools: toBool(env.VITE_ENABLE_DEV_TOOLS, isDev()),
    mockData: toBool(env.VITE_MOCK_DATA, false)
  }
}

// 导出配置实例
export const envConfig = getEnvConfig()

// 打印环境信息（仅在开发环境）
if (isDev()) {
  console.log('🌍 Environment Config:', {
    env: getAppEnv(),
    apiBaseUrl: envConfig.apiBaseUrl,
    wsBaseUrl: envConfig.wsBaseUrl,
    debug: envConfig.debug
  })
}

/**
 * 安全的 console.log，在生产环境中不输出
 */
export function debugLog(...args: any[]) {
  if (envConfig.debug) {
    console.log(...args)
  }
}

/**
 * 安全的 console.error，总是输出错误
 */
export function debugError(...args: any[]) {
  console.error(...args)
}

/**
 * 安全的 console.warn
 */
export function debugWarn(...args: any[]) {
  if (envConfig.debug) {
    console.warn(...args)
  }
}
