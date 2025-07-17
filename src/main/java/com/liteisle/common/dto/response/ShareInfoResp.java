package com.liteisle.common.dto.response;

import com.liteisle.common.enums.ItemType;
import lombok.Data;

@Data
public class ShareInfoResp {
    private ItemType itemType;  // "file" 或 "folder"
    private String itemName;
    private Long itemSize;    // 如果是文件，为文件大小；如果是文件夹，为文件夹总大小
    private Long totalFiles;  // 如果是文件夹，为包含的文件总数
}
