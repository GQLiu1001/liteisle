package com.liteisle.common.domain.request;

import java.util.List;
import lombok.Data;

@Data
public class ItemsOperationReq {
    private List<Long> fileIds;
    private List<Long> folderIds;
    private Long targetFolderId;
}
