package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class ItemsRenameReq {
    private Long fileId;
    private Long folderId;
    private String newName;
}
