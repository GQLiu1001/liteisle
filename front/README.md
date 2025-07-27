# Liteisle Desktop 前端

基于 Vue 3 + Vite + Electron 的桌面应用前端，支持自定义后端服务器地址。

## 🚀 快速开始

### 1. 安装依赖
```bash
npm install
```

### 2. 配置后端服务器地址
编辑 `.env.electron` 文件，修改API地址：
```bash
# API配置 - 请修改为你的后端服务器地址
VITE_API_BASE_URL=http://你的服务器IP:端口
VITE_WS_BASE_URL=ws://你的服务器IP:端口
```

### 3. 构建前端
```bash
npm run build
```

### 4. 启动应用
```bash
npm run electron:dev
```

## 📁 项目结构

```
├── src/                    # Vue源代码
├── public/                 # 静态资源
├── main.cjs               # Electron主进程
├── preload.js             # 预加载脚本
├── package.json           # 项目配置
├── .env.electron          # Electron环境配置
└── .env.production        # 生产环境配置
```

## ⚙️ 可用命令

```bash
# 开发模式（Web版本）
npm run dev

# 构建前端
npm run build

# Electron开发模式
npm run electron:dev

# 打包Electron应用
npm run electron:pack
```

## 🔧 环境配置

### 主要配置项
- `VITE_API_BASE_URL` - 后端API地址
- `VITE_WS_BASE_URL` - WebSocket地址
- `VITE_UPLOAD_MAX_SIZE` - 最大上传大小(MB)
- `VITE_UPLOAD_ALLOWED_TYPES` - 允许的文件类型

### 环境文件说明
- `.env.electron` - Electron桌面应用配置
- `.env.production` - 生产环境配置
- `.env.development` - 开发环境配置（可选）
## 📋 系统要求

- Node.js 16+
- npm 或 yarn
- 后端服务器运行在配置的地址

## 🎯 功能特性

- 📁 文件管理和云盘功能
- � 音乐播放和管理
- 📝 文档编辑和预览
- 🔄 文件传输和分享
- ⏰ 专注模式和番茄钟
- 🖼️ PicGo图片上传集成
- 🌐 WebSocket实时通信

## 🛠️ 开发说明

### 修改后端地址
1. 编辑对应的环境文件（`.env.electron` 或 `.env.production`）
2. 修改 `VITE_API_BASE_URL` 和 `VITE_WS_BASE_URL`
3. 重新构建：`npm run build`

### 调试模式
开发环境下可以启用调试模式：
```bash
VITE_DEBUG=true npm run dev
```

## 📞 技术支持

如遇问题，请检查：
1. 后端服务器是否正常运行
2. API地址配置是否正确
3. 网络连接是否正常
4. 浏览器控制台是否有错误信息