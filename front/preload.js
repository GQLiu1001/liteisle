const { contextBridge, ipcRenderer } = require('electron')

console.log('=== Preload 脚本开始执行 ===')

// 暴露 Electron API 给渲染进程
contextBridge.exposeInMainWorld('electronAPI', {
  // 窗口控制
  minimizeWindow: () => {
    console.log('preload: 发送窗口最小化请求')
    ipcRenderer.send('window-minimize')
  },
  maximizeWindow: () => {
    console.log('preload: 发送窗口最大化请求')
    ipcRenderer.send('window-maximize')
  },
  closeWindow: () => {
    console.log('preload: 发送窗口关闭请求')
    ipcRenderer.send('window-close')
  },
  
  // 托盘和退出相关
  minimizeToTray: () => {
    console.log('preload: 发送最小化到托盘请求')
    ipcRenderer.send('minimize-to-tray')
  },
  quitApp: () => {
    console.log('preload: 发送退出应用请求')
    ipcRenderer.send('quit-app')
  },
  
  // 监听窗口状态
  onMaximize: (callback) => ipcRenderer.on('window-maximized', callback),
  onUnmaximize: (callback) => ipcRenderer.on('window-unmaximized', callback),
  onShowCloseConfirmation: (callback) => ipcRenderer.on('show-close-confirmation', callback),
  
  // 文件系统操作
  selectDirectory: () => ipcRenderer.invoke('select-directory'),
  saveFileToDirectory: (data) => ipcRenderer.invoke('save-file-to-directory', data),
  
  // 系统信息
  getUsername: () => ipcRenderer.invoke('get-username')
})

console.log('=== Preload 脚本执行完成，electronAPI 已暴露 ===')
