export interface ElectronAPI {
  // 窗口控制
  minimizeWindow: () => void;
  maximizeWindow: () => void;
  closeWindow: () => void;
  
  // 托盘和退出相关
  minimizeToTray: () => void;
  quitApp: () => void;
  
  // 文件系统操作
  selectDirectory: () => Promise<Electron.OpenDialogReturnValue>;
  saveFileToDirectory: (data: { fileName: string; arrayBuffer: ArrayBuffer; downloadDirectory: string }) => Promise<{ success: boolean; filePath?: string; error?: string }>;

  // 窗口状态监听
  onMaximize: (callback: () => void) => void;
  onUnmaximize: (callback: () => void) => void;
  onShowCloseConfirmation: (callback: () => void) => void;
}

// 声明全局window对象上的electronAPI
declare global {
  interface Window {
    electronAPI?: ElectronAPI;
  }
}