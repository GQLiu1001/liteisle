package com.liteisle.module.chain;

import lombok.Data;

@Data
public class FocusRewardContext {

    /** 输入：本次专注的时长 */
    private final Integer focusMinutes;

    /** 输出：如果中奖，这里会存入奖励的岛屿URL */
    private String awardedIslandUrl;

    /** 状态标记：一旦有处理器成功处理，就设为true，终止链条继续传递 */
    private boolean handled = false;

    public FocusRewardContext(Integer focusMinutes) {
        this.focusMinutes = focusMinutes;
    }
}