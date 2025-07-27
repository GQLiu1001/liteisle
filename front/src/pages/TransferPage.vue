<template>
  <div class="min-h-full bg-liteisle-bg p-6">
    <div class="max-w-7xl mx-auto">
      <div class="h-[calc(100vh-10rem)] flex gap-6">
        <!-- 第一栏：传输分类导航 -->
        <div class="card w-64 flex-shrink-0">
          <div class="h-full flex flex-col p-4">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-bold text-morandi-900">传输管理</h2>
            </div>
            <nav class="space-y-2 flex-1 overflow-y-auto">
              <button
                v-for="cat in categories"
                :key="cat.type"
                @click="activeCategory = cat.type"
                :class="[
                  'w-full flex items-center gap-3 p-3 rounded-lg text-left transition-all duration-200',
                  activeCategory === cat.type
                    ? 'bg-teal-100 text-teal-800 border border-teal-300'
                    : 'text-morandi-700 hover:bg-morandi-100 border border-transparent'
                ]"
              >
                <component :is="cat.icon" :size="20" class="flex-shrink-0" />
                <div class="flex-1 min-w-0">
                  <div class="font-medium truncate">{{ cat.label }}</div>
                  <div class="text-xs text-morandi-500 truncate">
                    {{ getTaskCount(cat.type) }} 个任务
                  </div>
                </div>
              </button>
            </nav>
          </div>
        </div>

        <!-- 第二栏：传输列表 -->
        <div class="card flex-1 min-w-0">
          <div 
            class="h-full p-6 overflow-y-auto"
            @drop.prevent="handleFileDrop"
            @dragover.prevent="handleDragOver"
            @dragleave.prevent="handleDragLeave"
            :class="{ 'bg-teal-50/50': isDragging }"
          >
            <!-- 顶部操作栏 -->
            <div class="flex items-center justify-between mb-6">
              <div class="flex items-center gap-4">
                <button @click="triggerFileUpload" class="flex items-center gap-2 px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors shadow-sm">
                  <Upload :size="16" />
                  <span>上传文件</span>
                </button>
                <button @click="showLinkDialog = true" class="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors shadow-sm">
                  <Link :size="16" />
                  <span>分享链接下载</span>
                </button>
              </div>
              <button
                v-if="hasCompletedTasks && activeStatus === 'completed'"
                @click="showClearConfirm = true"
                class="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-gray-300 text-gray-700 rounded-lg text-sm hover:bg-gray-50 transition-colors shadow-sm"
              >
                <Trash2 :size="14" />
                <span>{{ isDownloadCategory ? '清空下载记录' : '清空上传记录' }}</span>
              </button>
            </div>

            <!-- 状态切换 -->
            <div class="flex items-center gap-6 border-b border-gray-200 mb-6">
              <button
                v-for="stat in statuses"
                :key="stat.type"
                @click="activeStatus = stat.type"
                class="pb-2 text-sm font-semibold transition-colors"
                :class="activeStatus === stat.type ? 'text-teal-600 border-b-2 border-teal-600' : 'text-gray-500 hover:text-gray-800'"
              >
                {{ stat.label }} {{ getStatusCount(stat.type) }}
              </button>
            </div>

            <!-- 任务列表 -->
            <div class="flex-1 h-[calc(100%-8rem)] relative">
              <div 
                v-if="filteredTasks.length === 0" 
                class="absolute inset-0 flex items-center justify-center text-center text-gray-500"
              >
                <div>
                  <FolderOpen :size="64" class="mx-auto mb-4 text-gray-300" />
                  <p>点击<button @click="triggerFileUpload" class="text-teal-600 font-semibold mx-1">上传文件</button>或拖拽/粘贴到此处上传</p>
                </div>
              </div>

              <div v-else class="space-y-3">
                <div 
                  v-for="task in filteredTasks" 
                  :key="task.id" 
                  class="p-4 bg-white/70 backdrop-blur-sm rounded-lg border border-morandi-200/50 hover:bg-white/90 hover:border-morandi-300 transition-all duration-200"
                >
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-4">
                      <component :is="task.icon" :size="28" class="text-gray-600" />
                      <div class="flex-1">
                        <p class="font-medium text-gray-800">{{ task.name }}</p>
                        <div class="text-sm text-gray-500 flex items-center gap-4 mt-1">
                          <span>{{ task.size }}</span>
                          <div v-if="activeStatus === 'progressing'" class="flex items-center">
                            <!-- 如果进度是100%显示完成，否则显示转圈 -->
                            <div v-if="task.progress === 100" class="w-32 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                              <div class="h-full bg-green-500 w-full"></div>
                            </div>
                            <div v-else class="flex items-center">
                              <div class="animate-spin rounded-full h-3 w-3 border-2 border-teal-500 border-t-transparent"></div>
                              <span class="ml-2 text-xs text-gray-500">处理中</span>
                            </div>
                          </div>
                          <span v-if="activeStatus === 'progressing' && task.category === 'download'">{{ task.speed || '计算中...' }}</span>
                          <span :class="getStatusClass(task)">{{ getStatusText(task) }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="flex items-center gap-2">
                      <button v-if="task.status === 'progressing'" @click="cancelTask(task.id)" class="text-gray-400 hover:text-red-500 transition-colors" title="取消任务">
                        <X :size="18" />
                      </button>
                      <button v-if="task.status === 'completed'" @click="showDeleteConfirmDialog(task)" class="p-1 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition-colors" title="删除记录">
                        <Trash2 :size="18" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 隐藏的文件上传input -->
    <input type="file" ref="fileInput" @change="handleFileSelect" multiple class="hidden" />

    <!-- 分享链接下载对话框 -->
    <div v-if="showLinkDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96">
        <h3 class="text-lg font-bold mb-4">分享链接下载</h3>
        <div class="relative">
          <input
            v-model="linkUrl"
            type="text"
            placeholder="请输入分享链接"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 pr-20 select-text"
            style="user-select: text !important;"
          />
          <button @click="pasteFromClipboard" class="absolute right-1 top-1/2 -translate-y-1/2 px-3 py-1 text-sm bg-gray-100 hover:bg-gray-200 rounded">
            粘贴
          </button>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="showLinkDialog = false"
            class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="startLinkDownload"
            :disabled="!linkUrl.trim() || shareStore.isVerifying"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50"
          >
            {{ shareStore.isVerifying ? '验证中...' : '验证链接' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96 shadow-xl">
        <h3 class="text-lg font-bold mb-2">确认删除</h3>
        <p class="text-sm text-gray-600 mb-6">
          {{ taskToDelete?.category === 'download' ? '确认删除这条下载记录？' : '您要如何处理这条记录？' }}
        </p>
        <div class="flex flex-col gap-3">
          <!-- 下载记录只显示删除记录选项 -->
          <template v-if="taskToDelete?.category === 'download'">
            <button
              @click="handleDeleteTask(false)"
              class="w-full px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
            >
              删除下载记录
            </button>
          </template>
          <!-- 其他记录显示两个选项 -->
          <template v-else>
            <button
              @click="handleDeleteTask(true)"
              class="w-full px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
            >
              同时删除文件和记录
            </button>
            <button
              @click="handleDeleteTask(false)"
              class="w-full px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              仅删除记录
            </button>
          </template>
        </div>
        <div class="flex justify-end mt-6">
          <button
            @click="showDeleteConfirm = false; taskToDelete = null"
            class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- 清空确认对话框 -->
    <div v-if="showClearConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96 shadow-xl">
        <h3 class="text-lg font-bold mb-2">清空{{ isDownloadCategory ? '下载' : '上传' }}记录</h3>
        <p class="text-sm text-gray-600 mb-6">
          {{ isDownloadCategory ? '确认清空当前分类下的所有下载记录？' : '您要如何处理当前分类下的这些记录？此操作不可逆。' }}
        </p>
        <div class="flex flex-col gap-3">
          <!-- 下载记录只显示清空记录选项 -->
          <template v-if="isDownloadCategory">
            <button @click="handleClearCompleted(false)" class="w-full px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
              清空下载记录
            </button>
          </template>
          <!-- 上传记录显示两个选项 -->
          <template v-else>
            <button @click="handleClearCompleted(true)" class="w-full px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
              同时删除文件和记录
            </button>
            <button @click="handleClearCompleted(false)" class="w-full px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors">
              仅删除记录
            </button>
          </template>
        </div>
        <div class="flex justify-end mt-6">
          <button @click="showClearConfirm = false" class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- 分享内容确认对话框 -->
    <div v-if="showShareConfirmDialog && shareConfirmInfo" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-96 shadow-xl">
        <h3 class="text-lg font-bold mb-4">确认转存</h3>
        <div class="mb-6">
          <div class="flex items-center gap-2 mb-2">
            <span class="text-sm font-medium text-gray-700">类型:</span>
            <span class="text-sm text-gray-600">{{ shareConfirmInfo.verifyResult.item_type?.toLowerCase() === 'file' ? '文件' : '文件夹' }}</span>
          </div>
          <div class="flex items-center gap-2 mb-2">
            <span class="text-sm font-medium text-gray-700">名称:</span>
            <span class="text-sm text-gray-600 truncate">{{ shareConfirmInfo.verifyResult.item_name }}</span>
          </div>
          <div class="flex items-center gap-2 mb-2">
            <span class="text-sm font-medium text-gray-700">大小:</span>
            <span class="text-sm text-gray-600">{{ transferStore.formatFileSize(shareConfirmInfo.verifyResult.item_size) }}</span>
          </div>
          <div v-if="shareConfirmInfo.verifyResult.total_files && shareConfirmInfo.verifyResult.total_files > 1" class="flex items-center gap-2">
            <span class="text-sm font-medium text-gray-700">文件数:</span>
            <span class="text-sm text-gray-600">{{ shareConfirmInfo.verifyResult.total_files }} 个文件</span>
          </div>
        </div>
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">选择保存位置:</label>
          <select
            v-model="selectedTargetFolderId"
            :disabled="isLoadingFolders"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 disabled:opacity-50"
          >
            <option value="0">🏠 根目录</option>
            <option v-if="isLoadingFolders" disabled>加载文件夹中...</option>
            <option
              v-for="folder in availableFolders"
              :key="folder.id"
              :value="folder.id"
            >
              {{ folder.name }}
            </option>
          </select>
        </div>
        <p class="text-sm text-gray-600 mb-6">确认要将此内容转存到您的网盘吗？</p>
        <div class="flex justify-end gap-3">
          <button
            @click="cancelSaveShare"
            class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            取消
          </button>
          <button
            @click="confirmSaveShare"
            :disabled="shareStore.isLoading"
            class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50"
          >
            {{ shareStore.isLoading ? '转存中...' : '确认转存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Upload, Download, Link, Trash2, FolderOpen, X } from 'lucide-vue-next';
import { useToast } from 'vue-toastification'
import { useDriveStore } from '@/store/DriveStore';
import { useTransferStore } from '@/store/TransferStore';
import { useShareStore } from '@/store/ShareStore';
import { useMusicStore } from '@/store/MusicStore';
import { useDocsStore } from '@/store/DocsStore';
import { API } from '@/utils/api';
import type { FileInfo } from '@/types/api';

const toast = useToast()

type CategoryType = 'upload' | 'download';
type StatusType = 'progressing' | 'completed';

interface Task {
  id: number;
  name: string;
  category: CategoryType;
  status: StatusType;
  size: string;
  speed: string;
  progress: number;
  icon: any;
  intervalId?: number;
}

const activeCategory = ref<CategoryType>('upload');
const activeStatus = ref<StatusType>('progressing');
const showLinkDialog = ref(false);
const showDeleteConfirm = ref(false);
const showClearConfirm = ref(false);
const showShareConfirmDialog = ref(false);
const taskToDelete = ref<Task | null>(null);
const linkUrl = ref('');
const shareConfirmInfo = ref<{
  shareInfo: { token: string; password?: string };
  verifyResult: any;
} | null>(null);
const selectedTargetFolderId = ref<number>(0);
const availableFolders = ref<Array<{ id: number; name: string }>>([]);
const isLoadingFolders = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

const driveStore = useDriveStore();
const transferStore = useTransferStore();
const shareStore = useShareStore();
const musicStore = useMusicStore();
const docsStore = useDocsStore();

const categories: { type: CategoryType; label: string; icon: any; }[] = [
  { type: 'upload', label: '上传', icon: Upload },
  { type: 'download', label: '下载', icon: Download },
];

const statuses: { type: StatusType; label: string; }[] = [
  { type: 'progressing', label: '进行中' },
  { type: 'completed', label: '已完成' },
];

// 移除未使用的模拟数据
// const allTasks = ref<Task[]>([]);
// let taskIdCounter = 0;

const getTaskCount = (category: CategoryType) => {
  const allTasks = [...transferStore.processingTasks, ...transferStore.completedTasks];
  return allTasks.filter((task: any) => task.transfer_type === category || task.transfer_type === category.toUpperCase()).length;
};

const getStatusCount = (status: StatusType) => {
  const allTasks = [...transferStore.processingTasks, ...transferStore.completedTasks];
  return allTasks.filter((task: any) => 
    task.transfer_type === activeCategory.value && 
    (status === 'progressing' ? 
      transferStore.processingTasks.includes(task) : 
      transferStore.completedTasks.includes(task)
    )
  ).length;
};

// 检查当前分类下是否有已完成任务
const hasCompletedTasks = computed(() => {
  const completedTasks = transferStore.completedTasks;
  const currentCategoryTasks = completedTasks.filter((task: any) => {
    const taskType = task.transfer_type?.toLowerCase();
    return taskType === activeCategory.value;
  });
  return currentCategoryTasks.length > 0;
});

// 检查当前分类是否为下载
const isDownloadCategory = computed(() => {
  return activeCategory.value === 'download';
});

const filteredTasks = computed(() => {
  let storeTasks: any[] = [];

  if (activeStatus.value === 'progressing') {
    storeTasks = transferStore.processingTasks;
  } else {
    storeTasks = transferStore.completedTasks;
  }

  const mappedTasks = storeTasks.map((task: any) => ({
    id: task.log_id || Math.random(),
    name: task.item_name || '未知文件',
    category: task.transfer_type?.toLowerCase() as CategoryType,
    status: activeStatus.value as StatusType,
    size: transferStore.formatFileSize(task.item_size || 0),
    speed: task.speed || '0 KB/s',
    progress: task.progress || 0,
    icon: task.transfer_type === 'UPLOAD' ? Upload : Download
  }))

  return mappedTasks.filter((t: any) =>
    t.category === activeCategory.value
  )
});

// formatBytes 函数已在 TransferStore 中提供，这里不需要重复定义

// 移除未使用的模拟函数，现在使用真实的TransferStore

const triggerFileUpload = () => {
  fileInput.value?.click();
};

const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files) {
    const files = Array.from(target.files);
    // 使用 TransferStore 处理传输页面上传任务（folder_id: 0）
    await transferStore.uploadFiles(files, '/上传');
    activeCategory.value = 'upload';
    activeStatus.value = 'progressing';
  }
};

const handleClearCompleted = async (deleteFiles: boolean) => {
    try {
        // 根据当前分类清空对应的任务
        const category = activeCategory.value; // 'upload' 或 'download'
        const categoryName = isDownloadCategory.value ? '下载' : '上传';
        
        const success = await transferStore.clearCompletedTasks(deleteFiles, category);
        if (success) {
            if (deleteFiles && !isDownloadCategory.value) {
                toast.success(`已清空所有${categoryName}记录并删除关联文件`);
            } else {
                toast.success(`已清空所有${categoryName}记录`);
            }
        } else {
            toast.error(`清空${categoryName}记录失败`);
        }
    } catch (error) {
        console.error(`清空${isDownloadCategory.value ? '下载' : '上传'}记录失败:`, error);
        toast.error(`清空${isDownloadCategory.value ? '下载' : '上传'}记录失败`);
    }
    showClearConfirm.value = false;
};

const cancelTask = async (id: number) => {
    try {
        // 根据任务类型调用不同的取消方法
        const task = [...transferStore.processingTasks, ...transferStore.completedTasks].find(t => t.log_id === id);
        if (task) {
            if (task.transfer_type === 'upload') {
                await transferStore.cancelUpload(id);
            } else {
                await transferStore.cancelDownload(id);
            }
            // toast 消息已在 store 中处理，这里不需要重复显示
        } else {
            toast.error('任务不存在');
        }
    } catch (error) {
        console.error('取消任务失败:', error);
        toast.error('取消任务失败');
    }
};

const showDeleteConfirmDialog = (task: Task) => {
    taskToDelete.value = task;
    showDeleteConfirm.value = true;
};

const handleDeleteTask = async (deleteFile: boolean) => {
    if (!taskToDelete.value) return;

    try {
        const success = await transferStore.deleteTransferRecord(taskToDelete.value.id, deleteFile);
        if (success) {
            // toast 消息已在 store 中处理，这里不需要重复显示
        }
    } catch (error) {
        console.error('删除传输记录失败:', error);
        toast.error('删除传输记录失败');
    }

    showDeleteConfirm.value = false;
    taskToDelete.value = null;
};

const pasteFromClipboard = async () => {
  try {
    linkUrl.value = await navigator.clipboard.readText();
  } catch (err) {
    alert('无法读取剪贴板，请检查浏览器权限设置。');
  }
};

const startLinkDownload = async () => {
  if (!linkUrl.value.trim()) {
    toast.error('请输入分享链接');
    return;
  }

  try {
    // 解析分享链接
    const shareInfo = shareStore.parseShareLink(linkUrl.value.trim());
    if (!shareInfo) {
      toast.error('分享链接格式不正确');
      return;
    }

    // 验证分享链接
    const verifyResult = await shareStore.verifyShare({
      share_token: shareInfo.token,
      share_password: shareInfo.password
    });

    if (!verifyResult) {
      return; // 错误信息已在store中显示
    }

    // 加载可用文件夹列表
    await loadAvailableFolders();

    // 设置默认保存位置为当前文件夹
    selectedTargetFolderId.value = driveStore.currentFolderId || 0;

    // 显示确认对话框
    shareConfirmInfo.value = {
      shareInfo,
      verifyResult
    };
    showShareConfirmDialog.value = true;

    // 关闭分享链接输入对话框
    showLinkDialog.value = false;

  } catch (error) {
    console.error('分享链接验证失败:', error);
    toast.error('分享链接验证失败');
  }
};

// 确认保存分享内容
const confirmSaveShare = async () => {
  if (!shareConfirmInfo.value) return;

  try {
    // 使用用户选择的文件夹作为保存位置
    const saveResult = await shareStore.saveShare({
      share_token: shareConfirmInfo.value.shareInfo.token,
      share_password: shareConfirmInfo.value.shareInfo.password,
      target_folder_id: selectedTargetFolderId.value
    });

    if (saveResult) {
      // 检查是否为秒传（code 200表示秒传成功，202表示正常转存）
      const isInstantTransfer = saveResult.code === 200;

      // 切换到上传标签页（转存是上传任务）
      activeCategory.value = 'upload';

      if (isInstantTransfer) {
        // 秒传完成，切换到已完成标签页
        activeStatus.value = 'completed';
        toast.success(`⚡ 秒传完成！已转存 ${saveResult.total_files_to_save || 0} 个文件`);

        // 立即刷新相关库（音乐、文档等）
        setTimeout(() => {
          console.log('🎵 转存秒传完成，刷新音乐库');
          musicStore.loadPlaylistsFromDrive();

          console.log('📄 转存秒传完成，刷新文档库');
          docsStore.loadCategoriesFromDrive();
        }, 500);
      } else {
        // 正常转存流程，切换到进行中标签页
        activeStatus.value = 'progressing';
        toast.success(`开始转存 ${saveResult.total_files_to_save || 0} 个文件，请等待处理完成`);
      }

      // 刷新传输历史记录，以便显示新的转存任务
      setTimeout(() => {
        transferStore.loadTransferHistory('processing');
        transferStore.loadTransferHistory('success');
      }, 1000);
    }

    // 关闭确认对话框
    showShareConfirmDialog.value = false;
    shareConfirmInfo.value = null;
    linkUrl.value = '';

  } catch (error) {
    console.error('保存分享内容失败:', error);
    toast.error('保存分享内容失败');
  }
};

// 取消保存分享内容
const cancelSaveShare = () => {
  showShareConfirmDialog.value = false;
  shareConfirmInfo.value = null;
  // 重新显示分享链接输入对话框
  showLinkDialog.value = true;
};

// 加载可用文件夹列表
const loadAvailableFolders = async () => {
  try {
    isLoadingFolders.value = true;
    // 获取文件夹层级信息
    const response = await API.folder.getFolderHierarchy();

    if (response.data && (response.data as any).code === 200 && (response.data as any).data) {
      const folderHierarchy = (response.data as any).data as any[];

      // 构建文件夹选项列表
      const folders: Array<{ id: number; name: string }> = [];

      // 添加系统文件夹（一级文件夹）
      folderHierarchy.forEach((folder: any) => {
        if (folder.folder_type === 'system') {
          folders.push({
            id: folder.id,
            name: `📁 ${folder.folder_name}`
          });
        }
      });

      // 加载所有系统文件夹的子文件夹
      for (const systemFolder of folderHierarchy.filter((f: any) => f.folder_type === 'system')) {
        try {
          const contentResponse = await API.folder.getFolderContent(systemFolder.id);
          if (contentResponse.data && (contentResponse.data as any).code === 200 && (contentResponse.data as any).data) {
            const content = (contentResponse.data as any).data;
            content.folders.forEach((subfolder: any) => {
              folders.push({
                id: subfolder.id,
                name: `📁 ${systemFolder.folder_name} / ${subfolder.folder_name}`
              });
            });
          }
        } catch (error) {
          console.warn(`加载 ${systemFolder.folder_name} 子文件夹失败:`, error);
        }
      }

      availableFolders.value = folders;
    }
  } catch (error) {
    console.error('加载文件夹列表失败:', error);
    // 如果加载失败，至少提供根目录选项
    availableFolders.value = [];
  } finally {
    isLoadingFolders.value = false;
  }
};

const getStatusText = (task: any) => {
  if (task.status === 'completed') {
    return '已完成';
  } else if (task.progress === 100) {
    return '完成';
  } else {
    return task.category === 'upload' ? '上传中' : '下载中';
  }
};

const getStatusClass = (task: any) => {
  if (task.status === 'completed' || task.progress === 100) {
    return 'text-green-600';
  } else {
    return 'text-blue-600';
  }
};

const isDragging = ref(false);

const handleDragOver = (event: DragEvent) => {
  isDragging.value = true;
  event.dataTransfer!.dropEffect = 'copy';
};

const handleDragLeave = () => {
  isDragging.value = false;
};

const handleFileDrop = async (event: DragEvent) => {
  isDragging.value = false;
  const files = Array.from(event.dataTransfer?.files || []);
  if (files.length > 0) {
    await transferStore.uploadFiles(files, '/上传');
    activeCategory.value = 'upload';
    activeStatus.value = 'progressing';
  }
};

const handlePaste = async (event: ClipboardEvent) => {
  const items = Array.from(event.clipboardData?.items || []);
  const files = items
    .filter(item => item.kind === 'file')
    .map(item => item.getAsFile())
    .filter((file): file is File => file !== null);

  if (files.length > 0) {
    await transferStore.uploadFiles(files, '/上传');
    activeCategory.value = 'upload';
    activeStatus.value = 'progressing';
  }
};

onMounted(async () => {
  document.addEventListener('paste', handlePaste);

  // 加载传输记录
  console.log('传输页面加载，开始获取传输记录');
  
  // 检查是否有卡住的活跃任务状态
  const stats = transferStore.getTransferStats()
  console.log('📊 传输状态统计:', stats)
  
  // 如果有活跃任务但没有进行中的任务，可能是状态卡住了
  if (stats.totalActive > 0 && stats.totalProcessing === 0) {
    console.warn('⚠️ 检测到可能的状态异常：有活跃任务但无进行中任务，将自动清理')
    transferStore.debugClearAllActiveTasks()
  }
  
  await transferStore.loadTransferSummary();
  await transferStore.loadTransferHistory('processing', true);
  await transferStore.loadTransferHistory('success', true);
});

onUnmounted(() => {
  document.removeEventListener('paste', handlePaste);
});
</script>

<style scoped>
.card {
  transition: background-color 0.3s ease;
}
</style> 