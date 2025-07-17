package com.liteisle.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TransferStatusEnum {
    PROCESSING("processing"),

    PAUSED("paused"),

    AVAILABLE("available"),

    FAILED("failed"),

    CANCELED("canceled");

    @EnumValue
    @JsonValue
    private final String value;
}
