package com.liteisle.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthCurrentUserResp {
    private String username;
    private String email;
    private String avatar;
    private Long storageUsed;
    private Long storageQuota;
}
