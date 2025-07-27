import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index'
import './style.css'
import Toast, { type PluginOptions, POSITION } from "vue-toastification";
import "vue-toastification/dist/index.css";

// 在开发环境下加载调试工具
if (import.meta.env.DEV) {
  import('./utils/debug-folders')
  import('./utils/websocket-test')

  // 添加全局错误监听器来捕获拖拽相关错误
  window.addEventListener('error', (e) => {
    if (e.message.includes('dragEvent is not defined')) {
      console.error('🐛 拖拽错误详情:', {
        message: e.message,
        filename: e.filename,
        lineno: e.lineno,
        colno: e.colno,
        error: e.error
      })
      console.trace('调用栈')
    }
  })
}

// 创建主应用
const app = createApp(App)
const pinia = createPinia()

const options: PluginOptions = {
    transition: "Vue-Toastification__bounce",
    maxToasts: 5,
    newestOnTop: true,
    position: POSITION.TOP_CENTER,
    timeout: 3000,
    closeOnClick: true,
    pauseOnFocusLoss: true,
    pauseOnHover: true,
    draggable: true,
    draggablePercent: 0.6,
    showCloseButtonOnHover: false,
    hideProgressBar: true,
    closeButton: "button",
    icon: true,
    rtl: false,
    containerClassName: "my-toast-container",
};

app.use(pinia)
app.use(router)
app.use(Toast, options);

app.mount('#app') 