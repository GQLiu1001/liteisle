package com.liteisle.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class MarkdownContentResp {
    private String content;
    private Long version;
}
