package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 存储唯一文件实体，实现秒传功能
 * @TableName storages
 */
@TableName(value ="storages")
@Data
public class Storages {
    /**
     * 物理存储记录ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件内容的哈希值 (SHA-256)，用于秒传功能的核心校验
     */
    private String fileHash;

    /**
     * 文件大小，单位：字节 (Bytes)
     */
    private Long fileSize;

    /**
     * 文件的MIME类型，例如 "audio/mpeg", "application/pdf"
     */
    private String mimeType;

    /**
     * 文件在对象存储服务(如R2)中的唯一路径或Key
     */
    private String storagePath;

    /**
     * 引用计数。当计数值为0时，可由后台垃圾回收任务安全删除物理文件
     */
    private Integer referenceCount;

    /**
     * 该物理文件首次被上传到系统的时间
     */
    private Date createTime;

    /**
     * 记录的最后更新时间
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
        Storages other = (Storages) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getFileHash() == null ? other.getFileHash() == null : this.getFileHash().equals(other.getFileHash()))
            && (this.getFileSize() == null ? other.getFileSize() == null : this.getFileSize().equals(other.getFileSize()))
            && (this.getMimeType() == null ? other.getMimeType() == null : this.getMimeType().equals(other.getMimeType()))
            && (this.getStoragePath() == null ? other.getStoragePath() == null : this.getStoragePath().equals(other.getStoragePath()))
            && (this.getReferenceCount() == null ? other.getReferenceCount() == null : this.getReferenceCount().equals(other.getReferenceCount()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getFileHash() == null) ? 0 : getFileHash().hashCode());
        result = prime * result + ((getFileSize() == null) ? 0 : getFileSize().hashCode());
        result = prime * result + ((getMimeType() == null) ? 0 : getMimeType().hashCode());
        result = prime * result + ((getStoragePath() == null) ? 0 : getStoragePath().hashCode());
        result = prime * result + ((getReferenceCount() == null) ? 0 : getReferenceCount().hashCode());
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
        sb.append(", fileHash=").append(fileHash);
        sb.append(", fileSize=").append(fileSize);
        sb.append(", mimeType=").append(mimeType);
        sb.append(", storagePath=").append(storagePath);
        sb.append(", referenceCount=").append(referenceCount);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}