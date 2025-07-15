package com.liteisle.common.domain.response;

import java.math.BigDecimal;
import java.util.List;

import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.FolderTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class DocumentViewResp {
    private List<Notebook> notebooks;
    private List<DocumentFile> files;

    @Data
    public static class Notebook {
        private Long id;
        private String folderName;
        private FolderTypeEnum folderType;
        private BigDecimal sortedOrder;
        private Integer documentCount;
    }

    @Data
    public static class DocumentFile {
        private Long id;
        private Long folderId;
        private String fileName;
        private FileTypeEnum fileType;
        private BigDecimal sortedOrder;
    }
}
