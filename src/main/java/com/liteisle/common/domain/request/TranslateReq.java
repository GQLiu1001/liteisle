package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class TranslateReq {
    /**
     * 用户划词选中的核心文本。
     */
    private String text;
    /**
     * 目标语言代码 默认中文。
     */
    private String targetLang;
    /**
     * 当前正在阅读或编辑的文件的ID。
     */
    private Long fileId;
}
