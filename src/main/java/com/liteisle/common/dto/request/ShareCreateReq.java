package com.liteisle.common.dto.request;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class ShareCreateReq {
    private Long fileId;         // 要分享的文件ID（可选）
    private Long folderId;       // 要分享的文件夹ID（可选）
    private Boolean isEncrypted; // 是否加密，true则会生成提取码
    /**
     * 前端传递有效期天数（0为永久或为1、7、30天），存入数据库的是过期日期
     */
    private Integer expiresInDays; // 有效期天数，0表示永久有效
}
