package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class AuthLoginReq {
    private String username;
    private String password;
}
