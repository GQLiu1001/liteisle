package com.liteisle.common.domain.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.FolderTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class FolderContentResp {
    private List<BreadcrumbItem> breadcrumb;
    private List<FolderItem> folders;
    private List<FileItem> files;

    @AllArgsConstructor
    @Data
    public static class BreadcrumbItem {
        private Long id;
        private String name;
    }
    
    @Data
    public static class FolderItem {
        private Long id;
        private String folderName;
        private FolderTypeEnum folderType; // "system", "playlist", "booklist" 等
        private Integer subCount;
        private BigDecimal sortedOrder;
        private Date createTime;
        private Date updateTime;
    }
    
    @Data
    public static class FileItem {
        private Long id;
        private String fileName;
        private FileTypeEnum fileType; // "music", "document" 等
        private Long fileSize;
        private FileStatusEnum fileStatus; // "available", "processing" 等
        private BigDecimal sortedOrder;
        private Date createTime;
        private Date updateTime;
    }
}
