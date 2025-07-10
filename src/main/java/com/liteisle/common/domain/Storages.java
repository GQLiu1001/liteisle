package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 存储唯一文件实体，避免重复上传
 * @TableName storages
 */
@TableName(value ="storages")
@Data
public class Storages {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件内容的哈希值 (e.g., SHA-256)
     */
    private String fileHash;

    /**
     * 
     */
    private Long fileSize;

    /**
     * 
     */
    private String mimeType;

    /**
     * 在对象存储(R2)中的路径或Key
     */
    private String storagePath;

    /**
     * 引用计数，为0时可由后台任务清理
     */
    private Integer referenceCount;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}