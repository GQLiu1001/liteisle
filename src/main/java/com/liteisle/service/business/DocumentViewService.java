package com.liteisle.service.business;

import com.liteisle.common.domain.response.DocumentViewResp;

public interface DocumentViewService {

    /**
     * 获取文档视图
     * @param content 文档内容
     * @return 文档视图
     */
    DocumentViewResp getDocumentView(String content);
}
