<template>
  <header 
    :class="[
      'fixed top-0 right-0 h-[50px] bg-transparent flex items-center justify-between px-8 draggable-area z-10 transition-all duration-300 select-none',
      uiStore.isSidebarVisible ? 'left-[150px]' : 'left-0'
    ]"
  >
    <!-- 左侧 - 开始专注按钮 -->
    <div class="mt-4">
      <button 
        @click="toggleFocus"
        :class="[
          'btn-primary flex items-center gap-2 px-6 py-2 text-lg',
          isFocusing ? 'from-red-500 to-red-600' : ''
        ]"
      >
        <PenTool :size="16" />
        <span>{{ isFocusing ? '停止专注' : '开始专注' }}</span>
      </button>
    </div>

    <!-- 中间 - 艺术字Logo -->
    <div class="absolute left-1/2 transform -translate-x-1/2 flex items-center">
      <img
        :src="titlePic"
        alt="Liteisle"
        class="h-8 lg:h-14 object-contain drop-shadow-lg hover:drop-shadow-xl transition-all duration-300 hover:scale-105"
      />
    </div>

    <!-- 右侧 - 用户头像和窗口控制 -->
    <div class="flex items-center gap-4">
      <!-- 用户头像下拉菜单 -->
      <div class="relative user-menu" @click="toggleUserMenu">
        <div class="w-8 h-8 rounded-full bg-white border-2 border-morandi-400 flex items-center justify-center cursor-pointer hover:border-teal-400 transition-colors overflow-hidden">
          <img 
            :src="authStore.user?.avatar || defaultUserPic" 
            alt="用户头像" 
            class="w-full h-full object-cover"
          />
        </div>
        
        <!-- 下拉菜单 -->
        <div 
          v-if="showUserMenu"
          class="absolute left-1/2 -translate-x-1/2 top-10 w-32 bg-white rounded-lg shadow-lg border border-morandi-200 py-2 z-50"
          @click.stop
        >
          <button 
            @click.stop="goToAccountSettings"
            class="w-full px-4 py-2 hover:bg-morandi-50 flex items-center justify-center gap-2 text-morandi-700 hover:text-teal-600 transition-colors"
          >
            <CircleEqual :size="16" />
            <span>详情</span>
          </button>
          <button 
            @click.stop="handleLogout"
            class="w-full px-4 py-2 hover:bg-morandi-50 flex items-center justify-center gap-2 text-morandi-700 hover:text-red-600 transition-colors"
          >
            <LogOut :size="16" />
            <span>注销</span>
          </button>
        </div>
      </div>

      <!-- 窗口控制按钮 -->
      <div class="flex items-center gap-2">
        <button @click="minimizeWindow" class="topbar-control">
          <Minus :size="16" />
        </button>
        <button @click="maximizeWindow" class="topbar-control">
          <Square :size="14" :class="{ 'opacity-50': isMaximized }" />
        </button>
        <button @click="closeWindow" class="topbar-control hover:bg-red-100 hover:text-red-600">
          <X :size="16" />
        </button>
      </div>
    </div>

    <div 
  v-if="showCloseConfirmation" 
  class="fixed inset-0 flex items-center justify-center z-[9999]"
  @click="showCloseConfirmation = false"
>
  <div 
    class="bg-white rounded-2xl p-6 max-w-sm w-full mx-4 shadow-2xl"
    @click.stop
  >
    <div class="text-center mb-4">
      <div class="w-12 h-12 mx-auto mb-3 bg-orange-100 rounded-full flex items-center justify-center">
        <X :size="24" class="text-orange-600" />
      </div>
      <h3 class="text-lg font-semibold text-gray-900 mb-2">确认关闭应用</h3>
      <p class="text-sm text-gray-600">您想要退出应用还是最小化到系统托盘？</p>
    </div>
    
    <div class="flex flex-col gap-2">
      <button 
        @click="handleMinimizeToTrayWithRemember"
        class="w-full px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors flex items-center justify-center gap-2 text-sm"
      >
        <Minimize :size="16" />
        <span>最小化到托盘</span>
      </button>
      
      <button 
        @click="handleQuitAppWithRemember"
        class="w-full px-4 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors flex items-center justify-center gap-2 text-sm"
      >
        <X :size="16" />
        <span>退出应用</span>
      </button>
      
      <button 
        @click="showCloseConfirmation = false"
        class="w-full px-4 py-2.5 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg transition-colors text-sm"
      >
        取消
      </button>
      <label class="flex items-center mt-2 select-none text-sm text-gray-500">
        <input type="checkbox" v-model="rememberCloseChoice" class="mr-2" />
        记住我的选择
      </label>
    </div>
  </div>
