package com.liteisle.common.dto.response;

import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.FolderTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;
@AllArgsConstructor
@Data
public class RecycleBinContentResp {
    private List<FolderItem> folders;
    private List<FileItem> files;
    
    @Data
    public static class FolderItem {
        private Long originalId;
        private String originalName;
        private FolderTypeEnum originalType;
        private Integer subCount;
        private Date deleteTime;
        private Date expireTime;
    }
    
    @Data
    public static class FileItem {
        private Long originalId;
        private String originalName;
        private FileTypeEnum originalType;
        private Long fileSize;
        private Date deleteTime;
        private Date expireTime;
    }
}
