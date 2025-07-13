package com.liteisle.common.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthInfoResp {
    private String username;
    private String email;
    private String avatar;
    private String token;
}
