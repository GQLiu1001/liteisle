package com.liteisle.common.domain.response;

import com.liteisle.common.enums.ItemType;
import lombok.Data;

import java.util.List;

@Data
public class DownloadSessionResp {
    private Long totalSize;
    private Long totalFiles;
    private List<Items> items;

    @Data
    public static class Items {
        private ItemType itemType;
        private String relativePath;
        private Long logId;
        private Long fileId;
        private Long size;
        private String downloadUrl;
    }
}
