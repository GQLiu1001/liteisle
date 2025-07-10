package com.liteisle.common.domain.request;

import com.liteisle.common.enums.TransferStatusEnum;
import lombok.Data;

@Data
public class TransferStatusUpdateReq {
    private TransferStatusEnum logStatus; // "success", "failed", "canceled"
    private String errorMessage; // 仅当 status 为 "failed" 时可能存在
    private Long transferDurationMs; // 传输持续时间（毫秒）
}
