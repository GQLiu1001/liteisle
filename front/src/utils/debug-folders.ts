/**
 * 系统文件夹调试工具
 * 用于检查和诊断用户系统文件夹的状态
 */

import { API } from '@/utils/api'

export interface SystemFolderStatus {
  exists: boolean
  id?: number
  name: string
  type: 'system'
}

export interface FolderDiagnostic {
  userId?: number
  systemFolders: SystemFolderStatus[]
  missingFolders: string[]
  totalFolders: number
  hasAllSystemFolders: boolean
}

/**
 * 检查用户系统文件夹状态
 */
export async function checkSystemFolders(): Promise<FolderDiagnostic> {
  try {
    console.log('🔍 开始检查系统文件夹状态...')
    
    const response = await API.folder.getFolderHierarchy()
    
    if (!response.data || (response.data as any).code !== 200 || !(response.data as any).data) {
      throw new Error('获取文件夹层级失败')
    }

    const folderHierarchy = (response.data as any).data as any[]
    console.log('📁 文件夹层级数据:', folderHierarchy)

    // 筛选系统文件夹
    const systemFolders = folderHierarchy.filter(f => f.folder_type === 'system')
    console.log('🏠 系统文件夹:', systemFolders)

    // 期望的系统文件夹
    const expectedSystemFolders = ['歌单', '文档', '分享', '上传']
    
    // 检查每个期望的系统文件夹
    const folderStatus: SystemFolderStatus[] = expectedSystemFolders.map(folderName => {
      const folder = systemFolders.find(f => f.folder_name === folderName)
      return {
        exists: !!folder,
        id: folder?.id,
        name: folderName,
        type: 'system' as const
      }
    })

    // 找出缺失的文件夹
    const missingFolders = folderStatus
      .filter(status => !status.exists)
      .map(status => status.name)

    const diagnostic: FolderDiagnostic = {
      systemFolders: folderStatus,
      missingFolders,
      totalFolders: folderHierarchy.length,
      hasAllSystemFolders: missingFolders.length === 0
    }

    console.log('📊 系统文件夹诊断结果:', diagnostic)
    
    return diagnostic
  } catch (error) {
    console.error('❌ 检查系统文件夹失败:', error)
    throw error
  }
}

/**
 * 打印系统文件夹状态报告
 */
export async function printSystemFolderReport(): Promise<void> {
  try {
    const diagnostic = await checkSystemFolders()
    
    console.log('\n📋 === 系统文件夹状态报告 ===')
    console.log(`总文件夹数量: ${diagnostic.totalFolders}`)
    console.log(`系统文件夹完整性: ${diagnostic.hasAllSystemFolders ? '✅ 完整' : '❌ 不完整'}`)
    
    console.log('\n📁 系统文件夹状态:')
    diagnostic.systemFolders.forEach(folder => {
      const status = folder.exists ? '✅' : '❌'
      const id = folder.exists ? `(ID: ${folder.id})` : '(缺失)'
      console.log(`  ${status} ${folder.name} ${id}`)
    })
    
    if (diagnostic.missingFolders.length > 0) {
      console.log('\n⚠️ 缺失的系统文件夹:')
      diagnostic.missingFolders.forEach(folderName => {
        console.log(`  - ${folderName}`)
      })
      
      console.log('\n💡 建议解决方案:')
      console.log('1. 联系后端开发者实现系统文件夹初始化接口')
      console.log('2. 手动在数据库中为用户创建缺失的系统文件夹')
      console.log('3. 检查用户注册流程是否正确创建了系统文件夹')
    }
    
    console.log('\n=== 报告结束 ===\n')
  } catch (error) {
    console.error('生成报告失败:', error)
  }
}

/**
 * 尝试初始化系统文件夹（如果后端支持）
 */
export async function tryInitializeSystemFolders(): Promise<boolean> {
  try {
    console.log('🔧 尝试初始化系统文件夹...')
    
    const response = await API.folder.initializeSystemFolders()
    
    if (response.data && (response.data as any).code === 200) {
      console.log('✅ 系统文件夹初始化成功')
      
      // 重新检查状态
      await printSystemFolderReport()
      return true
    } else {
      console.log('❌ 系统文件夹初始化失败:', response.data)
      return false
    }
  } catch (error) {
    console.error('❌ 初始化系统文件夹时发生错误:', error)
    console.log('💡 可能的原因:')
    console.log('1. 后端还未实现 /folders/initialize-system 接口')
    console.log('2. 用户权限不足')
    console.log('3. 网络连接问题')
    return false
  }
}

// 在开发环境下将调试工具挂载到全局对象
if (import.meta.env.DEV) {
  (window as any).debugFolders = {
    check: checkSystemFolders,
    report: printSystemFolderReport,
    init: tryInitializeSystemFolders
  }
  
  console.log('🛠️ 系统文件夹调试工具已加载')
  console.log('使用方法:')
  console.log('  debugFolders.check() - 检查系统文件夹状态')
  console.log('  debugFolders.report() - 打印详细报告')
  console.log('  debugFolders.init() - 尝试初始化系统文件夹')
}
