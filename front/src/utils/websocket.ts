import { ref, reactive } from 'vue'
import { envConfig } from '@/utils/env'
import type {
  WebSocketEvent,
  FileStatusUpdatedEvent,
  TransferLogUpdatedEvent,
  NotificationEvent
} from '@/types/api'

export type WebSocketEventType =
  | 'file.status.updated'
  | 'transfer.log.updated'
  | 'notification.new'
  | 'share.save.completed'

export type WebSocketEventHandler<T = any> = (payload: T) => void

class WebSocketManager {
  private ws: WebSocket | null = null
  private eventHandlers = new Map<WebSocketEventType, Set<WebSocketEventHandler>>()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private heartbeatInterval: number | null = null
  
  public connectionStatus = ref<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected')
  public lastError = ref<string | null>(null)

  constructor() {
    // 在页面卸载时关闭连接
    window.addEventListener('beforeunload', () => {
      this.disconnect()
    })
  }

  // 连接WebSocket
  connect(token?: string) {
    console.log('🔌 正在尝试连接WebSocket...')
    
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('🔌 WebSocket已连接')
      return
    }

    if (this.connectionStatus.value === 'connecting') {
      console.log('🔌 WebSocket正在连接中...')
      return
    }

    this.connectionStatus.value = 'connecting'
    console.log('🔌 正在连接WebSocket...')

    try {
      // 确保WebSocket连接的URL正确
      let wsUrl = `${envConfig.wsBaseUrl}/ws`

      // 通过URL参数传递token（最常用的方案）
      if (token) {
        wsUrl += `?token=${encodeURIComponent(token)}`
      }

      console.log('🔌 尝试连接WebSocket:', wsUrl.replace(/token=[^&?]+/, 'token=***'))

      // 创建WebSocket连接
      const socket = new WebSocket(wsUrl)

      this.ws = socket

      this.ws.onopen = () => {
        console.log('🔌 WebSocket连接成功，认证已通过')
        this.connectionStatus.value = 'connected'
        this.reconnectAttempts = 0
        this.startHeartbeat()

        // 认证已在握手时完成，无需再次发送认证信息
        console.log('✅ WebSocket认证成功，开始监听事件')
      }

      this.ws.onmessage = (event) => {
        try {
          // 处理心跳响应
          if (event.data === 'pong') {
            console.log('💓 收到心跳响应')
            return
          }

          const message = JSON.parse(event.data)
          console.log('📨 收到WebSocket消息:', message)
          this.handleMessage(message)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error, 'Raw data:', event.data)
        }
      }

      this.ws.onerror = (event) => {
        console.error('WebSocket错误:', event)
        this.connectionStatus.value = 'error'
        this.lastError.value = 'WebSocket连接错误'
      }

