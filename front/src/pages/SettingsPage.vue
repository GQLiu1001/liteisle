<template>
  <div class="min-h-full bg-liteisle-bg p-6">
    <div class="max-w-7xl mx-auto">
      <div class="h-[calc(100vh-10rem)] flex gap-6">
        <!-- 第一栏：设置分类导航 -->
        <div class="card w-64 flex-shrink-0">
          <div class="h-full flex flex-col p-4">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-bold text-morandi-900">系统设置</h2>
            </div>
            <nav class="space-y-2 flex-1 overflow-y-auto">
              <button
                v-for="category in settingsStore.categories"
                :key="category.id"
                @click="settingsStore.setCurrentCategoryId(category.id)"
                :class="[
                  'w-full flex items-center gap-3 p-3 rounded-lg text-left transition-all duration-200',
                  settingsStore.currentCategoryId === category.id
                    ? 'bg-teal-100 text-teal-800 border border-teal-300'
                    : 'text-morandi-700 hover:bg-morandi-100 border border-transparent'
                ]"
              >
                <component :is="category.icon" :size="20" class="flex-shrink-0" />
                <div class="flex-1 min-w-0">
                  <div class="font-medium truncate">{{ category.name }}</div>
                  <div class="text-xs text-morandi-500 truncate">{{ category.description }}</div>
                </div>
              </button>
            </nav>
          </div>
        </div>

        <!-- 第二栏：设置项 -->
        <div class="card flex-1 min-w-0">
          <div class="h-full p-6 overflow-y-auto">
            <!-- 通用设置 -->
            <div v-if="settingsStore.currentCategoryId === 'general'">
              <h3 class="text-xl font-bold text-morandi-900 mb-6">通用设置</h3>
              <div class="space-y-6">
                <!-- <div class="flex items-center justify-between">
                  <div>
                    <h4 class="font-medium text-morandi-900">开机自启</h4>
                    <p class="text-sm text-morandi-600">应用在电脑开机时自动运行</p>
                  </div>
                  <input 
                    type="checkbox" 
                    v-model="settingsStore.settings.launchAtStartup" 
                    class="w-5 h-5 text-teal-600 rounded focus:ring-teal-500" 
                  />
                </div> -->
                
                <div class="flex items-center justify-between">
                  <div>
                    <h4 class="font-medium text-morandi-900">下载目录</h4>
                    <p class="text-sm text-morandi-600">文件下载的默认保存位置</p>
                  </div>
                  <div class="flex items-center gap-2">
                    <input 
                      type="text" 
                      v-model="settingsStore.settings.downloadDirectory" 
                      @change="settingsStore.saveSettings()"
                      class="px-3 py-1 text-sm border border-morandi-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 select-text w-80"
                      placeholder="例如: D:\Downloads"
                      style="user-select: text !important;"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- PicGo图片上传设置 -->
            <div v-else-if="settingsStore.currentCategoryId === 'picgo'">
              <h3 class="text-xl font-bold text-morandi-900 mb-6">图片上传设置</h3>
              <div class="space-y-6">
                <div class="flex items-center justify-between">
                  <div>
                    <h4 class="font-medium text-morandi-900">启用PicGo上传</h4>
                    <p class="text-sm text-morandi-600">开启后在Markdown编辑器中粘贴图片将自动上传</p>
                  </div>
                  <input 
                    type="checkbox" 
                    v-model="settingsStore.settings.picgoEnabled" 
                    @change="settingsStore.saveSettings()"
                    class="w-5 h-5 text-teal-600 rounded focus:ring-teal-500" 
                  />
                </div>
                
                <div class="flex items-center justify-between">
                  <div>
                    <h4 class="font-medium text-morandi-900">PicGo应用路径</h4>
                    <p class="text-sm text-morandi-600">输入PicGo应用的exe文件完整路径</p>
                  </div>
                  <div class="flex items-center gap-2">
                    <input 
                      type="text" 
                      v-model="settingsStore.settings.picgoPath" 
                      @change="settingsStore.saveSettings()"
                      class="px-3 py-1 text-sm border border-morandi-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 select-text w-80"
                      placeholder="例如: D:\Programs\PicGo\PicGo.exe"
                      style="user-select: text !important;"
                    />
                  </div>
                </div>
                
                <!-- 测试上传 -->
                <div class="flex items-center justify-between">
                  <div>
                    <h4 class="font-medium text-morandi-900">测试上传</h4>
                    <p class="text-sm text-morandi-600">测试PicGo是否能正常上传图片（需要先配置图床）</p>
                  </div>
                  <button 
                    @click="testPicGoUpload"
                    :disabled="isTestingConnection"
                    class="px-3 py-1 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                  >
                    {{ isTestingConnection ? '测试中...' : '测试上传' }}
                  </button>
                </div>
                
                <!-- 使用说明 -->
                <div class="bg-blue-50 border-l-4 border-blue-400 p-4 rounded-lg">
                  <div class="flex">
                    <div class="flex-shrink-0">
                      <svg class="h-5 w-5 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                      </svg>
                    </div>
                    <div class="ml-3">
                      <h3 class="text-sm font-medium text-blue-800">使用说明</h3>
                      <div class="mt-2 text-sm text-blue-700">
                        <ul class="list-disc list-inside space-y-1">
                          <li>下载并安装PicGo应用：<a href="https://molunerfinn.com/PicGo/" target="_blank" class="text-blue-600 hover:underline">https://molunerfinn.com/PicGo/</a></li>
                          <li>在PicGo中配置你喜欢的图床（如七牛云、阿里云OSS、腾讯云COS等）</li>
                          <li>选择PicGo应用的安装路径（通常是PicGo.exe文件）</li>
                          <li>启用PicGo上传后，在Markdown编辑器中按Ctrl+V粘贴图片将自动上传</li>
                          <li>上传成功后会自动获取图片URL并插入到编辑器中</li>
                          <li>无需额外配置，就像在Typora中使用一样简单</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 账户与云盘设置 -->
            <div v-else-if="settingsStore.currentCategoryId === 'account'" class="h-full flex flex-col items-center justify-center text-center">
              <!-- 用户头像 -->
              <div class="relative mb-4 group">
                <div 
                  @click="triggerFileSelect"
                  class="w-32 h-32 bg-teal-100 rounded-full flex items-center justify-center cursor-pointer hover:bg-teal-200 transition-colors relative overflow-hidden"
                >
                  <img 
                    :src="authStore.user?.avatar || defaultUserPic" 
                    alt="用户头像" 
                    class="w-full h-full object-cover"
                  />
                  
                  <!-- 悬浮提示 -->
                  <div class="absolute inset-0 bg-black bg-opacity-50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <Upload :size="32" class="text-white" />
                  </div>
                </div>
                
                <!-- 文件输入框 -->
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  @change="handleFileChange"
                  class="hidden"
                />
              </div>
              
              <!-- 用户名和邮箱 -->
              <h3 class="text-xl font-bold text-morandi-900 mb-2">{{ settingsStore.settings.username }}</h3>
              <p class="text-sm text-morandi-500 mb-2">{{ authStore.user?.email || 'user@example.com' }}</p>
              <p class="text-xs text-morandi-400 mb-6">点击头像更换头像</p>
              
              <!-- 云盘容量信息 -->
              <div class="mb-6 w-full max-w-sm">
                <div class="flex items-center justify-center gap-2 mb-3">
                  <HardDrive :size="20" class="text-morandi-600" />
                  <h4 class="font-medium text-morandi-900">云盘容量</h4>
                </div>

                <div class="space-y-2">
                  <div class="flex justify-between text-sm">
                    <span class="text-morandi-600">已使用 {{ authStore.getStorageInfo.used }} / {{ authStore.getStorageInfo.quota }}</span>
                    <span class="font-medium text-morandi-900">{{ authStore.getStorageInfo.percentage.toFixed(1) }}%</span>
                  </div>

                  <!-- 进度条 -->
                  <div class="w-full bg-morandi-200 rounded-full h-2.5">
                    <div
                      class="bg-gradient-to-r from-teal-400 to-teal-600 h-2.5 rounded-full transition-all duration-300"
                      :style="{ width: authStore.getStorageInfo.percentage + '%' }"
                    ></div>
                  </div>

                  <div class="flex justify-between text-xs text-morandi-500">
                    <span>{{ authStore.getStorageInfo.used }}</span>
                    <span>{{ authStore.getStorageInfo.quota }}</span>
                  </div>
                </div>
              </div>
              

              
              <!-- 操作按钮 -->
              <div class="flex gap-3">
                <button
                  @click="showChangePasswordDialog = true"
                  class="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 transition-colors text-sm"
                >
                  修改密码
                </button>
                <button
                  v-if="authStore.user?.avatar"
                  @click="resetUserPicture"
                  class="px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition-colors text-sm"
                >
                  恢复默认头像
                </button>
              </div>
            </div>

            <!-- 我的分享 -->
            <div v-else-if="settingsStore.currentCategoryId === 'shares'" class="h-full flex flex-col">
              <h3 class="text-xl font-bold text-morandi-900 mb-6 flex-shrink-0">我的分享</h3>
              
              <!-- 分享列表 -->
              <div class="space-y-4 flex-1">
                <!-- 列表容器 -->
                <div 
                  class="bg-transparent rounded-lg h-full overflow-y-auto"
                  @scroll="handleShareScroll"
                  style="min-height: 400px; max-height: calc(100vh - 300px);"
                >
                  <div v-if="(!shareRecords || shareRecords.length === 0) && !isLoadingShare" class="p-8 text-center text-morandi-500">
                    暂无分享记录
                  </div>
                  <div v-else class="space-y-3">
                    <div 
                      v-for="share in shareRecords" 
                      :key="share.id"
                      class="p-4 bg-white/70 backdrop-blur-sm rounded-lg border border-morandi-200/50 hover:bg-white/90 hover:border-morandi-300 transition-all duration-200"
                    >
                      <div class="flex flex-col">
                        <!-- 第一行：文件名、标签和操作按钮 -->
                        <div class="flex items-center justify-between mb-2">
                          <div class="flex items-center gap-3">
                            <div class="text-sm font-medium text-morandi-900">
                              {{ share.share_item_name }}
                            </div>
                            <div class="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full">
                              {{ share.folder_id ? '文件夹' : '文件' }}
                            </div>
                            <div
                              :class="[
                                'px-2 py-1 text-xs rounded-full',
                                !share.expire_time || new Date(share.expire_time) > new Date() ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                              ]"
                            >
                              {{ !share.expire_time ? '永久有效' :
                                  new Date(share.expire_time) > new Date() ? '有效' : '已过期' }}
                            </div>
                          </div>
                          <div class="flex items-center gap-2">
                            <button 
                              @click="copyShareInfo(share)"
                              class="px-3 py-1 text-xs bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-colors"
                            >
                              一键复制
                            </button>
                            <button 
                              @click="cancelShare(share.id)"
                              class="px-3 py-1 text-xs bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition-colors"
                            >
                              取消分享
                            </button>
                          </div>
                        </div>
                        
                        <!-- 第二行：分享信息 -->
                        <div class="text-sm text-morandi-600 space-y-1">
                          <div>分享码: {{ share.share_token }}{{ share.share_password ? '&' + share.share_password : '' }}</div>
                          <div>有效期: {{ share.expire_time ? new Date(share.expire_time).toLocaleDateString() : '永久' }}</div>
                          <div>分享时间: {{ new Date(share.create_time).toLocaleDateString() }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                                      <!-- 加载更多指示器 -->
                    <div v-if="isLoadingShare" class="flex justify-center py-4">
                    <div class="w-4 h-4 border-2 border-teal-600 border-t-transparent rounded-full animate-spin"></div>
                  </div>
                  
                  <!-- 没有更多数据提示 -->
                                      <div v-if="!shareHasMore && shareRecords && shareRecords.length > 0" class="text-center py-4 text-sm text-morandi-500">
                    没有更多分享记录了
                  </div>
                </div>
              </div>
            </div>

            <!-- 专注记录 -->
            <div v-else-if="settingsStore.currentCategoryId === 'focus'" class="h-full flex flex-col">
              <h3 class="text-xl font-bold text-morandi-900 mb-6 flex-shrink-0">专注记录</h3>
              
              <!-- 原始专注记录 -->
              <div class="space-y-4 flex-1">
                <!-- 记录列表 -->
                <div 
                  class="bg-transparent rounded-lg h-full overflow-y-auto"
                  @scroll="handleScroll"
                  style="min-height: 400px; max-height: calc(100vh - 300px);"
                >
                                      <div v-if="(!focusRecords || focusRecords.length === 0) && !isLoadingFocus" class="p-8 text-center text-morandi-500">
                    暂无专注记录
                  </div>
                  <div v-else class="space-y-3">
                    <div
                      v-for="record in focusRecords"
                      :key="record.id"
                      class="p-4 bg-white/70 backdrop-blur-sm rounded-lg border border-morandi-200/50 hover:bg-white/90 hover:border-morandi-300 transition-all duration-200"
                    >
                      <div class="flex items-center justify-between">
                        <div class="flex-1">
                          <div class="flex items-center gap-3 mb-2">
                            <div class="text-sm font-medium text-morandi-900">
                              {{ formatDate(record.create_time) }}
                            </div>
                            <div class="px-2 py-1 bg-teal-100 text-teal-800 text-xs rounded-full">
                              专注
                            </div>
                          </div>
                          <div class="text-sm text-morandi-600">
                            <div>专注时长: {{ formatMinutes(record.focus_minutes) }}</div>
                          </div>
                        </div>
                        <div class="text-right">
                          <div class="text-lg font-bold text-morandi-900">
                            {{ formatMinutes(record.focus_minutes) }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 加载更多指示器 -->
                  <div v-if="isLoadingFocus" class="flex justify-center py-4">
                    <div class="w-4 h-4 border-2 border-teal-600 border-t-transparent rounded-full animate-spin"></div>
                  </div>
                  
                  <!-- 没有更多数据提示 -->
                  <div v-if="!hasMoreRecords && focusRecords.length > 0" class="text-center py-4 text-sm text-morandi-500">
                    没有更多专注记录了
                  </div>
                </div>
              </div>
            </div>

            <!-- 关于 -->
            <div v-else-if="settingsStore.currentCategoryId === 'about'" class="h-full flex flex-col items-center justify-center text-center">
              <div class="w-20 h-20 mb-4 bg-teal-100 rounded-full flex items-center justify-center overflow-hidden">
                <img :src="logoPic" alt="LiteIsle Logo" class="h-16 w-16 object-contain" />
              </div>
              
              <h3 class="text-xl font-bold text-morandi-900 mb-2">轻屿记 LiteIsle</h3>
              <p class="text-sm text-morandi-500 mb-6">版本 {{ settingsStore.settings.version }}</p>
              
              <!-- <div class="space-y-3">
                <button 
                  @click="checkForUpdates"
                  :disabled="isCheckingUpdates"
                  class="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 transition-colors text-sm disabled:opacity-50"
                >
                  {{ isCheckingUpdates ? '检查中...' : '检查更新' }}
                </button>
              </div> -->
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 修改密码对话框 -->
  <div 
    v-if="showChangePasswordDialog" 
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    @click="showChangePasswordDialog = false"
  >
    <div 
      class="bg-white rounded-lg p-6 w-96 max-w-[90vw]"
      @click.stop
    >
      <h3 class="text-lg font-bold text-morandi-900 mb-4">修改密码</h3>
      <form @submit.prevent="submitPasswordChange">
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-1">当前密码</label>
            <input
              type="password"
              v-model="passwordForm.currentPassword"
              required
              class="w-full px-3 py-2 border border-morandi-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 select-text"
              style="user-select: text !important;"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-1">新密码</label>
            <input
              type="password"
              v-model="passwordForm.newPassword"
              required
              minlength="6"
              class="w-full px-3 py-2 border border-morandi-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 select-text"
              style="user-select: text !important;"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-morandi-700 mb-1">确认新密码</label>
            <input
              type="password"
              v-model="passwordForm.confirmPassword"
              required
              minlength="6"
              class="w-full px-3 py-2 border border-morandi-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 select-text"
              style="user-select: text !important;"
            />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button 
            type="button"
            @click="showChangePasswordDialog = false"
            class="px-4 py-2 text-morandi-700 border border-morandi-300 rounded-lg hover:bg-morandi-50 transition-colors"
          >
            取消
          </button>
          <button 
            type="submit"
            :disabled="isChangingPassword"
            class="px-4 py-2 bg-teal-600 text-white rounded-lg hover:bg-teal-700 transition-colors disabled:opacity-50"
          >
            {{ isChangingPassword ? '修改中...' : '确认修改' }}
          </button>
        </div>
      </form>
    </div>
  </div>



  <!-- 移除了下载目录输入对话框（已改为直接输入） -->
</template>

<script setup lang="ts">
import { useFocusStore } from '@/store/FocusStore';
import { useAuthStore } from '@/store/AuthStore';
import { useSettingsStore } from '@/store/SettingsStore';
import { useShareStore } from '@/store/ShareStore';
import { useToast } from 'vue-toastification';
import { API } from '@/utils/api';
import { ref, computed, onMounted, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { HardDrive, FileText, Clock, Upload } from 'lucide-vue-next';
// 导入图片资源
import defaultUserPicUrl from '/defaultuserpic (2).png'
import logoPicUrl from '/logopic.png'

// 默认用户头像
const defaultUserPic = defaultUserPicUrl;
const logoPic = logoPicUrl;

// 使用存储
const settingsStore = useSettingsStore();
const focusStore = useFocusStore();
const authStore = useAuthStore();
const shareStore = useShareStore();
const toast = useToast();

// 修改密码对话框状态
const showChangePasswordDialog = ref(false);

// 修改密码表单数据
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 修改密码状态
const isChangingPassword = ref(false);

// 检查更新状态
const isCheckingUpdates = ref(false);

// 头像上传相关
const fileInput = ref<HTMLInputElement | null>(null);

// PicGo连接测试状态
const isTestingConnection = ref(false);

// 移除了下载目录输入对话框状态（已改为直接输入）

// 触发文件选择
const triggerFileSelect = () => {
  if (fileInput.value) {
    fileInput.value.click();
  }
};

// 处理文件选择
const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  
  if (file) {
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      alert('请选择图片文件');
      return;
    }
    
    // 验证文件大小 (最大5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert('图片文件大小不能超过5MB');
      return;
    }
    
    try {
      await authStore.updateUserPicture(file);
      alert('头像更新成功');
    } catch (error) {
      alert('头像更新失败: ' + (error instanceof Error ? error.message : '未知错误'));
    }
    
    // 清空文件输入框
    target.value = '';
  }
};

