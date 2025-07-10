package com.liteisle.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ItemType {
    FILE("file"),
    FOLDER("folder");
    private final String value;
}
