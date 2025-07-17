package com.liteisle.service;

import com.liteisle.common.domain.Users;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liteisle.common.dto.request.AuthForgotPasswordReq;
import com.liteisle.common.dto.request.AuthLoginReq;
import com.liteisle.common.dto.request.AuthRegisterReq;
import com.liteisle.common.dto.request.AuthResetPasswordReq;
import com.liteisle.common.dto.response.AuthCurrentUserResp;
import com.liteisle.common.dto.response.AuthInfoResp;
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
     * @param req 用户名密码
     * @return AuthInfoResp
     */
    AuthInfoResp login(AuthLoginReq req);

    /**
     * 用户注册
     * 创建token返回相关信息时，创建默认文件夹！
     * @param req AuthRegisterReq
     * @return AuthInfoResp
     */
    AuthInfoResp register(AuthRegisterReq req);

    /**
     * 发送验证码
     * @param email 邮箱
     */
    void sendVcode(String email);

    /**
     * 忘记密码
     * @param req AuthForgotPasswordReq
     */
    void forgotPassword(AuthForgotPasswordReq req);

    /**
     * 获取当前用户信息
     * @return AuthCurrentUserResp
     */
    AuthCurrentUserResp getCurrentUser();

    /**
     * 修改当前用户密码
     * @param req AuthResetPasswordReq
     */
    void resetPassword(AuthResetPasswordReq req);

    /**
     * 上传当前用户头像
     * @param file 用户上传的图像
     * @return 头像的URL
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
