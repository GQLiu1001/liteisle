package com.liteisle.common.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分享转存完成消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareSaveCompletedMessage {
    
    /**
     * 转存的文件数量
     */
    private Integer totalFiles;
    
    /**
     * 成功转存的文件数量
     */
    private Integer successCount;
    
    /**
     * 失败的文件数量
     */
    private Integer failedCount;
    
    /**
     * 目标文件夹ID
     */
    private Long targetFolderId;
    
    /**
     * 分享者用户ID
     */
    private Long sharerId;
    
    /**
     * 失败的文件列表（文件名）
     */
    private List<String> failedFiles;
}