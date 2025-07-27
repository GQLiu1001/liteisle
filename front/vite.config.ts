import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ command, mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')
  const isProduction = mode === 'electron' || mode === 'production'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    server: {
      port: 5173
      // Electron应用直接请求远程API，不需要代理
    },
    base: './',
    define: {
      // 将环境变量注入到应用中
      __APP_ENV__: JSON.stringify(mode)
    },
    build: {
      // 生产环境优化
      minify: isProduction ? 'esbuild' : false,
      // 使用 esbuild 进行压缩，它内置在 Vite 中
      ...(isProduction && {
        esbuild: {
          drop: ['console', 'debugger']
        }
      })
    }
  }
})