</div>
  </header>
  <template v-if="showNewIsleDialog">
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-[9999]" @click="showNewIsleDialog=false">
      <div class="bg-white rounded-2xl p-6 max-w-sm w-full mx-4 shadow-2xl" @click.stop>
        <div class="text-center mb-4">
          <h3 class="text-lg font-semibold text-gray-900 mb-2">恭喜获得新岛屿！</h3>
          <p class="text-sm text-gray-600">返回首页查看你的岛屿吧~</p>
          <img v-if="newIsleImageUrl" :src="newIsleImageUrl" alt="isle" class="w-full rounded-lg mt-4" />
        </div>
        <button 
          @click="() => { showNewIsleDialog=false; router.push({ name: 'home' }) }" 
          class="w-full px-4 py-2 bg-teal-500 hover:bg-teal-600 text-white rounded-lg transition-colors flex items-center justify-center gap-2 text-sm">
          返回首页
        </button>
      </div>
    </div>
  </template>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { PenTool, Minus, Square, X, LogOut, CircleEqual, Minimize } from 'lucide-vue-next'

// 导入图片资源
import defaultUserPicUrl from '/defaultuserpic (2).png'
import titlePicUrl from '/titlepic.png'

// 图片资源
const defaultUserPic = defaultUserPicUrl
const titlePic = titlePicUrl
import { http } from '@/utils/http'
import { API } from '@/utils/api'
import { useToast } from 'vue-toastification'
import { useFocusStore } from '@/store/FocusStore'
import { useAuthStore } from '@/store/AuthStore'
import { useUIStore } from '@/store/UIStore'
import { useSettingsStore } from '@/store/SettingsStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const focusStore = useFocusStore()
const authStore = useAuthStore()
const uiStore = useUIStore()
const settingsStore = useSettingsStore()
const { isFocusing } = storeToRefs(focusStore)

// 用户菜单状态
const showUserMenu = ref(false)

// 窗口状态
const isMaximized = ref(false)

// 关闭确认对话框状态
const showCloseConfirmation = ref(false)

const rememberCloseChoice = ref(false)

// 新岛屿弹窗状态
const showNewIsleDialog = ref(false)
const newIsleImageUrl = ref('')

const toggleFocus = async () => {
  if (isFocusing.value) {
    // 如果已经专注了至少5分钟，记录专注时长
    // if (focusStore.focusElapsedMinutes >= 5) {
    if (focusStore.focusElapsedMinutes >= 0.1) {
      try {
        const completedMinutes = focusStore.focusElapsedMinutes
        const response = await API.focus.recordFocus(completedMinutes)

        // 检查是否获得新岛屿
        // API返回的实际结构是嵌套的: { data: { code: 200, data: "岛屿URL", message: "操作成功" } }
        const apiResponse = response as any
        const islandUrl = apiResponse.data?.data

        if (islandUrl && typeof islandUrl === 'string' && islandUrl.trim() !== '') {
          newIsleImageUrl.value = islandUrl
          showNewIsleDialog.value = true
        }

        toast.success(`🎉 专注完成！用时 ${completedMinutes} 分钟`)
      } catch (error) {
        console.error('记录专注时长失败:', error)
        toast.error('记录专注失败')
      }
    } else {
      // 少于5分钟，只是提示停止
      toast.info('专注已停止（少于5分钟不记录）')
    }

    // 停止专注
    focusStore.stopFocus()
  } else {
    focusStore.startFocus()
  }
}

// 用户菜单交互
const toggleUserMenu = (event: Event) => {
  event.stopPropagation()
  showUserMenu.value = !showUserMenu.value
}

// 跳转到账户设置
const goToAccountSettings = () => {
  // 立即关闭菜单
  showUserMenu.value = false
  
  // 设置当前分类为账户与云盘
  settingsStore.setCurrentCategoryId('account')
  
  // 如果已经在设置页面，直接返回，不需要跳转
  if (route.name === 'settings') {
    // 强制确保菜单关闭
    nextTick(() => {
      showUserMenu.value = false
    })
    return
  }
  
  // 使用 nextTick 确保菜单关闭后再跳转
  nextTick(() => {
    router.push({ name: 'settings' })
  })
}

// 退出登录
const handleLogout = async () => {
  try {
    // 先关闭菜单
    showUserMenu.value = false
    
    // 调用认证存储的注销方法
    await authStore.logout()
    
    // 使用 nextTick 确保菜单关闭后再跳转
    nextTick(() => {
      router.push({ name: 'login' })
    })
  } catch (error) {
    console.error('注销失败:', error)
  }
}

// 处理全局点击事件，关闭菜单
const handleClickOutside = () => {
  showUserMenu.value = false
}

