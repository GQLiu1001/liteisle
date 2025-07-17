package com.liteisle.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class TransferSummaryResp {
    private Long uploadCount;
    private Long downloadCount;
}
