package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class ItemsRenameReq {
    private Long fileId;
    private Long folderId;
    private String newName;
}
