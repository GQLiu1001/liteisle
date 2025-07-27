<template>
  <div class="min-h-full bg-liteisle-bg p-4 lg:p-6">
    <div class="max-w-7xl mx-auto">
      <!-- 移动端布局 - 垂直堆叠 -->
      <div class="lg:hidden space-y-4">
        <!-- 当前播放信息 - 移动端置顶 -->
        <div class="card">
          <div class="flex items-center gap-4 p-4">
            <div class="w-16 h-16 rounded-full bg-gradient-to-br from-blue-400 to-teal-500 flex items-center justify-center flex-shrink-0">
              <Music :size="32" class="text-white" />
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-bold text-morandi-900 truncate">{{ musicStore.currentTrack?.file_name?.replace(/\.[^/.]+$/, '') || '暂无播放' }}</h3>
              <p class="text-xs text-morandi-500 truncate mb-1">{{ musicStore.currentTrack?.album || '未知专辑' }}</p>
              <p class="text-sm text-morandi-600 truncate">{{ musicStore.currentTrack?.artist || '未知艺术家' }}</p>
              <div class="flex items-center justify-between text-xs text-morandi-500 mt-2">
                <span>{{ musicStore.formatTime(musicStore.currentTime) }}</span>
                <span>{{ musicStore.formatTime(musicStore.duration) }}</span>
              </div>
            </div>
          </div>
          <!-- 播放进度条 -->
          <div class="px-4 pb-4">
            <div 
              class="w-full bg-morandi-200 rounded-full h-2 cursor-pointer relative"
              @click="seekToPosition"
              @mousedown="startDrag"
              @touchstart="startDrag"
            >
              <div 
                class="bg-teal-500 h-2 rounded-full transition-all duration-100"
                :style="{ width: progressPercentage + '%' }"
              ></div>
              <!-- 拖拽滑块 -->
              <div 
                class="absolute top-1/2 transform -translate-y-1/2 w-4 h-4 bg-teal-500 rounded-full shadow-lg cursor-pointer opacity-0 hover:opacity-100 transition-opacity sm:opacity-100"
                :style="{ left: `calc(${progressPercentage}% - 8px)` }"
                @mousedown.stop="startDrag"
                @touchstart.stop="startDrag"
              ></div>
            </div>
          </div>
        </div>

        <!-- 歌单选择器 - 移动端 -->
        <div class="card">
          <div class="p-4">
            <h3 class="text-lg font-bold text-morandi-900 mb-4">歌单</h3>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div
                v-for="playlist in playlists.slice(0, 4)"
                :key="playlist.id"
                @click="selectPlaylist(playlist)"
                :class="[
                  'flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all duration-200',
                  musicStore.currentPlaylist?.id === playlist.id
                    ? 'bg-teal-100 text-teal-800 border border-teal-300'
                    : 'hover:bg-morandi-100'
                ]"
              >
                <DiscAlbum :size="16" class="text-blue-500 flex-shrink-0" />
                <div class="flex-1 min-w-0">
                  <p class="font-medium truncate text-sm">{{ playlist.folder_name }}</p>
                  <p class="text-xs text-morandi-500">{{ getPlaylistDisplayCount(playlist) }} 首</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 歌曲列表 - 移动端 -->
        <div class="card">
          <div class="p-4">
            <div class="flex items-center justify-between mb-4">
              <div>
                <h2 class="text-lg font-bold text-morandi-900">
                  {{ musicStore.currentPlaylist?.folder_name || '选择歌单' }}
                </h2>
                <p class="text-sm text-morandi-500">{{ filteredTracks.length }} 首歌曲</p>
              </div>
              <button
                v-if="musicStore.currentPlaylist"
                @click="playAll"
                class="flex items-center gap-2 px-3 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors text-sm"
              >
                播放
              </button>
            </div>
            
            <!-- 搜索框 - 移动端 -->
            <div class="mb-4">
              <input
                v-model="musicStore.searchQuery"
                placeholder="搜索音乐..."
                class="w-full px-3 py-2 rounded-lg border border-morandi-300 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent text-sm select-text"
                style="user-select: text !important;"
              />
            </div>

            <!-- 歌曲列表 -->
            <div class="space-y-2 max-h-96 overflow-auto">
              <div
                v-for="(track, index) in filteredTracks.slice(0, 10)"
                :key="track.id"
                @click="playTrackImmediately(index)"
                :class="[
                  'flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all duration-200',
                  musicStore.currentTrack?.id === track.id 
                    ? 'bg-teal-50 border border-teal-300' 
                    : 'hover:bg-morandi-50'
                ]"
              >
                <div class="w-6 text-center">
                  <Music v-if="musicStore.currentTrack?.id === track.id" :size="14" class="text-teal-600" />
                  <span v-else class="text-sm text-morandi-500">{{ index + 1 }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-morandi-900 truncate text-sm">{{ track.file_name?.replace(/\.[^/.]+$/, '') || '未知音乐' }}</p>
                  <p class="text-xs text-morandi-500 truncate">{{ track.artist }}</p>
                </div>
                <div class="text-xs text-morandi-500">
                  {{ musicStore.formatTime(track.duration) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 桌面端布局 - 切换为更稳健的 Flexbox 布局 -->
      <div class="hidden lg:flex gap-6 h-[calc(100vh-10rem)]">
        <!-- 左侧歌单导航: 固定宽度，绝不压缩 -->
        <div class="w-72 flex-shrink-0">
          <div class="card h-full flex flex-col">
            <!-- 搜索框 -->
            <div class="mb-6">
              <input
                v-model="musicStore.searchQuery"
                placeholder="搜索音乐..."
                class="w-full px-4 py-2 rounded-lg border border-morandi-300 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent select-text"
                style="user-select: text !important;"
              />
            </div>

            <!-- 歌单 -->
            <div class="flex-1 overflow-auto" ref="playlistScrollContainer">
              <div class="space-y-2">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-bold text-morandi-900">歌单</h3>
                <button
                  @click="showCreatePlaylistDialog = true"
                  class="flex items-center gap-1 px-2 py-1 text-teal-600 hover:bg-teal-50 rounded-lg transition-colors text-sm"
                  title="添加歌单"
                >
                  <Plus :size="16" />
                  添加歌单
                </button>
              </div>
              <draggable
                v-model="currentPlaylistsList"
                item-key="id"
                class="space-y-2"
                :animation="150"
                ghost-class="ghost"
                chosen-class="chosen"
                drag-class="drag"
                @start="onPlaylistDragStart"
                @end="onPlaylistDragEnd"
                :force-fallback="false"
                :disabled="false"
              >
                <template #item="{ element: playlist }">
                  <div
                    @click="musicStore.selectPlaylist(playlist)"
                    @contextmenu.prevent="handlePlaylistContextMenu($event, playlist)"
                    :data-id="playlist.id"
                    :class="[
                      'flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all duration-200',
                      musicStore.currentPlaylist?.id === playlist.id 
                        ? 'bg-teal-100 text-teal-800 border border-teal-300' 
                        : 'hover:bg-morandi-100'
                    ]"
                  >
                    <DiscAlbum :size="20" class="text-blue-500" />
                    <div class="flex-1 min-w-0">
                      <p class="font-medium truncate">{{ playlist.folder_name }}</p>
                      <p class="text-sm text-morandi-500">
                        {{ getPlaylistDisplayCount(playlist) }} 首歌曲
                      </p>
                    </div>
                  </div>
                </template>
              </draggable>

              <!-- 空状态 -->
              <div v-if="musicStore.sortedPlaylists.length === 0" class="text-center py-8">
                <Music :size="32" class="mx-auto mb-3 text-morandi-400" />
                <p class="text-sm text-morandi-500">暂无歌单</p>
                <p class="text-xs text-morandi-400 mt-1">请先在云盘中上传音乐文件</p>
              </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间歌曲列表: 自动填充所有剩余空间 -->
        <div class="flex-1 min-w-0">
          <div class="card h-full flex flex-col">
            <!-- 列表头部 -->
            <div class="flex items-center justify-between mb-6">
              <div>
                <h2 class="font-bold text-morandi-900 lg:text-lg xl:text-xl">
                    {{ musicStore.currentPlaylist?.folder_name || '选择歌单' }}
                </h2>
                <p class="text-sm text-morandi-500">
                  {{ filteredTracks.length }} 首歌曲
                </p>
              </div>

              <!-- 列表操作 -->
              <div v-if="musicStore.currentPlaylist" class="flex items-center gap-2">
                <button
                  @click="playAll"
                  class="flex items-center gap-1 px-3 py-1.5 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors text-sm"
                >
                  <Play :size="16" />
                  播放
                </button>
                
                <!-- 添加音乐按钮 -->
                <button
                  @click="showUploadMusicDialog = true"
                  class="flex items-center gap-1 px-3 py-1.5 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors text-sm"
                >
                  <Plus :size="16" />
                  添加
                </button>
                
                <!-- 右侧面板切换按钮 - 始终显示 -->
                <button
                  @click="showRightPanel = !showRightPanel"
                  :class="[
                    'flex items-center gap-1 px-2 py-1.5 rounded-lg transition-colors text-sm',
                    showRightPanel 
                      ? 'bg-teal-100 text-teal-700 hover:bg-teal-200' 
                      : 'bg-morandi-200 text-morandi-700 hover:bg-morandi-300'
                  ]"
                >
                  {{ showRightPanel ? '隐藏' : '详情' }}
                </button>
              </div>
            </div>

            <!-- 歌曲列表内容保持不变 -->
            <!-- 歌曲列表 -->
            <div 
              class="flex-1 overflow-auto"
              ref="scrollContainer"
            >
              <div v-if="filteredTracks.length > 0" class="space-y-1">
                <!-- 可拖动的歌曲列表 -->
                <draggable
                  v-if="!musicStore.searchQuery"
                  v-model="localTracks"
                  item-key="id"
                  class="space-y-1"
                  :animation="150"
                  ghost-class="ghost"
                  chosen-class="chosen"
                  drag-class="drag"
                  @start="onDragStart"
                  @end="onDragEnd"
                >
                  <template #item="{ element: track, index }">
                    <div
                      @contextmenu.prevent="handleTrackContextMenu($event, track)"
                      :data-id="track.id"
                      :class="[
                        'flex items-center gap-4 p-3 rounded-lg cursor-pointer transition-all duration-200 group border-2',
                        musicStore.currentTrack?.id === track.id 
                          ? 'bg-teal-50 border-solid border-teal-500' 
                          : 'border-transparent hover:border-dashed hover:border-teal-300 hover:bg-morandi-50'
                      ]"
                    >
                      <!-- 拖动图标 -->
                      <div class="w-4 text-center opacity-30 group-hover:opacity-70 transition-opacity">
                        <div class="w-1 h-4 bg-morandi-400 rounded-full flex-shrink-0"></div>
                      </div>

                      <!-- 序号 / 播放状态 -->
                      <div class="w-8 text-center">
                        <span 
                          v-if="musicStore.currentTrack?.id !== track.id"
                          class="text-sm text-morandi-500"
                        >
                          {{ index + 1 }}
                        </span>
                        <Music 
                          v-else-if="isPlaying"
                          :size="16" 
                          class="text-teal-600 animate-pulse" 
                        />
                        <Music 
                          v-else
                          :size="16" 
                          class="text-teal-600" 
                        />
                      </div>

                      <!-- 歌曲信息 -->
                      <div 
                        class="flex-1 min-w-0" 
                        @click="playTrack(index)"
                        @dblclick="playTrackImmediately(index)"
                        @contextmenu.prevent="handleTrackContextMenu($event, track)"
                      >
                        <p class="font-medium text-morandi-900 truncate">{{ track.file_name?.replace(/\.[^/.]+$/, '') || '未知音乐' }}</p>
                        <p class="text-sm text-morandi-500 truncate">{{ track.artist }}</p>
                      </div>

                      <!-- 歌曲时长 -->
                      <div class="text-sm text-morandi-500">
                        {{ musicStore.formatTime(track.duration) }}
                      </div>
                    </div>
                  </template>
                </draggable>

                <!-- 搜索模式下的普通列表（不可拖动） -->
                <div v-else class="space-y-1">
                  <div
                    v-for="(track, index) in filteredTracks"
                    :key="track.id"
                    @click="playTrack(index)"
                    @dblclick="playTrackImmediately(index)"
                    @contextmenu.prevent="handleTrackContextMenu($event, track)"
                    :class="[
                      'flex items-center gap-4 p-3 rounded-lg cursor-pointer transition-all duration-200 group border-2',
                      musicStore.currentTrack?.id === track.id 
                        ? 'bg-teal-50 border-solid border-teal-500' 
                        : 'border-transparent hover:border-dashed hover:border-teal-300 hover:bg-morandi-50'
                    ]"
                  >
                    <!-- 序号 / 播放状态 -->
                    <div class="w-8 text-center">
                      <span 
                        v-if="musicStore.currentTrack?.id !== track.id"
                        class="text-sm text-morandi-500"
                      >
                        {{ index + 1 }}
                      </span>
                      <Music 
                        v-else-if="isPlaying"
                        :size="16" 
                        class="text-teal-600 animate-pulse" 
                      />
                      <Music 
                        v-else
                        :size="16" 
                        class="text-teal-600" 
                      />
                    </div>

                    <!-- 歌曲信息 -->
                    <div class="flex-1 min-w-0" @contextmenu.prevent="handleTrackContextMenu($event, track)">
                      <p class="font-medium text-morandi-900 truncate">{{ track.file_name?.replace(/\.[^/.]+$/, '') || '未知音乐' }}</p>
                      <p class="text-sm text-morandi-500 truncate">{{ track.artist || '未知艺术家' }}</p>
                    </div>

                    <!-- 歌曲时长 -->
                    <div class="text-sm text-morandi-500">
                      {{ musicStore.formatTime(track.duration || 0) }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 空状态 -->
              <div v-else class="flex-1 flex items-center justify-center">
                <div class="text-center">
                  <Music :size="48" class="mx-auto mb-4 text-morandi-400" />
                  <h3 class="text-lg font-medium text-morandi-700 mb-2">
                    {{ musicStore.searchQuery ? '未找到匹配的歌曲' : '歌单为空' }}
                  </h3>
                  <p class="text-morandi-500">
                    {{ musicStore.searchQuery ? '尝试其他搜索词' : '请先选择一个歌单' }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧播放详情: 固定宽度，绝不压缩 -->
        <div v-if="showRightPanel" class="w-80 flex-shrink-0">
          <div class="card h-full flex flex-col">
            
            <!-- 专辑封面 -->
            <div class="text-center mb-6">
              <div class="w-32 h-32 lg:w-40 lg:h-40 mx-auto mb-4 rounded-full bg-gradient-to-br from-blue-400 to-teal-500 flex items-center justify-center">
                <Music :size="48" class="text-white" />
              </div>
              <h3 class="font-bold text-morandi-900 truncate">{{ musicStore.currentTrack?.file_name?.replace(/\.[^/.]+$/, '') || '暂无播放' }}</h3>
                              <p class="text-sm text-morandi-600 truncate">{{ musicStore.currentTrack?.album || '未知专辑' }}</p>
                <p class="text-sm text-morandi-600 truncate">{{ musicStore.currentTrack?.artist || '未知艺术家' }}</p>
            </div>

            <!-- 播放进度 -->
            <div class="mb-6">
              <div class="flex items-center justify-between text-sm text-morandi-500 mb-2">
                <span>{{ musicStore.formatTime(musicStore.currentTime) }}</span>
                <span>{{ musicStore.formatTime(musicStore.duration) }}</span>
              </div>
              <div 
                class="w-full bg-morandi-200 rounded-full h-2 cursor-pointer relative"
                @click="seekToPosition"
                @mousedown="startDrag"
                @touchstart="startDrag"
              >
                <div 
                  class="bg-teal-500 h-2 rounded-full transition-all duration-100"
                  :style="{ width: progressPercentage + '%' }"
                ></div>
                <!-- 拖拽滑块 -->
                <div 
                  class="absolute top-1/2 transform -translate-y-1/2 w-4 h-4 bg-teal-500 rounded-full shadow-lg cursor-pointer opacity-0 hover:opacity-100 transition-opacity sm:opacity-100"
                  :style="{ left: `calc(${progressPercentage}% - 8px)` }"
                  @mousedown.stop="startDrag"
                  @touchstart.stop="startDrag"
                ></div>
              </div>
            </div>

            <!-- 歌词区域已移除 -->
          </div>
        </div>
      </div>
    </div>

    <!-- 新建歌单对话框 -->
    <div v-if="showCreatePlaylistDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96">
        <h3 class="text-lg font-bold mb-4">新建歌单</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-2">歌单名称</label>
            <input
              v-model="newPlaylistName"
              type="text"
              placeholder="请输入歌单名称"
              class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
              @keydown.enter="createNewPlaylist"
              style="user-select: text !important;"
            />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="showCreatePlaylistDialog = false"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="createNewPlaylist"
            :disabled="!newPlaylistName.trim()"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            创建
          </button>
        </div>
      </div>
    </div>

    <!-- 上传音乐对话框 -->
    <div v-if="showUploadMusicDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96">
        <h3 class="text-lg font-bold mb-4">添加音乐</h3>
        <div class="space-y-4">
          <div class="border-2 border-dashed border-morandi-300 rounded-lg p-8 text-center">
            <Music :size="32" class="mx-auto mb-3 text-morandi-400" />
            <p class="text-morandi-600 mb-2">点击选择音乐文件或拖拽到此处</p>
            <p class="text-xs text-morandi-400">支持 MP3、WAV、FLAC、AAC、M4A、OGG 格式</p>
            <input 
              type="file" 
              multiple 
              accept="audio/*"
              class="hidden" 
              ref="musicFileInput"
              @change="handleMusicFileSelect"
            />
            <button 
              @click="() => musicFileInput?.click()"
              class="mt-3 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors"
            >
              选择音乐文件
            </button>
          </div>
          
          <!-- 音乐文件列表 -->
          <div v-if="selectedMusicFiles.length > 0" class="max-h-32 overflow-auto">
            <h4 class="text-sm font-medium text-morandi-700 mb-2">待添加音乐：</h4>
            <div class="space-y-1">
              <div 
                v-for="(file, index) in selectedMusicFiles" 
                :key="index"
                class="flex items-center justify-between text-xs bg-morandi-50 p-2 rounded"
              >
                <span class="truncate flex-1">{{ file.name }}</span>
                <span class="text-morandi-500 ml-2">{{ (file.size / (1024 * 1024)).toFixed(1) }}MB</span>
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="showUploadMusicDialog = false; selectedMusicFiles = []"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="uploadMusicFiles"
            :disabled="selectedMusicFiles.length === 0"
            class="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            添加 ({{ selectedMusicFiles.length }})
          </button>
        </div>
      </div>
    </div>

    <!-- 重命名音乐对话框 -->
    <div v-if="showRenameTrackDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelRenameTrack">
      <div class="bg-white rounded-lg p-6 w-96" @click.stop>
        <h3 class="text-lg font-bold mb-4">重命名音乐</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-2">新名称</label>
            <input
              ref="renameTrackInput"
              v-model="renameTrackValue"
              type="text"
              placeholder="请输入新名称"
              class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
              @keydown.enter="confirmRenameTrack"
              @keydown.esc="cancelRenameTrack"
              style="user-select: text !important;"
            />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="cancelRenameTrack"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="confirmRenameTrack"
            :disabled="!renameTrackValue.trim()"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            确认
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 重命名歌单对话框 -->
  <div v-if="showRenamePlaylistDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelRenamePlaylist">
    <div class="bg-white rounded-lg p-6 w-96" @click.stop>
      <h3 class="text-lg font-bold mb-4">重命名歌单</h3>
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-morandi-700 mb-2">新名称</label>
          <input
            ref="renamePlaylistInput"
            v-model="renamePlaylistValue"
            type="text"
            placeholder="请输入新名称"
            class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
            @keydown.enter="confirmRenamePlaylist"
            @keydown.esc="cancelRenamePlaylist"
            style="user-select: text !important;"
          />
        </div>
      </div>
      <div class="flex justify-end gap-3 mt-6">
        <button
          @click="cancelRenamePlaylist"
          class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
        >
          取消
        </button>
        <button
          @click="confirmRenamePlaylist"
          :disabled="!renamePlaylistValue.trim()"
          class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          确认
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick, watch } from 'vue'
import { Music, DiscAlbum, Play, Upload, Plus } from 'lucide-vue-next'
import { useMusicStore } from '../store/MusicStore'
import { useDriveStore } from '../store/DriveStore'
import { useTransferStore } from '../store/TransferStore'
import { useContextMenuStore, type ContextMenuItem } from '@/store/ContextMenuStore'
import { useDocsStore } from '@/store/DocsStore'
import { useToast } from 'vue-toastification'
import draggable from 'vuedraggable'
import type { MusicFileInfo } from '@/types/api'
import { FileStatusEnum, FolderTypeEnum } from '@/types/api'
import { API } from '@/utils/api'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ensureWebSocketForUpload, onUploadComplete } from '@/utils/websocket'

