package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class MarkdownCreateReq {
    private String name;
    private Long folderId;
}