// 重置用户头像
const resetUserPicture = async () => {
  if (confirm('确定要恢复默认头像吗？')) {
    try {
      await authStore.resetAvatar();
    } catch (error) {
      alert('重置头像失败: ' + (error instanceof Error ? error.message : '未知错误'));
    }
  }
};



// 修改密码对话框提交
const submitPasswordChange = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.error('新密码与确认密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toast.error('新密码长度至少6位')
    return
  }
  isChangingPassword.value = true
  try {
    const resp: any = await API.auth.resetPassword({
      old_password: passwordForm.value.currentPassword,
      new_password: passwordForm.value.newPassword,
      confirm_password: passwordForm.value.confirmPassword
    })
    if (resp.data && resp.data.code === 200) {
      toast.success('密码修改成功')
      showChangePasswordDialog.value = false
      passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    } else {
      toast.error(resp.data?.message || '密码修改失败')
    }
  } catch (e: any) {
    toast.error(e.response?.data?.message || '密码修改失败')
  } finally {
    isChangingPassword.value = false
  }
}

// 专注记录相关状态
const currentFocusPage = ref(1)
const focusPageSize = ref(10)
const hasMoreRecords = ref(true)
const isLoadingRecords = ref(false)

// 从SettingsStore获取真实的专注记录和分享记录
const { focusRecords, focusTotal, isLoadingFocus, shareRecords, isLoadingShare, shareHasMore } = storeToRefs(settingsStore)

