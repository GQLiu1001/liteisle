package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class MarkdownCreateReq {
    private String name;
    private Long folderId;
}
