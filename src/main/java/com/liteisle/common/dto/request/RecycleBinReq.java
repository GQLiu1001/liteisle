package com.liteisle.common.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class RecycleBinReq {
    private List<Long> fileIds;
    private List<Long> folderIds;
}
