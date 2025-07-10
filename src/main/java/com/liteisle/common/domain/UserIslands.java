package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 记录用户已解锁的岛屿表
 * @TableName user_islands
 */
@TableName(value ="user_islands")
@Data
public class UserIslands {
    /**
     * 收集记录的唯一ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 外键，关联到 users.id，标识该岛屿的拥有者
     */
    private Long userId;

    /**
     * 岛屿的唯一业务代码，与后端代码中的岛屿配置相对应
     */
    private String islandUrl;

    /**
     * 岛屿的获取时间
     */
    private Date createTime;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        UserIslands other = (UserIslands) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getIslandUrl() == null ? other.getIslandUrl() == null : this.getIslandUrl().equals(other.getIslandUrl()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getIslandUrl() == null) ? 0 : getIslandUrl().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", islandUrl=").append(islandUrl);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}