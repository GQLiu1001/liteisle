package com.liteisle.common.domain.response;

import lombok.Data;

@Data
public class AuthInfoResp {
    private String username;
    private String email;
    private String avatar;
    private String token;
}
