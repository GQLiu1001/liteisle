package com.liteisle.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransferTypeEnum {
    UPLOAD("upload"),
    DOWNLOAD("download");
    @EnumValue
    @JsonValue
    private final String value;
}
