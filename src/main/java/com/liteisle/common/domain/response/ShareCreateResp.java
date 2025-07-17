package com.liteisle.common.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShareCreateResp {
    private String shareToken;    // 分享链接的唯一标识
    private String sharePassword; // 分享链接的提取码
}
