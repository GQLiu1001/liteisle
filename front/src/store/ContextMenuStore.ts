import { defineStore } from 'pinia'
import { reactive } from 'vue'

export interface ContextMenuItem {
  id: string
  label?: string
  icon?: string
  action?: () => void
  disabled?: boolean
  separator?: boolean
  type?: 'normal' | 'danger'
}

export interface ContextMenuState {
  show: boolean
  position: { x: number; y: number }
  items: ContextMenuItem[]
  data?: any // 右键菜单关联的数据
}

export const useContextMenuStore = defineStore('contextMenu', () => {
  const contextMenu = reactive<ContextMenuState>({
    show: false,
    position: { x: 0, y: 0 },
    items: [],
    data: null
  })

  // 显示右键菜单
  const showContextMenu = (
    event: MouseEvent,
    items: ContextMenuItem[],
    data?: any
  ) => {
    // 隐藏所有现有的右键菜单
    hideContextMenu()
    
    contextMenu.show = true
    contextMenu.position = { x: event.clientX, y: event.clientY }
    contextMenu.items = items
    contextMenu.data = data
    
    // 点击其他地方关闭菜单
    setTimeout(() => {
      document.addEventListener('click', hideContextMenu, { once: true })
    }, 0)
  }

  // 隐藏右键菜单
  const hideContextMenu = () => {
    contextMenu.show = false
    contextMenu.items = []
    contextMenu.data = null
  }

  // 执行菜单项动作
  const executeAction = (item: ContextMenuItem) => {
    if (!item.disabled && item.action) {
      item.action()
    }
    hideContextMenu()
  }

  return {
    contextMenu,
    showContextMenu,
    hideContextMenu,
    executeAction
  }
}) 