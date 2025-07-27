import { createPinia } from 'pinia'

// 全局状态管理
import { useUIStore } from './UIStore'
import { useContextMenuStore } from './ContextMenuStore'
import { useVditorStore } from './VditorStore'
import { useShareStore } from './ShareStore'
import { useIslandStore } from './IslandStore'
import { useSettingsStore } from './SettingsStore'

// 主要业务Store
import { useAuthStore } from './AuthStore'
import { useDriveStore } from './DriveStore'
import { useMusicStore } from './MusicStore'
import { useDocsStore } from './DocsStore'
import { useTransferStore } from './TransferStore'
import { useFocusStore } from './FocusStore'

const pinia = createPinia()

export {
  pinia,
  // 全局Store
  useUIStore,
  useContextMenuStore,
  useVditorStore,
  useShareStore,
  useIslandStore,
  useSettingsStore,

  // 主要业务Store
  useAuthStore,
  useDriveStore,
  useMusicStore,
  useDocsStore,
  useTransferStore,
  useFocusStore
} 