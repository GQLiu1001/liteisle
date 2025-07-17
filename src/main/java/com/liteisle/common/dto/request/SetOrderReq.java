package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class SetOrderReq {
    /**
     * 被移动到的位置前一个ID
     */
    private Long beforeId;
    /**
     * 被移动到的位置后一个ID
     */
    private Long afterId;
}
