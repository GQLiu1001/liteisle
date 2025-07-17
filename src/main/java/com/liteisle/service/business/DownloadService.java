package com.liteisle.service.business;

import com.liteisle.common.dto.request.ItemsSelectionReq;
import com.liteisle.common.dto.response.DownloadSessionResp;

public interface DownloadService {
    /**
     * 注册下载任务
     * @param req 下载任务参数List fileIds List folderIds
     * @return DownloadSessionResp
     */
    DownloadSessionResp registerDownload(ItemsSelectionReq req);
}
