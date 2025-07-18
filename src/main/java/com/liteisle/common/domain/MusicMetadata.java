package com.liteisle.common.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 音乐文件专属元数据表
 * @TableName music_metadata
 */
@TableName(value ="music_metadata")
@Data
public class MusicMetadata {
    /**
     * 主键，同时也是外键，关联到 files.id
     */
    @TableId
    private Long fileId;

    /**
     * 歌手名，由后台FFmpeg等工具解析填充
     */
    private String artist;

    /**
     * 专辑名
     */
    private String album;

    /**
     * 时长，单位：秒
     */
    private Integer duration;


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
        MusicMetadata other = (MusicMetadata) that;
        return (this.getFileId() == null ? other.getFileId() == null : this.getFileId().equals(other.getFileId()))
            && (this.getArtist() == null ? other.getArtist() == null : this.getArtist().equals(other.getArtist()))
            && (this.getAlbum() == null ? other.getAlbum() == null : this.getAlbum().equals(other.getAlbum()))
            && (this.getDuration() == null ? other.getDuration() == null : this.getDuration().equals(other.getDuration()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFileId() == null) ? 0 : getFileId().hashCode());
        result = prime * result + ((getArtist() == null) ? 0 : getArtist().hashCode());
        result = prime * result + ((getAlbum() == null) ? 0 : getAlbum().hashCode());
        result = prime * result + ((getDuration() == null) ? 0 : getDuration().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", fileId=").append(fileId);
        sb.append(", artist=").append(artist);
        sb.append(", album=").append(album);
        sb.append(", duration=").append(duration);
        sb.append("]");
        return sb.toString();
    }
}