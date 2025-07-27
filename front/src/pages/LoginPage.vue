<template>
  <div class="min-h-screen bg-liteisle-bg flex items-center justify-center select-none">
    <!-- 可拖拽的标题栏区域 - 登录页面 -->
    <div class="fixed top-0 left-0 w-full h-16 flex items-center justify-end px-4 z-50 draggable-area">
      <!-- 窗口控制按钮 -->
      <div class="flex items-center gap-2">
        <button @click="minimizeWindow" class="w-8 h-8 rounded-md flex items-center justify-center transition-all duration-200 hover:bg-black/10 cursor-pointer">
          <Minus :size="16" />
        </button>
        <button @click="maximizeWindow" class="w-8 h-8 rounded-md flex items-center justify-center transition-all duration-200 hover:bg-black/10 cursor-pointer">
          <Square :size="14" />
        </button>
        <button @click="closeWindow" class="w-8 h-8 rounded-md flex items-center justify-center transition-all duration-200 hover:bg-red-100 hover:text-red-600 cursor-pointer">
          <X :size="16" />
        </button>
      </div>
    </div>
    
    <div class="w-full max-w-md">
      <!-- Logo区域 -->
      <div class="text-center mb-12">
        <!-- 组合Logo：应用图标 + 艺术字 -->
        <div class="flex items-center justify-center gap-4 mb-6">
          <img :src="logoPic" alt="Liteisle Logo" class="h-16 w-16 object-contain drop-shadow-lg" />
          <img :src="titlePic" alt="Liteisle" class="h-20 object-contain" />
        </div>
        <h1 class="text-2xl font-light text-morandi-900 mb-2">欢迎来到 Liteisle</h1>
        <p class="text-morandi-600">专注学习，净化心灵</p>
      </div>

      <!-- 表单容器 -->
      <div class="card p-8">
        <!-- 标签导航 -->
        <div class="flex justify-center mb-8">
          <div class="bg-morandi-100 p-1 rounded-xl inline-flex">
            <button
              @click="activeTab = 'login'"
              :class="[
                'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-200',
                activeTab === 'login' 
                  ? 'bg-white text-teal-600 shadow-sm' 
                  : 'text-morandi-600 hover:text-morandi-800'
              ]"
            >
              登录
            </button>
            <button
              @click="activeTab = 'register'"
              :class="[
                'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-200',
                activeTab === 'register' 
                  ? 'bg-white text-teal-600 shadow-sm' 
                  : 'text-morandi-600 hover:text-morandi-800'
              ]"
            >
              注册
            </button>
            <button
              @click="activeTab = 'forgot'"
              :class="[
                'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-200',
                activeTab === 'forgot' 
                  ? 'bg-white text-teal-600 shadow-sm' 
                  : 'text-morandi-600 hover:text-morandi-800'
              ]"
            >
              忘记密码
            </button>
          </div>
        </div>

        <!-- 登录表单 -->
        <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="space-y-3">
          <!-- 用户名输入框 -->
          <div>
            <label for="login-username" class="block text-sm font-medium text-morandi-700 mb-2">用户名</label>
            <input
              id="login-username"
              v-model="loginForm.username"
              type="text"
              required
              class="w-full px-4 py-3 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入用户名"
              style="user-select: text !important;"
            />
          </div>

          <!-- 密码输入框 -->
          <div>
            <label for="login-password" class="block text-sm font-medium text-morandi-700 mb-2">密码</label>
            <input
              id="login-password"
              v-model="loginForm.password"
              type="password"
              required
              class="w-full px-4 py-3 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入密码"
              style="user-select: text !important;"
            />
          </div>

          <!-- 登录按钮 -->
          <button
            type="submit"
            :disabled="authStore.isLoading"
            class="w-full bg-teal-500 hover:bg-teal-600 disabled:bg-morandi-300 text-white font-medium py-3 px-4 rounded-xl transition-colors duration-200 flex items-center justify-center"
          >
            <Loader2 v-if="authStore.isLoading" :size="20" class="animate-spin mr-2" />
            {{ authStore.isLoading ? '登录中...' : '登录' }}
          </button>

          <!-- 底部提示 -->
          <div class="text-center">
            <p class="text-sm text-morandi-600 select-text">
              演示账号：admin / 123456
            </p>
          </div>
        </form>

        <!-- 注册表单 -->
        <form v-if="activeTab === 'register'" @submit.prevent="handleRegister" class="space-y-2">
          <!-- 用户名输入框 -->
          <div>
            <label for="register-username" class="block text-sm font-medium text-morandi-700 mb-2">用户名</label>
            <input
              id="register-username"
              v-model="registerForm.username"
              type="text"
              required
              class="w-full px-4 py-3 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入用户名"
              style="user-select: text !important;"
            />
          </div>

          <!-- 邮箱输入框 -->
          <div>
            <label for="register-email" class="block text-sm font-medium text-morandi-700 mb-2">邮箱</label>
            <input
              id="register-email"
              v-model="registerForm.email"
              type="email"
              required
              class="w-full px-4 py-3 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入邮箱地址"
              style="user-select: text !important;"
            />
          </div>

          <!-- 密码输入框 -->
          <div>
            <label for="register-password" class="block text-sm font-medium text-morandi-700 mb-2">密码</label>
            <input
              id="register-password"
              v-model="registerForm.password"
              type="password"
              required
              class="w-full px-4 py-3 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入密码"
              style="user-select: text !important;"
            />
          </div>

          <!-- 确认密码输入框 -->
          <div>
            <label for="register-confirm-password" class="block text-sm font-medium text-morandi-700 mb-2">确认密码</label>
            <input
              id="register-confirm-password"
              v-model="registerForm.confirmPassword"
              type="password"
              required
              class="w-full px-4 py-3 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请再次输入密码"
              style="user-select: text !important;"
            />
          </div>

          <!-- 邮箱验证码 -->
          <div>
            <label for="register-code" class="block text-sm font-medium text-morandi-700 mb-2">邮箱验证码</label>
            <div class="flex gap-3">
              <input
                id="register-code"
                v-model="registerForm.verificationCode"
                type="text"
                required
                class="flex-1 px-4 py-3 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
                placeholder="请输入验证码"
                style="user-select: text !important;"
              />
              <button
                type="button"
                @click="sendRegisterCode"
                :disabled="isCodeLoading || registerCodeCountdown > 0 || !registerForm.email"
                class="px-4 py-3 bg-morandi-500 hover:bg-morandi-600 disabled:bg-morandi-300 text-white text-sm rounded-xl transition-colors duration-200"
              >
                {{ registerCodeCountdown > 0 ? `${registerCodeCountdown}s` : (isCodeLoading ? '发送中...' : '获取验证码') }}
              </button>
            </div>
          </div>

          <!-- 注册按钮 -->
          <button
            type="submit"
            :disabled="authStore.isLoading"
            class="w-full bg-teal-500 hover:bg-teal-600 disabled:bg-morandi-300 text-white font-medium py-3 px-4 rounded-xl transition-colors duration-200 flex items-center justify-center"
          >
            <Loader2 v-if="authStore.isLoading" :size="20" class="animate-spin mr-2" />
            {{ authStore.isLoading ? '注册中...' : '注册账号' }}
          </button>
        </form>

        <!-- 忘记密码表单 -->
        <form v-if="activeTab === 'forgot'" @submit.prevent="handleForgotPassword" class="space-y-2">
          <!-- 用户名输入框 -->
          <div>
            <label for="forgot-username" class="block text-sm font-medium text-morandi-700 mb-1">用户名</label>
            <input
              id="forgot-username"
              v-model="forgotForm.username"
              type="text"
              required
              class="w-full px-4 py-2.5 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入用户名"
              style="user-select: text !important;"
            />
          </div>

          <!-- 邮箱输入框 -->
          <div>
            <label for="forgot-email" class="block text-sm font-medium text-morandi-700 mb-1">邮箱</label>
            <input
              id="forgot-email"
              v-model="forgotForm.email"
              type="email"
              required
              class="w-full px-4 py-2.5 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入邮箱地址"
              style="user-select: text !important;"
            />
          </div>

          <!-- 邮箱验证码 -->
          <div>
            <label for="forgot-code" class="block text-sm font-medium text-morandi-700 mb-1">邮箱验证码</label>
            <div class="flex gap-2">
              <input
                id="forgot-code"
                v-model="forgotForm.verificationCode"
                type="text"
                required
                class="flex-1 px-4 py-2.5 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
                placeholder="请输入验证码"
                style="user-select: text !important;"
              />
              <button
                type="button"
                @click="sendForgotCode"
                :disabled="isCodeLoading || forgotCodeCountdown > 0 || !forgotForm.email"
                class="px-3 py-2.5 bg-morandi-500 hover:bg-morandi-600 disabled:bg-morandi-300 text-white text-xs rounded-xl transition-colors duration-200 whitespace-nowrap"
              >
                {{ forgotCodeCountdown > 0 ? `${forgotCodeCountdown}s` : (isCodeLoading ? '发送中...' : '获取验证码') }}
              </button>
            </div>
          </div>

          <!-- 新密码输入框 -->
          <div>
            <label for="forgot-new-password" class="block text-sm font-medium text-morandi-700 mb-1">新密码</label>
            <input
              id="forgot-new-password"
              v-model="forgotForm.newPassword"
              type="password"
              required
              class="w-full px-4 py-2.5 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请输入新密码"
              style="user-select: text !important;"
            />
          </div>

          <!-- 确认密码输入框 -->
          <div>
            <label for="forgot-confirm-password" class="block text-sm font-medium text-morandi-700 mb-1">确认密码</label>
            <input
              id="forgot-confirm-password"
              v-model="forgotForm.confirmPassword"
              type="password"
              required
              class="w-full px-4 py-2.5 border border-morandi-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-teal-400 focus:border-transparent transition-all select-text"
              placeholder="请再次输入新密码"
              style="user-select: text !important;"
            />
          </div>

          <!-- 重置密码按钮 -->
          <button
            type="submit"
            :disabled="authStore.isLoading"
            class="w-full bg-teal-500 hover:bg-teal-600 disabled:bg-morandi-300 text-white font-medium py-2.5 px-4 rounded-xl transition-colors duration-200 flex items-center justify-center"
          >
            <Loader2 v-if="authStore.isLoading" :size="18" class="animate-spin mr-2" />
            {{ authStore.isLoading ? '重置中...' : '重置密码' }}
          </button>

        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Loader2, Minus, Square, X } from 'lucide-vue-next'
