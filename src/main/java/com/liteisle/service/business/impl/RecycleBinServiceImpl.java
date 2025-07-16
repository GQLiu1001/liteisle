package com.liteisle.service.business.impl;

import com.liteisle.common.domain.request.RecycleBinReq;
import com.liteisle.common.domain.response.RecycleBinContentResp;
import com.liteisle.service.business.RecycleBinService;
import org.springframework.stereotype.Service;

@Service
public class RecycleBinServiceImpl implements RecycleBinService {
    @Override
    public RecycleBinContentResp getRecycleBinContent() {
        return null;
    }

    @Override
    public void restoreItems(RecycleBinReq req) {

    }

    @Override
    public void purgeItems(RecycleBinReq req) {

    }

    @Override
    public void clearRecycleBin() {

    }
}
