package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件上传下载行为日志
 * @TableName transfer_log
 */
@TableName(value ="transfer_log")
@Data
public class TransferLog {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private Object transferType;

    /**
     * 
     */
    private Long fileId;

    /**
     * 
     */
    private Long folderId;

    /**
     * 
     */
    private String itemName;

    /**
     * 字节
     */
    private Long itemSize;

    /**
     * 传输行为状态: 处理中, 成功, 失败, 已取消
     */
    private Object status;

    /**
     * 
     */
    private String errorMessage;

    /**
     * 
     */
    private Integer durationMs;

    /**
     * 
     */
    private String clientIp;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}