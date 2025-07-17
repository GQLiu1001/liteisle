package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class AuthRegisterReq {
    private String username;
    private String email;
    private String password;
    private String vcode;
}
