<template>
  <div class="min-h-full bg-liteisle-bg p-6">
    <div class="max-w-7xl mx-auto">
      <div class="h-[calc(100vh-10rem)]">
        <!-- 主要内容区域 -->
        <div class="card h-full flex flex-col">
          <!-- 面包屑导航 -->
          <nav class="flex items-center gap-2 text-sm mb-6 bg-gradient-to-r from-teal-50 to-blue-50 px-4 py-3 rounded-lg border border-teal-100 shadow-sm" @click="hideContextMenu">
            <div class="font-semibold text-morandi-800 mr-2 flex-shrink-0">导航栏:</div>
            <div
              v-for="(path, index) in breadcrumbPaths"
              :key="index"
              class="relative inline-flex"
            >
              <button
                @click.stop="() => { hideContextMenu(); hideAllLeftClickMenus(); navigateToPath(index); }"
                class="flex items-center gap-1 px-3 py-1 rounded-md hover:bg-white/80 transition-all duration-200 shadow-sm"
                :class="{
                  'text-teal-700 font-semibold bg-white shadow-md': index === breadcrumbPaths.length - 1,
                  'text-morandi-600 hover:text-teal-600': index !== breadcrumbPaths.length - 1
                }"
              >
                <span>{{ path.name }}</span>
                <ChevronRight v-if="index < breadcrumbPaths.length - 1" :size="16" class="text-morandi-400" />
              </button>


            </div>
          </nav>

          <!-- 文件列表头部 -->
          <div class="mb-6 flex items-center">
            <!-- 左侧区域：标题和统计 -->
            <div class="w-72">
              <h2 class="text-xl font-bold text-morandi-900">
                {{ getCurrentLevelTitle() }}
              </h2>
              <span class="text-sm text-morandi-500">
                {{ filteredItems.length }} 个项目
              </span>
            </div>
            
            <!-- 中间区域：刷新按钮 + 搜索框和控制按钮 -->
            <div class="flex-1 flex justify-center items-center gap-6">
              <button
                @click.stop="refreshAndHide"
                :disabled="isRefreshing"
                class="p-2 rounded-lg border border-morandi-300 text-morandi-600 hover:bg-morandi-50 hover:border-morandi-400 transition-all duration-200 disabled:opacity-50"
                title="刷新"
              >
                <RotateCw :size="18" :class="{ 'animate-spin': isRefreshing }" />
              </button>
              <input
                v-model="driveStore.searchQuery"
                :placeholder="driveStore.isInRecycleBin ? '搜索回收站...' : '搜索文件...'"
                class="px-4 py-2 w-80 rounded-lg border border-morandi-300 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent select-text"
                style="user-select: text !important;"
              />
              
              <!-- 排序按钮 -->
              <div class="relative">
                <button
                  @click.stop="() => { hideContextMenu(); toggleSortMenu(); }"
                  class="flex items-center gap-1 px-3 py-2 rounded-lg border border-morandi-300 text-morandi-700 text-sm bg-white hover:bg-morandi-50 focus:outline-none focus:ring-2 focus:ring-teal-500 transition-colors"
                  title="排序选项"
                >
                  <ListOrdered :size="20" />
                  <ChevronRight :size="12" :class="{ 'rotate-90': showSortMenu }" class="transition-transform" />
                </button>
                
                <!-- 排序菜单 -->
                <div
                  v-if="showSortMenu"
                  class="absolute top-full left-0 mt-1 bg-white rounded-lg shadow-lg border border-morandi-200 py-2 min-w-[160px] z-50"
                  @click.stop
                >
                  <button
                    v-for="option in sortOptions"
                    :key="option.value"
                    @click.stop="selectSort(option.value)"
                    :class="[
                      'w-full px-4 py-2 text-left text-sm hover:bg-morandi-50 flex items-center gap-2 transition-colors',
                      sortBy === option.value ? 'text-teal-600 bg-teal-50' : 'text-morandi-700'
                    ]"
                  >
                     <span>{{ option.label }}</span>
                     <ChevronRight v-if="sortBy === option.value" :size="14" class="ml-auto text-teal-600" />
                  </button>

                  <hr class="my-2 border-morandi-200">

                  <!-- 排序方向 -->
                  <button
                    @click.stop="toggleSortOrder"
                    class="w-full px-4 py-2 text-left text-sm hover:bg-morandi-50 flex items-center gap-2 transition-colors text-morandi-700"
                  >
                    <span>{{ sortOrder === 'ASC' ? '升序' : '降序' }}</span>
                    <ChevronRight :size="14" class="ml-auto" :class="{ 'rotate-180': sortOrder === 'DESC' }" />
                  </button>
                </div>
              </div>
              
              <!-- 视图切换按钮 -->
              <div class="flex items-center border border-morandi-300 rounded-lg">
                <button
                  @click.stop="() => { hideContextMenu(); hideAllLeftClickMenus(); viewMode = 'grid'; }"
                  :class="[
                    'px-3 py-2 text-sm transition-colors',
                    viewMode === 'grid'
                      ? 'bg-teal-500 text-white'
                      : 'text-morandi-600 hover:bg-morandi-50'
                  ]"
                  class="rounded-l-lg"
                  title="网格视图"
                >
                  <Grid2x2 :size="16" />
                </button>
                <button
                  @click.stop="() => { hideContextMenu(); hideAllLeftClickMenus(); viewMode = 'list'; }"
                  :class="[
                    'px-3 py-2 text-sm transition-colors',
                    viewMode === 'list'
                      ? 'bg-teal-500 text-white'
                      : 'text-morandi-600 hover:bg-morandi-50'
                  ]"
                  class="rounded-r-lg"
                  title="列表视图"
                >
                  <Logs :size="16" />
                </button>
              </div>
            </div>
            
            <!-- 右侧区域：固定宽度确保位置一致 -->
            <div class="w-24 flex justify-end">
              <!-- 正常模式：回收站按钮 -->
              <button
                v-if="!driveStore.isInRecycleBin"
                @click.stop="() => { hideContextMenu(); hideAllLeftClickMenus(); openRecycleBin(); }"
                @dragover.prevent="handleTrashDragOver"
                @dragleave="handleTrashDragLeave"
                @drop="handleTrashDrop"
                class="flex items-center justify-center w-10 h-10 rounded-lg border transition-all duration-200"
                :class="[
                  isDraggingOverTrash ? 'border-red-500 bg-red-50 text-red-600 scale-110' : 'border-red-300 text-red-600 hover:bg-red-50 hover:border-red-400'
                ]"
                title="回收站"
              >
                <Trash2 :size="20" />
              </button>
              
              <!-- 回收站模式：操作按钮 -->
              <div v-if="driveStore.isInRecycleBin" class="flex items-center gap-2">
                <button
                  @click.stop="() => { hideContextMenu(); hideAllLeftClickMenus(); restoreAllItems(); }"
                  class="flex items-center justify-center w-9 h-9 rounded-lg border border-green-300 text-green-600 hover:bg-green-50 hover:border-green-400 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="一键还原"
                  :disabled="driveStore.recycleBinItems.length === 0"
                >
                  <RefreshCcw :size="16" />
                </button>
                <button
                  @click.stop="() => { hideContextMenu(); hideAllLeftClickMenus(); deleteAllItems(); }"
                  class="flex items-center justify-center w-9 h-9 rounded-lg border border-red-300 text-red-600 hover:bg-red-50 hover:border-red-400 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="一键删除"
                  :disabled="driveStore.recycleBinItems.length === 0"
                >
                  <Shredder :size="16" />
                </button>
              </div>
            </div>
          </div>

                        <!-- 内容视图 -->
          <div
            class="flex-1 overflow-auto relative"
            @contextmenu.prevent="showEmptyContextMenu"
            @mousedown="startSelection"
            ref="scrollContainer"
          >
            <!-- 选择框 -->
            <div
              v-if="isSelecting"
              class="absolute border-2 border-blue-500 bg-blue-100/20 pointer-events-none z-10"
              :style="{
                left: `${Math.min(selectionStart.x, selectionEnd.x)}px`,
                top: `${Math.min(selectionStart.y, selectionEnd.y)}px`,
                width: `${Math.abs(selectionEnd.x - selectionStart.x)}px`,
                height: `${Math.abs(selectionEnd.y - selectionStart.y)}px`
              }"
            ></div>

            <!-- 网格视图 -->
            <div v-if="viewMode === 'grid'" class="grid grid-cols-6 gap-4 pb-4">
              <!-- 现有文件和文件夹 -->
              <div
                v-for="item in filteredItems"
                :key="item.id"
                :data-item-id="item.id"
                class="item-card p-4 rounded-lg border-2 transition-all duration-200 cursor-pointer relative"
                :class="getDragOverClass(item)"
                @click="handleItemClick(item, $event)"
                @dblclick="handleItemDoubleClick(item)"
                @contextmenu.prevent="showContextMenu($event, item)"
                :draggable="!driveStore.isInRecycleBin && !item.isLocked"
                @dragstart="handleDragStart($event, item)"
                @dragover.prevent="handleDragOver($event, item)"
                @dragleave="handleDragLeave"
                @drop="handleDrop($event, item)"
              >
                <!-- 多选框 - 只在选中时显示 -->
                <div 
                  v-if="selectedItemIds.has(item.id)" 
                  class="absolute top-2 left-2 z-10 pointer-events-auto"
                >
                  <div class="w-5 h-5 bg-teal-500 rounded-full flex items-center justify-center shadow-md">
                    <svg class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
                    </svg>
                  </div>
                </div>

                <div class="flex flex-col items-center pointer-events-none">
                  <div class="w-12 h-12 mb-3 flex items-center justify-center">
                    <FileMusic v-if="item.type === 'folder' && getCurrentLevel() === 0 && (item.name === '歌单' || item.name === '音乐')" :size="48" class="text-blue-500" />
                    <LibraryBig v-else-if="item.type === 'folder' && getCurrentLevel() === 0 && (item.name === '书单' || item.name === '文档')" :size="48" class="text-red-500" />
                    <FileUp v-else-if="item.type === 'folder' && getCurrentLevel() === 0 && item.name === '上传'" :size="48" class="text-green-500" />
                    <Share2 v-else-if="item.type === 'folder' && getCurrentLevel() === 0 && item.name === '分享'" :size="48" class="text-purple-500" />
                    <FolderClosed v-else-if="item.type === 'folder'" :size="48" class="text-blue-500" />
                    <Music v-else-if="item.type === 'audio'" :size="48" class="text-green-500" />
                    <FileText v-else :size="48" class="text-morandi-500" />
                  </div>

                  <p class="text-sm text-center font-medium text-morandi-900 truncate w-full">
                    {{ item.name }}
                  </p>

                  <p class="text-xs text-morandi-500 mt-1">
                    <template v-if="driveStore.isInRecycleBin && viewMode === 'list'">
                      <span class="block"> {{ getDeleteInfo(item).dateStr }}</span>
                      <span class="block text-red-500 text-xs">{{ getDeleteInfo(item).daysLeft }} 天后清除</span>
                    </template>
                    <template v-else>
                      <span v-if="item.type !== 'folder'">{{ formatFileSize(item.size) }}</span>
                      <span v-else>{{ item.itemCount }} 项</span>
                    </template>
                  </p>
                </div>
              </div>

              <!-- 新建文件夹项（只在第一级系统文件夹内显示，非回收站模式） -->
              <div
                v-if="!driveStore.isInRecycleBin && !driveStore.searchQuery && getCurrentLevel() === 1"
                @click="createNewFolder"
                class="p-4 rounded-lg border-2 border-dashed border-morandi-300 hover:border-teal-400 hover:bg-teal-50 transition-all duration-200 cursor-pointer"
              >
                <div class="flex flex-col items-center">
                  <div class="w-12 h-12 mb-3 flex items-center justify-center">
                    <FolderClosed :size="48" class="text-teal-500" />
                  </div>

                  <p class="text-sm text-center font-medium text-morandi-600">
                    新建文件夹
                  </p>

                  <p class="text-xs text-morandi-400 mt-1">
                    点击创建
                  </p>
                </div>
              </div>

              <!-- 上传文件项（仅在第二级显示，非回收站模式） -->
              <div
                v-if="!driveStore.isInRecycleBin && !driveStore.searchQuery && getCurrentLevel() === 2"
                @click.stop="uploadFiles"
                class="p-4 rounded-lg border-2 border-dashed border-green-300 hover:border-green-400 hover:bg-green-50 transition-all duration-200 cursor-pointer"
              >
                <div class="flex flex-col items-center">
                  <div class="w-12 h-12 mb-3 flex items-center justify-center">
                    <Upload :size="48" class="text-green-500" />
                  </div>

                  <p class="text-sm text-center font-medium text-green-600">
                    上传文件
                  </p>

                  <p class="text-xs text-green-400 mt-1">
                    点击上传
                  </p>
                </div>
              </div>
            </div>

            <!-- 列表视图 -->
            <div v-if="viewMode === 'list'" class="space-y-2 pb-4">
              <!-- 列表头部 -->
              <div class="flex items-center px-4 py-2 bg-morandi-50 rounded-lg text-sm font-medium text-morandi-600">
                <div class="flex-1">名称</div>
                <div class="w-20 text-right" v-if="!driveStore.isInRecycleBin">大小</div>
                <div class="w-20 text-right" v-else>大小</div>
                <div class="w-32 text-right" v-if="!driveStore.isInRecycleBin">修改时间</div>
                <div class="w-32 text-right" v-else>删除时间</div>
                <div class="w-32 text-right" v-if="!driveStore.isInRecycleBin">创建时间</div>
                <div class="w-32 text-right" v-else>清除时间</div>
              </div>
              
              <!-- 现有文件和文件夹 -->
              <div
                v-for="item in filteredItems"
                :key="item.id"
                :data-item-id="item.id"
                class="item-card flex items-center px-4 py-3 rounded-lg border transition-all duration-200 cursor-pointer"
                :class="getDragOverClass(item, true)"
                @click="handleItemClick(item, $event)"
                @dblclick="handleItemDoubleClick(item)"
                @contextmenu.prevent="showContextMenu($event, item)"
                :draggable="!driveStore.isInRecycleBin && !item.isLocked"
                @dragstart="handleDragStart($event, item)"
                @dragover.prevent="handleDragOver($event, item)"
                @dragleave="handleDragLeave"
                @drop="handleDrop($event, item)"
              >
                <!-- 多选框 - 只在选中时显示 -->
                <div class="mr-3 pointer-events-auto w-5 flex justify-center">
                  <div 
                    v-if="selectedItemIds.has(item.id)"
                    class="w-4 h-4 bg-teal-500 rounded-full flex items-center justify-center shadow-sm"
                  >
                    <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path>
                    </svg>
                  </div>
                </div>

                <!-- 图标和名称 -->
                <div class="flex items-center flex-1 gap-3 pointer-events-none">
                  <div class="w-8 h-8 flex items-center justify-center">
                    <FileMusic v-if="item.type === 'folder' && getCurrentLevel() === 0 && (item.name === '歌单' || item.name === '音乐')" :size="20" class="text-blue-500" />
                    <LibraryBig v-else-if="item.type === 'folder' && getCurrentLevel() === 0 && (item.name === '书单' || item.name === '文档')" :size="20" class="text-red-500" />
                    <FileUp v-else-if="item.type === 'folder' && getCurrentLevel() === 0 && item.name === '上传'" :size="20" class="text-green-500" />
                    <Share2 v-else-if="item.type === 'folder' && getCurrentLevel() === 0 && item.name === '分享'" :size="20" class="text-purple-500" />
                    <FolderClosed v-else-if="item.type === 'folder'" :size="20" class="text-blue-500" />
                    <Music v-else-if="item.type === 'audio'" :size="20" class="text-green-500" />
                    <FileText v-else :size="20" class="text-morandi-500" />
                  </div>
                  <span class="text-sm font-medium text-morandi-900 truncate">{{ item.name }}</span>
                </div>
                
                <!-- 文件大小 / 删除信息 -->
                <div class="w-20 text-right text-xs text-morandi-500">
                  <template v-if="driveStore.isInRecycleBin">
                    <span>{{ formatFileSize(item.size) }}</span>
                  </template>
                  <template v-else>
                    <span v-if="item.type !== 'folder'">{{ formatFileSize(item.size) }}</span>
                    <span v-else>{{ item.itemCount }} 项</span>
                  </template>
                </div>
                
                <!-- 修改/删除时间 -->
                <div class="w-32 text-right text-xs text-morandi-500">
                  <template v-if="driveStore.isInRecycleBin"> {{ getDeleteInfo(item).dateStr }}</template>
                  <template v-else>{{ formatDate(item.modifiedAt) }}</template>
                </div>
                
                <!-- 创建/清除时间 -->
                <div class="w-32 text-right text-xs text-morandi-500">
                  <template v-if="driveStore.isInRecycleBin">{{ getDeleteInfo(item).daysLeft }} 天后清除</template>
                  <template v-else>{{ formatDate(item.createdAt || item.modifiedAt) }}</template>
                </div>
              </div>
              
              <!-- 新建文件夹项（列表模式，只在第一级系统文件夹内显示） -->
              <div
                v-if="!driveStore.isInRecycleBin && !driveStore.searchQuery && getCurrentLevel() === 1"
                @click="createNewFolder"
                class="flex items-center px-4 py-3 rounded-lg border-2 border-dashed border-morandi-300 hover:border-teal-400 hover:bg-teal-50 transition-all duration-200 cursor-pointer"
              >
                <div class="flex items-center flex-1 gap-3">
                  <div class="w-8 h-8 flex items-center justify-center">
                    <FolderClosed :size="20" class="text-teal-500" />
                  </div>
                  <span class="text-sm font-medium text-morandi-600">新建文件夹</span>
                </div>
                <div class="w-20 text-right text-xs text-morandi-400">-</div>
                <div class="w-32 text-right text-xs text-morandi-400">-</div>
                <div class="w-32 text-right text-xs text-morandi-400">-</div>
              </div>
              
              <!-- 上传文件项（列表模式） -->
              <div
                v-if="!driveStore.isInRecycleBin && !driveStore.searchQuery && getCurrentLevel() === 2"
                @click="uploadFiles"
                class="flex items-center px-4 py-3 rounded-lg border-2 border-dashed border-green-300 hover:border-green-400 hover:bg-green-50 transition-all duration-200 cursor-pointer"
              >
                <div class="flex items-center flex-1 gap-3">
                  <div class="w-8 h-8 flex items-center justify-center">
                    <FileText :size="20" class="text-green-500" />
                  </div>
                  <span class="text-sm font-medium text-green-600">上传文件</span>
                </div>
                <div class="w-20 text-right text-xs text-green-400">-</div>
                <div class="w-32 text-right text-xs text-green-400">-</div>
                <div class="w-32 text-right text-xs text-green-400">-</div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredItems.length === 0 && driveStore.searchQuery" class="flex-1 flex items-center justify-center">
              <div class="text-center">
                <div class="w-16 h-16 bg-morandi-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <FolderClosed :size="32" class="text-morandi-400" />
                </div>
                <h3 class="text-lg font-medium text-morandi-700 mb-2">
                  {{ driveStore.isInRecycleBin ? '回收站中未找到匹配的文件' : '未找到匹配的文件' }}
                </h3>
                <p class="text-morandi-500">
                  尝试其他搜索词
                </p>
              </div>
            </div>

            <!-- 完全空状态（无文件且无搜索） -->
            <div v-if="filteredItems.length === 0 && !driveStore.searchQuery" class="flex-1 flex items-center justify-center">
              <div class="text-center">
                <div class="w-16 h-16 bg-morandi-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <FolderClosed :size="32" class="text-morandi-400" />
                </div>
                <h3 class="text-lg font-medium text-morandi-700 mb-2">
                  {{ driveStore.isInRecycleBin ? '回收站为空' : (getCurrentLevel() === 0 ? '云盘为空' : '文件夹为空') }}
                </h3>
                <p class="text-morandi-500">
                  {{ driveStore.isInRecycleBin ? '没有已删除的文件' : getEmptyStateMessage() }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建文件夹对话框 -->
    <div v-if="showCreateFolderDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="showCreateFolderDialog = false">
      <div class="bg-white rounded-lg p-6 w-96" @click.stop>
        <h3 class="text-lg font-bold mb-4">新建文件夹</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-2">文件夹名称</label>
            <input
              v-model="newFolderName"
              type="text"
              placeholder="请输入文件夹名称"
              class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
              @keydown.enter="confirmCreateFolder"
              style="user-select: text !important; pointer-events: auto !important;"
              ref="folderNameInput"
            />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="showCreateFolderDialog = false"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="confirmCreateFolder"
            :disabled="!newFolderName.trim()"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            创建
          </button>
        </div>
      </div>
    </div>

    <!-- 上传文件对话框 -->
    <div v-if="showUploadDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelUpload">
      <div class="bg-white rounded-lg p-6 w-96" @click.stop>
        <h3 class="text-lg font-bold mb-4">上传文件</h3>
        <div class="space-y-4">
          <div class="border-2 border-dashed border-morandi-300 rounded-lg p-8 text-center">
            <FileText :size="32" class="mx-auto mb-3 text-morandi-400" />
            <p class="text-morandi-600 mb-2">点击选择文件或拖拽到此处</p>
            <p class="text-xs text-morandi-400">支持音频、文档等多种格式</p>
            <input 
              type="file" 
              multiple 
              class="hidden" 
              ref="fileInput"
              @change="handleFileSelect"
            />
            <button 
              @click="() => (fileInput as HTMLInputElement | null)?.click()"
              class="mt-3 px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors"
            >
              选择文件
            </button>
          </div>
          
          <!-- 文件列表 -->
          <div v-if="selectedFiles.length > 0" class="max-h-32 overflow-auto">
            <h4 class="text-sm font-medium text-morandi-700 mb-2">待上传文件：</h4>
            <div class="space-y-1">
              <div 
                v-for="(file, index) in selectedFiles" 
                :key="index"
                class="flex items-center justify-between text-xs bg-morandi-50 p-2 rounded"
              >
                <span class="truncate flex-1">{{ file.name }}</span>
                <span class="text-morandi-500 ml-2">{{ formatFileSize(file.size) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="cancelUpload"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="confirmUpload"
            :disabled="selectedFiles.length === 0"
            class="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            上传 ({{ selectedFiles.length }})
          </button>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div 
      v-if="showContextMenuState" 
      :style="{ top: contextMenuPosition.y + 'px', left: contextMenuPosition.x + 'px' }"
      class="fixed z-50 bg-white rounded-lg shadow-lg border border-morandi-200 py-2 min-w-[120px]"
      @click.stop="preventHide"
    >
        <!-- 统一的右键菜单 - 根据选中状态动态调整 -->
        <template v-if="selectedItem || selectedItemIds.size > 0">
          <!-- 回收站模式 -->
          <template v-if="driveStore.isInRecycleBin">
            <button
              @click.stop="selectedItemIds.size === 1 ? restoreItem() : restoreMultipleItems()"
              class="w-full px-4 py-2 text-left text-sm text-green-600 hover:bg-green-50 flex items-center gap-2"
            >
              恢复{{ selectedItemIds.size > 1 ? ` (${selectedItemIds.size})` : '' }}
            </button>
            <button
              @click.stop="handleDeleteClick"
              class="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
            >
              删除{{ selectedItemIds.size > 1 ? ` (${selectedItemIds.size})` : '' }}
            </button>
          </template>
          
          <!-- 云盘正常模式 -->
          <template v-else>
            <!-- 打开 - 多选时禁用 -->
            <button
              :disabled="selectedItemIds.size > 1"
              @click.stop="handleOpenClick"
              :class="[
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2',
                selectedItemIds.size > 1 
                  ? 'text-morandi-400 cursor-not-allowed' 
                  : 'text-morandi-700 hover:bg-morandi-50'
              ]"
            >
              打开
            </button>
            
            <!-- 下载 - 支持多选 -->
            <button
              @click.stop="handleDownloadClick"
              class="w-full px-4 py-2 text-left text-sm text-morandi-700 hover:bg-morandi-50 flex items-center gap-2"
            >
              下载{{ selectedItemIds.size > 1 ? ` (${selectedItemIds.size})` : '' }}
            </button>
            
            <!-- 分享 - 多选时禁用，一级文件夹也禁用 -->
            <button
              :disabled="selectedItemIds.size > 1 || isFirstLevelFolder(selectedItem)"
              @click.stop="handleShareClick"
              :class="[
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2',
                selectedItemIds.size > 1 || isFirstLevelFolder(selectedItem)
                  ? 'text-morandi-400 cursor-not-allowed' 
                  : 'text-morandi-700 hover:bg-morandi-50'
              ]"
            >
              分享
            </button>
            
            <hr class="my-1 border-morandi-200">
            
            <!-- 复制 - 支持多选，根目录不显示 -->
            <button
              v-if="getCurrentLevel() !== 0"
              @click.stop="selectedItemIds.size === 1 ? copyItem() : copyMultipleItems()"
              class="w-full px-4 py-2 text-left text-sm text-morandi-700 hover:bg-morandi-50 flex items-center gap-2"
            >
              复制{{ selectedItemIds.size > 1 ? ` (${selectedItemIds.size})` : '' }}
            </button>
            
            <!-- 剪切 - 支持多选，但锁定项目时禁用，根目录不显示 -->
            <button
              v-if="!hasLockedItems() && getCurrentLevel() !== 0"
              @click.stop="selectedItemIds.size === 1 ? cutItem() : cutMultipleItems()"
              class="w-full px-4 py-2 text-left text-sm text-morandi-700 hover:bg-morandi-50 flex items-center gap-2"
            >
              剪切{{ selectedItemIds.size > 1 ? ` (${selectedItemIds.size})` : '' }}
            </button>
            
            <!-- 粘贴 - 多选时禁用 -->
            <button
              v-if="clipboard.length && clipboardAction"
              :disabled="selectedItemIds.size > 1"
              @click.stop="selectedItemIds.size === 1 ? pasteItem() : null"
              :class="[
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2',
                selectedItemIds.size > 1 
                  ? 'text-morandi-400 cursor-not-allowed' 
                  : 'text-morandi-700 hover:bg-morandi-50'
              ]"
            >
              粘贴
            </button>
            
            <hr class="my-1 border-morandi-200" v-if="!hasLockedItems() && getCurrentLevel() !== 0">
            
            <!-- 删除 - 支持多选，但锁定项目时禁用，根目录不显示 -->
            <button
              v-if="!hasLockedItems() && getCurrentLevel() !== 0"
              @click.stop="selectedItemIds.size === 1 ? showDeleteConfirm() : deleteMultipleItems()"
              class="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
            >
              删除{{ selectedItemIds.size > 1 ? ` (${selectedItemIds.size})` : '' }}
            </button>
            
            <!-- 重命名 - 多选时禁用，根目录不显示 -->
            <button
              v-if="getCurrentLevel() !== 0"
              :disabled="selectedItemIds.size > 1"
              @click.stop="selectedItemIds.size === 1 ? showRenameDialog() : null"
              :class="[
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2',
                selectedItemIds.size > 1 
                  ? 'text-morandi-400 cursor-not-allowed' 
                  : 'text-morandi-700 hover:bg-morandi-50'
              ]"
            >
              重命名
            </button>
            
            <!-- 移动到 - 多选时禁用，根目录不显示 -->
            <button
              v-if="getCurrentLevel() !== 0"
              :disabled="selectedItemIds.size > 1"
              @click.stop="selectedItemIds.size === 1 ? showMoveDialog() : null"
              :class="[
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2',
                selectedItemIds.size > 1 
                  ? 'text-morandi-400 cursor-not-allowed' 
                  : 'text-morandi-700 hover:bg-morandi-50'
              ]"
            >
              移动到
            </button>
            
            <hr class="my-1 border-morandi-200">

            <!-- 文件夹类型 - 只对文件夹显示，多选时禁用 -->
            <div
              v-if="selectedItem && selectedItem.type === 'folder' && selectedItemIds.size === 1"
              class="w-full px-4 py-2 text-left text-sm text-morandi-600 bg-morandi-25 flex items-center gap-2 cursor-default"
            >
              <span class="text-xs">类型:</span>
              <span class="font-medium">{{ getFolderTypeDisplayName(selectedItem.folder_type) }}</span>
            </div>

            <!-- 详细信息 - 多选时禁用 -->
            <button
              :disabled="selectedItemIds.size > 1"
              @click.stop="selectedItemIds.size === 1 ? showItemDetails() : null"
              :class="[
                'w-full px-4 py-2 text-left text-sm flex items-center gap-2',
                selectedItemIds.size > 1
                  ? 'text-morandi-400 cursor-not-allowed'
                  : 'text-morandi-700 hover:bg-morandi-50'
              ]"
            >
              详细信息
            </button>
          </template>
        </template>

        <!-- 空白处右键菜单 -->
        <template v-else>
          <button
            v-if="clipboard.length && clipboardAction && getCurrentLevel() !== 0 && !driveStore.isInRecycleBin"
            @click.stop="pasteItem"
            class="w-full px-4 py-2 text-left text-sm text-morandi-700 hover:bg-morandi-50 flex items-center gap-2"
          >
            粘贴
          </button>
          <button
            v-if="getCurrentLevel() === 1 && !driveStore.isInRecycleBin"
            @click.stop="() => { hideContextMenu(); createNewFolder(); }"
            class="w-full px-4 py-2 text-left text-sm text-morandi-700 hover:bg-morandi-50 flex items-center gap-2"
          >
            新建文件夹
          </button>
          <button
            v-if="getCurrentLevel() === 2 && !driveStore.isInRecycleBin"
            @click.stop="() => { hideContextMenu(); uploadFiles(); }"
            class="w-full px-4 py-2 text-left text-sm text-morandi-700 hover:bg-morandi-50 flex items-center gap-2"
          >
            上传文件
          </button>
          <button
            @click.stop="refreshAndHide"
            class="w-full px-4 py-2 text-left text-sm text-morandi-700 hover:bg-morandi-50 flex items-center gap-2"
          >
            刷新
          </button>
        </template>
    </div>

    <!-- 重命名对话框 -->
    <div v-if="showRenameDialogState" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelRename">
      <div class="bg-white rounded-lg p-6 w-96" @click.stop>
        <h3 class="text-lg font-bold mb-4">重命名</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-2">新名称</label>
                          <input
              ref="renameInput"
              v-model="renameValue"
              type="text"
              placeholder="请输入新名称"
              class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
              @keydown.enter="confirmRename"
              @keydown.esc="cancelRename"
              style="user-select: text !important;"
            />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="cancelRename"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="confirmRename"
            :disabled="!renameValue.trim()"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            确认
          </button>
        </div>
      </div>
    </div>

    <!-- 移动对话框 -->
    <div v-if="showMoveDialogState" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelMove">
      <div class="bg-white rounded-lg p-6 w-96" @click.stop>
        <h3 class="text-lg font-bold mb-4">移动到</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-2">选择目标文件夹</label>
            <div class="max-h-64 overflow-auto border border-morandi-300 rounded-lg">
              <div 
                @click="selectMoveTarget('/')"
                :class="[
                  'px-4 py-2 cursor-pointer border-b border-morandi-100 hover:bg-morandi-50',
                  moveTargetPath === '/' ? 'bg-teal-50 text-teal-700' : ''
                ]"
              >
                <div class="flex items-center gap-2">
                  <FolderClosed :size="16" />
                  <span>根目录</span>
                </div>
              </div>
              <div 
                v-for="folder in availableFolders" 
                :key="folder.id"
                @click="selectMoveTarget(folder.path)"
                :class="[
                  'px-4 py-2 cursor-pointer border-b border-morandi-100 hover:bg-morandi-50',
                  moveTargetPath === folder.path ? 'bg-teal-50 text-teal-700' : ''
                ]"
              >
                <div class="flex items-center gap-2" :style="{ paddingLeft: (folder.level || 0) * 20 + 'px' }">
                  <FolderClosed :size="16" />
                  <span>{{ folder.name }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="cancelMove"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="confirmMove"
            :disabled="!moveTargetPath || moveTargetPath === getParentPath(selectedItem?.path || '')"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            移动
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirmState" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelDelete">
      <div class="bg-white rounded-lg p-6 w-96" @click.stop>
        <h3 class="text-lg font-bold mb-4 text-red-600">确认删除</h3>
        <div class="mb-6">
          <p class="text-morandi-700">
            确定要删除 <span class="font-medium">"{{ selectedItem?.name }}"</span> 吗？
          </p>
          <p class="text-sm text-morandi-500 mt-2">
            {{ selectedItem?.type === 'folder' ? '此操作将删除文件夹及其所有内容，' : '' }}删除后可在回收站中恢复。
          </p>
        </div>
        <div class="flex justify-end gap-3">
          <button
            @click="cancelDelete"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="confirmDelete"
            class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- 详细信息对话框 -->
    <div v-if="showItemDetailsState" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="closeItemDetails">
      <div class="bg-white rounded-lg p-6 w-96" @click.stop>
        <h3 class="text-lg font-bold mb-4">详细信息</h3>
        <div v-if="selectedItem" class="space-y-3">
          <div class="flex justify-between">
            <span class="text-sm text-morandi-600">名称：</span>
            <span class="text-sm font-medium">{{ selectedItem.name }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-sm text-morandi-600">类型：</span>
            <span class="text-sm font-medium">
              {{ selectedItem.type === 'folder' ? '文件夹' : 
                  selectedItem.type === 'audio' ? '音频文件' : 
                  selectedItem.type === 'document' ? '文档' : '其他文件' }}
            </span>
          </div>
          <div v-if="selectedItem.type !== 'folder'" class="flex justify-between">
            <span class="text-sm text-morandi-600">大小：</span>
            <span class="text-sm font-medium">{{ formatFileSize(selectedItem.size) }}</span>
          </div>
          <div v-if="selectedItem.type === 'folder'" class="flex justify-between">
            <span class="text-sm text-morandi-600">包含项目：</span>
            <span class="text-sm font-medium">{{ selectedItem.itemCount || 0 }} 项</span>
          </div>
          <div class="flex justify-between">
            <span class="text-sm text-morandi-600">修改时间：</span>
            <span class="text-sm font-medium">{{ formatDate(selectedItem.modifiedAt) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-sm text-morandi-600">路径：</span>
            <span class="text-sm font-medium text-morandi-500 break-all select-text">{{ selectedItem.path }}</span>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="closeItemDetails"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 分享对话框 -->
    <div v-if="showShareDialogState" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="closeShareDialog">
      <div class="bg-white rounded-lg p-6 w-[500px]" @click.stop>
        <h3 class="text-lg font-bold mb-4">分享文件</h3>
        <div v-if="selectedItem" class="space-y-4">
          <!-- 分享项目信息 -->
          <div class="p-4 bg-morandi-50 rounded-lg">
            <div class="flex items-center gap-3">
              <FolderClosed v-if="selectedItem.type === 'folder'" :size="20" class="text-teal-600" />
              <FileText v-else :size="20" class="text-blue-600" />
              <div>
                <div class="font-medium">{{ selectedItem.name }}</div>
                <div class="text-sm text-morandi-600">
                  {{ selectedItem.type === 'folder' ? `文件夹 • ${selectedItem.itemCount || 0} 项` : `文件 • ${formatFileSize(selectedItem.size)}` }}
                </div>
              </div>
            </div>
          </div>

          <!-- 分享设置 -->
          <div class="grid grid-cols-2 gap-4">
            <!-- 有效期设置 -->
            <div>
              <label class="block text-sm font-medium text-morandi-700 mb-2">有效期</label>
              <select
                v-model="shareExpireTime"
                class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
              >
                <option :value="1">1天</option>
                <option :value="7">7天</option>
                <option :value="30">30天</option>
                <option :value="0">永久</option>
              </select>
            </div>

            <!-- 访问密码设置 -->
            <div>
              <div class="flex items-center gap-2 mb-2">
                <input
                  v-model="enableSharePassword"
                  type="checkbox"
                  class="w-4 h-4 text-teal-600 border-morandi-300 rounded focus:ring-teal-500"
                />
                <label class="text-sm font-medium text-morandi-700">设置访问密码</label>
              </div>
              <p v-if="enableSharePassword" class="text-xs text-morandi-500 mt-1">
                密码将由系统自动生成
              </p>
            </div>
          </div>

        </div>
        
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="closeShareDialog"
            class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
          >
            关闭
          </button>
          <button
            @click="generateShareLink"
            :disabled="isGeneratingShare"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50"
          >
            {{ isGeneratingShare ? '生成中...' : '生成链接' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useDriveStore } from '../store/DriveStore'
import { useTransferStore } from '../store/TransferStore'
import { useSettingsStore } from '../store/SettingsStore'
import { useShareStore } from '../store/ShareStore'
import { useContextMenuStore } from '../store/ContextMenuStore'
import { useRouter } from 'vue-router'
import { useToast } from 'vue-toastification'
import type { FolderInfo, FileInfo } from '@/types/api'
import { FolderTypeEnum } from '@/types/api'
import { API } from '@/utils/api'
import {
  Upload, FolderClosed, ChevronRight, Music, FileText, Trash2, Shredder, RefreshCcw, RotateCw, ListOrdered, Logs, Grid2x2, 
  FileMusic, LibraryBig, FileUp, Share2
} from 'lucide-vue-next'
import { useMusicStore } from '@/store/MusicStore'

const toast = useToast()

// 由于DriveStoreV5使用了不同的数据结构，暂时创建一个兼容的类型
interface DriveItem {
  id: number
  name: string
  type: 'folder' | 'file' | 'audio' | 'document'
  size?: number
  path: string
  parentId?: number
  children?: DriveItem[]
  modifiedAt?: Date
  createdAt?: Date
  level?: number
  itemCount?: number
  folder_type?: string  // 文件夹类型：system/playlist/booklist
  // 其他必要属性...
}

interface BreadcrumbPath {
  name: string
  path: string
  id: number
}

// 使用 DriveStore 和 TransferStore
const driveStore = useDriveStore()
const transferStore = useTransferStore()
const settingsStore = useSettingsStore()
const shareStore = useShareStore()
const contextMenuStore = useContextMenuStore()
const router = useRouter()
const musicStore = useMusicStore()

// 响应式数据
const showCreateFolderDialog = ref(false)
const showUploadDialog = ref(false)
const newFolderName = ref('')
const selectedFiles = ref<File[]>([])

// 当前文件夹信息（用于创建子文件夹时确定类型）
const currentFolderInfo = ref<FolderInfo | null>(null)

// 排序和视图相关
const sortBy = ref<'name' | 'size' | 'modifiedAt' | 'createdAt' | 'default'>('default')
const sortOrder = ref<'ASC' | 'DESC'>('ASC')
const viewMode = ref<'grid' | 'list'>('grid')
const showSortMenu = ref(false)

// 右键菜单相关
const showContextMenuState = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const selectedItem = ref<DriveItem | null>(null)
let contextMenuLock = false

// 重命名相关
const showRenameDialogState = ref(false)
const renameValue = ref('')
const renameInput = ref<HTMLInputElement | null>(null)

// 移动相关
const showMoveDialogState = ref(false)
const moveTargetPath = ref('')

// 删除确认
const showDeleteConfirmState = ref(false)

// 详细信息
const showItemDetailsState = ref(false)

// 刷新状态
const isRefreshing = ref(false)

// 剪贴板
const clipboard = ref<DriveItem[]>([])
const clipboardAction = ref<'copy' | 'cut' | null>(null)

// 分享相关
const showShareDialogState = ref(false)
const shareExpireTime = ref(7) // 分享链接有效期（天）
const enableSharePassword = ref(false)
const isGeneratingShare = ref(false)

// 排序选项
const sortOptions = [
  { value: 'default', label: '默认排序' },
  { value: 'name', label: '按名称' },
  { value: 'modifiedAt', label: '按修改时间' },
  { value: 'createdAt', label: '按创建时间' },
  { value: 'size', label: '按大小' }
] as const

// 计算属性
const breadcrumbPaths = computed<BreadcrumbPath[]>(() => {
  if (driveStore.isInRecycleBin) {
    return [
      { name: '云盘', path: '/' },
      { name: '回收站', path: '/recycle' }
    ]
  }
  
  // 使用DriveStore的breadcrumb数据
  const result = [{ name: '云盘', path: '/', id: 0 }]

  if (driveStore.breadcrumb && driveStore.breadcrumb.length > 0) {
    driveStore.breadcrumb.forEach((crumb: any, index: number) => {
      // 存储面包屑项目的ID，用于导航
      result.push({
        name: crumb.name,
        path: `/folder/${crumb.id}`, // 使用文件夹ID构建路径
        id: crumb.id
      })
    })
  }

  return result
})

const currentItems = computed(() => {
  if (driveStore.isInRecycleBin) {
    return driveStore.recycleBinItems
  }

  // 直接使用DriveStore的数据，它已经根据当前文件夹ID返回了正确的内容
  return driveStore.driveItems
})

const filteredItems = computed(() => {
  // 使用currentItems，它已经包含了正确的时间字段映射
  let itemsToDisplay: DriveItem[] = currentItems.value

  // 搜索过滤
  if (driveStore.searchQuery) {
    const query = driveStore.searchQuery.toLowerCase()
    itemsToDisplay = itemsToDisplay.filter((item: DriveItem) =>
      item.name.toLowerCase().includes(query)
    )
  }

  // 注意：排序现在由后端处理，所以我们不需要在前端再次排序
  // 但是我们仍然保持文件夹在前的逻辑
  return [...itemsToDisplay].sort((a: DriveItem, b: DriveItem) => {
    if (a.type === 'folder' && b.type !== 'folder') return -1
    if (a.type !== 'folder' && b.type === 'folder') return 1
    return 0 // 保持后端排序
  })
})

const currentFolder = computed(() => {
  return findItemByPath(driveStore.currentPath)
})

// 允许拖拽/移动文件夹的根目录文件夹名单
const fixedRootFolderNames = ['音乐', '文档', '上传', '分享']

// 判断目标文件夹是否为根目录下固定允许的文件夹
const isRootFixedFolder = (folder: DriveItem): boolean => {
  const depth = folder.path.split('/').filter(Boolean).length
  return depth === 1 && fixedRootFolderNames.includes(folder.name)
}

const availableFolders = computed(() => {
  console.log('availableFolders computed - selectedItem:', selectedItem.value)
  console.log('availableFolders computed - driveStore.folderHierarchy:', driveStore.folderHierarchy)

  // 处理folderHierarchy可能是API响应对象的情况
  let hierarchy: any[] = []

  if (Array.isArray(driveStore.folderHierarchy)) {
    hierarchy = driveStore.folderHierarchy
  } else if (driveStore.folderHierarchy && (driveStore.folderHierarchy as any).data) {
    // 如果是API响应对象，提取data字段
    hierarchy = Array.isArray((driveStore.folderHierarchy as any).data) ? (driveStore.folderHierarchy as any).data : []
  }

  console.log('availableFolders - extracted hierarchy:', hierarchy)

  if (hierarchy.length === 0) {
    console.log('availableFolders - no hierarchy data')
    return []
  }

  // 构建层级结构，根据parent_id关系计算层级
  const buildFolderTree = (parentId: number, currentLevel: number = 1): any[] => {
    const children = hierarchy.filter(folder => folder.parent_id === parentId)
    const result: any[] = []

    children.forEach(folder => {
      // 排除当前选中的文件夹（不能移动到自己）
      if (selectedItem.value && folder.id === selectedItem.value.id) {
        return
      }

      const folderItem = {
        ...folder,
        name: folder.folder_name,
        type: 'folder' as const,
        size: 0,
        itemCount: 0,
        createdAt: new Date(),
        modifiedAt: new Date(),
        isLocked: folder.folder_type === 'system',
        path: `/folder/${folder.id}`,
        level: currentLevel  // 使用计算出的层级
      }

      result.push(folderItem)

      // 递归添加子文件夹，层级+1
      const subFolders = buildFolderTree(folder.id, currentLevel + 1)
      result.push(...subFolders)
    })

    return result
  }

  // 从根目录开始构建树（parent_id = 0）
  const result = buildFolderTree(0, 1)

  console.log('availableFolders result:', result)
  return result
})

// 辅助函数
const getCurrentLevel = (): number => {
  // 根据面包屑长度判断层级
  // breadcrumb.length === 0: 根目录 (level 0)
  // breadcrumb.length === 1: 第一级系统文件夹内 (level 1)
  // breadcrumb.length === 2: 第二级用户文件夹内 (level 2)
  return driveStore.breadcrumb.length
}

const getCurrentLevelTitle = (): string => {
  if (driveStore.isInRecycleBin) return '回收站'
  
  // 如果有面包屑，使用最后一个面包屑作为标题
  if (driveStore.breadcrumb && driveStore.breadcrumb.length > 0) {
    return driveStore.breadcrumb[driveStore.breadcrumb.length - 1].name
  }
  
  // 否则根据当前文件夹ID判断
  if (driveStore.currentFolderId === 0) {
    return '云盘'
  }
  
  return '文件夹'
}

const getEmptyStateMessage = (): string => {
  const level = getCurrentLevel()
  if (level === 0) return '点击右侧的"新建文件夹"开始创建内容'
  if (level === 1) return '点击右侧的"新建文件夹"创建分类'
  if (level === 2) return '点击右侧的"上传文件"添加内容'
  return '此文件夹为空'
}

const findItemByPath = (path: string): DriveItem | undefined => {
  const pathParts = path.split('/').filter(p => p.length > 0)
  if (pathParts.length === 0) return undefined
  
  // 确保driveItems存在且为数组
  if (!driveStore.driveItems || !Array.isArray(driveStore.driveItems)) {
    return undefined
  }
  
  let current = driveStore.driveItems.find((item: DriveItem) => item.name === pathParts[0])
  
  for (let i = 1; i < pathParts.length && current; i++) {
    current = current.children?.find((item: DriveItem) => item.name === pathParts[i])
  }
  
  return current
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 获取文件夹类型的中文显示名称
const getFolderTypeDisplayName = (folderType: string): string => {
  switch (folderType) {
    case 'system':
      return '系统文件夹'
    case 'playlist':
      return '歌单'
    case 'booklist':
      return '书单'
    default:
      return '未知类型'
  }
}

// 事件处理
const navigateToPath = (index: number) => {
  const breadcrumbItem = breadcrumbPaths.value[index]

  // 如果在回收站模式下点击面包屑，退出回收站模式
  if (driveStore.isInRecycleBin) {
    driveStore.exitRecycleBin()
    return
  }

  selectedItemId.value = null // 清除选中状态

  // 根据面包屑项目的索引导航
  if (index === 0) {
    // 点击"云盘"，导航到根目录
    driveStore.navigateToRoot()
  } else {
    // 点击其他面包屑项目，导航到对应的文件夹
    driveStore.navigateToFolder(breadcrumbItem.id)
  }
}

const selectedItemId = ref<number | null>(null)

const handleItemClick = (item: DriveItem, event: MouseEvent) => {
  if (driveStore.isInRecycleBin) {
    return
  }

  // Ctrl/Command 键多选
  if (event.ctrlKey || event.metaKey) {
    if (selectedItemIds.value.has(item.id)) {
      selectedItemIds.value.delete(item.id)
      if (selectedItemId.value === item.id) {
        selectedItemId.value = null
      }
    } else {
      selectedItemIds.value.add(item.id)
      selectedItemId.value = item.id
    }
  } else {
    // 普通点击
    selectedItemIds.value.clear()
    selectedItemIds.value.add(item.id)
    selectedItemId.value = item.id
  }
}



const handleItemDoubleClick = async (item: DriveItem) => {
  const now = Date.now()
  if (now - lastFolderOpenTime.value < 300) {
    return
  }
  lastFolderOpenTime.value = now

  if (driveStore.isInRecycleBin) {
    // 在回收站中双击恢复文件
    const itemName = item.name
    driveStore.restoreItem(item.id)
    selectedItemId.value = null
    toast.success(`"${itemName}" 已恢复`)
    return
  }

  if (item.type === 'folder') {
    // 记录当前文件夹信息（用于后续创建子文件夹）
    if (getCurrentLevel() === 0) {
      // 如果是从根目录点击系统文件夹，记录系统文件夹信息
      currentFolderInfo.value = {
        id: item.id,
        folder_name: item.name,
        folder_type: item.folder_type || 'system' as any,
        sub_count: item.itemCount || 0,
        sorted_order: 0,
        create_time: item.createdAt?.toISOString() || new Date().toISOString(),
        update_time: item.modifiedAt?.toISOString() || new Date().toISOString()
      }
    }

    // 双击文件夹进入该文件夹
    driveStore.navigateToFolder(item.id)
  } else if (item.type === 'audio') {
    const parts = item.path.split('/').filter(p=>p)
    let playlistName = ''
    if (parts.length>=2) {
      playlistName = parts[parts.length-2]
    }
    const songName = item.name
    // 立即在 MusicStore 播放
    try {
      await musicStore.playSongFromDrive(playlistName, songName)
    } catch {}
    router.push({ path:'/music', query:{ playlist: playlistName, song: songName }})
  } else if (item.type === 'document') {
    console.log('跳转到文档页面，文档信息:', item)
    router.push({
      path: '/docs',
      query: {
        path: item.path,
        fileName: item.name,
        fileId: item.id.toString()
      }
    })
  } else {
    // 文档或其他文件类型的点击逻辑 - 之后实现
    console.log('Clicked on file:', item)
  }
}

const createNewFolder = () => {
  const level = getCurrentLevel()
  console.log('createNewFolder called, level:', level, 'breadcrumb:', driveStore.breadcrumb)

  if (level !== 1) {
    toast.error('只能在第一级分类中创建文件夹')
    return
  }
  hideContextMenu() // 隐藏右键菜单
  showCreateFolderDialog.value = true
  newFolderName.value = ''

  // 在下一个tick自动聚焦输入框
  nextTick(() => {
    const input = document.querySelector('input[placeholder="请输入文件夹名称"]') as HTMLInputElement
    if (input) {
      input.focus()
      input.select()
    }
  })
}

const confirmCreateFolder = async () => {
  const folderName = newFolderName.value.trim()
  if (!folderName) return

  const level = getCurrentLevel()
  console.log('confirmCreateFolder called, level:', level, 'folderName:', folderName)

  if (level !== 1) {
    toast.error('只能在第一级分类中创建文件夹')
    return
  }

  // 确定父文件夹ID和文件夹类型
  const parentFolderId = driveStore.currentFolderId

  // 使用记录的文件夹信息或面包屑信息
  let parentFolderName = ''
  if (currentFolderInfo.value) {
    parentFolderName = currentFolderInfo.value.folder_name
  } else if (driveStore.breadcrumb.length > 0) {
    parentFolderName = driveStore.breadcrumb[driveStore.breadcrumb.length - 1].name
  }

  console.log('parentFolderId:', parentFolderId, 'parentFolderName:', parentFolderName, 'currentFolderInfo:', currentFolderInfo.value, 'breadcrumb:', driveStore.breadcrumb)

  if (!parentFolderName) {
    toast.error('无法确定父文件夹')
    return
  }

  // 根据父文件夹类型确定要创建的文件夹类型
  let folderType: FolderTypeEnum
  if (parentFolderName === '歌单') {
    folderType = FolderTypeEnum.PLAYLIST
  } else if (parentFolderName === '书单' || parentFolderName === '文档') {
    folderType = FolderTypeEnum.BOOKLIST
  } else {
    toast.error('只能在歌单或书单文件夹下创建子文件夹')
    return
  }

  // 调用DriveStore的createFolder方法
  const success = await driveStore.createFolder({
    name: folderName,
    parent_id: parentFolderId,
    folder_type: folderType
  })

  if (success) {
    showCreateFolderDialog.value = false
    newFolderName.value = ''
  }
}

const uploadFiles = () => {
  // 检查是否在正确的层级
  const level = getCurrentLevel()

  if (level !== 2) {
    toast.error(`只能在第二级文件夹中上传文件，当前层级：${level}`)
    return
  }

  // 隐藏右键菜单
  hideContextMenu()

  // 打开上传对话框
  showUploadDialog.value = true
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  selectedFiles.value = files
}

const cancelUpload = () => {
  showUploadDialog.value = false
  selectedFiles.value = []
}

const confirmUpload = async () => {
  if (selectedFiles.value.length === 0) return

  const level = getCurrentLevel()
  if (level !== 2) {
    toast.error('只能在第二级文件夹中上传文件')
    return
  }

  const fileCount = selectedFiles.value.length

  try {
    // 使用 TransferStore 处理上传
    for (const file of selectedFiles.value) {
      await transferStore.uploadFile(file, driveStore.currentFolderId)
    }

    toast.success(`已开始上传 ${fileCount} 个文件`)

    // 刷新当前目录
    await driveStore.refresh()
  } catch (error) {
    console.error('上传失败:', error)
    toast.error('上传失败，请重试')
  } finally {
    // 清理状态
    showUploadDialog.value = false
    selectedFiles.value = []
  }
}

const getFileType = (fileName: string): 'audio' | 'document' | 'other' => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  if (['mp3', 'wav', 'flac', 'aac'].includes(ext || '')) return 'audio'
  if (['pdf', 'doc', 'docx', 'txt', 'md'].includes(ext || '')) return 'document'
  return 'other'
}

const openRecycleBin = () => {
  driveStore.openRecycleBin()
  driveStore.setSearchQuery('')
}

const exitRecycleBin = () => {
  driveStore.exitRecycleBin()
  driveStore.setSearchQuery('')
}

const getParentPath = (path: string): string => {
  const parts = path.split('/')
  parts.pop()
  return parts.join('/') || '/'
}

// 右键菜单事件处理
const showContextMenu = (event: MouseEvent, item: DriveItem) => {
  event.preventDefault() // 阻止默认右键菜单
  event.stopPropagation() // 阻止事件冒泡
  
  // 如果已经有菜单显示或者锁定状态，直接返回，不处理
  if (showContextMenuState.value || contextMenuLock) {
    return
  }
  
  // 加锁防止重复触发
  contextMenuLock = true
  
  // 如果当前是多选状态，且右键点击的是已选中的项目之一，保持多选状态
  if (selectedItemIds.value.size > 1 && selectedItemIds.value.has(item.id)) {
    // 保持当前多选状态，不改变选中项
    selectedItem.value = item // 设置为当前右键的项目，但不改变多选状态
  } else if (!selectedItemIds.value.has(item.id)) {
    // 如果点击的项目不在选中列表中，只选中当前项目
    selectedItemIds.value.clear()
    selectedItemIds.value.add(item.id)
    selectedItemId.value = item.id
    selectedItem.value = item
  } else {
    // 如果是单选状态且点击的是已选中项目，保持当前状态
    selectedItem.value = item
  }
  
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  showContextMenuState.value = true
  
  // 点击其他地方关闭菜单
  document.addEventListener('click', hideContextMenu, { once: true })
}

const hideContextMenu = () => {
  showContextMenuState.value = false
  contextMenuLock = false // 解锁
  selectedItem.value = null // 总是清空selectedItem，避免幽灵菜单
}

// 只关闭菜单和解锁，但保留selectedItem（用于对话框）
const closeContextMenuOnly = () => {
  showContextMenuState.value = false
  contextMenuLock = false // 解锁
}

// 隐藏所有左键菜单
const hideAllLeftClickMenus = () => {
  showSortMenu.value = false
  // 如果以后有其他左键菜单，可以在这里添加
}

const preventHide = () => {
  // 阻止菜单自动隐藏
}

// 下载功能
const downloadItem = async () => {
  if (!selectedItem.value) return
  
  try {
    // 构建选择参数
    const selection = {
      file_ids: selectedItem.value.type === 'folder' ? [] : [selectedItem.value.id],
      folder_id: selectedItem.value.type === 'folder' ? selectedItem.value.id : null
    }
    
    console.log('🔽 开始下载:', selectedItem.value.name, '选择参数:', selection)
    
    // 创建下载会话
    const downloadSession = await transferStore.createDownloadSession(selection)
    
    if (downloadSession && downloadSession.files_d && downloadSession.files_d.length > 0) {
      // 开始下载每个文件
      for (const fileItem of downloadSession.files_d) {
        await transferStore.downloadFile(fileItem)
      }
      
      console.log('🎉 下载任务全部启动')
    } else {
      toast.error('下载会话创建失败或没有可下载的文件')
    }
  } catch (error) {
    console.error('下载失败:', error)
    toast.error('下载失败，请重试')
  } finally {
    hideContextMenu()
  }
}

// 多选下载功能
const downloadMultipleItems = async () => {
  if (selectedItemIds.value.size === 0) return
  
  try {
    // 构建选择参数 - API只支持单个文件夹或多个文件
    const fileIds: number[] = []
    let folderId: number | null = null
    
    selectedItemIds.value.forEach(itemId => {
      const item = filteredItems.value.find(item => item.id === itemId)
      if (item) {
        if (item.type === 'folder') {
          // API只支持单个文件夹，取第一个
          if (folderId === null) {
            folderId = item.id
          }
        } else {
          // 所有非文件夹类型（file、audio、document）都当作文件处理
          fileIds.push(item.id)
        }
      }
    })
    
    // 如果选择了多个文件夹，需要分别创建下载会话
    if (folderId && fileIds.length > 0) {
      toast.info('检测到同时选择了文件和文件夹，将分别处理')
    }
    
    const selection = {
      file_ids: fileIds.length > 0 ? fileIds : undefined,
      folder_id: folderId
    }
    
    console.log('🔽 开始多选下载:', selection)
    
    // 创建下载会话
    const downloadSession = await transferStore.createDownloadSession(selection)
    
    if (downloadSession && downloadSession.files_d && downloadSession.files_d.length > 0) {
      // 开始下载每个文件
      for (const fileItem of downloadSession.files_d) {
        await transferStore.downloadFile(fileItem)
      }
      
      console.log('🎉 多选下载任务全部启动')
    } else {
      toast.error('下载会话创建失败或没有可下载的文件')
    }
  } catch (error) {
    console.error('多选下载失败:', error)
    toast.error('多选下载失败，请重试')
  } finally {
    // 清除选择状态
    selectedItemIds.value.clear()
  }
}

// 右键菜单点击处理函数
const handleDownloadClick = () => {
  // 下载操作需要selectedItem.value，所以使用closeContextMenuOnly保留状态
  if (selectedItemIds.value.size === 1) {
    downloadItem().then(() => {
      selectedItem.value = null // 下载完成后清理状态
    })
  } else {
    downloadMultipleItems()
  }
  closeContextMenuOnly() // 只关闭菜单但保留selectedItem
}

const handleOpenClick = () => {
  // 打开操作会在内部处理菜单隐藏
  if (selectedItemIds.value.size === 1) {
    openItem()
  } else {
    hideContextMenu()
  }
}

const handleShareClick = () => {
  // showShareDialog内部已经处理菜单隐藏，所以这里只需要执行操作
  if (selectedItemIds.value.size === 1 && selectedItem.value && !isFirstLevelFolder(selectedItem.value)) {
    showShareDialog()
  } else {
    // 如果条件不满足，手动隐藏菜单
    hideContextMenu()
  }
}

const handleDeleteClick = () => {
  // 删除操作会在内部处理菜单隐藏
  if (selectedItemIds.value.size === 1) {
    deleteItem()
  } else {
    deleteMultipleItems()
  }
}

// 复制功能
const copyItem = () => {
  if (selectedItem.value) {
    clipboard.value = [{ ...selectedItem.value }]
    clipboardAction.value = 'copy'
    toast.success(`已复制 "${selectedItem.value.name}" 到剪贴板`)
  }
  hideContextMenu()
}

// 剪切功能
const cutItem = () => {
  if (selectedItem.value) {
    clipboard.value = [{ ...selectedItem.value }]
    clipboardAction.value = 'cut'
    toast.success(`已剪切 "${selectedItem.value.name}" 到剪贴板`)
  }
  hideContextMenu()
}

// 重命名功能
const showRenameDialog = () => {
  if (selectedItem.value) {
    renameValue.value = selectedItem.value.name
    showRenameDialogState.value = true
    
    // 延迟聚焦，确保DOM完全渲染
    setTimeout(() => {
      if (renameInput.value) {
        renameInput.value.focus()
        renameInput.value.select()
        console.log('重命名输入框已聚焦')
      } else {
        console.error('重命名输入框未找到')
      }
    }, 100)
  }
  closeContextMenuOnly(); // 只关闭菜单但保留selectedItem
}

const confirmRename = async () => {
  const newName = renameValue.value.trim()
  if (!newName || !selectedItem.value) return

  // 检查名称是否与原名称相同
  if (newName === selectedItem.value.name) {
    cancelRename()
    return
  }

  // 检查名称是否包含非法字符
  const invalidChars = /[<>:"/\\|?*]/
  if (invalidChars.test(newName)) {
    toast.error('文件名不能包含以下字符：< > : " / \\ | ? *')
    return
  }

  const item = selectedItem.value
  const oldName = item.name

  try {
    // 调用DriveStore的重命名方法，传递正确的参数格式
    const renameData = {
      file_id: item.type === 'folder' ? null : item.id,
      folder_id: item.type === 'folder' ? item.id : null,
      new_name: newName
    }

    await driveStore.renameItem(renameData)

    showRenameDialogState.value = false
    renameValue.value = ''
    // DriveStore 已经显示了成功提示
  } catch (error) {
    console.error('重命名失败:', error)
    toast.error('重命名失败，请重试')
  }
}

const cancelRename = () => {
  showRenameDialogState.value = false
  renameValue.value = ''
}

// 加载文件夹层级用于移动对话框
const loadFolderHierarchyForMoveDialog = async () => {
  try {
    // 调用DriveStore的方法加载文件夹层级
    await driveStore.loadFolderHierarchy()
  } catch (error) {
    console.error('加载文件夹层级失败:', error)
  }
}

// 移动功能
const showMoveDialog = async () => {
  if (selectedItem.value) {
    moveTargetPath.value = ''

    console.log('showMoveDialog - selectedItem:', selectedItem.value)

    // 加载文件夹层级数据
    await loadFolderHierarchyForMoveDialog()

    console.log('showMoveDialog - after loading hierarchy:', driveStore.folderHierarchy)

    showMoveDialogState.value = true
  }
  closeContextMenuOnly(); // 只关闭菜单但保留selectedItem
}

const selectMoveTarget = (path: string) => {
  moveTargetPath.value = path
}

const confirmMove = async () => {
  if (!selectedItem.value || !moveTargetPath.value) return

  console.log('confirmMove - selectedItem:', selectedItem.value)
  console.log('confirmMove - moveTargetPath:', moveTargetPath.value)

  if (moveTargetPath.value === '/') {
    toast.error('不能移动到根目录')
    return
  }

  const item = selectedItem.value
  const targetPath = moveTargetPath.value

  // 从目标路径中提取文件夹ID
  // targetPath格式: "/folder/7" -> 提取ID 7
  let targetFolderId: number
  if (targetPath.startsWith('/folder/')) {
    targetFolderId = parseInt(targetPath.replace('/folder/', ''))
  } else {
    toast.error('无效的目标路径')
    return
  }

  console.log('confirmMove - targetFolderId:', targetFolderId)

  // 构建移动请求数据
  const moveData = {
    file_ids: item.type === 'folder' ? [] : [item.id],
    folder_ids: item.type === 'folder' ? [item.id] : [],
    target_folder_id: targetFolderId
  }

  console.log('confirmMove - moveData:', moveData)

  // 调用DriveStore的moveItems方法
  try {
    const success = await driveStore.moveItems(moveData)

    if (success) {
      // 移动成功，关闭对话框
      showMoveDialogState.value = false
      moveTargetPath.value = ''
      selectedItem.value = null
    }
  } catch (error) {
    console.error('移动操作失败:', error)
  }
}

const cancelMove = () => {
  showMoveDialogState.value = false
  moveTargetPath.value = ''
  selectedItem.value = null
}

// 删除功能（移动到回收站）
const showDeleteConfirm = () => {
  if (selectedItem.value) {
    showDeleteConfirmState.value = true
  }
  closeContextMenuOnly(); // 只关闭菜单但保留selectedItem
}

const confirmDelete = async () => {
  if (!selectedItem.value) return

  const item = selectedItem.value

  // 如果在回收站模式下，是真正删除
  if (driveStore.isInRecycleBin) {
    // 根据项目类型调用相应的删除API
    if (item.type === 'folder') {
      await driveStore.deleteRecycleBinItems({
        file_ids: [],
        folder_ids: [item.original_id || item.id]
      })
    } else {
      await driveStore.deleteRecycleBinItems({
        file_ids: [item.original_id || item.id],
        folder_ids: []
      })
    }
    showDeleteConfirmState.value = false
    selectedItem.value = null
    return
  }

  // 正常模式下移动到回收站 - 调用API
  try {
    if (item.type === 'folder') {
      await driveStore.deleteItems({
        file_ids: [],
        folder_ids: [item.id]
      })
    } else {
      await driveStore.deleteItems({
        file_ids: [item.id],
        folder_ids: []
      })
    }

    showDeleteConfirmState.value = false
    selectedItem.value = null
    // DriveStore.deleteItems 已经显示了成功提示，这里不需要重复显示，也不需要手动刷新
  } catch (error) {
    console.error('删除失败:', error)
    toast.error('删除失败，请重试')
  }
}

const cancelDelete = () => {
  showDeleteConfirmState.value = false
  selectedItem.value = null
}

// 回收站相关功能
const restoreAllItems = async () => {
  if (driveStore.recycleBinItems.length === 0) {
    toast.info('回收站为空，无需恢复')
    return
  }

  if (confirm(`确定要恢复回收站中的所有 ${driveStore.recycleBinItems.length} 个项目吗？`)) {
    try {
      // 从回收站数据中提取 original_id
      const fileIds = driveStore.recycleBinFiles.map((file: any) => file.original_id).filter(id => id)
      const folderIds = driveStore.recycleBinFolders.map((folder: any) => folder.original_id).filter(id => id)

      console.log('Restoring - fileIds:', fileIds, 'folderIds:', folderIds)

      if (fileIds.length === 0 && folderIds.length === 0) {
        toast.error('没有找到可恢复的项目')
        return
      }

      await driveStore.restoreRecycleBinItems({
        file_ids: fileIds,
        folder_ids: folderIds
      })
    } catch (error) {
      console.error('恢复失败:', error)
      toast.error('恢复失败，请重试')
    }
  }
}

const deleteAllItems = async () => {
  if (driveStore.recycleBinItems.length === 0) {
    toast.info('回收站为空，无需清空')
    return
  }

  if (confirm('确定要永久删除回收站中的所有项目吗？此操作不可恢复。')) {
    try {
      await driveStore.emptyRecycleBin()
    } catch (error) {
      console.error('清空回收站失败:', error)
      toast.error('清空回收站失败，请重试')
    }
  }
}

const deleteItem = async (itemToDelete?: DriveItem) => {
  const item = itemToDelete || selectedItem.value
  if (!item) return;

  try {
    if (driveStore.isInRecycleBin) {
      // 在回收站中，彻底删除项目
      if (item.type === 'folder') {
        await driveStore.deleteRecycleBinItems({
          file_ids: [],
          folder_ids: [item.id]
        })
      } else {
        await driveStore.deleteRecycleBinItems({
          file_ids: [item.id],
          folder_ids: []
        })
      }
      // DriveStore 已经显示了成功提示
    } else {
      // 正常模式下，移动到回收站
      if (item.type === 'folder') {
        await driveStore.deleteItems({
          file_ids: [],
          folder_ids: [item.id]
        })
      } else {
        await driveStore.deleteItems({
          file_ids: [item.id],
          folder_ids: []
        })
      }
      // DriveStore 已经显示了成功提示
    }
  } catch (error) {
    console.error('删除失败:', error)
    toast.error('删除失败，请重试')
  }

  hideContextMenu(); // 使用统一的隐藏菜单方法
};

const restoreItem = async () => {
  if (!selectedItem.value) return;
  const itemName = selectedItem.value.name;

  try {
    if (selectedItem.value.type === 'folder') {
      await driveStore.restoreRecycleBinItems({
        file_ids: [],
        folder_ids: [selectedItem.value.id]
      })
    } else {
      await driveStore.restoreRecycleBinItems({
        file_ids: [selectedItem.value.id],
        folder_ids: []
      })
    }
    // DriveStore 已经显示了成功提示
  } catch (error) {
    console.error('恢复失败:', error)
    toast.error('恢复失败，请重试')
  }

  showContextMenuState.value = false;
  selectedItem.value = null;
};



// 打开功能 - 复用双击逻辑
const openItem = () => {
  if (selectedItem.value) {
    // 直接调用双击处理函数，实现相同的打开逻辑
    handleItemDoubleClick(selectedItem.value)
  }
  hideContextMenu()
}

// 详细信息功能
const showItemDetails = () => {
  if (selectedItem.value) {
    showItemDetailsState.value = true
    closeContextMenuOnly() // 只关闭菜单但保留selectedItem
  }
}

const closeItemDetails = () => {
  showItemDetailsState.value = false
  hideContextMenu() // 确保状态被正确清理
}

// 刷新功能
const refreshItems = async () => {
  isRefreshing.value = true
  selectedItemId.value = null // 清除选中状态
  
  try {
    // 调用 DriveStore 的真正刷新方法
    await driveStore.refresh()
    
    // 根据当前模式显示不同的刷新消息
    if (driveStore.isInRecycleBin) {
      console.log('回收站刷新完成')
      toast.success('回收站刷新完成')
    } else {
      console.log('云盘刷新完成')
      toast.success('云盘刷新完成')
    }
  } catch (error) {
    console.error('刷新失败:', error)
    toast.error('刷新失败，请重试')
  } finally {
    isRefreshing.value = false
    hideContextMenu()
  }
}

// 排序功能
const selectSort = async (criteria: 'name' | 'size' | 'modifiedAt' | 'createdAt' | 'default') => {
  sortBy.value = criteria
  showSortMenu.value = false

  // 映射前端排序字段到后端字段
  let backendSortBy: string
  switch (criteria) {
    case 'name':
      backendSortBy = 'name'
      break
    case 'createdAt':
      backendSortBy = 'create_time'
      break
    case 'modifiedAt':
      backendSortBy = 'update_time'
      break
    case 'size':
      backendSortBy = 'file_size'
      break
    case 'default':
    default:
      backendSortBy = 'sorted_order'
      break
  }

  // 调用DriveStore的排序方法
  try {
    await driveStore.setSorting(backendSortBy, sortOrder.value)
  } catch (error) {
    console.error('排序失败:', error)
    toast.error('排序失败，请重试')
  }
}

// 切换排序方向
const toggleSortOrder = async () => {
  sortOrder.value = sortOrder.value === 'ASC' ? 'DESC' : 'ASC'

  // 如果当前有选中的排序方式，重新应用排序
  if (sortBy.value) {
    await selectSort(sortBy.value)
  }
}

// 关闭排序菜单
const closeSortMenu = () => {
  showSortMenu.value = false
}

// 切换排序菜单
const toggleSortMenu = () => {
  // 如果要打开排序菜单，先关闭其他所有菜单
  if (!showSortMenu.value) {
    hideContextMenu() // 关闭右键菜单
    // 这里不需要调用hideAllLeftClickMenus()因为当前只有排序菜单
  }
  
  showSortMenu.value = !showSortMenu.value
  
  if (showSortMenu.value) {
    // 点击其他地方关闭菜单
    document.addEventListener('click', closeSortMenu, { once: true })
  }
}

// 空白区域右键菜单
const showEmptyContextMenu = (event: MouseEvent) => {
  // 关键检查：如果右键点击的目标是任何一个文件/文件夹项，
  // 那么应该触发该项的右键菜单，而不是空白菜单。所以这里直接返回。
  if ((event.target as HTMLElement).closest('.item-card')) {
    return
  }

  // 阻止默认菜单
  event.preventDefault()

  // 如果已有菜单显示，先隐藏
  if (showContextMenuState.value) {
    showContextMenuState.value = false
  }

  // 核心逻辑：清空所有与"项目选择"相关的状态
  selectedItem.value = null
  selectedItemId.value = null
  selectedItemIds.value.clear()
  
  // 设置新菜单的位置并显示
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  showContextMenuState.value = true
  
  // 绑定一次性点击事件来关闭菜单
  document.addEventListener('click', hideContextMenu, { once: true })
}



// 粘贴功能
const pasteItem = async () => {
  if (!clipboard.value.length || !clipboardAction.value) return

  const isCut = clipboardAction.value === 'cut'

  try {
    // 准备API请求数据
    const fileIds = clipboard.value.filter(item => item.type !== 'folder').map(item => item.id)
    const folderIds = clipboard.value.filter(item => item.type === 'folder').map(item => item.id)

    const operationData = {
      file_ids: fileIds,
      folder_ids: folderIds,
      target_folder_id: driveStore.currentFolderId
    }

    let success = false

    if (isCut) {
      // 剪切操作：调用移动API
      success = await driveStore.moveItems(operationData)
    } else {
      // 复制操作：调用复制API
      success = await driveStore.copyItems(operationData)
    }

    if (success) {
      // 清空剪贴板
      clipboard.value = []
      clipboardAction.value = null

      // 刷新当前目录
      await driveStore.loadFolderContent(driveStore.currentFolderId)

      const action = isCut ? '移动' : '复制'
      const itemCount = fileIds.length + folderIds.length
      toast.success(`${action}了 ${itemCount} 个项目`)
    }
  } catch (error) {
    console.error('粘贴操作失败:', error)
    const action = isCut ? '移动' : '复制'
    toast.error(`${action}失败`)
  }

  hideContextMenu()
}

// 格式化日期
const formatDate = (date: Date | undefined): string => {
  if (!date || !(date instanceof Date) || isNaN(date.getTime())) {
    console.log('formatDate: invalid date received:', date)
    return '--'
  }

  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 判断是否为一级文件夹（不显示分享选项）
const isFirstLevelFolder = (item: DriveItem | null): boolean => {
  if (!item || item.type !== 'folder') return false
  return item.level === 1 // 一级大类文件夹
}

// 分享功能
const showShareDialog = () => {
  if (selectedItem.value) {
    showShareDialogState.value = true
    // 重置分享设置
    shareExpireTime.value = 7
    enableSharePassword.value = false
    isGeneratingShare.value = false
  }
  closeContextMenuOnly(); // 只关闭菜单但保留selectedItem
}

const closeShareDialog = () => {
  showShareDialogState.value = false
  selectedItem.value = null
}

const generateShareLink = async () => {
  if (!selectedItem.value) return

  isGeneratingShare.value = true

  try {
    // 调用真实的分享API
    const shareData = {
      file_id: selectedItem.value.type !== 'folder' ? selectedItem.value.id : null,
      folder_id: selectedItem.value.type === 'folder' ? selectedItem.value.id : null,
      is_encrypted: enableSharePassword.value,
      expires_in_days: shareExpireTime.value
    }

    const response = await shareStore.createShare(shareData)

    if (response) {
      // ShareStore.createShare 已经处理了剪贴板复制，这里不需要重复操作
      // 关闭分享对话框
      closeShareDialog()
    }

  } catch (error) {
    console.error('生成分享链接失败:', error)
    toast.error('生成分享链接失败，请重试')
  } finally {
    isGeneratingShare.value = false
  }
}

const fileInput = ref<HTMLInputElement | null>(null)

const handleDragStart = (event: DragEvent, item: DriveItem) => {
  if (item.isLocked) {
    event.preventDefault()
    return
  }

  // 如果拖拽的项目不在选中列表中，只选中当前项目
  if (!selectedItemIds.value.has(item.id)) {
    selectedItemIds.value.clear()
    selectedItemIds.value.add(item.id)
    selectedItemId.value = item.id
  }

  if (event.dataTransfer) {
    // 如果是多选，传递所有选中项目的ID
    const selectedIds = Array.from(selectedItemIds.value)
    event.dataTransfer.setData('text/plain', JSON.stringify({
      type: 'multiple',
      ids: selectedIds,
      count: selectedIds.length
    }))
    event.dataTransfer.effectAllowed = 'move'

    // 自定义预览：显示拖拽项目数量
    const originEl = (event.currentTarget as HTMLElement)
    const svgEl = originEl.querySelector('svg')?.cloneNode(true) as HTMLElement | null
    if (svgEl) {
      const wrapper = document.createElement('div')
      wrapper.style.position = 'fixed'
      wrapper.style.top = '-100px'
      wrapper.style.left = '-100px'
      wrapper.style.width = 'auto'
      wrapper.style.height = '32px'
      wrapper.style.display = 'flex'
      wrapper.style.alignItems = 'center'
      wrapper.style.justifyContent = 'center'
      wrapper.style.background = 'rgba(59, 130, 246, 0.9)'
      wrapper.style.color = 'white'
      wrapper.style.borderRadius = '6px'
      wrapper.style.padding = '4px 8px'
      wrapper.style.fontSize = '12px'
      wrapper.style.fontWeight = 'bold'
      wrapper.style.gap = '4px'
      
      svgEl.style.width = '16px'
      svgEl.style.height = '16px'
      wrapper.appendChild(svgEl)
      
      if (selectedIds.length > 1) {
        const countText = document.createElement('span')
        countText.textContent = selectedIds.length.toString()
        wrapper.appendChild(countText)
      }
      
      document.body.appendChild(wrapper)
      event.dataTransfer.setDragImage(wrapper, 16, 16)
      setTimeout(() => document.body.removeChild(wrapper), 0)
    }
  }
  
  // 开始全局拖拽监听
  startGlobalDragListening()
}

// 拖拽状态
const dragOverTargetId = ref<string | null>(null)

const handleDragOver = (event: DragEvent, targetItem: DriveItem) => {
  if (targetItem.type === 'folder') {
    dragOverTargetId.value = targetItem.id
  }
}

const handleDragLeave = () => {
  dragOverTargetId.value = null
}

const handleDrop = (event: DragEvent, targetItem: DriveItem) => {
  dragOverTargetId.value = null
  if (targetItem.type !== 'folder' || !event.dataTransfer) return

  const dragData = event.dataTransfer.getData('text/plain')
  
  try {
    const parsedData = JSON.parse(dragData)
    
    if (parsedData.type === 'multiple' && parsedData.ids) {
      // 处理多选拖拽
      const itemsToMove = parsedData.ids
        .map((id: number) => findItemInStore(id))
        .filter((item: DriveItem | null) => item !== null)
      
      if (itemsToMove.length === 0) return
      
      // 若拖拽项包含文件夹，且目标不是根目录固定文件夹，则禁止
      if (itemsToMove.some((item: DriveItem) => item.type === 'folder') && !isRootFixedFolder(targetItem)) {
        toast.error('禁止将文件夹拖拽到非根目录文件夹，防止嵌套')
        return
      }
      
      // 检查是否有任何项目拖拽到自己身上
      if (itemsToMove.some((item: DriveItem) => item.id === targetItem.id)) return
      
      // 直接移动所有选中的项目
      itemsToMove.forEach((item: DriveItem) => {
        selectedItem.value = item
        moveTargetPath.value = targetItem.path
        confirmMove()
      })
      
      // 清空选择
      selectedItemIds.value.clear()
      selectedItemId.value = null
      
      toast.success(`已将 ${itemsToMove.length} 个项目移动到 "${targetItem.name}"`)
    }
  } catch (e) {
    // 如果不是JSON格式，按原来的单项拖拽处理
    const draggedItemId = parseInt(dragData, 10)
    if (isNaN(draggedItemId) || draggedItemId === targetItem.id) return

    const itemToMove = findItemInStore(draggedItemId)
    if (itemToMove) {
      // 单文件夹拖拽，同样只允许目标为根目录固定文件夹
      if (itemToMove.type === 'folder' && !isRootFixedFolder(targetItem)) {
        toast.error('禁止将文件夹拖拽到非根目录文件夹，防止嵌套')
        return
      }
      selectedItem.value = itemToMove
      moveTargetPath.value = targetItem.path
      confirmMove()
      toast.success(`已将 "${itemToMove.name}" 移动到 "${targetItem.name}"`)
    }
  }
}

const getDragOverClass = (item: DriveItem, isList: boolean = false) => {
  const baseClasses = isList 
    ? 'border-transparent hover:border-morandi-300 hover:bg-morandi-50' 
    : 'border-transparent hover:border-morandi-300 hover:bg-morandi-50'

  const selectedClasses = selectedItemIds.value.has(item.id)
    ? 'border-blue-500 bg-blue-100 shadow-md ring-2 ring-blue-300 ring-opacity-50'
    : ''

  const dragOverClasses = item.id === dragOverTargetId.value
    ? isList ? 'bg-teal-100 border-teal-300' : 'border-teal-400 bg-teal-50'
    : ''

  return [baseClasses, selectedClasses, dragOverClasses].filter(Boolean)
}

const findItemInStore = (itemId: number): DriveItem | null => {
  const find = (items: DriveItem[]): DriveItem | null => {
    for (const item of items) {
      if (item.id === itemId) return item
      if (item.children) {
        const found = find(item.children)
        if (found) return found
      }
    }
    return null
  }
  
  // 先在正常项目中查找
  const foundInDrive = find(driveStore.driveItems)
  if (foundInDrive) return foundInDrive
  
  // 如果没找到，在回收站中查找
  return driveStore.recycleBinItems.find((item: DriveItem) => item.id === itemId) || null
}











// 防止快速连击导致重复打开
const lastFolderOpenTime = ref(0)

const refreshAndHide = async () => {
  hideContextMenu()
  hideAllLeftClickMenus()
  await refreshItems()
}
// 拖拽自动滚动方法
const handleDragOverScroll = (event: DragEvent) => {
  if (!scrollContainer.value) return
  
  const container = scrollContainer.value
  const rect = container.getBoundingClientRect()
  const mouseY = event.clientY
  const scrollZone = 50 // 滚动触发区域的高度
  
  // 只有当鼠标在容器范围内或接近容器边缘时才处理滚动
  if (mouseY < rect.top - scrollZone || mouseY > rect.bottom + scrollZone) {
    return
  }
  
  // 计算滚动速度
  let newScrollSpeed = 0
  
  if (mouseY < rect.top + scrollZone && mouseY > rect.top - scrollZone) {
    // 靠近顶部，向上滚动
    const distance = rect.top + scrollZone - mouseY
    newScrollSpeed = -Math.min(15, Math.max(2, distance / 3))
  } else if (mouseY > rect.bottom - scrollZone && mouseY < rect.bottom + scrollZone) {
    // 靠近底部，向下滚动
    const distance = mouseY - (rect.bottom - scrollZone)
    newScrollSpeed = Math.min(15, Math.max(2, distance / 3))
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
    console.error('DrivePage handleGlobalDragOver error:', error)
  }
}

// 添加新的响应式数据
const isDraggingOverTrash = ref(false)

// 添加回收站拖拽相关方法
const handleTrashDragOver = () => {
  isDraggingOverTrash.value = true
}

const handleTrashDragLeave = () => {
  isDraggingOverTrash.value = false
}

const handleTrashDrop = async (event: DragEvent) => {
  isDraggingOverTrash.value = false
  if (!event.dataTransfer) return

  const dragData = event.dataTransfer.getData('text/plain')
  
  try {
    const parsedData = JSON.parse(dragData)
    
    if (parsedData.type === 'multiple' && parsedData.ids) {
      // 处理多选拖拽到回收站
      const itemsToDelete = parsedData.ids
        .map((id: string) => findItemInStore(id))
        .filter((item: DriveItem | null) => item !== null && !item.isLocked)
      
      if (itemsToDelete.length === 0) {
        toast.error('所选文件已锁定，无法删除')
        return
      }
      
      // 检查是否有锁定文件
      const lockedItems = parsedData.ids
        .map((id: string) => findItemInStore(id))
        .filter((item: DriveItem | null) => item !== null && item.isLocked)
      
      if (lockedItems.length > 0) {
        toast.error(`有 ${lockedItems.length} 个文件已锁定，无法删除`)
        return
      }
      
      // 调用API删除所有选中的项目
      const fileIds = itemsToDelete.filter(item => item.type !== 'folder').map(item => item.id)
      const folderIds = itemsToDelete.filter(item => item.type === 'folder').map(item => item.id)

      try {
        await driveStore.deleteItems({
          file_ids: fileIds,
          folder_ids: folderIds
        })

        // DriveStore.deleteItems 已经显示了成功提示，这里不需要重复显示，也不需要手动刷新
      } catch (error) {
        console.error('批量删除失败:', error)
        toast.error('删除失败，请重试')
        return
      }
      
      // 清空选择
      selectedItemIds.value.clear()
      selectedItemId.value = null
    }
  } catch (e) {
    // 如果不是JSON格式，按原来的单项拖拽处理
    const draggedItemId = dragData
    const itemToDelete = findItemInStore(draggedItemId)
    
    if (itemToDelete) {
      // 如果是锁定的文件，不允许删除
      if (itemToDelete.isLocked) {
        toast.error('此文件已锁定，无法删除')
        return
      }

      // 调用API删除单个项目
      try {
        if (itemToDelete.type === 'folder') {
          await driveStore.deleteItems({
            file_ids: [],
            folder_ids: [itemToDelete.id]
          })
        } else {
          await driveStore.deleteItems({
            file_ids: [itemToDelete.id],
            folder_ids: []
          })
        }

        // DriveStore.deleteItems 已经显示了成功提示，这里不需要重复显示，也不需要手动刷新
      } catch (error) {
        console.error('删除失败:', error)
        toast.error('删除失败，请重试')
      }
    }
  }
}

// 添加多选相关的响应式数据
const isSelecting = ref(false)
const selectionStart = ref({ x: 0, y: 0 })
const selectionEnd = ref({ x: 0, y: 0 })
const selectedItemIds = ref<Set<number>>(new Set())
const initialSelectedIds = ref<Set<number>>(new Set())

// 拖拽自动滚动相关
const scrollContainer = ref<HTMLElement | null>(null)
const autoScrollTimer = ref<number | null>(null)
const scrollSpeed = ref(0)

// 多选框相关方法
const startSelection = (event: MouseEvent) => {
  // 如果点击的是项目本身或者不是左键点击，不启动框选
  if ((event.target as HTMLElement).closest('.item-card') || event.button !== 0) {
    return
  }

  // 核心逻辑：只要在空白处按下鼠标，就立即隐藏右键菜单
  // 这是杜绝所有幽灵菜单的根本方法
  hideContextMenu()

  const container = event.currentTarget as HTMLElement
  const rect = container.getBoundingClientRect()
  
  isSelecting.value = true
  selectionStart.value = {
    x: event.clientX - rect.left + container.scrollLeft,
    y: event.clientY - rect.top + container.scrollTop
  }
  selectionEnd.value = { ...selectionStart.value }

  // 保存当前选择状态
  initialSelectedIds.value = new Set(selectedItemIds.value)

  // 如果没有按住 Ctrl/Command 键，清除之前的选择
  if (!event.ctrlKey && !event.metaKey) {
    selectedItemIds.value.clear()
    selectedItemId.value = null
    selectedItem.value = null // 同时清空selectedItem，避免状态不一致
    initialSelectedIds.value.clear()
  }

  // 防止文字选择
  event.preventDefault()

  // 添加全局鼠标事件监听
  document.addEventListener('mousemove', handleGlobalMouseMove, { passive: false })
  document.addEventListener('mouseup', handleGlobalMouseUp, { passive: false })
}

const handleGlobalMouseMove = (event: MouseEvent) => {
  if (!isSelecting.value) return

  // 防止默认行为
  event.preventDefault()

  const container = document.querySelector('.flex-1.overflow-auto') as HTMLElement
  if (!container) return

  const rect = container.getBoundingClientRect()
  
  // 限制鼠标位置在容器范围内
  const mouseX = Math.max(rect.left, Math.min(rect.right, event.clientX))
  const mouseY = Math.max(rect.top, Math.min(rect.bottom, event.clientY))
  
  selectionEnd.value = {
    x: mouseX - rect.left + container.scrollLeft,
    y: mouseY - rect.top + container.scrollTop
  }

  updateSelectedItems()
}

const handleGlobalMouseUp = (event: MouseEvent) => {
  if (!isSelecting.value) return
  
  event.preventDefault()
  isSelecting.value = false
  
  // 移除全局事件监听
  document.removeEventListener('mousemove', handleGlobalMouseMove)
  document.removeEventListener('mouseup', handleGlobalMouseUp)

  // 如果只选中了一个项目，更新 selectedItemId
  if (selectedItemIds.value.size === 1) {
    selectedItemId.value = Array.from(selectedItemIds.value)[0]
  }
}

const updateSelectedItems = () => {
  const container = document.querySelector('.flex-1.overflow-auto') as HTMLElement
  if (!container) return

  const rect = container.getBoundingClientRect()
  const left = Math.min(selectionStart.value.x, selectionEnd.value.x)
  const top = Math.min(selectionStart.value.y, selectionEnd.value.y)
  const right = Math.max(selectionStart.value.x, selectionEnd.value.x)
  const bottom = Math.max(selectionStart.value.y, selectionEnd.value.y)

  // 从初始选择状态开始
  selectedItemIds.value = new Set(initialSelectedIds.value)

  // 检查每个项目是否在选择框内
  const items = document.querySelectorAll('.item-card')
  items.forEach((item) => {
    const itemRect = item.getBoundingClientRect()
    const itemLeft = itemRect.left - rect.left + container.scrollLeft
    const itemTop = itemRect.top - rect.top + container.scrollTop
    const itemRight = itemLeft + itemRect.width
    const itemBottom = itemTop + itemRect.height

    const itemIdStr = item.getAttribute('data-item-id')
    if (!itemIdStr) return

    const itemId = parseInt(itemIdStr, 10)
    if (isNaN(itemId)) return

    // 检查是否有重叠 - 更精确的碰撞检测
    const isOverlapping = !(
      itemRight < left ||
      itemLeft > right ||
      itemBottom < top ||
      itemTop > bottom
    )

    if (isOverlapping) {
      selectedItemIds.value.add(itemId)
    }
  })
}

const hasLockedItems = () => {
  return Array.from(selectedItemIds.value).some((id: number) => findItemInStore(id)?.isLocked)
}

const copyMultipleItems = () => {
  const selectedItems = Array.from(selectedItemIds.value)
    .map((id: number) => findItemInStore(id))
    .filter((item): item is DriveItem => item !== null)
  if (!selectedItems.length) return
  clipboard.value = selectedItems.map(item => ({ ...item }))
  clipboardAction.value = 'copy'
  toast.success(`已复制 ${selectedItems.length} 个项目到剪贴板`)
  hideContextMenu()
}

const cutMultipleItems = () => {
  const selectedItems = Array.from(selectedItemIds.value)
    .map((id: number) => findItemInStore(id))
    .filter((item): item is DriveItem => item !== null)
  if (!selectedItems.length) return
  clipboard.value = selectedItems.map(item => ({ ...item }))
  clipboardAction.value = 'cut'
  toast.success(`已剪切 ${selectedItems.length} 个项目到剪贴板`)
  hideContextMenu()
}

const deleteMultipleItems = async () => {
  const selectedItems = Array.from(selectedItemIds.value)
    .map((id: number) => findItemInStore(id))
    .filter((item): item is DriveItem => item !== null)
  if (!selectedItems.length) return

  try {
    // 分离文件和文件夹ID
    const fileIds = selectedItems.filter(item => item.type !== 'folder').map(item => item.id)
    const folderIds = selectedItems.filter(item => item.type === 'folder').map(item => item.id)

    if (driveStore.isInRecycleBin) {
      // 在回收站中，彻底删除项目
      await driveStore.deleteRecycleBinItems({
        file_ids: fileIds,
        folder_ids: folderIds
      })
    } else {
      // 正常模式下，移动到回收站
      await driveStore.deleteItems({
        file_ids: fileIds,
        folder_ids: folderIds
      })
    }
    // DriveStore 已经显示了成功提示
  } catch (error) {
    console.error('批量删除失败:', error)
    toast.error('批量删除失败，请重试')
  }

  selectedItemIds.value.clear()
  selectedItemId.value = null
  hideContextMenu()
}

const restoreMultipleItems = async () => {
  const selectedItems = Array.from(selectedItemIds.value)
    .map((id: number) => findItemInStore(id))
    .filter((item): item is DriveItem => item !== null)
  if (!selectedItems.length) return
  
  try {
    // 分离文件和文件夹ID
    const fileIds = selectedItems.filter(item => item.type !== 'folder').map(item => item.id)
    const folderIds = selectedItems.filter(item => item.type === 'folder').map(item => item.id)

    await driveStore.restoreRecycleBinItems({
      file_ids: fileIds,
      folder_ids: folderIds
    })
    // DriveStore 已经显示了成功提示
  } catch (error) {
    console.error('批量恢复失败:', error)
    toast.error('批量恢复失败，请重试')
  }

  selectedItemIds.value.clear()
  selectedItemId.value = null
  hideContextMenu()
}

// 组件卸载时清理定时器
onUnmounted(() => {
  clearAutoScroll()
})

// 添加辅助函数与计算
const getDeleteInfo = (item: DriveItem) => {
  if (!item.deletedAt || !(item.deletedAt instanceof Date) || isNaN(item.deletedAt.getTime())) {
    return { dateStr: '--', daysLeft: '--' }
  }

  const deleted = item.deletedAt
  let expiry: Date

  // 如果有expireAt字段，使用它；否则计算30天后的日期
  if (item.expireAt && item.expireAt instanceof Date && !isNaN(item.expireAt.getTime())) {
    expiry = item.expireAt
  } else {
    expiry = new Date(deleted.getTime() + 30 * 24 * 60 * 60 * 1000)
  }

  const now = new Date()
  const daysLeft = Math.max(0, Math.ceil((expiry.getTime() - now.getTime()) / (24 * 60 * 60 * 1000)))
  return { dateStr: deleted.toLocaleDateString('zh-CN'), daysLeft }
}

// 用户已收集的岛屿（从后端获取）
const isles = computed((): Island[] => {
  // 暂时返回空数组，避免错误
  return []
})

// 移除重复的分享函数，使用 generateShareLink 函数

// 初始化数据
onMounted(async () => {
  try {
    // 确保先加载根目录内容，然后应用排序
    await driveStore.loadFolderContent(0)
    await selectSort('default')
  } catch (error) {
    console.error('加载云盘数据失败:', error)
    // 如果加载失败，尝试直接加载根目录
    try {
      await driveStore.loadFolderContent(0)
    } catch (fallbackError) {
      console.error('加载根目录失败:', fallbackError)
    }
  }
})

</script> 