// 组件挂载时添加全局点击监听
onMounted(async () => {
  document.addEventListener('click', handleClickOutside)

  // 每次组件挂载都刷新用户信息（获取最新的头像、用户名等）
  if (authStore.isAuthenticated) {
    await authStore.getCurrentUser()
  }

  // 检查electronAPI状态
  console.log('=== ElectronAPI 状态检查 ===')
  console.log('window.electronAPI:', window?.electronAPI)
  console.log('minimizeWindow 方法:', typeof window?.electronAPI?.minimizeWindow)
  console.log('maximizeWindow 方法:', typeof window?.electronAPI?.maximizeWindow)
  console.log('closeWindow 方法:', typeof window?.electronAPI?.closeWindow)
  console.log('========================')

  if (window?.electronAPI) {
    window.electronAPI.onMaximize(() => {
      isMaximized.value = true
    })
    window.electronAPI.onUnmaximize(() => {
      isMaximized.value = false
    })
    // 监听关闭确认对话框请求
    window.electronAPI.onShowCloseConfirmation(() => {
      // 检查是否记住了选择
      const remember = localStorage.getItem('closeRemember') === '1'
      const action = localStorage.getItem('closeAction')
      if (remember && action) {
        if (action === 'minimize') {
          handleMinimizeToTray()
        } else if (action === 'quit') {
          handleQuitApp()
        }
      } else {
        showCloseConfirmation.value = true
      }
    })
  } else {
    console.warn('electronAPI 不可用，窗口控制按钮可能无法正常工作')
  }

  // 临时：页面加载后自动展示新岛屿弹窗预览
  // showNewIsleDialog.value = true
})

// 监听路由变化，自动关闭菜单
watch(route, () => {
  showUserMenu.value = false
})

// 组件卸载时移除监听器
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 窗口控制函数
const minimizeWindow = () => {
  try {
    if (window?.electronAPI?.minimizeWindow) {
      window.electronAPI.minimizeWindow()
      console.log('窗口最小化成功')
    } else {
      console.error('electronAPI.minimizeWindow 不可用')
      // 在非Electron环境下的后备方案
      if (window.parent !== window) {
        window.parent.postMessage({ type: 'minimize-window' }, '*')
      }
    }
  } catch (error) {
    console.error('最小化窗口时出错:', error)
  }
}

const maximizeWindow = () => {
  try {
    if (window?.electronAPI?.maximizeWindow) {
      window.electronAPI.maximizeWindow()
      console.log('窗口最大化/还原成功')
    } else {
      console.error('electronAPI.maximizeWindow 不可用')
      // 在非Electron环境下的后备方案
      if (window.parent !== window) {
        window.parent.postMessage({ type: 'maximize-window' }, '*')
      }
    }
  } catch (error) {
    console.error('最大化窗口时出错:', error)
  }
}

const closeWindow = () => {
  try {
    if (window?.electronAPI?.closeWindow) {
      window.electronAPI.closeWindow()
      console.log('窗口关闭请求发送成功')
    } else {
      console.error('electronAPI.closeWindow 不可用')
      // 在非Electron环境下的后备方案
      if (window.parent !== window) {
        window.parent.postMessage({ type: 'close-window' }, '*')
      } else {
        // 最后的后备方案
        window.close()
      }
    }
  } catch (error) {
    console.error('关闭窗口时出错:', error)
  }
}

// 处理最小化到托盘
const handleMinimizeToTray = () => {
  try {
    if (window?.electronAPI?.minimizeToTray) {
      window.electronAPI.minimizeToTray()
      showCloseConfirmation.value = false
      console.log('最小化到托盘成功')
    } else {
      console.error('electronAPI.minimizeToTray 不可用')
    }
  } catch (error) {
    console.error('最小化到托盘时出错:', error)
  }
}

// 处理退出应用
const handleQuitApp = () => {
  try {
    if (window?.electronAPI?.quitApp) {
      window.electronAPI.quitApp()
      console.log('退出应用请求发送成功')
    } else {
      console.error('electronAPI.quitApp 不可用')
      // 后备方案
      if (typeof window !== 'undefined') {
        window.close()
      }
    }
  } catch (error) {
    console.error('退出应用时出错:', error)
  }
}

function handleMinimizeToTrayWithRemember() {
  if (rememberCloseChoice.value) {
    localStorage.setItem('closeAction', 'minimize')
    localStorage.setItem('closeRemember', '1')
  } else {
    localStorage.removeItem('closeAction')
    localStorage.removeItem('closeRemember')
  }
  handleMinimizeToTray()
}

function handleQuitAppWithRemember() {
  if (rememberCloseChoice.value) {
    localStorage.setItem('closeAction', 'quit')
    localStorage.setItem('closeRemember', '1')
  } else {
    localStorage.removeItem('closeAction')
    localStorage.removeItem('closeRemember')
  }
  handleQuitApp()
}
</script>

<style scoped>
.topbar-control {
  @apply p-2 rounded-lg text-morandi-600 hover:bg-morandi-100 transition-colors;
}

.draggable-area {
  -webkit-app-region: drag;
}

.draggable-area button,
.draggable-area .user-menu {
  -webkit-app-region: no-drag;
}
</style> 