import { useAuthStore } from '@/store/AuthStore'
import { useToast } from 'vue-toastification'

// 导入图片资源
import logoPicUrl from '/logopic.png'
import titlePicUrl from '/titlepic.png'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

// 图片资源
const logoPic = logoPicUrl
const titlePic = titlePicUrl

// 活跃标签页
const activeTab = ref<'login' | 'register' | 'forgot'>('login')

// 表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

const forgotForm = reactive({
  username: '',
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载状态
const isCodeLoading = ref(false)

// 验证码倒计时
const registerCodeCountdown = ref(0)
const forgotCodeCountdown = ref(0)

// 窗口控制函数
const minimizeWindow = () => {
  try {
    if (typeof window !== 'undefined' && window?.electronAPI?.minimizeWindow) {
      window.electronAPI.minimizeWindow()
      console.log('窗口最小化成功')
    } else {
      console.error('electronAPI.minimizeWindow 不可用')
    }
  } catch (error) {
    console.error('最小化窗口时出错:', error)
  }
}

const maximizeWindow = () => {
  try {
    if (typeof window !== 'undefined' && window?.electronAPI?.maximizeWindow) {
      window.electronAPI.maximizeWindow()
      console.log('窗口最大化/还原成功')
    } else {
      console.error('electronAPI.maximizeWindow 不可用')
    }
  } catch (error) {
    console.error('最大化窗口时出错:', error)
  }
}

const closeWindow = () => {
  try {
    // 在登录页面直接退出应用
    if (typeof window !== 'undefined' && window?.electronAPI?.quitApp) {
      window.electronAPI.quitApp()
      console.log('应用退出成功')
    } else if (typeof window !== 'undefined' && window?.electronAPI?.closeWindow) {
      window.electronAPI.closeWindow()
      console.log('窗口关闭成功')
    } else {
      console.error('electronAPI 不可用')
      // 后备方案
      if (typeof window !== 'undefined') {
        window.close()
      }
    }
  } catch (error) {
    console.error('关闭窗口时出错:', error)
  }
}

// 处理登录
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    toast.error('请填写用户名和密码')
    return
  }

  const success = await authStore.login({
    username: loginForm.username,
    password: loginForm.password
  })

  if (success) {
    // 登录成功，跳转到首页
    router.push('/home')
  }
}

