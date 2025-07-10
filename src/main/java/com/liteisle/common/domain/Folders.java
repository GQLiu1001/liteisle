package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 逻辑文件夹，统一管理歌单、文档分类等
 * @TableName folders
 */
@TableName(value ="folders")
@Data
public class Folders {
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
     * 父文件夹ID, NULL表示根目录
     */
    private Long parentId;

    /**
     * 
     */
    private String name;

    /**
     * system: 音乐/文档/上传/分享, playlist: 用户创建的歌单, notebook: 文档分类,upload:系统文件夹-》“上传”里的文件夹,share:系统文件夹-》“分享”里的文件夹
     */
    private Object folderType;

    /**
     * 用于用户自定义排序
     */
    private Double sortedOrder;

    /**
     * 软删除标记，用于回收站功能
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
}