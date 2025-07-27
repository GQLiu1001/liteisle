<template>
  <div 
    :class="[
      'h-full bg-liteisle-bg',
      'p-4 lg:p-6',
      { 'pb-[10px]': docsStore.currentDocument }
    ]"
  >
    <div class="mx-auto h-full">
      <!-- 详情视图 - 只在文档页面显示 -->
      <div v-if="docsStore.selectedDocument && route.path === '/docs'" class="h-full rounded-2xl bg-white shadow-lg">
        <!-- PDF 展示 -->
        <div v-if="docsStore.selectedDocument.file_name.toLowerCase().endsWith('.pdf')" class="h-full">
          <PDFViewer 
            :file-path="docsStore.selectedDocument.id?.toString() || ''"
            :file-name="docsStore.selectedDocument.file_name"
            @close="docsStore.selectedDocument = null"
          />
        </div>

        <!-- Word 展示 -->
        <div v-else-if="docsStore.selectedDocument.file_name.toLowerCase().match(/\.(doc|docx)$/) " class="h-full">
          <WordViewer 
            :file-path="docsStore.selectedDocument.id?.toString() || ''"
            :file-name="docsStore.selectedDocument.file_name"
            @close="docsStore.selectedDocument = null"
          />
        </div>

        <!-- PowerPoint 展示 -->
        <div v-else-if="docsStore.selectedDocument.file_name.toLowerCase().match(/\.(ppt|pptx)$/)" class="h-full">
          <PowerPointViewer 
            :file-path="docsStore.selectedDocument.id?.toString() || ''"
            :file-name="docsStore.selectedDocument.file_name"
            @close="docsStore.selectedDocument = null"
          />
        </div>

        <!-- Excel 展示 -->
        <div v-else-if="docsStore.selectedDocument.file_name.toLowerCase().match(/\.(xls|xlsx)$/)" class="h-full">
          <ExcelViewer 
            :file-path="docsStore.selectedDocument.id?.toString() || ''"
            :file-name="docsStore.selectedDocument.file_name"
            @close="docsStore.selectedDocument = null"
          />
        </div>

        <!-- Markdown 展示 -->
        <div v-else-if="docsStore.selectedDocument.file_name.toLowerCase().endsWith('.md')" class="h-full">
          <MarkdownViewer
            :file-path="docsStore.selectedDocument.id?.toString() || ''"
            :file-name="docsStore.selectedDocument.file_name"
            v-model:content="docsStore.currentMarkdownContent"
            @close="docsStore.selectedDocument = null"
            @save="docsStore.saveMarkdown"
          />
        </div>

        <!-- 其他格式 -->
        <div v-else class="card flex-1 flex flex-col min-h-0">
          <div class="flex items-center p-4 border-b border-morandi-200 flex-shrink-0">
            <button 
              @click="docsStore.selectedDocument = null" 
              class="flex items-center gap-2 text-morandi-700 hover:text-teal-600 transition-colors"
            >
              <ChevronLeft :size="20" />
              <span class="font-medium">返回列表</span>
            </button>
          </div>
          <div class="p-4 flex-1 overflow-y-auto">
            <div class="h-full flex flex-col">
              <div class="flex items-center justify-between pb-4 border-b border-morandi-200 mb-4 flex-shrink-0">
                <div>
                  <h1 class="text-2xl font-bold text-morandi-900">{{ docsStore.selectedDocument.file_name }}</h1>
                </div>
              </div>
              <div class="flex-1 overflow-y-auto -mr-4 -ml-4 pr-4 pl-4">
                <div class="h-full flex items-center justify-center">
                  <div class="text-center">
                    <FileText :size="64" class="mx-auto text-morandi-400 mb-4" />
                    <p class="text-morandi-600">暂不支持此文件格式的在线预览</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="max-w-7xl mx-auto">
        <div class="h-[calc(100vh-10rem)]">
          <div class="flex gap-6 h-full">
            <!-- 第一栏：书单导航 -->
            <div class="card w-64 flex-shrink-0 relative">
              <div class="h-full flex flex-col p-4">
                <div class="relative mb-4">
                  <input 
                    v-model="docsStore.searchQuery"
                    placeholder="搜索所有文档..."
                    class="w-full px-4 py-2 rounded-lg border border-morandi-300 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent select-text"
                    style="user-select: text !important;"
                  />
                </div>
                <div class="flex items-center justify-between mb-4">
                  <h2 class="text-lg font-bold text-morandi-900">书单</h2>
                  <button
                    @click="showCreateCategoryDialog = true"
                    class="flex items-center gap-1 px-2 py-1 text-teal-600 hover:bg-teal-50 rounded-lg transition-colors text-sm"
                                          title="添加书单"
                  >
                    <svg :size="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="w-4 h-4">
                      <line x1="12" y1="5" x2="12" y2="19"></line>
                      <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    添加书单
                  </button>
                </div>
                <nav class="space-y-2 flex-1 overflow-y-auto">
                  <draggable
                    v-model="currentCategoriesList"
                    item-key="id"
                    class="space-y-2"
                    :animation="150"
                    ghost-class="ghost"
                    chosen-class="chosen"
                    drag-class="drag"
                    @start="onCategoryDragStart"
                    @end="onCategoryDragEnd"
                    :force-fallback="false"
                    :disabled="false"
                  >
                    <template #item="{ element: category }">
                      <button
                        @click="docsStore.selectBooklist(category)"
                        @contextmenu.prevent="handleCategoryContextMenu($event, category)"
                        :data-id="category.id"
                        :class="[
                          'w-full flex items-center gap-3 p-3 rounded-lg text-left transition-all duration-200 cursor-pointer',
                          docsStore.currentBooklist?.id === category.id
                            ? 'bg-teal-100 text-teal-800 border border-teal-300' 
                            : 'text-morandi-700 hover:bg-morandi-100 border border-transparent'
                        ]"
                      >
                        <BookImage :size="20" />
                        <div class="flex-1">
                          <div class="font-medium">{{ category.folder_name }}</div>
                          <div class="text-xs text-morandi-500">{{ category.sub_count || 0 }} 篇</div>
                        </div>
                      </button>
                    </template>
                  </draggable>
                  
                  <!-- 空状态提示 -->
                  <div v-if="!docsStore.sortedBooklists || docsStore.sortedBooklists.length === 0" class="text-center py-8">
                    <BookImage :size="32" class="mx-auto mb-2 text-morandi-400" />
                    <p class="text-sm text-morandi-500">暂无书单</p>
                    <p class="text-xs text-morandi-400 mt-1">点击上方"添加书单"开始创建</p>
                  </div>
                </nav>
              </div>
            </div>

            <!-- 第二栏：文档列表 -->
            <div class="card flex-1 min-w-0">
              <div class="h-full flex flex-col p-4">
                <div class="flex items-center justify-between mb-4">
                                      <h2 class="text-lg font-bold text-morandi-900 truncate pr-2" :title="docsStore.currentBooklist?.folder_name || '选择书单'">
                      {{ docsStore.currentBooklist?.folder_name || '选择书单' }}
                  </h2>
                  <div class="flex items-center gap-2">
                    <button
                      v-if="docsStore.currentBooklist"
                      @click="showUploadDocumentDialog = true"
                      class="flex items-center gap-1 px-2 py-1.5 bg-blue-500 text-white hover:bg-blue-600 rounded-lg transition-colors text-sm"
                      title="上传文档"
                    >
                      <Upload :size="16" />
                      上传文档
                    </button>
                    <button
                      v-if="docsStore.currentBooklist"
                      @click="showAddMarkdownDialog = true"
                      class="flex items-center gap-1 px-2 py-1.5 bg-green-500 text-white hover:bg-green-600 rounded-lg transition-colors text-sm"
                      title="新建Markdown文档"
                    > 
                      <Plus :size="16" />
                      新建MD
                    </button>
                  </div>
                </div>

                <!-- 文档列表区域 -->
                <div v-if="docsStore.filteredDocuments && docsStore.filteredDocuments.length > 0" class="flex-1 overflow-y-auto -mr-2 pr-2" ref="scrollContainer">
                  <draggable
                    v-if="!docsStore.searchQuery"
                    v-model="currentDocsList"
                    item-key="id"
                    class="space-y-1"
                    :animation="150"
                    ghost-class="ghost"
                    chosen-class="chosen"
                    drag-class="drag"
                    @start="onDragStart"
                    @end="onDocumentDragEnd"
                    :force-fallback="false"
                    :disabled="false"
                  >
                    <template #item="{ element: document }">
                      <div
                        @dblclick="docsStore.setCurrentDocument(document)"
                        @contextmenu.prevent="showDocumentContextMenu($event, document)"
                        :data-id="document.id"
                        :class="[
                            'flex items-center gap-4 p-3 rounded-lg cursor-pointer transition-all duration-200 group border-2',
                            'border-transparent hover:border-dashed hover:border-teal-300 hover:bg-morandi-50'
                        ]"
                      >
                          <div class="w-4 text-center opacity-30 group-hover:opacity-70 transition-opacity">
                            <div class="w-1 h-4 bg-morandi-400 rounded-full"></div>
                          </div>
                          
                          <div class="flex-shrink-0">
                            <FileText :size="24" :class="getFileIconColor(document.type)" />
                          </div>
                          
                          <div class="flex-1 min-w-0">
                            <h3 class="font-medium text-morandi-900 truncate">{{ document.file_name }}</h3>
                          </div>
                      </div>
                    </template>
                  </draggable>

                  <div v-else class="space-y-1">
                    <div
                      v-for="document in docsStore.filteredDocuments"
                      :key="document.id"
                      @dblclick="docsStore.setCurrentDocument(document)"
                      @contextmenu.prevent="showDocumentContextMenu($event, document)"
                      :class="[
                        'flex items-center gap-4 p-3 rounded-lg cursor-pointer transition-all duration-200 group border-2',
                        'border-transparent hover:bg-morandi-50'
                      ]"
                    >
                      <div class="w-4 text-center opacity-0">
                        <div class="w-1 h-4 bg-morandi-400 rounded-full"></div>
                      </div>
                      <div class="flex-shrink-0">
                        <FileText :size="24" :class="getFileIconColor(document.type)" />
                      </div>
                      <div class="flex-1 min-w-0">
                        <h3 class="font-medium text-morandi-900 truncate">{{ document.file_name }}</h3>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 空状态 -->
                <div v-else class="flex-1 flex items-center justify-center">
                  <div class="text-center">
                    <FileText :size="48" class="mx-auto mb-4 text-morandi-400" />
                    <h3 class="text-lg font-medium text-morandi-700 mb-2">
                      {{ docsStore.searchQuery ? '未找到匹配的文档' : (docsStore.currentBooklist ? '书单为空' : '请选择一个书单') }}
                    </h3>
                    <p class="text-morandi-500">
                      {{ docsStore.searchQuery ? '尝试其他搜索词或添加新文档' : (docsStore.currentBooklist ? '点击上传按钮添加文档' : '从左侧选择一个书单查看文档') }}
                    </p>
                    <!-- 调试信息 -->
                    <div class="mt-4 text-xs text-gray-400" v-if="docsStore.currentBooklist">
                      <p>当前书单ID: {{ docsStore.currentBooklist.id }}</p>
                      <p>所有文档数量: {{ docsStore.allDocuments.length }}</p>
                      <p>当前书单文档数量: {{ docsStore.currentBooklistDocuments.length }}</p>
                      <p>过滤后文档数量: {{ docsStore.filteredDocuments.length }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 添加文档对话框 -->
      <div v-if="showAddDocumentDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg p-6 w-96">
          <h3 class="text-lg font-bold mb-4">添加文档</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-morandi-700 mb-2">文档名称</label>
              <input
                v-model="newDocumentName"
                placeholder="请输入文档名称"
                class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
                style="user-select: text !important;"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-morandi-700 mb-2">文档类型</label>
              <select
                v-model="newDocumentType"
                class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
              >
                <option value="markdown">Markdown</option>
                <option value="pdf">PDF</option>
                <option value="doc">Word文档(.doc)</option>
                <option value="docx">Word文档(.docx)</option>
                <option value="ppt">PowerPoint(.ppt)</option>
                <option value="pptx">PowerPoint(.pptx)</option>
                <option value="xls">Excel(.xls)</option>
                <option value="xlsx">Excel(.xlsx)</option>
                <option value="txt">文本</option>
              </select>
            </div>

          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="showAddDocumentDialog = false"
              class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
            >
              取消
            </button>
            <button
              @click="addDocument"
              class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors"
            >
              添加
            </button>
          </div>
        </div>
      </div>

      <!-- 创建书单对话框 -->
      <div v-if="showCreateCategoryDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg p-6 w-96">
                      <h3 class="text-lg font-bold mb-4">新建书单</h3>
          <div class="space-y-4">
            <div>
                              <label class="block text-sm font-medium text-morandi-700 mb-2">书单名称</label>
              <input
                v-model="newCategoryName"
                type="text"
                placeholder="请输入分类名称"
                class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
                @keydown.enter="createNewCategory"
                style="user-select: text !important;"
              />
            </div>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="showCreateCategoryDialog = false"
              class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
            >
              取消
            </button>
            <button
              @click="createNewCategory"
              :disabled="!newCategoryName.trim()"
              class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              创建
            </button>
          </div>
        </div>
      </div>

      <!-- 上传文档对话框 -->
      <div v-if="showUploadDocumentDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg p-6 w-96">
          <h3 class="text-lg font-bold mb-4">上传文档</h3>
          <div class="space-y-4">
            <div class="border-2 border-dashed border-morandi-300 rounded-lg p-8 text-center">
              <FileText :size="32" class="mx-auto mb-3 text-morandi-400" />
              <p class="text-morandi-600 mb-2">点击选择文档文件或拖拽到此处</p>
              <p class="text-xs text-morandi-400">支持 PDF、Word、PowerPoint、Excel、文本等格式</p>
              <input 
                type="file" 
                multiple 
                accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.md"
                class="hidden" 
                ref="documentFileInput"
                @change="handleDocumentFileSelect"
              />
              <button 
                @click="() => documentFileInput?.click()"
                class="mt-3 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
              >
                选择文档文件
              </button>
            </div>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="showUploadDocumentDialog = false; selectedDocumentFiles = []"
              class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
            >
              取消
            </button>
            <button
              @click="uploadDocumentFiles"
              :disabled="selectedDocumentFiles.length === 0"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              上传 ({{ selectedDocumentFiles.length }})
            </button>
          </div>
        </div>
      </div>

      <!-- 新建Markdown文档对话框 -->
      <div v-if="showAddMarkdownDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg p-6 w-96">
          <h3 class="text-lg font-bold mb-4">新建Markdown文档</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-morandi-700 mb-2">文档名称</label>
              <input
                v-model="newDocumentName"
                type="text"
                placeholder="请输入文档名称"
                class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 select-text"
                @keydown.enter="createMarkdownDocument"
                style="user-select: text !important;"
              />
            </div>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="showAddMarkdownDialog = false"
              class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
            >
              取消
            </button>
            <button
              @click="createMarkdownDocument"
              :disabled="!newDocumentName.trim()"
              class="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              创建
            </button>
          </div>
        </div>
      </div>

      <!-- 翻译浮窗 -->
      <div
        v-if="docsStore.showTranslation"
        :style="{ 
          left: docsStore.translationPosition.x + 'px', 
          top: docsStore.translationPosition.y + 'px' 
        }"
        class="fixed bg-white rounded-lg shadow-lg border border-morandi-200 p-4 z-50 max-w-xs"
      >
        <div class="flex items-center justify-between mb-2">
          <h4 class="font-medium text-morandi-700">翻译结果</h4>
          <button
            @click="docsStore.hideTranslationPopup()"
            class="p-1 hover:bg-morandi-100 rounded transition-colors"
          >
            <X :size="16" class="text-morandi-500" />
          </button>
        </div>
                  <div class="text-sm text-morandi-600 mb-2 select-text">
          原文：{{ docsStore.selectedText }}
        </div>
        <div class="text-sm text-morandi-900 select-text">
          {{ docsStore.translationResult }}
        </div>
      </div>

      <!-- 重命名文档对话框 -->
      <div v-if="showRenameDocDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelRenameDocument">
        <div class="bg-white rounded-lg p-6 w-96" @click.stop>
          <h3 class="text-lg font-bold mb-4">重命名文档</h3>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-morandi-700 mb-2">新名称</label>
              <input
                ref="renameDocInput"
                v-model="renameDocValue"
                type="text"
                placeholder="请输入新名称"
                class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
                @keydown.enter="confirmRenameDocument"
                @keydown.esc="cancelRenameDocument"
                style="user-select: text !important;"
              />
            </div>
          </div>
          <div class="flex justify-end gap-3 mt-6">
            <button
              @click="cancelRenameDocument"
              class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
            >
              取消
            </button>
            <button
              @click="confirmRenameDocument"
              :disabled="!renameDocValue.trim()"
              class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              确认
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 重命名分类对话框 -->
  <div v-if="showRenameCategoryDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click="cancelRenameCategory">
    <div class="bg-white rounded-lg p-6 w-96" @click.stop>
      <h3 class="text-lg font-bold mb-4">重命名分类</h3>
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-morandi-700 mb-2">新名称</label>
          <input
            ref="renameCategoryInput"
            v-model="renameCategoryValue"
            type="text"
            placeholder="请输入新名称"
            class="w-full px-4 py-2 border border-morandi-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 select-text"
            @keydown.enter="confirmRenameCategory"
            @keydown.esc="cancelRenameCategory"
            style="user-select: text !important;"
          />
        </div>
      </div>
      <div class="flex justify-end gap-3 mt-6">
        <button
          @click="cancelRenameCategory"
          class="px-4 py-2 text-morandi-600 hover:bg-morandi-100 rounded-lg transition-colors"
        >
          取消
        </button>
        <button
          @click="confirmRenameCategory"
          :disabled="!renameCategoryValue.trim()"
          class="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          确认
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, onMounted, nextTick } from 'vue';
import { storeToRefs } from 'pinia';
import { useDocsStore } from '../store/DocsStore';
import { useDriveStore } from '../store/DriveStore';
import { useTransferStore } from '../store/TransferStore';
import { useUIStore } from '@/store/UIStore';
import { useContextMenuStore, type ContextMenuItem } from '@/store/ContextMenuStore';
import { useRoute } from 'vue-router';
import { useToast } from 'vue-toastification';
import type { FolderInfo, FileInfo } from '@/types/api';
import { FolderTypeEnum } from '@/types/api';
import { API } from '@/utils/api';
import { 
  Upload,
  FileText, 
  X,
  Plus,
  ChevronLeft,
  BookImage
} from 'lucide-vue-next';
import draggable from 'vuedraggable';
import PDFViewer from '@/components/PDFViewer.vue';
import WordViewer from '@/components/WordViewer.vue';
import PowerPointViewer from '@/components/PowerPointViewer.vue';
import ExcelViewer from '@/components/ExcelViewer.vue';
import MarkdownViewer from '@/components/MarkdownViewer.vue';
import { ensureWebSocketForUpload, onUploadComplete } from '@/utils/websocket';

