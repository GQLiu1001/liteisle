package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class ShareSaveReq {
    private String shareToken;    // 分享链接的唯一标识
    private String sharePassword; // 分享链接的提取码
    private Long userId; // 分享发起人
    /**
     * 用户自定义保存路径
     */
    private Long targetFolderId; //自定义保存路径
}
