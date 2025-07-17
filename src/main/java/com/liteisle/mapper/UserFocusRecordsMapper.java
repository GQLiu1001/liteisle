package com.liteisle.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.liteisle.common.domain.UserFocusRecords;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteisle.common.dto.response.FocusStatsPageResp;
import org.apache.ibatis.annotations.Select;

/**
* @author 11965
* @description 针对表【user_focus_records(用户单次专注会话记录表)】的数据库操作Mapper
* @createDate 2025-07-10 20:09:48
* @Entity com.liteisle.common.domain.UserFocusRecords
*/
public interface UserFocusRecordsMapper extends BaseMapper<UserFocusRecords> {

    Integer getTotalFocusMinutesInMonth(Integer year, Integer month, Long userId);

    @Select("SELECT * FROM user_focus_records WHERE user_id = #{userId} ORDER BY create_time DESC")
    IPage<FocusStatsPageResp.FocusRecord> selectFocusRecordsPage(IPage<FocusStatsPageResp.FocusRecord> page, Long userId);
}