const musicStore = useMusicStore()
const driveStore = useDriveStore()
const transferStore = useTransferStore()
const contextMenuStore = useContextMenuStore()
const docsStore = useDocsStore()
const toast = useToast()
const route = useRoute()

// 使用 storeToRefs 获取响应式数据
const { playlists, allMusicFiles, isLoading } = storeToRefs(musicStore)

// 本地拖拽列表
const localTracks = ref<MusicFileInfo[]>([])

// 监听 Pinia store 中当前歌单的变化，并更新本地列表
watch(() => musicStore.currentPlaylistTracks, (newTracks) => {
  // 使用 slice() 创建一个浅拷贝，避免直接引用
  localTracks.value = newTracks ? [...newTracks] : []
}, { immediate: true, deep: true })

// 响应式状态
const showRightPanel = ref(true)
const showCreatePlaylistDialog = ref(false)
const showUploadMusicDialog = ref(false)
const newPlaylistName = ref('')
const selectedMusicFiles = ref<File[]>([])
const musicFileInput = ref<HTMLInputElement | null>(null)

// 右键菜单相关
const selectedTrack = ref<MusicFileInfo | null>(null)
const selectedPlaylist = ref<any | null>(null) // Playlist type is not directly imported, use 'any' for now

// 重命名相关
const showRenameTrackDialog = ref(false)
const renameTrackValue = ref('')
const renameTrackInput = ref<HTMLInputElement | null>(null)

