package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class MarkdownUpdateReq {
    private String content;
    private Long version;
}
