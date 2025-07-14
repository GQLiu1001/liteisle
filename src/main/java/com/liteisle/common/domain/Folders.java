package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 用户逻辑文件夹结构表
 * @TableName folders
 */
@TableName(value ="folders")
@Data
public class Folders {
    /**
     * 文件夹唯一ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 外键，关联到 users.id，标识此文件夹的拥有者
     */
    private Long userId;

    /**
     * 父文件夹ID。值为0表示根目录下的文件夹
     */
    private Long parentId;

    /**
     * 文件夹在UI上显示的名称
     */
    private String folderName;

    /**
     * 只有 system, playlist, notebook 三类。
     */
    private Object folderType;

    /**
     * 用于用户自定义排序的浮点数值
     */
    private BigDecimal sortedOrder;

    /**
     * 软删除标记。非NULL表示已移入回收站
     */
    private Date deleteTime;

    /**
     * 文件夹创建时间
     */
    private Date createTime;

    /**
     * 文件夹最后更新时间（如重命名）
     */
    private Date updateTime;

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
        Folders other = (Folders) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getParentId() == null ? other.getParentId() == null : this.getParentId().equals(other.getParentId()))
            && (this.getFolderName() == null ? other.getFolderName() == null : this.getFolderName().equals(other.getFolderName()))
            && (this.getFolderType() == null ? other.getFolderType() == null : this.getFolderType().equals(other.getFolderType()))
            && (this.getSortedOrder() == null ? other.getSortedOrder() == null : this.getSortedOrder().equals(other.getSortedOrder()))
            && (this.getDeleteTime() == null ? other.getDeleteTime() == null : this.getDeleteTime().equals(other.getDeleteTime()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getParentId() == null) ? 0 : getParentId().hashCode());
        result = prime * result + ((getFolderName() == null) ? 0 : getFolderName().hashCode());
        result = prime * result + ((getFolderType() == null) ? 0 : getFolderType().hashCode());
        result = prime * result + ((getSortedOrder() == null) ? 0 : getSortedOrder().hashCode());
        result = prime * result + ((getDeleteTime() == null) ? 0 : getDeleteTime().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
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
        sb.append(", parentId=").append(parentId);
        sb.append(", folderName=").append(folderName);
        sb.append(", folderType=").append(folderType);
        sb.append(", sortedOrder=").append(sortedOrder);
        sb.append(", deleteTime=").append(deleteTime);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}