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
     * 验证用户名密码，通过-》返回jwt token
     * @param req
     */
    AuthInfoResp login(AuthLoginReq req);

    /**
     * 用户注册
     * 创建token返回相关信息时，创建默认文件夹！
     * @param req
     */
    AuthInfoResp register(AuthRegisterReq req);

    /**
     * 发送验证码
     * @param email
     */
    void sendVcode(String email);

    /**
     * 忘记密码
     * @param req
     */
    void forgotPassword(AuthForgotPasswordReq req);

    /**
     * 获取当前用户信息
     */
    AuthCurrentUserResp getCurrentUser();

    /**
     * 修改当前用户密码
     * @param req
     */
    void resetPassword(AuthResetPasswordReq req);

    /**
     * 上传当前用户头像
     * @param file
     */
    String uploadPicture(MultipartFile file);

    /**
     * 重置当前用户头像为默认头像
     */
    void resetPicture();

    /**
     * 退出登录
     */
    void logout();
}
