/**
 * WebSocket 连接测试工具
 */

export function testWebSocketConnection(url: string = 'ws://localhost:8002/ws', token?: string): Promise<boolean> {
  return new Promise((resolve) => {
    // 如果提供了token，添加到URL中
    let testUrl = url
    if (token) {
      testUrl += `?token=${encodeURIComponent(token)}`
    } else {
      // 尝试从localStorage获取token
      const savedToken = localStorage.getItem('access_token')
      if (savedToken) {
        testUrl += `?token=${encodeURIComponent(savedToken)}`
      }
    }

    console.log('🧪 测试WebSocket连接:', testUrl.replace(/token=[^&?]+/, 'token=***'))

    const ws = new WebSocket(testUrl)
    let resolved = false
    
    const cleanup = () => {
      if (!resolved) {
        resolved = true
        ws.close()
      }
    }
    
    // 设置超时
    const timeout = setTimeout(() => {
      if (!resolved) {
        console.log('❌ WebSocket连接超时')
        cleanup()
        resolve(false)
      }
    }, 5000)
    
    ws.onopen = () => {
      console.log('✅ WebSocket连接成功')
      clearTimeout(timeout)
      cleanup()
      resolve(true)
    }
    
    ws.onerror = (error) => {
      console.log('❌ WebSocket连接失败:', error)
      clearTimeout(timeout)
      cleanup()
      resolve(false)
    }
    
    ws.onclose = (event) => {
      console.log('🔌 WebSocket连接关闭:', event.code, event.reason)
      if (!resolved) {
        clearTimeout(timeout)
        cleanup()
        resolve(false)
      }
    }
  })
}

export async function diagnoseWebSocketIssues() {
  console.log('🔍 开始WebSocket诊断...')

  // 检查是否有token
  const token = localStorage.getItem('access_token')
  if (!token) {
    console.log('❌ 未找到访问令牌，请先登录')
    return null
  }

  console.log('✅ 找到访问令牌，开始测试连接...')

  // 测试不同的端口和路径
  const testUrls = [
    'ws://localhost:8002/ws',
    'ws://localhost:8080/ws',
    'ws://localhost:3000/ws',
    'ws://127.0.0.1:8002/ws'
  ]

  for (const url of testUrls) {
    const success = await testWebSocketConnection(url, token)
    if (success) {
      console.log(`✅ 找到可用的WebSocket服务: ${url}`)
      return url
    }
  }

  console.log('❌ 未找到可用的WebSocket服务')
  console.log('💡 可能的解决方案:')
  console.log('1. 检查后端WebSocket服务是否启动')
  console.log('2. 确认WebSocket认证配置')
  console.log('3. 检查token是否有效')
  console.log('4. 检查防火墙设置')

  return null
}

// 在开发环境下将测试工具挂载到全局对象
if (import.meta.env.DEV) {
  (window as any).testWebSocket = {
    test: testWebSocketConnection,
    diagnose: diagnoseWebSocketIssues
  }
  
  console.log('🛠️ WebSocket测试工具已加载')
  console.log('使用方法:')
  console.log('  testWebSocket.test() - 测试默认WebSocket连接')
  console.log('  testWebSocket.test("ws://localhost:8080/ws") - 测试指定URL')
  console.log('  testWebSocket.diagnose() - 自动诊断WebSocket问题')
}
