package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 管理公开分享链接
 * @TableName share_links
 */
@TableName(value ="share_links")
@Data
public class ShareLinks {
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
    private String shareToken;

    /**
     * 
     */
    private String sharePassword;

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
    private Date expireTime;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}