package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 记录用户已解锁的岛屿
 * @TableName user_islands
 */
@TableName(value ="user_islands")
@Data
public class UserIslands {
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
     * 岛屿的唯一代码，与后端配置对应
     */
    private String islandCode;

    /**
     * 
     */
    private Date createTime;
}