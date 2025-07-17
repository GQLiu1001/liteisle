package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 管理公开分享链接
 * @TableName share_links
 */
@TableName(value ="share_links")
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShareLinks {
    /**
     * 分享记录唯一ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 外键，关联到 users.id，标识分享的创建者
     */
    private Long ownerId;

    /**
     * 分享链接的唯一公开凭证，是URL的一部分
     */
    private String shareToken;

    /**
     * 加密后的分享提取码（如果分享是加密的）
     */
    private String sharePassword;

    /**
     * 外键，关联到 files.id，当分享目标是单个文件时填充
     */
    private Long fileId;

    /**
     * 外键，关联到 folders.id，当分享目标是文件夹时填充
     */
    private Long folderId;

    /**
     * 分享链接的过期时间，NULL表示永久有效
     */
    private Date expireTime;

    /**
     * 分享创建时间
     */
    private Date createTime;

    /**
     * 分享信息更新时间
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
        ShareLinks other = (ShareLinks) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getOwnerId() == null ? other.getOwnerId() == null : this.getOwnerId().equals(other.getOwnerId()))
            && (this.getShareToken() == null ? other.getShareToken() == null : this.getShareToken().equals(other.getShareToken()))
            && (this.getSharePassword() == null ? other.getSharePassword() == null : this.getSharePassword().equals(other.getSharePassword()))
            && (this.getFileId() == null ? other.getFileId() == null : this.getFileId().equals(other.getFileId()))
            && (this.getFolderId() == null ? other.getFolderId() == null : this.getFolderId().equals(other.getFolderId()))
            && (this.getExpireTime() == null ? other.getExpireTime() == null : this.getExpireTime().equals(other.getExpireTime()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getOwnerId() == null) ? 0 : getOwnerId().hashCode());
        result = prime * result + ((getShareToken() == null) ? 0 : getShareToken().hashCode());
        result = prime * result + ((getSharePassword() == null) ? 0 : getSharePassword().hashCode());
        result = prime * result + ((getFileId() == null) ? 0 : getFileId().hashCode());
        result = prime * result + ((getFolderId() == null) ? 0 : getFolderId().hashCode());
        result = prime * result + ((getExpireTime() == null) ? 0 : getExpireTime().hashCode());
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
        sb.append(", userId=").append(ownerId);
        sb.append(", shareToken=").append(shareToken);
        sb.append(", sharePassword=").append(sharePassword);
        sb.append(", fileId=").append(fileId);
        sb.append(", folderId=").append(folderId);
        sb.append(", expireTime=").append(expireTime);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}