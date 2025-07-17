package com.liteisle.common.dto.response;

import java.util.Date;
import java.util.List;

import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.common.enums.TransferTypeEnum;
import lombok.Data;

@Data
public class TransferLogPageResp {
    private Long total;
    private Integer currentPage;
    private Integer pageSize;
    private List<TransferRecord> records;
    
    @Data
    public static class TransferRecord {
        private Long logId;
        private String itemName;
        private Long itemSize;
        private TransferTypeEnum transferType; // "upload" 或 "download"
        private Date createTime;
    }
}
