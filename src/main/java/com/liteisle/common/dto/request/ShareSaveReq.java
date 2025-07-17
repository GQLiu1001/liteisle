package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class ShareSaveReq {
    private String shareToken;    // 分享链接的唯一标识
    private String sharePassword; // 分享链接的提取码
    /**
     * 用户自定义保存路径
     */
    private Long targetFolderId; //自定义保存路径
}
