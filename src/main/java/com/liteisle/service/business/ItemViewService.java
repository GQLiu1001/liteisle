package com.liteisle.service.business;

import com.liteisle.common.dto.request.ItemsDeleteReq;
import com.liteisle.common.dto.request.ItemsOperationReq;
import com.liteisle.common.dto.request.ItemsRenameReq;
import com.liteisle.common.dto.request.SetOrderReq;
import com.liteisle.common.dto.response.ItemDetailResp;

public interface ItemViewService {
    /**
     * 重命名文件/文件夹
     * @param req 请求体包含文件/文件夹ID 和名称
     */
    void renameItem(ItemsRenameReq req);

    /**
     * 移动问文件或者文件夹
     * @param req 请求体包含文件/文件夹ID 和目标文件夹ID
     */
    void moveItems(ItemsOperationReq req);

    /**
     * 删除文件或者文件夹
     * @param req 请求体包含文件/文件夹ID
     */
    void deleteItems(ItemsDeleteReq req);

    /**
     * 获取项目详情 (文件/文件夹)
     * @param itemId 项目ID
     * @param itemType 项目类型
     * @return 项目详情
     */
    ItemDetailResp getItemDetail(Long itemId, String itemType);

    /**
     * 自定义排序 (文件/文件夹)
     * @param itemId 项目ID
     * @param req 排序请求体 包含 beforeId 和 afterId
     * @param itemType 项目类型
     */
    void setItemOrder(Long itemId, SetOrderReq req, String itemType);

    /**
     * 复制项目 (文件/文件夹)
     * @param req 复制请求体
     */
    void copyItems(ItemsOperationReq req);
}