// 歌单重命名相关
const showRenamePlaylistDialog = ref(false)
const renamePlaylistValue = ref('')
const renamePlaylistInput = ref<HTMLInputElement | null>(null)

// 拖拽相关
const draggedItemId = ref<string | null>(null)
const draggedPlaylistId = ref<string | null>(null)

// 拖拽自动滚动相关
const scrollContainer = ref<HTMLElement | null>(null)
const playlistScrollContainer = ref<HTMLElement | null>(null)
const autoScrollTimer = ref<number | null>(null)
const scrollSpeed = ref(0)

// 进度条拖拽相关
const isDragging = ref(false)
const dragStartTime = ref(0)

// 从 store 解构进度百分比（保持与底栏同步）
const { progressPercentage } = storeToRefs(musicStore)

// 计算属性 - 使用 store 中的 filteredTracks
const filteredTracks = computed(() => musicStore.filteredTracks)

// 计算当前是否正在播放
const isPlaying = computed(() => musicStore.playState === musicStore.PlayState.PLAYING)

// 可拖动的歌单
const currentPlaylistsList = computed({
  get: () => musicStore.sortedPlaylists || [],
  set: (newPlaylists: any[]) => { 
    // 不在这里处理排序，避免与@end事件重复调用
    // 排序逻辑统一在onPlaylistDragEnd中处理
  }
})

