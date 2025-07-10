package com.liteisle.common.domain.response;

import java.util.Date;

import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.enums.ItemType;
import lombok.Data;

@Data
public class SearchContentResp {


    private ItemType itemType; // "file" 或 "folder"
    private Long id;
    private String name;
    private String path;

    // 文件特有字段
    private FileTypeEnum fileType; // 仅当 itemType 为 "file" 时存在
    private Long fileSize; // 仅当 itemType 为 "file" 时存在
    private FileStatusEnum fileStatus; // 仅当 itemType 为 "file" 时存在

    // 文件夹特有字段
    private Integer subItemCount; // 仅当 itemType 为 "folder" 时存在

    private Date updateTime;

}
