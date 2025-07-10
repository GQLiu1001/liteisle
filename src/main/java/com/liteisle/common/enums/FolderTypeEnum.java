package com.liteisle.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
@AllArgsConstructor
@Getter
public enum FolderTypeEnum {
    SYSTEM("system"),

    PLAYLIST("playlist"),

    NOTEBOOK("notebook");

    @EnumValue
    @JsonValue
    private final String value;

}