const docsStore = useDocsStore();
const driveStore = useDriveStore();
const transferStore = useTransferStore();
const uiStore = useUIStore();
const contextMenuStore = useContextMenuStore();
const toast = useToast();
const route = useRoute();

// 使用 storeToRefs 获取响应式数据
const { booklists, allDocuments, isLoading } = storeToRefs(docsStore);
const showAddDocumentDialog = ref(false);
const showUploadDocumentDialog = ref(false);
const showAddMarkdownDialog = ref(false);
const showCreateCategoryDialog = ref(false);
const newDocumentName = ref('');
const newDocumentType = ref('markdown');

const newCategoryName = ref('');
const selectedDocumentFiles = ref<File[]>([]);
const documentFileInput = ref<HTMLInputElement | null>(null);
const draggedItemId = ref<string | null>(null);
const draggedCategoryId = ref<string | null>(null);

// 拖拽自动滚动相关
const scrollContainer = ref<HTMLElement | null>(null);
const autoScrollTimer = ref<number | null>(null);
const scrollSpeed = ref(0);

// 右键菜单相关
const selectedDocument = ref<FileInfo | null>(null);
const selectedCategory = ref<FolderInfo | null>(null);

// 重命名相关
const showRenameDocDialog = ref(false);
const renameDocValue = ref('');
const renameDocInput = ref<HTMLInputElement | null>(null);

