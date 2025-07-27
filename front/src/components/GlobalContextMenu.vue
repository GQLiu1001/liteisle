<template>
  <div 
    v-if="contextMenuStore.contextMenu.show" 
    :style="{ 
      top: contextMenuStore.contextMenu.position.y + 'px', 
      left: contextMenuStore.contextMenu.position.x + 'px' 
    }"
    class="fixed z-50 bg-white rounded-lg shadow-lg border border-morandi-200 py-2 min-w-[120px]"
    @click.stop
  >
    <template v-for="item in contextMenuStore.contextMenu.items" :key="item.id">
      <!-- 分隔线 -->
      <hr v-if="item.separator" class="my-1 border-morandi-200" />
      
      <!-- 菜单项 -->
      <button 
        v-else
        @click="contextMenuStore.executeAction(item)"
        :disabled="item.disabled"
        :class="[
          'w-full px-4 py-2 text-left text-sm flex items-center gap-2 transition-colors',
          item.type === 'danger' 
            ? 'text-red-600 hover:bg-red-50 disabled:text-red-300' 
            : 'text-morandi-700 hover:bg-morandi-50 disabled:text-morandi-400',
          item.disabled ? 'cursor-not-allowed' : 'cursor-pointer'
        ]"
      >
        <component v-if="item.icon" :is="item.icon" :size="16" />
        {{ item.label }}
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useContextMenuStore } from '@/store/ContextMenuStore'

const contextMenuStore = useContextMenuStore()
</script> 