// 拖动开始事件
const onDragStart = (event: any) => {
  // 清除可能的文档状态污染
  if (docsStore.selectedDocument) {
    docsStore.setCurrentDocument(null)
  }
  // 开始全局拖拽监听
  startGlobalDragListening()
};

// 拖动结束事件
const onDragEnd = (event: { oldIndex?: number; newIndex?: number }) => {
  if (typeof event.oldIndex === 'number' && typeof event.newIndex === 'number' && event.oldIndex !== event.newIndex) {
    // 实现歌曲重排序
    musicStore.reorderTracksInCurrentPlaylist(event.oldIndex, event.newIndex);
  }
  // 停止全局拖拽监听
  stopGlobalDragListening();
}

// 歌单拖动开始事件
const onPlaylistDragStart = (event: any) => {
  if (event.item) {
    draggedPlaylistId.value = event.item.dataset.id;
  }
  // 开始歌单的全局拖拽监听
  startPlaylistGlobalDragListening()
};

// 歌单拖动结束事件
const onPlaylistDragEnd = (event: {oldIndex: number, newIndex: number}) => {
  if (event.oldIndex !== event.newIndex) {
    // 实现播放列表重排序
    musicStore.reorderPlaylists(event.oldIndex, event.newIndex);
  }
  // 停止歌单的全局拖拽监听
  stopPlaylistGlobalDragListening()
  draggedPlaylistId.value = null;
}