// 分类重命名相关
const showRenameCategoryDialog = ref(false);
const renameCategoryValue = ref('');
const renameCategoryInput = ref<HTMLInputElement | null>(null);

watch(() => docsStore.currentDocument, (newDoc) => {
  if (newDoc) {
    uiStore.setSidebarVisible(false);
  } else {
    uiStore.setSidebarVisible(true);
  }
}, { immediate: true }); // Use immediate to run on component mount

onUnmounted(() => {
  // Ensure sidebar is visible when leaving the page
  uiStore.setSidebarVisible(true);
  // Also reset the document state to avoid side-effects
  docsStore.setCurrentDocument(null);
  // 清理自动滚动定时器
  clearAutoScroll();
});

onMounted(async () => {
  console.log('DocsPage mounted, 开始加载分类数据')

  // 先加载分类数据
  await docsStore.loadCategoriesFromDrive()

  // 处理路由参数
  const { path, fileName, fileId } = route.query
  console.log('路由参数:', { path, fileName, fileId })

  if (path && typeof path === 'string') {
    // 优先使用文件名或ID来查找文档
    const searchKey = (fileName as string) || path
    console.log('尝试加载文档:', searchKey)
    await docsStore.loadDocumentByPath(searchKey)
  }
})

// 添加对分类数据的监听，用于调试
watch(() => docsStore.booklists, (newBooklists) => {
  console.log('分类数据变化:', newBooklists)
  console.log('分类数量:', newBooklists?.length || 0)
}, { immediate: true, deep: true })

