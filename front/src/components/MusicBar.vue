<template>
  <!-- 浮动音乐栏 - 与内容区域对齐，响应式适配 -->
  <div 
    :class="[
      'fixed bottom-0 right-0 z-20 px-4 lg:px-6 select-none',
      uiStore.isSidebarVisible ? 'left-[150px]' : 'left-0'
    ]"
  >
    <!-- 文档页面的折叠箭头按钮 -->
    <div 
      v-if="isDocumentPage && !showFullMusicBar"
      class="flex justify-center"
    >
      <button
        @click="toggleMusicBar"
        class="bg-transparent backdrop-blur-sm border  hover:bg-gray-600/90 transition-all duration-300 rounded-t-xl px-4 py-1 shadow-md hover:shadow-lg group"
        title="展开音乐栏"
      >
        <div class="flex items-center gap-1.5">
          <ChevronUp :size="12"class="text-black-400/80 group-hover:text-gray-300 transition-colors"/>
        </div>
      </button>
    </div>

    <!-- 完整音乐栏 - 条件显示 -->
    <div 
      v-if="!isDocumentPage || showFullMusicBar"
      class="bg-liteisle-sidebar rounded-t-[40px] shadow-xl border border-morandi-300 border-b-0 px-4 lg:px-8 py-3 lg:py-4 backdrop-blur-sm">
      <!-- 文档页面模式的收起按钮 -->
      <div v-if="isDocumentPage && showFullMusicBar" class="flex justify-center mb-2">
        <button
          @click="toggleMusicBar"
          class="text-morandi-600 hover:text-morandi-800 transition-colors rounded-t-xl px-4 py-1 p-1 rounded-full hover:bg-morandi-100"
          title="收起音乐栏"
        >
          <ChevronDown :size="12" />
        </button>
      </div>
      <!-- 移动端布局 -->
      <div class="lg:hidden">
        <div class="flex items-center justify-between">
          <!-- 左侧 - 音乐信息（简化版） -->
          <div class="flex items-center gap-3 min-w-0 flex-1">
            <div 
              @click="goToMusicPage"
              class="w-10 h-10 bg-gradient-to-r from-blue-400 to-teal-500 rounded-xl flex items-center justify-center cursor-pointer hover:scale-105 transition-transform"
              title="进入音乐页面"
            >
              <Music :size="20" class="text-white" />
            </div>
            <div class="min-w-0 flex-1">
              <p class="font-semibold text-morandi-900 truncate text-sm">{{ currentTrackInfo.name }}</p>
              <p class="text-xs text-morandi-700">{{ musicStore.formatTime(musicStore.currentTime) }} / {{ musicStore.formatTime(musicStore.duration) }}</p>
            </div>
          </div>

          <!-- 右侧 - 主要播放控制（简化版） -->
          <div class="flex items-center gap-2">
            <button 
              @click="musicStore.previousTrack" 
              class="w-8 h-8 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
              title="上一首"
            >
              <SkipBack :size="16" />
            </button>
            
            <button 
              @click="musicStore.togglePlay" 
              class="w-10 h-10 rounded-full bg-white text-morandi-800 flex items-center justify-center"
              :title="musicStore.playState === musicStore.PlayState.PLAYING ? '暂停' : '播放'"
            >
              <Play v-if="musicStore.playState === musicStore.PlayState.STOPPED || musicStore.playState === musicStore.PlayState.PAUSED" :size="18" />
              <Pause v-else :size="18" />
            </button>
            
            <button 
              @click="musicStore.nextTrack" 
              class="w-8 h-8 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
              title="下一首"
            >
              <SkipForward :size="16" />
            </button>

            <!-- 音量按钮（移动端） -->
            <button 
              @click="musicStore.toggleMute"
              class="w-8 h-8 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
              :title="musicStore.isMuted ? '取消静音' : '静音'"
            >
              <Volume2 v-if="!musicStore.isMuted" :size="16" />
              <VolumeX v-else :size="16" />
            </button>

            <!-- 歌单按钮（移动端） -->
            <button 
              @click="togglePlaylistPanel" 
              class="w-8 h-8 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
              :class="{ 'bg-white/60': showPlaylistPanel }"
              data-playlist-button
              title="歌单"
            >
              <List :size="16" />
            </button>
          </div>
        </div>
      </div>

      <!-- 桌面端布局 -->
      <div class="hidden lg:flex items-center justify-between">
        <!-- 左侧区域 - 音乐信息 -->
        <div class="flex items-center gap-4 min-w-0 flex-1">
          <div 
            @click="goToMusicPage"
            class="w-12 h-12 bg-gradient-to-r from-blue-400 to-teal-500 rounded-2xl flex items-center justify-center cursor-pointer hover:scale-105 transition-transform"
            title="进入音乐页面"
          >
            <Music :size="24" class="text-white" />
          </div>
          <div class="min-w-0">
            <p class="font-semibold text-morandi-900 truncate">{{ currentTrackInfo.name }}</p>
            <p class="text-sm text-morandi-700">{{ musicStore.formatTime(musicStore.currentTime) }} / {{ musicStore.formatTime(musicStore.duration) }}</p>
          </div>
        </div>

        <!-- 中间区域 - 主要播放控制 -->
        <div class="flex items-center gap-3">
          <button
            @click="musicStore.previousTrack"
            class="w-10 h-10 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
            title="上一首"
          >
            <SkipBack :size="20" />
          </button>

          <button
            @click="musicStore.togglePlay"
            class="w-12 h-12 rounded-full bg-white text-morandi-800 flex items-center justify-center"
            :title="musicStore.playState === musicStore.PlayState.PLAYING ? '暂停' : '播放'"
          >
            <Play v-if="musicStore.playState === musicStore.PlayState.STOPPED || musicStore.playState === musicStore.PlayState.PAUSED" :size="20" />
            <Pause v-else :size="20" />
          </button>

          <button
            @click="musicStore.nextTrack"
            class="w-10 h-10 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
            title="下一首"
          >
            <SkipForward :size="20" />
          </button>
        </div>

        <!-- 右侧区域 - 其他控制按钮 -->
        <div class="flex items-center gap-3 flex-1 justify-end">
          <!-- 音量控制 -->
          <div class="relative" ref="volumeRef">
            <button 
              @click="musicStore.toggleMute"
              @mouseenter="clearVolumeTimer(); showVolumeSlider = true"
              @mouseleave="startHideVolumeTimer"
              class="w-10 h-10 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
              :title="musicStore.isMuted ? '取消静音' : '静音'"
            >
              <Volume2 v-if="!musicStore.isMuted" :size="20" />
              <VolumeX v-else :size="20" />
            </button>
            
            <!-- 音量滑块弹出 - 固定宽度 -->
            <div 
              v-if="showVolumeSlider"
              @mouseenter="clearVolumeTimer()"
              @mouseleave="startHideVolumeTimer"
              class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 bg-white rounded-lg shadow-lg p-3 border border-morandi-200 w-16"
            >
              <div class="flex flex-col items-center gap-2 h-24">
                <span class="text-xs text-morandi-600">{{ Math.round(musicStore.volume * 100) }}%</span>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.01"
                  :value="musicStore.volume"
                  @input="setVolume"
                  class="w-2 h-16 bg-morandi-200 rounded-lg appearance-none cursor-pointer volume-slider vertical-slider"
                  title="音量"
                  :style="`--value: ${musicStore.volume * 100}%`"
                />
              </div>
            </div>
          </div>
          
          <!-- 播放模式循环按钮 -->
          <button
            @click="musicStore.setPlayMode(musicStore.playMode === 'order' ? 'shuffle' : musicStore.playMode === 'shuffle' ? 'repeat' : 'order')"
            class="w-10 h-10 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center transition-all duration-200"
            :class="{
              'bg-white/60': musicStore.playMode !== 'order'
            }"
            :title="getPlayModeText()"
          >
            <Repeat v-if="musicStore.playMode === 'order'" :size="20" />
            <Shuffle v-else-if="musicStore.playMode === 'shuffle'" :size="20" />
            <Repeat1 v-else :size="20" />
          </button>
          
          <!-- 歌单按钮 -->
          <button 
            @click="togglePlaylistPanel" 
            class="w-10 h-10 rounded-full bg-white/30 hover:bg-white/50 flex items-center justify-center"
            :class="{ 'bg-white/60': showPlaylistPanel }"
            data-playlist-button
            title="歌单"
          >
            <List :size="20" />
          </button>
        </div>
      </div>
    </div>

    <!-- 歌单展开面板 - 响应式调整 -->
    <div 
      v-if="showPlaylistPanel"
      ref="playlistRef"
      class="fixed bottom-16 lg:bottom-20 right-4 lg:right-6 w-72 lg:w-80 bg-white rounded-lg shadow-xl border border-morandi-200 z-30 max-h-80 lg:max-h-96 flex flex-col"
    >
      <!-- 面板头部 - 快速切换歌单 -->
      <div class="p-4 border-b border-morandi-200">
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-semibold text-morandi-900">歌单</h3>
          <div class="relative">
            <button 
              @click="showPlaylistSelector = !showPlaylistSelector"
              class="px-3 py-1 text-sm bg-teal-500 text-white rounded-md hover:bg-teal-600"
            >
              切换歌单
            </button>
            
            <!-- 歌单选择器 -->
            <div 
              v-if="showPlaylistSelector"
              ref="selectorRef"
              class="absolute top-full right-0 mt-1 bg-white border border-morandi-200 rounded-lg shadow-lg z-40 max-h-48 overflow-auto w-40"
            >
          <div 
            v-for="playlist in musicStore.playlists"
            :key="playlist.id"
            @click="selectPlaylist(playlist, $event)"
            class="p-2 hover:bg-morandi-50 cursor-pointer border-b border-morandi-100 last:border-b-0 flex items-center justify-between"
            :class="{ 'bg-teal-50 text-teal-700': musicStore.currentPlaylist?.id === playlist.id }"
          >
            <span class="font-medium text-sm truncate">{{ playlist.folder_name }}</span>
          </div>
            </div>
          </div>
        </div>
        
        <p class="text-sm text-morandi-600">
          {{ musicStore.currentPlaylist?.folder_name || '未选择歌单' }} · {{ musicStore.currentPlaylistTracks.length || 0 }} 首歌曲
        </p>
      </div>
      
      <!-- 歌曲列表 -->
      <div class="flex-1 overflow-auto p-2">
        <div
          v-for="(track, index) in musicStore.currentPlaylistTracks"
          :key="track.id"
          @click="playTrackFromPanel(index)"
          class="flex items-center gap-3 p-2 rounded-md hover:bg-morandi-50 cursor-pointer"
          :class="{ 'bg-teal-50 border border-teal-200': musicStore.currentTrack?.id === track.id }"
        >
          <div class="w-6 text-center">
            <span v-if="musicStore.currentTrack?.id !== track.id" class="text-xs text-morandi-500">{{ index + 1 }}</span>
            <Music v-else :size="12" class="text-teal-600" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-morandi-900 truncate">{{ track.file_name?.replace(/\.[^/.]+$/, '') || '未知音乐' }}</p>
            <p class="text-xs text-morandi-500 truncate">{{ track.artist || '未知艺术家' }}</p>
          </div>
          <span class="text-xs text-morandi-500">{{ musicStore.formatTime(track.duration || 0) }}</span>
        </div>
        
        <!-- 空状态 -->
        <div v-if="!musicStore.currentPlaylistTracks.length" class="text-center py-8">
          <Music :size="32" class="mx-auto mb-2 text-morandi-400" />
          <p class="text-sm text-morandi-500">歌单为空</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Repeat1, Shuffle, Music, Play, Pause, SkipBack, SkipForward, Volume2, VolumeX, Repeat, List, ChevronUp, ChevronDown } from 'lucide-vue-next'
