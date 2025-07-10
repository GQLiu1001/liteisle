package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件元数据及处理状态
 * @TableName files
 */
@TableName(value ="files")
@Data
public class Files {
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
    private Long folderId;

    /**
     * 正式存储ID，在处理完成前可为空
     */
    private Long storageId;

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private String extension;

    /**
     * 
     */
    private Object fileType;

    /**
     * 文件状态
     */
    private Object status;

    /**
     * 文件在云存储中的临时路径
     */
    private String tempStoragePath;

    /**
     * 
     */
    private String errorMessage;

    /**
     * 用于用户自定义排序
     */
    private Double sortedOrder;

    /**
     * 
     */
    private Date deleteTime;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 
     */
    private String artist;

    /**
     * 
     */
    private String album;

    /**
     * 秒
     */
    private Integer duration;

    /**
     * 
     */
    private String coverArtUrl;

    /**
     * 
     */
    private String content;

    /**
     * 
     */
    private Long version;
}