// 方法 - 直接使用 musicStore 中的方法

const playTrack = (index: number) => {
  const track = filteredTracks.value[index]
  if (track && musicStore.currentPlaylist) {
    // 只设置当前播放曲目，不立即播放
    musicStore.currentTrack = track
  }
}

const playTrackImmediately = async (index: number) => {
  const track = filteredTracks.value[index]
  if (track && musicStore.currentPlaylist) {
    try {
      // 立即播放指定歌曲
      await musicStore.playTrack(track, musicStore.currentPlaylist)
      console.log('开始播放:', track.file_name)
    } catch (error) {
      console.error('播放失败:', error)
      toast.error('播放失败，请重试')
    }
  }
}

const playAll = () => {
  if (filteredTracks.value.length > 0 && musicStore.currentPlaylist) {
    const firstTrack = filteredTracks.value[0]
    musicStore.playTrack(firstTrack, musicStore.currentPlaylist)
  }
}

const seekToPosition = (event: MouseEvent) => {
  if (isDragging.value) return // 拖拽时不响应点击
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const percentage = (event.clientX - rect.left) / rect.width
  const newTime = percentage * musicStore.duration
  musicStore.seek(newTime)
}

// 拖拽进度条相关方法
const startDrag = (event: MouseEvent | TouchEvent) => {
  event.preventDefault()
  isDragging.value = true
  
  const handleMove = (e: MouseEvent | TouchEvent) => {
    if (!isDragging.value) return
    
    const target = (event.currentTarget as HTMLElement).closest('.cursor-pointer') as HTMLElement
    if (!target) return
    
    const rect = target.getBoundingClientRect()
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
    const percentage = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width))
    const newTime = percentage * musicStore.duration
    
    musicStore.seek(newTime)
  }
  
  const handleEnd = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', handleMove)
    document.removeEventListener('mouseup', handleEnd)
    document.removeEventListener('touchmove', handleMove)
    document.removeEventListener('touchend', handleEnd)
  }
  
  document.addEventListener('mousemove', handleMove)
  document.addEventListener('mouseup', handleEnd)
  document.addEventListener('touchmove', handleMove)
  document.addEventListener('touchend', handleEnd)
}

const getPlaylistDisplayCount = (playlist: any) => { // Playlist type is not directly imported, use 'any' for now
  // 获取该播放列表中的所有歌曲
  const playlistTracks = musicStore.allMusicFiles.filter((file: MusicFileInfo) =>
    file.folder_id === playlist.id &&
    (file.file_status === FileStatusEnum.AVAILABLE || !file.file_status) // 兼容没有file_status字段的情况
  )

  // 如果没有搜索条件，显示总数
  if (!musicStore.searchQuery) {
    return playlistTracks.length
  }

  // 如果有搜索条件，显示该歌单中匹配的数量
  return playlistTracks.filter((track: any) =>
    (track.file_name || '').toLowerCase().includes(musicStore.searchQuery.toLowerCase()) ||
    (track.artist || '').toLowerCase().includes(musicStore.searchQuery.toLowerCase())
  ).length
}

// 加载云盘音乐数据
const loadMusicFromDrive = () => {
  // 从 DriveStore 中获取数据，这样音乐页面和网盘页面就使用同一套数据了
  musicStore.loadPlaylistsFromDrive()
}

