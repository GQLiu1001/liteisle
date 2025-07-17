package com.liteisle.common.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class ItemsDeleteReq {
    private List<Long> fileIds;
    private List<Long> folderIds;
}
