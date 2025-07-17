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

    SUCCESS("success"),

    FAILED("failed"),

    CANCELED("canceled");

    @EnumValue
    @JsonValue
    private final String value;

    // 自定义静态方法，根据 value 查找枚举
    public static TransferStatusEnum fromValue(String value) {
        for (TransferStatusEnum enumConstant : TransferStatusEnum.values()) {
            if (enumConstant.value.equals(value)) {
                return enumConstant;
            }
        }
        // 找不到可抛自定义异常或返回 null，根据业务决定
        throw new IllegalArgumentException("参数错误" + value);
    }
}