onMounted(async () => {
  // 确保进入音乐页面时清除文档状态，防止PDF查看器误显示
  if (docsStore.selectedDocument) {
    console.log('清除文档页面状态，防止PDF误显示')
    docsStore.setCurrentDocument(null)
  }

  // 添加用户交互监听，解决浏览器自动播放限制
  const enableAudioPlayback = () => {
    // 创建一个静音的音频来"解锁"音频播放权限
    const audio = new Audio()
    audio.volume = 0
    audio.play().then(() => {
      console.log('音频播放权限已解锁')
    }).catch(() => {
      console.log('音频播放权限解锁失败')
    })

    // 移除监听器，只需要执行一次
    document.removeEventListener('click', enableAudioPlayback)
    document.removeEventListener('keydown', enableAudioPlayback)
  }

  // 监听用户的第一次交互
  document.addEventListener('click', enableAudioPlayback, { once: true })
  document.addEventListener('keydown', enableAudioPlayback, { once: true })

  loadMusicFromDrive()
  const { playlist, song } = route.query
  // 只有没有当前播放才根据 query 设置当前播放，否则保持现有播放状态（不重置进度）
  if (
    playlist && song &&
    typeof playlist === 'string' &&
    typeof song === 'string' &&
    (!musicStore.currentTrack || musicStore.currentTrack?.file_name !== song)
  ) {
    musicStore.playSongFromDrive(playlist, song)
  }
})

// 拖拽自动滚动方法
const handleDragOverScroll = (event: DragEvent) => {
  if (!scrollContainer.value) return
  
  const container = scrollContainer.value
  const rect = container.getBoundingClientRect()
  const mouseY = event.clientY
  const scrollZone = 50 // 滚动触发区域的高度
  
  // 计算滚动速度
  let newScrollSpeed = 0
  
  if (mouseY < rect.top + scrollZone) {
    // 靠近顶部，向上滚动
    const distance = rect.top + scrollZone - mouseY
    newScrollSpeed = -Math.min(10, Math.max(1, distance / 5))
  } else if (mouseY > rect.bottom - scrollZone) {
    // 靠近底部，向下滚动
    const distance = mouseY - (rect.bottom - scrollZone)
    newScrollSpeed = Math.min(10, Math.max(1, distance / 5))
  }
  
  if (newScrollSpeed !== scrollSpeed.value) {
    scrollSpeed.value = newScrollSpeed
    
    if (autoScrollTimer.value) {
      clearInterval(autoScrollTimer.value)
      autoScrollTimer.value = null
    }
    
    if (scrollSpeed.value !== 0) {
      autoScrollTimer.value = window.setInterval(() => {
        if (scrollContainer.value) {
          scrollContainer.value.scrollTop += scrollSpeed.value
        }
      }, 16) // 60fps
    }
  }
}

const clearAutoScroll = () => {
  if (autoScrollTimer.value) {
    clearInterval(autoScrollTimer.value)
    autoScrollTimer.value = null
  }
  scrollSpeed.value = 0
}

// 全局拖拽监听
const startGlobalDragListening = () => {
  // 添加全局dragover事件监听
  document.addEventListener('dragover', handleGlobalDragOver, { passive: false })
  document.addEventListener('dragend', stopGlobalDragListening, { passive: false })
  document.addEventListener('drop', stopGlobalDragListening, { passive: false })
}

const stopGlobalDragListening = () => {
  document.removeEventListener('dragover', handleGlobalDragOver)
  document.removeEventListener('dragend', stopGlobalDragListening)
  document.removeEventListener('drop', stopGlobalDragListening)
  clearAutoScroll()
}

const handleGlobalDragOver = (event: DragEvent) => {
  try {
    event.preventDefault()
    handleDragOverScroll(event)
  } catch (error) {
    console.error('MusicPage handleGlobalDragOver error:', error)
  }
}

// 歌单全局拖拽监听
const startPlaylistGlobalDragListening = () => {
  // 添加全局dragover事件监听
  document.addEventListener('dragover', handlePlaylistGlobalDragOver, { passive: false })
  document.addEventListener('dragend', stopPlaylistGlobalDragListening, { passive: false })
  document.addEventListener('drop', stopPlaylistGlobalDragListening, { passive: false })
}

const stopPlaylistGlobalDragListening = () => {
  document.removeEventListener('dragover', handlePlaylistGlobalDragOver)
  document.removeEventListener('dragend', stopPlaylistGlobalDragListening)
  document.removeEventListener('drop', stopPlaylistGlobalDragListening)
  clearAutoScroll()
}

const handlePlaylistGlobalDragOver = (event: DragEvent) => {
  try {
    event.preventDefault()
    handlePlaylistDragOverScroll(event)
  } catch (error) {
    console.error('MusicPage handlePlaylistGlobalDragOver error:', error)
  }
}

// 歌单拖拽滚动处理
const handlePlaylistDragOverScroll = (event: DragEvent) => {
  if (!playlistScrollContainer.value) return
  
  const container = playlistScrollContainer.value
  const rect = container.getBoundingClientRect()
  const y = event.clientY
  
  // 检查是否在容器内
  if (y < rect.top || y > rect.bottom) {
    clearAutoScroll()
    return
  }
  
  const triggerZone = 50 // 触发区域大小
  const scrollZoneTop = rect.top + triggerZone
  const scrollZoneBottom = rect.bottom - triggerZone
  
  let newScrollSpeed = 0
  
  if (y < scrollZoneTop) {
    // 向上滚动
    const intensity = (scrollZoneTop - y) / triggerZone
    newScrollSpeed = -Math.max(2, Math.min(15, intensity * 15))
  } else if (y > scrollZoneBottom) {
    // 向下滚动
    const intensity = (y - scrollZoneBottom) / triggerZone
    newScrollSpeed = Math.max(2, Math.min(15, intensity * 15))
  }
  
  if (newScrollSpeed !== scrollSpeed.value) {
    scrollSpeed.value = newScrollSpeed
    
    if (autoScrollTimer.value) {
      clearInterval(autoScrollTimer.value)
      autoScrollTimer.value = null
    }
    
    if (scrollSpeed.value !== 0) {
      autoScrollTimer.value = window.setInterval(() => {
        if (container) {
          container.scrollTop += scrollSpeed.value
        }
      }, 16) // 60fps
    }
  }
}

