package com.liteisle.common.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DownloadSessionResp {
    private Long totalSize;
    private Long totalFiles;
    private FolderD folderD;
    private List<FilesD> FilesD;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class FolderD {
        private Long folderId;
        private String folderName;
        private String relativePath;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class FilesD {
        private Long logId;
        private Long fileId;
        private String fileName;
        private String relativePath;
        private Long size;
        private String downloadUrl;
    }
}