watch(() => docsStore.currentBooklist, (newBooklist) => {
  console.log('当前选中分类变化:', newBooklist)
  if (newBooklist) {
    console.log('当前分类的文档数量:', docsStore.currentBooklistDocuments.length)
    console.log('当前分类的文档列表:', docsStore.currentBooklistDocuments)
    console.log('过滤后的文档列表:', docsStore.filteredDocuments)
  }
}, { immediate: true })

// 监听所有文档数据变化
watch(() => docsStore.allDocuments, (newDocs) => {
  console.log('所有文档数据变化:', newDocs)
  console.log('文档总数:', newDocs?.length || 0)
}, { immediate: true, deep: true })

const onDragStart = (event: any) => {
  if (event.item) {
    draggedItemId.value = event.item.dataset.id;
  }
  // 开始全局拖拽监听
  startGlobalDragListening();
};

// 分类拖动开始事件
const onCategoryDragStart = (event: any) => {
  if (event.item) {
    draggedCategoryId.value = event.item.dataset.id;
  }
  // 开始全局拖拽监听
  startGlobalDragListening();
};

// 分类拖动结束事件
const onCategoryDragEnd = (event: {oldIndex: number, newIndex: number}) => {
  if (event.oldIndex !== event.newIndex) {
    docsStore.reorderCategories(event.oldIndex, event.newIndex);
  }
  draggedCategoryId.value = null;
};

