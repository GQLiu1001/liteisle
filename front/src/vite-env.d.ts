/// <reference types="vite/client" />

declare module 'vue-activity-calendar' {
  import { DefineComponent } from 'vue'
  
  interface ActivityCalendarProps {
    data?: Array<{ date: string; count: number }>
    width?: number
    height?: number
    cellLength?: number
    cellInterval?: number
    cellBorderRadius?: number
    colors?: string[]
    levelMapper?: (count: number) => number
    fontSize?: number
    showLevelFlag?: boolean
    levelFlagText?: string[]
    showHeader?: boolean
    showWeekDayFlag?: boolean
    header?: string[]
    backgroundColor?: string
  }
  
  export const ActivityCalendar: DefineComponent<ActivityCalendarProps>
  export default ActivityCalendar
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '@/*' {
  const value: any
  export default value
}

declare module '*.ts' {
  const value: any
  export default value
}

declare module 'pinia' {
  const value: any
  export default value
  export const createPinia: any
  export const storeToRefs: any
}

declare module 'fs-extra' {
  const value: any
  export default value
}

declare module 'vue-router' {
  const value: any
  export default value
  export const createRouter: any
  export const createWebHistory: any
  export const createWebHashHistory: any
  export const useRoute: any
  export const useRouter: any
  export type RouteRecordRaw = any
}

declare module 'lucide-vue-next' {
  export const CircleEqual: any
  export const CloudLightning: any
  const value: any
  export default value
  export const X: any
  export const Home: any
  export const HardDrive: any
  export const Music: any
  export const FileText: any
  export const Settings: any
  export const PenTool: any
  export const User: any
  export const Minus: any
  export const Square: any
  export const Play: any
  export const Pause: any
  export const SkipBack: any
  export const SkipForward: any
  export const Volume2: any
  export const VolumeX: any
  export const Repeat: any
  export const List: any
  export const ChevronUp: any
  export const ChevronLeft: any
  export const ChevronRight: any
  export const Clock: any
  export const Loader2: any
  export const LogOut: any
  export const Trash2: any
  export const Shredder: any
  export const RefreshCcw: any
  export const FolderSync: any
  export const ListOrdered: any
  export const Logs: any
  export const Grid2x2: any
  export const FolderClosed: any
  export const FolderLock: any
  export const Upload: any
  export const Shuffle: any
  export const Repeat1: any
  export const DiscAlbum: any
  export const BookImage: any
  export const Cloud: any
  export const ArrowRightLeft: any
  export const Download: any
  export const Share2: any
  export const UploadCloud: any
  export const DownloadCloud: any
  export const ArrowUp: any
  export const ArrowDown: any
  export const ArrowLeft: any
  export const ArrowRight: any
  export const ArrowUpLeft: any
  export const ArrowUpRight: any
  export const ArrowDownLeft: any
  export const ArrowDownRight: any
  export const File: any
  export const Film: any
  export const Link: any
  export const FolderOpen: any
  export const FolderClosed: any
  export const Folder: any
  export const FolderPlus: any
  export const FolderMinus: any
  export const FolderX: any
  export const FolderCheck: any
  export const Maximize: any
  export const Minimize: any
  export const ChevronDown: any
  export const ChevronUp: any
  export const ChevronLeft: any
  export const ChevronRight: any
  export const ChevronDown: any
  export const ChevronUp: any
  export const ChevronLeft: any
  export const Plus: any
  export const Minus: any
  export const X: any
  export const Check: any
  export const ArrowLeft: any
  export const ArrowRight: any
  export const ArrowUp: any
  export const ArrowDown: any
  export const RotateCw: any
  export const Star: any
  export const Clock: any
  export const Calendar: any
  export const CalendarCheck: any
  export const CalendarX: any
  export const CalendarCheck: any
  export const CloudLightning: any
  export const Share2: any
  export const Share: any
  export const Lock: any
  
} 