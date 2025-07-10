package com.liteisle.common.domain.response;

import lombok.Data;

@Data
public class AuthCurrentUserResp {
    private String username;
    private String email;
    private String avatar;
    private Long storageUsed;
    private Long storageQuota;
}