// 专注统计
const focusStats = computed(() => ({
  totalMinutes: focusStore.calendarData?.total_focus_minutes || 0, // 使用日历数据中的总专注分钟数
  totalDays: focusStore.totalFocusCount,
  streakDays: focusStore.getFocusStreak() // 调用方法获取连续天数
}));

// 格式化方法
const formatTotalTime = (minutes: number): string => {
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = Math.floor(minutes % 60);
  
  if (hours >= 1) {
    return `${hours}h ${remainingMinutes}m`;
  }
  return `${remainingMinutes}m`;
};

const formatMinutes = (minutes: number | undefined): string => {
  if (minutes === undefined || minutes === null || isNaN(minutes)) {
    console.warn('Invalid minutes value:', minutes);
    return '0m';
  }

  const validMinutes = Math.floor(minutes);

  if (validMinutes >= 60) {
    const hours = Math.floor(validMinutes / 60);
    const remainingMinutes = validMinutes % 60;
    return remainingMinutes > 0 ? `${hours}h ${remainingMinutes}m` : `${hours}h`;
  }
  return `${validMinutes}m`;
};

const formatDate = (dateStr: string): string => {
  if (!dateStr) {
    return 'Invalid Date';
  }

  const date = new Date(dateStr);

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    console.warn('Invalid date string:', dateStr);
    return 'Invalid Date';
  }

  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  if (date.toDateString() === today.toDateString()) {
    return '今天';
  } else if (date.toDateString() === yesterday.toDateString()) {
    return '昨天';
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: 'long',
      day: 'numeric',
      weekday: 'short'
    });
  }
};