// 组件卸载时清理定时器
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    // TODO: 清理资源
  console.log('页面卸载，清理音乐资源')
    clearAutoScroll()
  })
}

// 新建歌单的方法
const createNewPlaylist = async () => {
  const playlistName = newPlaylistName.value.trim()
  if (!playlistName) return

  try {
    // 首先加载文件夹层级以获取系统文件夹信息
    await driveStore.loadFolderHierarchy()

    console.log('所有文件夹层级:', driveStore.folderHierarchy)
    const systemFolders = driveStore.folderHierarchy.filter(f => f.folder_type === 'system')
    console.log('系统文件夹:', systemFolders)
    console.log('系统文件夹名称:', systemFolders.map(f => f.folder_name))

    // 查找"歌单"系统文件夹
    const musicSystemFolder = driveStore.folderHierarchy.find(
      folder => folder.folder_type === 'system' && folder.folder_name === '歌单'
    )

    console.log('找到的歌单文件夹:', musicSystemFolder)

    if (!musicSystemFolder) {
      console.error('未找到歌单系统文件夹，可用的系统文件夹:', driveStore.folderHierarchy.filter(f => f.folder_type === 'system'))

      // 尝试从根目录加载文件夹
      await driveStore.loadFolderContent(0)
      const rootMusicFolder = driveStore.folders.find(
        folder => folder.folder_type === 'system' && folder.folder_name === '歌单'
      )

      if (rootMusicFolder) {
        console.log('从根目录找到歌单文件夹:', rootMusicFolder)
        // 使用找到的文件夹继续创建
        await API.folder.createFolder({
          name: playlistName,
          parent_id: rootMusicFolder.id,
          folder_type: FolderTypeEnum.PLAYLIST
        })

        toast.success(`歌单 "${playlistName}" 创建成功`)
        await musicStore.loadPlaylistsFromDrive()
        showCreatePlaylistDialog.value = false
        newPlaylistName.value = ''
        return
      }

      toast.error('未找到歌单系统文件夹')
      return
    }

    // 直接调用API创建歌单文件夹
    await API.folder.createFolder({
      name: playlistName,
      parent_id: musicSystemFolder.id,
      folder_type: FolderTypeEnum.PLAYLIST
    })

    toast.success(`歌单 "${playlistName}" 创建成功`)
    // 重新加载音乐数据以显示新创建的歌单
    await musicStore.loadPlaylistsFromDrive()
  } catch (error) {
    console.error('创建歌单失败:', error)
    toast.error('创建歌单失败')
  }

  showCreatePlaylistDialog.value = false
  newPlaylistName.value = ''
}

// 上传音乐文件的方法
const uploadMusicFiles = async () => {
  if (selectedMusicFiles.value.length === 0) return

  // 获取当前歌单的路径，如果没有则上传到歌单根目录
  const currentPlaylist = musicStore.currentPlaylist
  const targetPath = currentPlaylist ? `/歌单/${currentPlaylist.folder_name}` : '/歌单'

  // 确保WebSocket连接已建立
  ensureWebSocketForUpload()

  // 使用 TransferStore 处理上传
  const fileCount = selectedMusicFiles.value.length
  await transferStore.uploadFiles(selectedMusicFiles.value, targetPath)

  showUploadMusicDialog.value = false
  selectedMusicFiles.value = []
  toast.success(`已开始上传 ${fileCount} 个音乐文件`)

  // 监听上传完成事件，刷新歌单数据
  const unsubscribe = onUploadComplete(() => {
    musicStore.loadPlaylistsFromDrive()
    unsubscribe() // 取消监听
  })
}

// 处理音乐文件选择
const handleMusicFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || []).filter(file => {
    const ext = file.name.split('.').pop()?.toLowerCase()
    return ['mp3', 'wav', 'flac', 'aac', 'm4a', 'ogg'].includes(ext || '')
  })
  selectedMusicFiles.value = files
}

// 歌曲右键菜单相关方法
const handleTrackContextMenu = (event: MouseEvent, track: MusicFileInfo) => {
  event.preventDefault()
  selectedTrack.value = track
  
  const menuItems: ContextMenuItem[] = [
    {
      id: 'open',
      label: '播放',
      action: () => openTrack()
    },
    {
      id: 'rename',
      label: '重命名',
      action: () => showRenameTrackDialogFn()
    },
    {
      id: 'separator1',
      separator: true
    },
    {
      id: 'delete',
      label: '删除',
      type: 'danger',
      action: () => deleteTrack()
    }
  ]
  
  contextMenuStore.showContextMenu(event, menuItems, track)
}

const openTrack = () => {
  if (selectedTrack.value) {
    // 找到当前歌单中的索引
    const index = musicStore.currentPlaylistTracks.findIndex((t: MusicFileInfo) => t.id === selectedTrack.value?.id) ?? -1
    if (index !== -1) {
      playTrackImmediately(index)
    }
  }
}

const showRenameTrackDialogFn = () => {
  if (selectedTrack.value) {
    renameTrackValue.value = selectedTrack.value.file_name?.replace(/\.[^/.]+$/, '') || ''
    showRenameTrackDialog.value = true
    
    // 隐藏右键菜单
    contextMenuStore.hideContextMenu()
    
    // 在下一个tick自动聚焦并选中文本
    nextTick(() => {
      if (renameTrackInput.value) {
        renameTrackInput.value.focus()
        renameTrackInput.value.select()
      }
    })
  }
}