import { useMusicStore } from '../store/MusicStore'
import { useDocsStore } from '@/store/DocsStore'
import { useRoute, useRouter } from 'vue-router'
import { useUIStore } from '@/store/UIStore'
import { storeToRefs } from 'pinia'

const musicStore = useMusicStore()
const uiStore = useUIStore()
const docsStore = useDocsStore()
const { progressPercentage } = storeToRefs(musicStore)
const route = useRoute()
const router = useRouter()

// 新增的响应式状态
const showVolumeSlider = ref(false)
const showPlaylistPanel = ref(false)
const showPlaylistSelector = ref(false)
const showFullMusicBar = ref(false)

// 文档页面检测 - 正确的逻辑：打开文件时收起音乐栏
const isDocumentPage = computed(() => {
  // 在文档页面且打开了文件时，收起音乐栏给办公区域更多空间
  const isDocsRoute = route.path === '/docs'
  const hasOpenDocument = docsStore.selectedDocument !== null

  // 只有在文档页面且打开了文件时才收起音乐栏
  return isDocsRoute && hasOpenDocument
})

// 菜单引用
const volumeRef = ref<HTMLElement>()
const playlistRef = ref<HTMLElement>()
const selectorRef = ref<HTMLElement>()

// 全局点击事件处理
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as Node

  // 音量菜单通过鼠标悬停控制，不需要点击检测

  // 检查歌单面板 - 点击面板内部不关闭，只有点击外部才关闭
  if (showPlaylistPanel.value) {
    const playlistButtons = document.querySelectorAll('[data-playlist-button]')
    let isPlaylistButtonClick = false
    playlistButtons.forEach(button => {
      if (button.contains(target)) {
        isPlaylistButtonClick = true
      }
    })
    
    // 如果点击的是歌单按钮，不处理（让按钮自己的点击事件处理）
    if (isPlaylistButtonClick) {
      return
    }
    
    // 如果点击的是面板内部，不关闭
    if (playlistRef.value && playlistRef.value.contains(target)) {
      return
    }
    
    // 点击外部才关闭
    showPlaylistPanel.value = false
    showPlaylistSelector.value = false
  }

  // 检查歌单选择器 - 点击内部不关闭
  if (showPlaylistSelector.value && selectorRef.value) {
    const toggleButton = selectorRef.value.parentElement?.querySelector('button')
    
    // 如果点击的是切换按钮，不处理
    if (toggleButton?.contains(target)) {
      return
    }
    
    // 如果点击的是选择器内部，不关闭
    if (selectorRef.value.contains(target)) {
      return
    }
    
    // 点击外部才关闭
    showPlaylistSelector.value = false
  }
}

