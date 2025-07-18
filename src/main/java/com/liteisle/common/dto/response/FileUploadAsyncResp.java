package com.liteisle.common.dto.response;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@AllArgsConstructor
@Data
public class FileUploadAsyncResp {
    private Long logId;
    private Long fileId;
    private TransferStatusEnum logStatus;
    private InitialFileData initialFileData;

    @AllArgsConstructor
    @Data
    public static class InitialFileData {
        private Long id;
        private String name;
        private FileTypeEnum fileType;
        private FileStatusEnum fileStatus;
        private BigDecimal sortedOrder;
        private Date createTime;
        private Date updateTime;
    }
}