// 处理注册
const handleRegister = async () => {
  if (!registerForm.username || !registerForm.email || 
      !registerForm.password || !registerForm.confirmPassword || 
      !registerForm.verificationCode) {
    toast.error('请填写所有必填项')
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    toast.error('两次输入的密码不一致')
    return
  }

  const success = await authStore.register({
    username: registerForm.username,
    email: registerForm.email,
    password: registerForm.password,
    vcode: registerForm.verificationCode
  })

  if (success) {
    // 注册成功，跳转到首页
    router.push('/home')
  }
}

// 处理忘记密码
const handleForgotPassword = async () => {
  // 验证密码确认
  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    toast.error('两次输入的密码不一致，请重新输入')
    return
  }

  if (forgotForm.newPassword.length < 6) {
    toast.error('密码长度不能少于6位')
    return
  }

  if (!forgotForm.username || !forgotForm.email || !forgotForm.verificationCode) {
    toast.error('请填写完整信息')
    return
  }

  try {
    const success = await authStore.forgotPassword({
      username: forgotForm.username,
      email: forgotForm.email,
      vcode: forgotForm.verificationCode,
      new_password: forgotForm.newPassword,
      confirm_password: forgotForm.confirmPassword
    })

    if (success) {
      toast.success(`密码重置成功！请使用新密码登录`)

      // 重置成功后切换到登录页面
      activeTab.value = 'login'
      loginForm.username = forgotForm.username

      // 清空忘记密码表单
      Object.assign(forgotForm, {
        username: '',
        email: '',
        verificationCode: '',
        newPassword: '',
        confirmPassword: ''
      })
    }
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}

