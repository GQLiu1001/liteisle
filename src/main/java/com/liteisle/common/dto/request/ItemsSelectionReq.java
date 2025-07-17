package com.liteisle.common.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class ItemsSelectionReq {
    private List<Long> fileIds;
    private Long folderId;
}
