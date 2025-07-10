package com.liteisle.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileStatusEnum {
    PROCESSING("processing"),

    AVAILABLE("available"),

    FAILED("failed");

    @EnumValue
    @JsonValue
    private final String value;
}
