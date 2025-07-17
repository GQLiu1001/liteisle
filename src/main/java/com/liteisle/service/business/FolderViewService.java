package com.liteisle.service.business;

import com.liteisle.common.domain.response.FolderContentResp;

public interface FolderViewService {
    /**
     *  获取指定文件夹内容
     *
     * @param folderId 文件夹id，特别地，0代表根目录。
     * @param sortBy   排序字段，可选值: `name`, `file_size`, `create_time`, `update_time`, `sorted_order` (默认)
     * @return 文件夹内容
     */
    FolderContentResp getFolderContent(Long folderId, String sortBy ,String sortOrder,String content);
}
