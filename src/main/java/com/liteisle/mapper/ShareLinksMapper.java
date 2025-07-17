package com.liteisle.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.liteisle.common.domain.ShareLinks;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteisle.common.domain.response.ShareRecordPageResp;

/**
* @author 11965
* @description 针对表【share_links(管理公开分享链接)】的数据库操作Mapper
* @createDate 2025-07-10 20:09:48
* @Entity com.liteisle.common.domain.ShareLinks
*/
public interface ShareLinksMapper extends BaseMapper<ShareLinks> {

    IPage<ShareRecordPageResp.ShareRecord> getShareRecords(IPage<ShareRecordPageResp.ShareRecord> page, Long ownerId);
}