// 文档拖动结束事件
const onDocumentDragEnd = (event: {oldIndex: number, newIndex: number}) => {
  if (event.oldIndex !== event.newIndex) {
    docsStore.reorderDocumentsInCurrentCategory(event.oldIndex, event.newIndex);
  }
};

const currentDocsList = computed({
  get() {
    return docsStore.filteredDocuments;
  },
  set(newDocs: any[]) { // Changed to any[] as Document type is removed
    if (!draggedItemId.value) return;

    const oldIndex = docsStore.currentBooklistDocuments.findIndex((d: any) => d.id === draggedItemId.value); // Changed to any
    const newIndex = newDocs.findIndex((d: any) => d.id === draggedItemId.value); // Changed to any

    if (oldIndex !== undefined && oldIndex !== -1 && newIndex !== -1) {
      docsStore.reorderDocumentsInCurrentCategory(oldIndex, newIndex);
    }
    
    draggedItemId.value = null;
  }
});

// 可拖动的分类列表
const currentCategoriesList = computed({
  get() {
    console.log('获取分类列表:', docsStore.sortedBooklists)
    return docsStore.sortedBooklists || [];
  },
  set(newCategories: FolderInfo[]) {
    // 不在这里处理排序，避免与@end事件重复调用
    // 排序逻辑统一在onCategoryDragEnd中处理
  }
});

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
    console.error('DocsPage handleGlobalDragOver error:', error)
  }
}

