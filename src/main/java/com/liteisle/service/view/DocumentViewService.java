package com.liteisle.service.view;

import com.liteisle.common.dto.response.DocumentViewResp;

public interface DocumentViewService {

    /**
     * 获取文档视图
     * @param content 文档内容
     * @return 文档视图
     */
    DocumentViewResp getDocumentView(String content);
}
