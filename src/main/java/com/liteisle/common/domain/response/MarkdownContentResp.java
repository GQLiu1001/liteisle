package com.liteisle.common.domain.response;

import lombok.Data;

@Data
public class MarkdownContentResp {
    private String storageUrl;
    private String content;
    private Long version;
}
