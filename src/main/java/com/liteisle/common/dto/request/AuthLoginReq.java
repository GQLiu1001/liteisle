package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class AuthLoginReq {
    private String username;
    private String password;
}
