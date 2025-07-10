package com.liteisle.common.domain.request;

import lombok.Data;

@Data
public class AuthForgotPasswordReq {
    private String username;
    private String email;
    private String vcode;
    private String newPassword;
    private String confirmPassword;
}
