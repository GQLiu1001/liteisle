package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文档文件专属元数据表
 * @TableName document_metadata
 */
@TableName(value ="document_metadata")
@Data
public class DocumentMetadata {
    /**
     * 主键，同时也是外键，关联到 files.id
     */
    @TableId
    private Long fileId;

    /**
     * Markdown文件的纯文本内容，用于全文搜索
     */
    private String content;

    /**
     * 用于Markdown文件的乐观锁版本号
     */
    private Long version;

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
        DocumentMetadata other = (DocumentMetadata) that;
        return (this.getFileId() == null ? other.getFileId() == null : this.getFileId().equals(other.getFileId()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getVersion() == null ? other.getVersion() == null : this.getVersion().equals(other.getVersion()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFileId() == null) ? 0 : getFileId().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", fileId=").append(fileId);
        sb.append(", content=").append(content);
        sb.append(", version=").append(version);
        sb.append("]");
        return sb.toString();
    }
}