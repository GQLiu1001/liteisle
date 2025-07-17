package com.liteisle.common.dto.request;

import lombok.Data;

@Data
public class AuthResetPasswordReq {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
