package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import lombok.Data;

/**
 * 文件通用基础信息表
 * @TableName files
 */
@TableName(value ="files")
@Data
public class Files {
    /**
     * 文件唯一ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 外键，关联到 users.id，标识此文件的拥有者
     */
    private Long userId;

    /**
     * 外键，关联到 folders.id，标识文件所在的文件夹
     */
    private Long folderId;

    /**
     * 外键，关联到 storages.id，文件处理成功后填充
     */
    private Long storageId;

    /**
     * 文件名（通常包含扩展名）
     */
    private String fileName;

    /**
     * 文件扩展名，如 "mp3", "pdf", 用于前端显示
     */
    private String fileExtension;

    /**
     * 文件的业务大类，用于甄别应关联哪个元数据表
     */
    private FileTypeEnum fileType;

    /**
     * 文件状态。processing:后台处理中; available:完全可用; failed:处理失败
     */
    private FileStatusEnum fileStatus;

    /**
     * 用于用户自定义排序的浮点数值
     */
    private BigDecimal sortedOrder;

    /**
     * 软删除标记。非NULL表示已移入回收站
     */
    private Date deleteTime;

    /**
     * 文件逻辑记录的创建时间
     */
    private Date createTime;

    /**
     * 文件元数据的最后更新时间
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
        Files other = (Files) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getFolderId() == null ? other.getFolderId() == null : this.getFolderId().equals(other.getFolderId()))
            && (this.getStorageId() == null ? other.getStorageId() == null : this.getStorageId().equals(other.getStorageId()))
            && (this.getFileName() == null ? other.getFileName() == null : this.getFileName().equals(other.getFileName()))
            && (this.getFileExtension() == null ? other.getFileExtension() == null : this.getFileExtension().equals(other.getFileExtension()))
            && (this.getFileType() == null ? other.getFileType() == null : this.getFileType().equals(other.getFileType()))
            && (this.getFileStatus() == null ? other.getFileStatus() == null : this.getFileStatus().equals(other.getFileStatus()))
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
        result = prime * result + ((getFolderId() == null) ? 0 : getFolderId().hashCode());
        result = prime * result + ((getStorageId() == null) ? 0 : getStorageId().hashCode());
        result = prime * result + ((getFileName() == null) ? 0 : getFileName().hashCode());
        result = prime * result + ((getFileExtension() == null) ? 0 : getFileExtension().hashCode());
        result = prime * result + ((getFileType() == null) ? 0 : getFileType().hashCode());
        result = prime * result + ((getFileStatus() == null) ? 0 : getFileStatus().hashCode());
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
        sb.append(", folderId=").append(folderId);
        sb.append(", storageId=").append(storageId);
        sb.append(", fileName=").append(fileName);
        sb.append(", fileExtension=").append(fileExtension);
        sb.append(", fileType=").append(fileType);
        sb.append(", fileStatus=").append(fileStatus);
        sb.append(", sortedOrder=").append(sortedOrder);
        sb.append(", deleteTime=").append(deleteTime);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}