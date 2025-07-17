package com.liteisle.common.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.FolderTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class MusicViewResp {
    private List<Playlist> playlists;
    private List<MusicFile> files;
    
    @Data
    public static class Playlist {
        private Long id;
        private String folderName;
        private FolderTypeEnum folderType;
        private BigDecimal sortedOrder;
        private Integer subCount;
    }
    
    @Data
    public static class MusicFile {
        private Long id;
        private Long folderId;
        private String fileName;
        private FileTypeEnum fileType;
        private BigDecimal sortedOrder;
        private String artist;
        private String album;
        private Integer duration; // 单位为秒
        private String coverArtUrl;
    }
}
