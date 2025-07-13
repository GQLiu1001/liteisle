package com.liteisle.service;

import com.liteisle.common.domain.Users;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.domain.request.AuthForgotPasswordReq;
import com.liteisle.common.domain.request.AuthLoginReq;
import com.liteisle.common.domain.request.AuthRegisterReq;
import com.liteisle.common.domain.request.AuthResetPasswordReq;
import com.liteisle.common.domain.response.AuthCurrentUserResp;
import com.liteisle.common.domain.response.AuthInfoResp;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 11965
* @description 针对表【users(用户账户与基本信息表)】的数据库操作Service
* @createDate 2025-07-10 20:09:48
*/
public interface UsersService extends IService<Users> {
    /**
     * 用户登录
     * 验证用户名密码，通过返回jwt token
     * @param req
     * @return
     */
    AuthInfoResp login(AuthLoginReq req);

    /**
     * 用户注册
     * 创建token返回相关信息时，创建默认文件夹！
     * @param req
     * @return
     */
    AuthInfoResp register(AuthRegisterReq req);

    void sendVcode(String email);

    void forgotPassword(AuthForgotPasswordReq req);

    AuthCurrentUserResp getCurrentUser();

    void resetPassword(AuthResetPasswordReq req);

    String uploadPicture(MultipartFile file);

    void resetPicture();

    void logout(Long userId, String token);
}