const formatSessionDate = (timestamp: number): string => {
  const date = new Date(timestamp);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  
  if (date.toDateString() === today.toDateString()) {
    return '今天';
  } else if (date.toDateString() === yesterday.toDateString()) {
    return '昨天';
  } else {
    return date.toLocaleDateString('zh-CN', { 
      month: 'long', 
      day: 'numeric',
      weekday: 'short'
    });
  }
};

const formatSessionTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit'
  });
};

// 加载专注记录
const loadFocusRecords = async (page: number = 1, reset: boolean = false) => {
  try {
    settingsStore.isLoadingFocus = true
    console.log('开始加载专注记录，页码:', page)

    const response = await API.focus.getRecords(page, 10)
    console.log('专注记录API响应:', response)

    if (response.data && response.data.code === 200 && response.data.data) {
      const recordsData = response.data.data
      console.log('专注记录数据:', recordsData)

      if (page === 1) {
        settingsStore.focusRecords = recordsData.records || []
      } else {
        settingsStore.focusRecords.push(...(recordsData.records || []))
      }

      settingsStore.focusCurrentPage = page
      settingsStore.focusTotal = recordsData.total || 0
      settingsStore.hasMoreRecords = settingsStore.focusRecords.length < (recordsData.total || 0)
    } else {
      console.warn('专注记录API响应格式错误:', response.data)
      toast.error(response.data?.message || '加载专注记录失败')
    }
  } catch (error) {
    console.error('加载专注记录失败:', error)
    toast.error('加载专注记录失败')
  } finally {
    settingsStore.isLoadingFocus = false
  }
}