// 辅助函数
const getFileIconColor = (type: string): string => {
  switch (type) {
    case 'markdown': return 'text-blue-500'
    case 'pdf': return 'text-red-500'
    case 'doc':
    case 'docx': return 'text-indigo-500'
    case 'ppt':
    case 'pptx': return 'text-orange-500'
    case 'xls':
    case 'xlsx': return 'text-green-500'
    case 'txt': return 'text-gray-500'
    default: return 'text-morandi-500'
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (date: Date): string => {
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: 'short', 
    day: 'numeric' 
  })
}

// 事件处理
const handleTextSelection = () => {
  const selection = window.getSelection()
  if (selection && selection.toString().trim()) {
    const selectedText = selection.toString().trim()
    if (selectedText.length > 0) {
      docsStore.setSelectedText(selectedText)
      
      // 获取选择区域的位置
      const range = selection.getRangeAt(0)
      const rect = range.getBoundingClientRect()
      
      // 显示翻译浮窗
      docsStore.showTranslationPopup(rect.right + 10, rect.top)
      docsStore.translateText(selectedText)
    }
  }
}

const addDocument = () => {
  if (!newDocumentName.value.trim()) return
  
  const getFileExtension = (type: string) => {
    switch (type) {
      case 'markdown': return '.md'
      case 'pdf': return '.pdf'
      case 'doc': return '.doc'
      case 'docx': return '.docx'
      case 'ppt': return '.ppt'
      case 'pptx': return '.pptx'
      case 'xls': return '.xls'
      case 'xlsx': return '.xlsx'
      case 'txt': return '.txt'
      default: return '.txt'
    }
  }
  
  const newDoc = {
    name: newDocumentName.value + getFileExtension(newDocumentType.value),
    type: newDocumentType.value,
    size: Math.floor(Math.random() * 10485760),
    modifiedAt: new Date(),
    path: `/文档/${docsStore.currentCategoryData?.folder_name}/${newDocumentName.value}`,
    categoryId: docsStore.currentCategory,
    summary: '用户添加的文档',
    content: newDocumentType.value === 'markdown' ? '# ' + newDocumentName.value + '\n\n这是一个新文档，请开始编辑内容。' : undefined
  }
  
  docsStore.addDocument(docsStore.currentCategory, newDoc)
  
  // 重置表单
  showAddDocumentDialog.value = false
  newDocumentName.value = ''
  newDocumentType.value = 'markdown'

}

// 创建新分类的方法
const createNewCategory = async () => {
  const categoryName = newCategoryName.value.trim()
  if (!categoryName) return

  try {
    // 首先加载文件夹层级以获取系统文件夹信息
    await driveStore.loadFolderHierarchy()

    console.log('所有文件夹层级:', driveStore.folderHierarchy)
    const systemFolders = driveStore.folderHierarchy.filter(f => f.folder_type === 'system')
    console.log('系统文件夹:', systemFolders)
    console.log('系统文件夹名称:', systemFolders.map(f => f.folder_name))

    // 查找"书单"或"文档"系统文件夹
    const docSystemFolder = driveStore.folderHierarchy.find(
      folder => folder.folder_type === 'system' && (folder.folder_name === '书单' || folder.folder_name === '文档')
    )

    console.log('找到的文档文件夹:', docSystemFolder)

    if (!docSystemFolder) {
      console.error('未找到书单/文档系统文件夹，可用的系统文件夹:', driveStore.folderHierarchy.filter(f => f.folder_type === 'system'))

      // 尝试从根目录加载文件夹
      await driveStore.loadFolderContent(0)
      const rootDocFolder = driveStore.folders.find(
        folder => folder.folder_type === 'system' && (folder.folder_name === '书单' || folder.folder_name === '文档')
      )

      if (rootDocFolder) {
        console.log('从根目录找到文档文件夹:', rootDocFolder)
        // 使用找到的文件夹继续创建
        await API.folder.createFolder({
          name: categoryName,
          parent_id: rootDocFolder.id,
          folder_type: FolderTypeEnum.BOOKLIST
        })

        toast.success(`书单 "${categoryName}" 创建成功`)
        await docsStore.loadCategoriesFromDrive()
        showCreateCategoryDialog.value = false
        newCategoryName.value = ''
        return
      }

      toast.error('未找到书单系统文件夹')
      return
    }

    // 直接调用API创建书单文件夹
    await API.folder.createFolder({
      name: categoryName,
      parent_id: docSystemFolder.id,
      folder_type: FolderTypeEnum.BOOKLIST
    })

    toast.success(`书单 "${categoryName}" 创建成功`)
    // 重新加载文档数据以显示新创建的书单
    await docsStore.loadCategoriesFromDrive()
  } catch (error) {
    console.error('创建书单失败:', error)
    toast.error('创建书单失败')
  }

  showCreateCategoryDialog.value = false
  newCategoryName.value = ''
}

