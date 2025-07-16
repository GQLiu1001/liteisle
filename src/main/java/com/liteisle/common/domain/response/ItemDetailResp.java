package com.liteisle.common.domain.response;

import java.util.Date;

import com.liteisle.common.enums.ItemType;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class ItemDetailResp {
    private Long id;
    private String name;
    private ItemType itemType; // "file" 或 "folder"
    private Long size;
    private String path;
    private Date createTime;
    private Date updateTime;
}
