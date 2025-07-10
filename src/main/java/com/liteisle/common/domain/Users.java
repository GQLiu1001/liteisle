package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户账户与基本信息表
 * @TableName users
 */
@TableName(value ="users")
@Data
public class Users {
    /**
     * 用户唯一标识ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，用于登录，全系统唯一
     */
    private String username;

    /**
     * 用户邮箱，用于登录和接收通知，全系统唯一
     */
    private String email;

    /**
     * 加密后的用户密码 (例如使用BCrypt)
     */
    private String password;

    /**
     * 用户头像的URL地址
     */
    private String avatar;

    /**
     * 用户总存储空间配额，单位：字节 (Bytes)，默认5GB
     */
    private Long storageQuota;

    /**
     * 用户已使用的存储空间，单位：字节 (Bytes)，通过异步消息队列在文件操作后更新
     */
    private Long storageUsed;

    /**
     * 账户创建时间
     */
    private Date createTime;

    /**
     * 账户信息最后更新时间
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
        Users other = (Users) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUsername() == null ? other.getUsername() == null : this.getUsername().equals(other.getUsername()))
            && (this.getEmail() == null ? other.getEmail() == null : this.getEmail().equals(other.getEmail()))
            && (this.getPassword() == null ? other.getPassword() == null : this.getPassword().equals(other.getPassword()))
            && (this.getAvatar() == null ? other.getAvatar() == null : this.getAvatar().equals(other.getAvatar()))
            && (this.getStorageQuota() == null ? other.getStorageQuota() == null : this.getStorageQuota().equals(other.getStorageQuota()))
            && (this.getStorageUsed() == null ? other.getStorageUsed() == null : this.getStorageUsed().equals(other.getStorageUsed()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
        result = prime * result + ((getEmail() == null) ? 0 : getEmail().hashCode());
        result = prime * result + ((getPassword() == null) ? 0 : getPassword().hashCode());
        result = prime * result + ((getAvatar() == null) ? 0 : getAvatar().hashCode());
        result = prime * result + ((getStorageQuota() == null) ? 0 : getStorageQuota().hashCode());
        result = prime * result + ((getStorageUsed() == null) ? 0 : getStorageUsed().hashCode());
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
        sb.append(", username=").append(username);
        sb.append(", email=").append(email);
        sb.append(", password=").append(password);
        sb.append(", avatar=").append(avatar);
        sb.append(", storageQuota=").append(storageQuota);
        sb.append(", storageUsed=").append(storageUsed);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}