// 初始化全局点击监听
if (typeof document !== 'undefined') {
  document.addEventListener('click', handleClickOutside)
}

// 音量控制方法
let volumeHideTimer: number | null = null

const startHideVolumeTimer = () => {
  volumeHideTimer = setTimeout(() => {
    showVolumeSlider.value = false
  }, 300) // 300ms延迟隐藏
}

const clearVolumeTimer = () => {
  if (volumeHideTimer) {
    clearTimeout(volumeHideTimer)
    volumeHideTimer = null
  }
}

const setVolume = (event: Event) => {
  const target = event.target as HTMLInputElement
  musicStore.setVolume(parseFloat(target.value))
}

// 进度条点击跳转
const seekToPosition = (event: MouseEvent) => {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const percentage = (event.clientX - rect.left) / rect.width
  const newTime = percentage * musicStore.duration
  musicStore.seek(newTime)
}

// 播放模式相关方法
const getPlayModeText = () => {
  switch (musicStore.playMode) {
    case 'order':
      return '顺序播放'
    case 'shuffle':
      return '随机播放'
    case 'repeat':
      return '单曲循环'
    default:
      return '顺序播放'
  }
}

// 歌单面板方法
const togglePlaylistPanel = async () => {
  // 如果还没有加载音乐数据，先加载
  if (musicStore.playlists.length === 0) {
    await musicStore.loadPlaylistsFromDrive()
  }
  
  showPlaylistPanel.value = !showPlaylistPanel.value
  showPlaylistSelector.value = false
}

