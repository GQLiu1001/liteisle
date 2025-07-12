package com.liteisle.common.domain.response;

import lombok.Data;

@Data
public class TranslateResp {
    /**
     * 原始请求的文本，便于前端核对。
     */
    private String translatedText;
    /**
     * 翻译后的文本。
     */
    private String originalText;
}
