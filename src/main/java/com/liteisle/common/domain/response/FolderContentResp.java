package com.liteisle.common.domain.response;

import java.util.Date;
import java.util.List;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.FolderTypeEnum;
import lombok.Data;

@Data
public class FolderContentResp {
    private List<BreadcrumbItem> breadcrumb;
    private List<FolderItem> folders;
    private List<FileItem> files;
    
    @Data
    public static class BreadcrumbItem {
        private Long id;
        private String name;
    }
    
    @Data
    public static class FolderItem {
        private Long id;
        private String name;
        private FolderTypeEnum folderType; // "system", "playlist", "notebook" 等
        private Integer subItemCount;
        private Double sortedOrder;
        private Date createTime;
        private Date updateTime;
    }
    
    @Data
    public static class FileItem {
        private Long id;
        private String name;
        private FileTypeEnum fileType; // "music", "document" 等
        private Long fileSize;
        private FileStatusEnum fileStatus; // "available", "processing" 等
        private Double sortedOrder;
        private Date createTime;
        private Date updateTime;
    }
}
