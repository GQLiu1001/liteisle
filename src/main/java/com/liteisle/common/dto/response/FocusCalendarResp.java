package com.liteisle.common.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class FocusCalendarResp {
    /**
     * 年月，格式如 "2024-07"
     */
    private String yearMonth;
    /**
     * 签到日期列表 bitmap
     */
    private List<Integer> checkInDays;
    private Integer totalCheckInCount;  // 总签到次数
    /**
     * 当月总专注分钟数
     */
    private Integer totalFocusMinutes;
}
