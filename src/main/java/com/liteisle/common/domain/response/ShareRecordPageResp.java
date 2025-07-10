package com.liteisle.common.domain.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ShareRecordPageResp {
    private Long total;
    private Integer currentPage;
    private Integer pageSize;
    private List<ShareRecord> records;
    
    @Data
    public static class ShareRecord {
        private Long id;
        private Long fileId;
        private Long folderId;
        private String shareItemName;
        private String shareToken;
        private String sharePassword;
        private Date createTime;
        private Date expireTime;
    }
}
