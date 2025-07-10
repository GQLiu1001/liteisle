package com.liteisle.common.domain.response;

import java.util.Date;

import com.liteisle.common.enums.FolderTypeEnum;
import lombok.Data;

@Data
public class FolderInfoResp {
    private Long id;
    private String name;
    private FolderTypeEnum folderType; // "system", "playlist", "notebook" 等
    private Long parentId;
    private Date createTime;
    private Date updateTime;
}
