package com.liteisle.service.business;

import com.liteisle.common.dto.request.TranslateReq;
import com.liteisle.common.dto.response.TranslateResp;

public interface TranslateService {

    /**
     * 划词翻译
     * @param req 请求参数
     * @return 翻译结果与原文
     */
    TranslateResp translate(TranslateReq req);
}
