package com.liteisle.service.business;

import com.liteisle.common.dto.request.RecycleBinReq;
import com.liteisle.common.dto.response.RecycleBinContentResp;

public interface RecycleBinService {
    /**
     * 获取回收站内容
     *
     * @return RecycleBinContentResp
     */
    RecycleBinContentResp getRecycleBinContent(String content);

    /**
     * 恢复回收站项目
     *
     * @param req RecycleBinReq fileIds folderIds
     */
    void restoreItems(RecycleBinReq req);

    /**
     * 彻底删除回收站项目
     *
     * @param req RecycleBinReq fileIds folderIds
     */
    void purgeItems(RecycleBinReq req);

    /**
     * 清空回收站
     */
    void clearRecycleBin();
}
