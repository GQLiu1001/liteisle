import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUIStore = defineStore('ui', () => {
  const isSidebarVisible = ref(true)

  const setSidebarVisible = (visible: boolean) => {
    isSidebarVisible.value = visible
  }

  return {
    isSidebarVisible,
    setSidebarVisible,
  }
}) 