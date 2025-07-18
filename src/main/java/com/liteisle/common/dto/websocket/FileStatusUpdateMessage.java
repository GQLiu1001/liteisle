package com.liteisle.common.dto.websocket;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件状态更新消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileStatusUpdateMessage {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 传输日志ID
     */
    private Long logId;
    
    /**
     * 文件状态
     */
    private FileStatusEnum fileStatus;
    
    /**
     * 传输状态
     */
    private TransferStatusEnum transferStatus;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    
    /**
     * 进度百分比（0-100）
     */
    private Integer progress;
}