      this.ws.onclose = (event) => {
        console.log('WebSocket连接已关闭:', event.code)
        this.connectionStatus.value = 'disconnected'
        this.ws = null
        this.stopHeartbeat()

        // 只有在不是主动关闭的情况下才重连
        if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.scheduleReconnect()
        }
      }
    } catch (error) {
      console.error('创建WebSocket连接失败:', error)
      this.connectionStatus.value = 'error'
      this.lastError.value = '创建WebSocket连接失败'
    }
  }

  disconnect() {
    this.stopHeartbeat()
    
    if (this.ws) {
      this.ws.close(1000, '主动断开连接')
      this.ws = null
    }
    
    this.connectionStatus.value = 'disconnected'
  }

  private scheduleReconnect() {
    this.reconnectAttempts++
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1)
    
    console.log(`${delay}ms后尝试第${this.reconnectAttempts}次重连...`)
    
    setTimeout(() => {
      if (this.connectionStatus.value !== 'connected') {
        // 重新获取token尝试连接
        const token = localStorage.getItem('access_token')
        this.connect(token || undefined)
      }
    }, delay)
  }

  private startHeartbeat() {
    this.stopHeartbeat()

    this.heartbeatInterval = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        // 发送纯字符串心跳，符合后端期望
        this.ws.send('ping')
      }
    }, 30000) // 30秒心跳
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  private send(event: string, payload: any) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      const message: WebSocketEvent = { event, payload }
      this.ws.send(JSON.stringify(message))
    }
  }

  private handleMessage(message: any) {
    console.log('🔄 处理WebSocket事件:', message.event, 'Data:', message.data)
    const handlers = this.eventHandlers.get(message.event as WebSocketEventType)

    if (handlers) {
      console.log(`📢 找到 ${handlers.size} 个事件处理器 for ${message.event}`)
      handlers.forEach((handler, index) => {
        try {
          console.log(`🎯 执行处理器 ${index + 1}/${handlers.size}:`, message.event)
          // 使用 message.data 而不是 message.payload
          handler(message.data || message.payload)
        } catch (error) {
          console.error(`WebSocket事件处理器错误 (${index + 1}):`, error)
        }
      })
    } else {
      console.warn(`❌ 未找到事件处理器: ${message.event}`)
      console.log('📋 当前已注册的事件:', Array.from(this.eventHandlers.keys()))
    }
  }

  // 订阅事件
  on<T = any>(event: WebSocketEventType, handler: WebSocketEventHandler<T>) {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, new Set())
    }
    
    this.eventHandlers.get(event)!.add(handler)
    
    // 返回取消订阅函数
    return () => {
      const handlers = this.eventHandlers.get(event)
      if (handlers) {
        handlers.delete(handler)
        if (handlers.size === 0) {
          this.eventHandlers.delete(event)
        }
      }
    }
  }

  // 取消订阅事件
  off(event: WebSocketEventType, handler?: WebSocketEventHandler) {
    if (!handler) {
      // 如果没有指定处理器，移除该事件的所有处理器
      this.eventHandlers.delete(event)
    } else {
      const handlers = this.eventHandlers.get(event)
      if (handlers) {
        handlers.delete(handler)
        if (handlers.size === 0) {
          this.eventHandlers.delete(event)
        }
      }
    }
  }

  // 一次性订阅
  once<T = any>(event: WebSocketEventType, handler: WebSocketEventHandler<T>) {
    const unsubscribe = this.on(event, (payload: T) => {
      handler(payload)
      unsubscribe()
    })
    
    return unsubscribe
  }
}

// 创建全局WebSocket实例
export const wsManager = new WebSocketManager()

// 便捷的订阅函数
export function onFileStatusUpdated(handler: WebSocketEventHandler<FileStatusUpdatedEvent>) {
  return wsManager.on('file.status.updated', handler)
}

export function onTransferLogUpdated(handler: WebSocketEventHandler<TransferLogUpdatedEvent>) {
  return wsManager.on('transfer.log.updated', handler)
}

export function onNotification(handler: WebSocketEventHandler<NotificationEvent>) {
  return wsManager.on('notification.new', handler)
}

export function onShareSaveCompleted(handler: WebSocketEventHandler<any>) {
  return wsManager.on('share.save.completed', handler)
}

// 连接和断开连接的便捷函数
export function connectWebSocket(token?: string) {
  wsManager.connect(token)
}

export function disconnectWebSocket() {
  wsManager.disconnect()
}

/**
 * 确保WebSocket连接用于上传操作
 * 在上传前自动建立连接，并返回一个清理函数
 */
export function ensureWebSocketForUpload(): () => void {
  const token = localStorage.getItem('access_token')
  if (token) {
    connectWebSocket(token)
  }

  // 返回一个空的清理函数，因为WebSocket连接是全局管理的
  // 实际的连接关闭由页面卸载事件或手动调用处理
  return () => {
    // 可以在这里添加特定的清理逻辑，如果需要的话
  }
}

/**
 * 监听上传完成并执行回调
 * @param callback 上传完成后的回调函数
 * @returns 取消监听的函数
 */
export function onUploadComplete(callback: () => void): () => void {
  return onFileStatusUpdated((payload) => {
    if (payload.transferStatus === 'SUCCESS') {
      // 延迟执行回调，确保后端数据已更新
      setTimeout(callback, 1000)
    }
  })
}

export default wsManager