const selectPlaylist = (playlist: any, event?: MouseEvent) => {
  // 阻止事件冒泡，防止触发全局点击监听器
  if (event) {
    event.stopPropagation()
  }
  
  musicStore.selectPlaylist(playlist)
  showPlaylistSelector.value = false
}

const playTrackFromPanel = (index: number) => {
  const tracks = musicStore.currentPlaylistTracks
  if (tracks[index]) {
    musicStore.playTrack(tracks[index])
    showPlaylistPanel.value = false
    showPlaylistSelector.value = false
  }
}

// 切换音乐栏显示状态
const toggleMusicBar = () => {
  showFullMusicBar.value = !showFullMusicBar.value
}

// 跳转到音乐页面
const goToMusicPage = async () => {
  // 如果还没有加载音乐数据，先加载
  if (musicStore.playlists.length === 0) {
    await musicStore.loadPlaylistsFromDrive()
  }

  if (musicStore.currentPlaylist && musicStore.currentTrack) {
    // 传递当前歌单和歌曲信息到音乐页面
    router.push({
      name: 'music',
      query: {
        playlist: musicStore.currentPlaylist.folder_name,
        song: musicStore.currentTrack.file_name
      }
    })
  } else {
      // 如果没有当前歌单，直接跳转到音乐页面
    router.push({ name: 'music' })
  }
}

// 格式化当前播放信息
const currentTrackInfo = computed(() => {
  if (!musicStore.currentTrack) return { name: '暂无音乐', artist: '', album: '' }
  
  return {
    name: musicStore.currentTrack.file_name?.replace(/\.[^/.]+$/, '') || '未知音乐',
    artist: musicStore.currentTrack.artist || '未知艺术家',
    album: musicStore.currentPlaylist?.folder_name || '未知专辑'
  }
})

// 获取当前歌单的歌曲数量
const getPlaylistTrackCount = (playlist: any) => {
  return musicStore.getPlaylistTrackCount(playlist.id)
}
</script>

<style scoped>
.volume-slider {
  background: linear-gradient(to top, #14B8A6 var(--value, 0%), #E2E8F0 var(--value, 0%));
}

/* 垂直滑块样式 */
.vertical-slider {
  writing-mode: bt-lr; /* IE */
  writing-mode: vertical-lr; /* 标准写法 */
  -webkit-appearance: none;
  appearance: none;
  /* 移除rotate，因为它搞反了方向 */
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  background: #14B8A6;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.volume-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  background: #14B8A6;
  border-radius: 50%;
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.volume-slider::-moz-range-track {
  background: transparent;
}

/* 统一滚动条样式 */
.overflow-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.overflow-auto::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.overflow-auto::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style> 