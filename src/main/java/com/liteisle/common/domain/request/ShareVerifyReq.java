package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class ShareVerifyReq {
    private String shareToken;    // 分享链接的唯一标识
    private String sharePassword; // 分享链接的提取码
}
