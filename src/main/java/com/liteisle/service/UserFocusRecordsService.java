package com.liteisle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteisle.common.domain.UserFocusRecords;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.domain.response.FocusCalendarResp;
import com.liteisle.common.domain.response.FocusStatsPageResp;

/**
* @author 11965
* @description 针对表【user_focus_records(用户单次专注会话记录表)】的数据库操作Service
* @createDate 2025-07-10 20:09:48
*/
public interface UserFocusRecordsService extends IService<UserFocusRecords> {

    /**
     * 创建专注记录
     * ！责任链概率片段是否获得岛屿
     * @param min 专注时长
     * @return 创建的专注记录id
     */
    String createFocusRecord(Integer min);

    /**
     * 获取专注总次数
     * @return 总次数
     */
    Long getFocusTotalCount();

    /**
     * 获取专注日历数据
     * @param year 年
     * @param month 月
     * @return 专注日历数据
     */
    FocusCalendarResp getFocusCalendar(Integer year, Integer month);

    /**
     * 获取专注记录列表
     * @param page 分页参数
     * @return 专注记录列表
     */
    IPage<FocusStatsPageResp.FocusRecord> getFocusRecords(IPage<FocusStatsPageResp.FocusRecord> page);
}
