package com.liteisle.common.domain.request;

import com.liteisle.common.enums.FolderTypeEnum;
import lombok.Data;

@Data
public class FolderCreateReq {
    private String name;
    private Long parentId;
    private FolderTypeEnum folderType; // "system", "playlist", "booklist" 等
}
