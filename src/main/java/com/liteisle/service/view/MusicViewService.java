package com.liteisle.service.view;

import com.liteisle.common.dto.response.MusicViewResp;

public interface MusicViewService {
    /**
     * 获取音乐页面信息
     * @param content 搜索内容
     * @return 音乐页面信息
     */
    MusicViewResp getMusicView(String content);
}