// 加载更多专注记录
const loadMoreFocusRecords = async () => {
  if (!hasMoreRecords.value || isLoadingFocus.value) return
  await loadFocusRecords(currentFocusPage.value + 1)
}

// 刷新专注记录
const refreshFocusRecords = async () => {
  await loadFocusRecords(1)
}

// 滚动加载处理
const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement;
  const scrollTop = target.scrollTop;
  const scrollHeight = target.scrollHeight;
  const clientHeight = target.clientHeight;
  
  // 当滚动到接近底部时触发加载更多（距离底部50px）
  if (scrollTop + clientHeight >= scrollHeight - 50 && hasMoreRecords.value && !isLoadingRecords.value) {
    loadMoreFocusRecords();
  }
};

// 分享列表滚动加载处理
const handleShareScroll = (event: Event) => {
  const target = event.target as HTMLElement;
  const scrollTop = target.scrollTop;
  const scrollHeight = target.scrollHeight;
  const clientHeight = target.clientHeight;
  
  // 当滚动到接近底部时触发加载更多（距离底部50px）
  if (scrollTop + clientHeight >= scrollHeight - 50 && shareHasMore.value && !isLoadingShare.value) {
    settingsStore.loadMoreShareItems();
  }
};

// 移除了选择下载目录函数（已改为直接输入）

