package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件上传下载行为日志表
 * @TableName transfer_log
 */
@TableName(value ="transfer_log")
@Data
public class TransferLog {
    /**
     * 传输日志唯一ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 外键，关联到 users.id，标识操作用户
     */
    private Long userId;

    /**
     * 传输类型：上传或下载
     */
    private Object transferType;

    /**
     * 外键，关联到 files.id (如果传输对象是单个文件)
     */
    private Long fileId;

    /**
     * 外键，关联到 folders.id (如果传输对象是文件夹)
     */
    private Long folderId;

    /**
     * 被传输项目（文件/文件夹）的名称，做冗余以备查询
     */
    private String itemName;

    /**
     * 被传输项目的总大小，单位：字节 (Bytes)
     */
    private Long itemSize;

    /**
     * 传输行为状态: 处理中, 成功, 失败, 已取消
     */
    private Object logStatus;

    /**
     * 当status为failed或canceled时，记录相关信息
     */
    private String errorMessage;

    /**
     * 传输总耗时，单位：毫秒 (ms)
     */
    private Integer transferDurationMs;

    /**
     * 发起传输的客户端IP地址，用于安全审计
     */
    private String clientIp;

    /**
     * 传输任务创建时间
     */
    private Date createTime;

    /**
     * 传输任务状态的最后更新时间
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
        TransferLog other = (TransferLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getTransferType() == null ? other.getTransferType() == null : this.getTransferType().equals(other.getTransferType()))
            && (this.getFileId() == null ? other.getFileId() == null : this.getFileId().equals(other.getFileId()))
            && (this.getFolderId() == null ? other.getFolderId() == null : this.getFolderId().equals(other.getFolderId()))
            && (this.getItemName() == null ? other.getItemName() == null : this.getItemName().equals(other.getItemName()))
            && (this.getItemSize() == null ? other.getItemSize() == null : this.getItemSize().equals(other.getItemSize()))
            && (this.getLogStatus() == null ? other.getLogStatus() == null : this.getLogStatus().equals(other.getLogStatus()))
            && (this.getErrorMessage() == null ? other.getErrorMessage() == null : this.getErrorMessage().equals(other.getErrorMessage()))
            && (this.getTransferDurationMs() == null ? other.getTransferDurationMs() == null : this.getTransferDurationMs().equals(other.getTransferDurationMs()))
            && (this.getClientIp() == null ? other.getClientIp() == null : this.getClientIp().equals(other.getClientIp()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getTransferType() == null) ? 0 : getTransferType().hashCode());
        result = prime * result + ((getFileId() == null) ? 0 : getFileId().hashCode());
        result = prime * result + ((getFolderId() == null) ? 0 : getFolderId().hashCode());
        result = prime * result + ((getItemName() == null) ? 0 : getItemName().hashCode());
        result = prime * result + ((getItemSize() == null) ? 0 : getItemSize().hashCode());
        result = prime * result + ((getLogStatus() == null) ? 0 : getLogStatus().hashCode());
        result = prime * result + ((getErrorMessage() == null) ? 0 : getErrorMessage().hashCode());
        result = prime * result + ((getTransferDurationMs() == null) ? 0 : getTransferDurationMs().hashCode());
        result = prime * result + ((getClientIp() == null) ? 0 : getClientIp().hashCode());
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
        sb.append(", transferType=").append(transferType);
        sb.append(", fileId=").append(fileId);
        sb.append(", folderId=").append(folderId);
        sb.append(", itemName=").append(itemName);
        sb.append(", itemSize=").append(itemSize);
        sb.append(", logStatus=").append(logStatus);
        sb.append(", errorMessage=").append(errorMessage);
        sb.append(", transferDurationMs=").append(transferDurationMs);
        sb.append(", clientIp=").append(clientIp);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}