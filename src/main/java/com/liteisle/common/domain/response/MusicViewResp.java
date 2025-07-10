package com.liteisle.common.domain.response;

import java.util.List;

import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.FolderTypeEnum;
import lombok.Data;

@Data
public class MusicViewResp {
    private List<Playlist> playlists;
    private List<MusicFile> files;
    
    @Data
    public static class Playlist {
        private Long id;
        private String name;
        private FolderTypeEnum folderType;
        private Double sortedOrder;
        private Integer musicCount;
    }
    
    @Data
    public static class MusicFile {
        private Long id;
        private Long folderId;
        private String name;
        private FileTypeEnum fileType;
        private Double sortedOrder;
        private String artist;
        private String album;
        private Integer duration; // 单位为秒
        private String coverArtUrl;
    }
}
