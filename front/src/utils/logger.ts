/**
 * 日志管理工具
 * 在生产环境中自动禁用调试日志，避免内存泄漏
 */

import { envConfig } from './env'

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3
}

class Logger {
  private maxLogs = 1000 // 最大日志条数
  private logs: Array<{ level: LogLevel; message: string; timestamp: Date; args: any[] }> = []

  private shouldLog(level: LogLevel): boolean {
    // 生产环境只显示 WARN 和 ERROR
    if (!envConfig.debug && level < LogLevel.WARN) {
      return false
    }
    return true
  }

  private addToHistory(level: LogLevel, message: string, args: any[]) {
    // 只在调试模式下保存历史记录
    if (!envConfig.debug) return

    this.logs.push({
      level,
      message,
      timestamp: new Date(),
      args
    })

    // 限制日志数量，防止内存泄漏
    if (this.logs.length > this.maxLogs) {
      this.logs.shift() // 移除最旧的日志
    }
  }

  debug(message: string, ...args: any[]) {
    if (!this.shouldLog(LogLevel.DEBUG)) return
    
    console.log(`🐛 [DEBUG] ${message}`, ...args)
    this.addToHistory(LogLevel.DEBUG, message, args)
  }

  info(message: string, ...args: any[]) {
    if (!this.shouldLog(LogLevel.INFO)) return
    
    console.log(`ℹ️ [INFO] ${message}`, ...args)
    this.addToHistory(LogLevel.INFO, message, args)
  }

  warn(message: string, ...args: any[]) {
    if (!this.shouldLog(LogLevel.WARN)) return
    
    console.warn(`⚠️ [WARN] ${message}`, ...args)
    this.addToHistory(LogLevel.WARN, message, args)
  }

  error(message: string, ...args: any[]) {
    // 错误总是显示
    console.error(`❌ [ERROR] ${message}`, ...args)
    this.addToHistory(LogLevel.ERROR, message, args)
  }

  // 获取日志历史（仅调试模式）
  getHistory() {
    return envConfig.debug ? this.logs : []
  }

  // 清空日志历史
  clearHistory() {
    this.logs = []
  }

  // 获取内存使用情况
  getMemoryUsage() {
    if (!envConfig.debug) return null
    
    return {
      logCount: this.logs.length,
      maxLogs: this.maxLogs,
      memoryEstimate: `${(this.logs.length * 100 / 1024).toFixed(2)} KB` // 粗略估计
    }
  }
}

// 创建全局日志实例
export const logger = new Logger()

// 便捷的导出函数
export const debugLog = logger.debug.bind(logger)
export const infoLog = logger.info.bind(logger)
export const warnLog = logger.warn.bind(logger)
export const errorLog = logger.error.bind(logger)

// 在开发环境中显示日志配置
if (envConfig.debug) {
  logger.info('Logger initialized', {
    environment: envConfig.appEnv,
    debugMode: envConfig.debug,
    maxLogs: 1000
  })
}
