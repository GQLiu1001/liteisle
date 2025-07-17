package com.liteisle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.liteisle.common.domain.ShareLinks;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.dto.request.ShareCreateReq;
import com.liteisle.common.dto.request.ShareSaveReq;
import com.liteisle.common.dto.request.ShareVerifyReq;
import com.liteisle.common.dto.response.ShareCreateResp;
import com.liteisle.common.dto.response.ShareInfoResp;
import com.liteisle.common.dto.response.ShareRecordPageResp;
import com.liteisle.common.dto.response.ShareSaveAsyncResp;

/**
 * @author 11965
 * @description 针对表【share_links(管理公开分享链接)】的数据库操作Service
 * @createDate 2025-07-10 20:09:48
 */
public interface ShareLinksService extends IService<ShareLinks> {
    /**
     * 创建分享链接
     *
     * @param req 请求参数
     * @return 响应参数
     */
    ShareCreateResp createShare(ShareCreateReq req);

    /**
     * 验证分享链接
     *
     * @param req 验证参数
     * @return 响应参数
     */
    ShareInfoResp verifyShare(ShareVerifyReq req);

    /**
     * 保存分享内容到自己的云盘
     *
     * @param req 保存参数
     * @return 响应参数
     */
    ShareSaveAsyncResp saveShare(ShareSaveReq req);

    /**
     * 获取我的分享记录
     *
     * @param page 分页参数
     * @return 响应参数
     */
    IPage<ShareRecordPageResp.ShareRecord> getShareRecords(IPage<ShareRecordPageResp.ShareRecord> page);

    /**
     * 删除/取消分享链接，使其链接和提取码失效。
     *
     * @param shareId 分享id
     */
    void deleteShare(Long shareId);
}