// 移除了选择PicGo路径函数（已改为直接输入）

// 移除了下载目录确认和取消函数（已改为直接输入）

// 测试PicGo上传
const testPicGoUpload = async () => {
  if (!settingsStore.settings.picgoPath) {
    alert('请先选择PicGo应用路径');
    return;
  }
  
  isTestingConnection.value = true;
  
  try {
    // 简单验证：检查路径是否看起来是PicGo
    const path = settingsStore.settings.picgoPath.toLowerCase();
    if (!path.includes('picgo') || !path.endsWith('.exe')) {
      alert('⚠️ 路径可能不正确\n请确保选择的是 PicGo.exe 文件');
      return;
    }
    
    // 测试PicGo HTTP服务连接
    const port = 36677; // PicGo默认端口
    
    // 尝试POST到/upload端点测试连接（这是PicGo的正确端点和方法）
    try {
      const response = await fetch(`http://127.0.0.1:${port}/upload`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({}), // 发送空请求测试PicGo服务连接
        signal: AbortSignal.timeout(5000) // 5秒超时
      });
      
      // 即使返回错误状态码，但不是404说明服务正在运行
      if (response.status !== 404) {
        // 尝试读取响应内容来确认这是PicGo
        const responseText = await response.text();
        
        alert(`✅ PicGo连接测试成功！

🎉 PicGo HTTP服务正在运行
📁 路径: ${settingsStore.settings.picgoPath}
🌐 服务地址: http://127.0.0.1:${port}/upload
📊 响应状态: ${response.status}

✨ 现在可以在Markdown编辑器中使用Ctrl+V粘贴图片自动上传了！

📋 使用步骤:
1. 复制图片到剪贴板（截图或复制文件）
2. 在Markdown编辑器中按Ctrl+V
3. 图片将自动上传并插入链接

⚠️ 重要提醒：
• 请确保已在PicGo中配置好图床设置
• 如使用Gitee图床，请检查Token权限和仓库访问
• 建议图片大小控制在1MB以内
• 如遇上传失败，系统会自动重试并提供备选方案`);
      } else {
        throw new Error('PicGo服务未响应 (404)');
      }
    } catch (error) {
      
      // 如果是网络错误，说明端口没有服务在运行
      if (error instanceof Error && error.name === 'TypeError' && error.message.includes('fetch')) {
        throw new Error(`❌ 无法连接到PicGo服务

🔧 解决方案:
1. 启动PicGo应用程序
2. 在PicGo设置中开启"HTTP监听服务"
3. 确认端口号为36677（默认）
4. 检查防火墙是否阻止了连接

📂 当前PicGo路径: ${settingsStore.settings.picgoPath}`);
              } else {
          throw new Error(`连接测试失败: ${error instanceof Error ? error.message : '未知错误'}`);
        }
    }
    
  } catch (error) {
    alert(`❌ PicGo连接测试失败

🔍 可能的原因：
1. PicGo应用没有运行
2. PicGo的HTTP服务器功能未开启
3. 端口36677被占用或被防火墙阻止

📋 解决步骤：
1. 确保PicGo应用正在运行
2. 在PicGo中开启"开启服务器"功能
3. 检查端口36677是否可用
4. 关闭防火墙或添加端口例外

💡 提示：在PicGo设置中找到"Server"或"服务器"选项并启用`);
  } finally {
    isTestingConnection.value = false;
  }
};

