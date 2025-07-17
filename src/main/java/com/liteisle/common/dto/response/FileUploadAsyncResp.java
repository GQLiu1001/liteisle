package com.liteisle.common.dto.response;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.TransferStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class FileUploadAsyncResp {
    private Long logId;
    private Long fileId;
    private TransferStatusEnum logStatus; // "processing" 或 "available"
    private InitialFileData initialFileData;
    
    @Data
    public static class InitialFileData {
        private Long id;
        private String name;
        private FileTypeEnum fileType;
        private FileStatusEnum fileStatus;
        private Double sortedOrder;
        private Date createTime;
        private Date updateTime;
    }
}
