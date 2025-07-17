package com.liteisle.common.dto.response;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ShareSaveAsyncResp {
    private Integer totalFilesToSave;
    private List<InitialFileData> initialFileDataList;
    
    @Data
    public static class InitialFileData {
        private Long id;
        private String name;
        private FileTypeEnum fileType;
        private FileStatusEnum fileStatus;
        private Date createTime;
        private Date updateTime;
    }
}