const confirmRenameTrack = () => {
  const newName = renameTrackValue.value.trim()
  if (!newName || !selectedTrack.value) return

  // 检查名称是否与原名称相同
  if (newName === (selectedTrack.value.file_name?.replace(/\.[^/.]+$/, '') || '')) {
    cancelRenameTrack()
    return
  }

  // 检查名称是否包含非法字符
  const invalidChars = /[<>:"/\\|?*]/
  if (invalidChars.test(newName)) {
    toast.error('文件名不能包含以下字符：< > : " / \\ | ? *')
    return
  }

  const oldName = selectedTrack.value.file_name?.replace(/\.[^/.]+$/, '') || ''
  
  // 调用MusicStore的重命名方法（如果有的话，或者直接修改文件名）
  if (selectedTrack.value.file_name) {
    const ext = selectedTrack.value.file_name.split('.').pop()
    selectedTrack.value.file_name = ext ? `${newName}.${ext}` : newName
  }
  
  showRenameTrackDialog.value = false
  renameTrackValue.value = ''
  selectedTrack.value = null
  toast.success(`"${oldName}" 已重命名为 "${newName}"`)
}

const cancelRenameTrack = () => {
  showRenameTrackDialog.value = false
  renameTrackValue.value = ''
  selectedTrack.value = null
}

const deleteTrack = () => {
  if (selectedTrack.value) {
    const trackName = selectedTrack.value.file_name?.replace(/\.[^/.]+$/, '') || '未知音乐'
    if (confirm(`确定要删除音乐 "${trackName}" 吗？`)) {
      // TODO: 实现删除歌曲
      console.log('TODO: 删除歌曲', selectedTrack.value.id)
      toast.success(`音乐 "${trackName}" 已删除`)
    }
  }
}

// 歌单右键菜单相关方法
const handlePlaylistContextMenu = (event: MouseEvent, playlist: any) => { // Playlist type is not directly imported, use 'any' for now
  selectedPlaylist.value = playlist
  
  const menuItems: ContextMenuItem[] = [
    {
      id: 'open',
      label: '打开',
      action: () => openPlaylist()
    },
    {
      id: 'rename',
      label: '重命名',
      action: () => showRenamePlaylistDialogFn()
    },
    {
      id: 'separator1',
      separator: true
    },
    {
      id: 'delete',
      label: '删除',
      type: 'danger',
      action: () => deletePlaylist()
    }
  ]
  
  contextMenuStore.showContextMenu(event, menuItems, playlist)
}

const selectPlaylist = async (playlist: any) => {
  console.log('点击歌单:', playlist)
  console.log('选择前的数据状态:', {
    allMusicFiles: musicStore.allMusicFiles.length,
    currentPlaylist: musicStore.currentPlaylist
  })

  await musicStore.selectPlaylist(playlist)

  console.log('选择后的数据状态:', {
    allMusicFiles: musicStore.allMusicFiles.length,
    currentPlaylist: musicStore.currentPlaylist,
    currentPlaylistTracks: musicStore.currentPlaylistTracks.length
  })
}

const openPlaylist = () => {
  if (selectedPlaylist.value) {
    selectPlaylist(selectedPlaylist.value)
  }
}

const showRenamePlaylistDialogFn = () => {
  if (selectedPlaylist.value) {
    renamePlaylistValue.value = selectedPlaylist.value.folder_name
    showRenamePlaylistDialog.value = true
    
    // 在下一个tick自动聚焦并选中文本
    nextTick(() => {
      if (renamePlaylistInput.value) {
        renamePlaylistInput.value.focus()
        renamePlaylistInput.value.select()
      }
    })
  }
}

const confirmRenamePlaylist = () => {
  const newName = renamePlaylistValue.value.trim()
  if (!newName || !selectedPlaylist.value) return

  // 检查名称是否与原名称相同
  if (newName === selectedPlaylist.value.folder_name) {
    cancelRenamePlaylist()
    return
  }

  // 检查名称是否包含非法字符
  const invalidChars = /[<>:"/\\|?*]/
  if (invalidChars.test(newName)) {
    toast.error('文件名不能包含以下字符：< > : " / \\ | ? *')
    return
  }

  const oldName = selectedPlaylist.value.folder_name
  
  // 调用MusicStore的重命名方法（如果有的话，或者直接修改）
  selectedPlaylist.value.folder_name = newName
  
  showRenamePlaylistDialog.value = false
  renamePlaylistValue.value = ''
  toast.success(`歌单 "${oldName}" 已重命名为 "${newName}"`)
}

const cancelRenamePlaylist = () => {
  showRenamePlaylistDialog.value = false
  renamePlaylistValue.value = ''
}

const deletePlaylist = () => {
  if (selectedPlaylist.value) {
    const playlistName = selectedPlaylist.value.folder_name
    if (confirm(`确定要删除歌单 "${playlistName}" 吗？`)) {
      // TODO: 实现删除播放列表
      console.log('TODO: 删除播放列表', selectedPlaylist.value.id)
      toast.success(`歌单 "${playlistName}" 已删除`)
    }
  }
}
</script>

<style scoped>
/* 拖动状态的样式 */
.ghost {
  opacity: 0.5;
  background: #c8f5e9;
  border: 2px dashed #14b8a6;
}

.chosen {
  opacity: 0.8;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.drag {
  opacity: 0.8;
  transform: rotate(2deg);
}

/* 拖动过渡动画 */
.flip-list-move {
  transition: transform 0.3s;
}

/* 进度条拖拽滑块样式增强 */
.cursor-pointer:hover .absolute {
  opacity: 1 !important;
}

/* 移动端触摸友好的滑块 */
@media (max-width: 640px) {
  .absolute.w-4.h-4 {
    width: 20px;
    height: 20px;
    opacity: 0.8 !important;
  }
}
</style> 