// 上传文档文件的方法
const uploadDocumentFiles = async () => {
  if (selectedDocumentFiles.value.length === 0) return

  // 获取当前分类的路径，如果没有则上传到书单根目录
  const currentCategory = docsStore.currentCategoryData
  const targetPath = currentCategory ? `/书单/${currentCategory.folder_name}` : '/书单'

  // 确保WebSocket连接已建立
  ensureWebSocketForUpload()

  const fileCount = selectedDocumentFiles.value.length

  // 使用 TransferStore 处理上传
  await transferStore.uploadFiles(selectedDocumentFiles.value, targetPath)

  showUploadDocumentDialog.value = false
  selectedDocumentFiles.value = []
  toast.success(`已开始上传 ${fileCount} 个文档文件`)

  // 监听上传完成事件，刷新分类数据
  const unsubscribe = onUploadComplete(() => {
    docsStore.loadCategoriesFromDrive()
    unsubscribe() // 取消监听
  })
}

// 新建Markdown文档的方法
const createMarkdownDocument = async () => {
  const docName = newDocumentName.value.trim()
  if (!docName) return

  try {
    // 获取当前笔记本的ID
    const currentBooklist = docsStore.currentBooklist
    if (!currentBooklist?.id) {
      console.error('当前没有选中的笔记本')
      return
    }

    // 调用DocsStore的API创建Markdown文档
    const success = await docsStore.createMarkdownDocument(docName, currentBooklist.id)

    if (success) {
      showAddMarkdownDialog.value = false
      newDocumentName.value = ''
    }
  } catch (error) {
    console.error('创建Markdown文档失败:', error)
  }
}

// 处理文档文件选择
const handleDocumentFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  selectedDocumentFiles.value = files
}

// 右键菜单相关方法
const showDocumentContextMenu = (event: MouseEvent, doc: any) => { // Changed to any as Document type is removed
  selectedDocument.value = doc
  
  const menuItems: ContextMenuItem[] = [
    {
      id: 'open',
      label: '打开',
      action: () => openDocument()
    },
    {
      id: 'rename',
      label: '重命名',
      action: () => showRenameDocumentDialog()
    },
    {
      id: 'separator1',
      separator: true
    },
    {
      id: 'delete',
      label: '删除',
      type: 'danger',
      action: () => deleteDocument()
    }
  ]
  
  contextMenuStore.showContextMenu(event, menuItems, doc)
}

const openDocument = () => {
  if (selectedDocument.value) {
    docsStore.setCurrentDocument(selectedDocument.value)
  }
}

const showRenameDocumentDialog = () => {
  if (selectedDocument.value) {
    renameDocValue.value = selectedDocument.value.file_name
    showRenameDocDialog.value = true
    
    // 在下一个tick自动聚焦并选中文本
    nextTick(() => {
      if (renameDocInput.value) {
        renameDocInput.value.focus()
        renameDocInput.value.select()
      }
    })
  }
}