// 发送注册验证码
const sendRegisterCode = async () => {
  if (!registerForm.email) {
    toast.error('请先填写邮箱地址')
    return
  }

  try {
    isCodeLoading.value = true
    await authStore.sendVerificationCode(registerForm.email)
    
    toast.success('验证码已发送到您的邮箱')
    startCodeCountdown()
  } catch (error: any) {
    console.error('发送验证码错误:', error)
    if (error.code === 'ECONNREFUSED' || error.message?.includes('Network Error')) {
      toast.error('无法连接到后端服务器，请检查服务器是否启动 (localhost:8002)')
    } else {
      toast.error(error.response?.data?.message || '发送验证码失败，请重试')
    }
  } finally {
    isCodeLoading.value = false
  }
}

// 发送忘记密码验证码
const sendForgotCode = async () => {
  if (!forgotForm.email) {
    toast.error('请先填写邮箱地址')
    return
  }

  try {
    isCodeLoading.value = true
    await authStore.sendVerificationCode(forgotForm.email)
    
    toast.success('验证码已发送到您的邮箱')
    startCodeCountdown()
  } catch (error: any) {
    console.error('发送验证码错误:', error)
    if (error.code === 'ECONNREFUSED' || error.message?.includes('Network Error')) {
      toast.error('无法连接到后端服务器，请检查服务器是否启动 (localhost:8002)')
    } else {
      toast.error(error.response?.data?.message || '发送验证码失败，请重试')
    }
  } finally {
    isCodeLoading.value = false
  }
}

// 开始验证码倒计时
const startCodeCountdown = () => {
  registerCodeCountdown.value = 60
  forgotCodeCountdown.value = 60
  
  const timer = setInterval(() => {
    if (registerCodeCountdown.value > 0) {
      registerCodeCountdown.value--
    }
    if (forgotCodeCountdown.value > 0) {
      forgotCodeCountdown.value--
    }
    
    if (registerCodeCountdown.value <= 0 && forgotCodeCountdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}
</script>

<style scoped>
.draggable-area {
  -webkit-app-region: drag;
}

.draggable-area button {
  -webkit-app-region: no-drag;
}
</style>