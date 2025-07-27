<template>
  <div class="env-info p-4 bg-gray-100 rounded-lg">
    <h3 class="text-lg font-semibold mb-3">环境配置信息</h3>
    
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="space-y-2">
        <div class="flex justify-between">
          <span class="font-medium">应用环境:</span>
          <span class="px-2 py-1 rounded text-sm" :class="envBadgeClass">
            {{ config.appEnv }}
          </span>
        </div>
        
        <div class="flex justify-between">
          <span class="font-medium">应用版本:</span>
          <span>{{ config.appVersion }}</span>
        </div>
        
        <div class="flex justify-between">
          <span class="font-medium">调试模式:</span>
          <span :class="config.debug ? 'text-green-600' : 'text-red-600'">
            {{ config.debug ? '开启' : '关闭' }}
          </span>
        </div>
      </div>
      
      <div class="space-y-2">
        <div class="flex justify-between">
          <span class="font-medium">API地址:</span>
          <span class="text-sm text-blue-600">{{ config.apiBaseUrl }}</span>
        </div>
        
        <div class="flex justify-between">
          <span class="font-medium">WebSocket:</span>
          <span class="text-sm text-blue-600">{{ config.wsBaseUrl }}</span>
        </div>
        
        <div class="flex justify-between">
          <span class="font-medium">上传限制:</span>
          <span>{{ config.uploadMaxSize }}MB</span>
        </div>
      </div>
    </div>
    
    <div class="mt-4 pt-4 border-t border-gray-200">
      <h4 class="font-medium mb-2">功能开关</h4>
      <div class="flex flex-wrap gap-2">
        <span v-if="config.enableWebSocket" class="px-2 py-1 bg-green-100 text-green-800 rounded text-sm">
          WebSocket
        </span>
        <span v-if="config.enablePicGo" class="px-2 py-1 bg-blue-100 text-blue-800 rounded text-sm">
          PicGo
        </span>
        <span v-if="config.enableFocusMode" class="px-2 py-1 bg-purple-100 text-purple-800 rounded text-sm">
          专注模式
        </span>
        <span v-if="config.enableDevTools" class="px-2 py-1 bg-yellow-100 text-yellow-800 rounded text-sm">
          开发工具
        </span>
      </div>
    </div>
    
    <div class="mt-4 pt-4 border-t border-gray-200">
      <h4 class="font-medium mb-2">支持的文件类型</h4>
      <div class="text-sm text-gray-600">
        {{ config.uploadAllowedTypes.join(', ') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { envConfig } from '@/utils/env'

const config = envConfig

const envBadgeClass = computed(() => {
  switch (config.appEnv) {
    case 'production':
      return 'bg-green-100 text-green-800'
    case 'local':
      return 'bg-blue-100 text-blue-800'
    case 'development':
      return 'bg-yellow-100 text-yellow-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
})
</script>

<style scoped>
.env-info {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
</style>
