package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class MarkdownUpdateReq {
    private String content;
    private Long version;
}
