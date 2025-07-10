package com.liteisle.common.domain.response;

import java.util.List;

import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.FolderTypeEnum;
import lombok.Data;

@Data
public class DocumentViewResp {
    private List<Notebook> notebooks;
    private List<DocumentFile> files;

    @Data
    public static class Notebook {
        private Long id;
        private String name;
        private FolderTypeEnum folderType;
        private Double sortedOrder;
        private Integer documentCount;
    }

    @Data
    public static class DocumentFile {
        private Long id;
        private Long folderId;
        private String name;
        private FileTypeEnum fileType;
        private Double sortedOrder;
    }
}
