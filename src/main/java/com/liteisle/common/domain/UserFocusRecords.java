package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户单次专注会话记录表
 * @TableName user_focus_records
 */
@TableName(value ="user_focus_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFocusRecords {
    /**
     * 专注记录唯一ID (主键)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 外键，关联到 users.id，标识该记录所属的用户
     */
    private Long userId;

    /**
     * 单次完整专注会话的时长（单位：分钟）
     */
    private Integer focusMinutes;

    /**
     * 专注完成并记录的时间点
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
        UserFocusRecords other = (UserFocusRecords) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getFocusMinutes() == null ? other.getFocusMinutes() == null : this.getFocusMinutes().equals(other.getFocusMinutes()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getFocusMinutes() == null) ? 0 : getFocusMinutes().hashCode());
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
        sb.append(", focusMinutes=").append(focusMinutes);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}