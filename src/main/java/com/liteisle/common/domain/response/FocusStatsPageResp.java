package com.liteisle.common.domain.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class FocusStatsPageResp {
    private Long total;
    private Long currentPage;
    private Long pageSize;
    private List<FocusRecord> records;
    
    @Data
    public static class FocusRecord {
        private Long id;        // 记录的ID
        private Integer focusMinutes;  // 专注时长（分钟）
        private Date createTime;
    }
}
