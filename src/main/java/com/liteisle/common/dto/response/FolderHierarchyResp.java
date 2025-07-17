package com.liteisle.common.dto.response;

import com.liteisle.common.enums.FolderTypeEnum;
import lombok.Data;

@Data
public class FolderHierarchyResp {
    private Long id;
    private String folderName;
    private FolderTypeEnum folderType;
    private Long parentId;

}