const confirmRenameDocument = async () => {
  const newName = renameDocValue.value.trim()
  if (!newName || !selectedDocument.value) return

  // 检查名称是否与原名称相同
  if (newName === selectedDocument.value.file_name) {
    cancelRenameDocument()
    return
  }

  // 检查名称是否包含非法字符
  const invalidChars = /[<>:"/\\|?*]/
  if (invalidChars.test(newName)) {
    toast.error('文件名不能包含以下字符：< > : " / \\ | ? *')
    return
  }

  const oldName = selectedDocument.value.file_name
  
  try {
    // 调用DocsStore的真正重命名方法
    const success = await docsStore.renameDocument(selectedDocument.value.id, newName)
    
    if (success) {
      showRenameDocDialog.value = false
      renameDocValue.value = ''
      // DocsStore 已经显示了成功提示
    }
  } catch (error) {
    console.error('重命名文档失败:', error)
    toast.error('重命名文档失败，请重试')
  }
}

const cancelRenameDocument = () => {
  showRenameDocDialog.value = false
  renameDocValue.value = ''
}

const deleteDocument = async () => {
  if (selectedDocument.value) {
    const docName = selectedDocument.value.file_name
    if (confirm(`确定要删除文档 "${docName}" 吗？`)) {
      // 调用DocsStore的删除方法
      const success = await docsStore.deleteDocument(selectedDocument.value.id)
      if (success) {
        toast.success(`文档 "${docName}" 已删除`)
      }
    }
  }
}

// 分类右键菜单相关方法
const handleCategoryContextMenu = (event: MouseEvent, category: FolderInfo) => {
  selectedCategory.value = category
  
  const menuItems: ContextMenuItem[] = [
    {
      id: 'open',
      label: '打开',
      action: () => openCategory()
    },
    {
      id: 'rename',
      label: '重命名',
      action: () => showRenameCategoryDialogFn()
    },
    {
      id: 'separator1',
      separator: true
    },
    {
      id: 'delete',
      label: '删除',
      type: 'danger',
      action: () => deleteCategory()
    }
  ]
  
  contextMenuStore.showContextMenu(event, menuItems, category)
}

const openCategory = () => {
  if (selectedCategory.value) {
    docsStore.setCurrentCategory(selectedCategory.value.id)
  }
}

const showRenameCategoryDialogFn = () => {
  if (selectedCategory.value) {
    renameCategoryValue.value = selectedCategory.value.folder_name
    showRenameCategoryDialog.value = true
    
    // 在下一个tick自动聚焦并选中文本
    nextTick(() => {
      if (renameCategoryInput.value) {
        renameCategoryInput.value.focus()
        renameCategoryInput.value.select()
      }
    })
  }
}

const confirmRenameCategory = () => {
  const newName = renameCategoryValue.value.trim()
  if (!newName || !selectedCategory.value) return

  // 检查名称是否与原名称相同
  if (newName === selectedCategory.value.folder_name) {
    cancelRenameCategory()
    return
  }

  // 检查名称是否包含非法字符
  const invalidChars = /[<>:"/\\|?*]/
  if (invalidChars.test(newName)) {
    toast.error('文件名不能包含以下字符：< > : " / \\ | ? *')
    return
  }

  const oldName = selectedCategory.value.folder_name
  
  // 调用DocsStore的重命名方法
  selectedCategory.value.folder_name = newName
  
  showRenameCategoryDialog.value = false
  renameCategoryValue.value = ''
  selectedCategory.value = null
  toast.success(`分类 "${oldName}" 已重命名为 "${newName}"`)
}

const cancelRenameCategory = () => {
  showRenameCategoryDialog.value = false
  renameCategoryValue.value = ''
  selectedCategory.value = null
}

const deleteCategory = () => {
  if (selectedCategory.value) {
    const categoryName = selectedCategory.value.folder_name
    if (confirm(`确定要删除分类 "${categoryName}" 吗？该操作将同时删除分类下的所有文档。`)) {
      // 调用DocsStore的删除方法
      docsStore.deleteCategory(selectedCategory.value.id)
      toast.success(`分类 "${categoryName}" 已删除`)
    }
  }
}
</script>

<style scoped>
/* 自定义滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Prose 样式优化 */
.prose {
  color: #374151;
  max-width: none;
}

.prose h1 {
  color: #1f2937;
  font-weight: 800;
  font-size: 1.875rem;
  margin-bottom: 1rem;
  line-height: 1.2;
}

.prose h2 {
  color: #1f2937;
  font-weight: 700;
  font-size: 1.5rem;
  margin-top: 2rem;
  margin-bottom: 1rem;
  line-height: 1.3;
}

.prose h3 {
  color: #374151;
  font-weight: 600;
  font-size: 1.25rem;
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
  line-height: 1.4;
}

.prose p {
  margin-bottom: 1rem;
  line-height: 1.7;
}

.prose code {
  background-color: #f3f4f6;
  color: #dc2626;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  font-size: 0.875em;
}

.prose pre {
  background-color: #1f2937;
  color: #f9fafb;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin: 1rem 0;
}

.prose pre code {
  background-color: transparent;
  color: inherit;
  padding: 0;
}

.prose ul {
  margin: 1rem 0;
  padding-left: 1.5rem;
}

.prose li {
  margin-bottom: 0.5rem;
  line-height: 1.6;
}

.prose input[type="checkbox"] {
  margin-right: 0.5rem;
}

/* 文本选择样式 */
::selection {
  background-color: #bfdbfe;
  color: #1e40af;
}

/* 行号限制 */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ghost {
  opacity: 0.5;
  background: #c8ebf9;
}
</style> 