// 检查更新
const checkForUpdates = async () => {
  isCheckingUpdates.value = true;
  
  try {
    const result = await settingsStore.checkForUpdates();
    alert(result);
  } catch (error) {
    alert('检查更新失败');
  } finally {
    isCheckingUpdates.value = false;
  }
};

// 取消分享
const cancelShare = async (shareId: number) => {
  if (confirm('确定要取消这个分享吗？')) {
    try {
      const success = await settingsStore.cancelShare(shareId);
      if (success) {
        // toast 消息已在 store 中处理
      }
    } catch (error) {
      console.error('取消分享失败:', error);
      toast.error('取消分享失败');
    }
  }
};

// 一键复制分享信息 - 使用 token&password 格式
const copyShareInfo = async (share: any) => {
  try {
    let copyContent = share.share_token;
    if (share.share_password) {
      copyContent += `&${share.share_password}`;
    }

    await navigator.clipboard.writeText(copyContent);
    toast.success('分享链接已复制到剪贴板');
  } catch (error) {
    console.error('复制失败:', error);
    toast.error('复制失败');
  }
};

// 监听分类切换，按需加载数据
watch(() => settingsStore.currentCategoryId, async (newCategoryId) => {
  console.log('设置分类切换到:', newCategoryId)
  
  if (newCategoryId === 'shares') {
    console.log('加载分享数据...')
    await settingsStore.loadShareRecords(1);
  } else if (newCategoryId === 'focus') {
    console.log('加载专注记录数据...')
    await loadFocusRecords(1);
  } else if (newCategoryId === 'account') {
    console.log('切换到账户设置，刷新用户信息...')
    // 切换到账户设置时重新获取用户信息（额度、头像等）
    if (authStore.isAuthenticated) {
      await authStore.getCurrentUser();
    }
  }
}, { immediate: true }); // immediate: true 确保初始时也会执行

// 初始化数据
onMounted(async () => {
  // 加载保存的设置
  await settingsStore.loadSettings();
  // 每次打开设置页面都重新请求用户信息（获取最新的额度、头像等信息）
  if (authStore.isAuthenticated) {
    await authStore.getCurrentUser();
  }
});
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

/* 自定义滑块样式 */
input[type="range"]::-webkit-slider-thumb {
  appearance: none;
  width: 20px;
  height: 20px;
  background: #14B8A6;
  border-radius: 50%;
  cursor: pointer;
}

input[type="range"]::-moz-range-thumb {
  width: 20px;
  height: 20px;
  background: #14B8A6;
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

/* 复选框样式优化 */
input[type="checkbox"]:checked {
  background-color: #14B8A6;
  border-color: #14B8A6;
}

input[type="checkbox"]:focus {
  ring-color: #14B8A6;
}
</style> 