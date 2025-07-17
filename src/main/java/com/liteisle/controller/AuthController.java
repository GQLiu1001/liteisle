package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.constant.SystemConstant;
import com.liteisle.common.dto.request.AuthForgotPasswordReq;
import com.liteisle.common.dto.request.AuthLoginReq;
import com.liteisle.common.dto.request.AuthRegisterReq;
import com.liteisle.common.dto.request.AuthResetPasswordReq;
import com.liteisle.common.dto.response.AuthCurrentUserResp;
import com.liteisle.common.dto.response.AuthInfoResp;
import com.liteisle.service.core.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@Tag(name = "用户认证接口")
public class AuthController {

    @Resource
    private UsersService usersService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录接口")
    @PostMapping("/login")
    public Result<AuthInfoResp> login(@RequestBody AuthLoginReq req) {
        AuthInfoResp resp = usersService.login(req);
        return Result.success(resp);
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "用户注册接口")
    @PostMapping("/register")
    public Result<AuthInfoResp> register(@RequestBody AuthRegisterReq req) {
        AuthInfoResp resp = usersService.register(req);
        return Result.success(resp);
    }

    /**
     * 发送验证码
     */
    @Operation(summary = "发送验证码", description = "发送邮箱验证码")
    @PostMapping("/send-vcode")
    public Result<Void> sendVcode(@RequestParam String email) {
        usersService.sendVcode(email);
        return Result.success();
    }

    /**
     * 忘记密码
     */
    @Operation(summary = "忘记密码", description = "忘记密码重置接口")
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody AuthForgotPasswordReq req) {
        usersService.forgotPassword(req);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<AuthCurrentUserResp> getCurrentUser() {
        AuthCurrentUserResp resp = usersService.getCurrentUser();
        return Result.success(resp);
    }

    /**
     * 修改当前用户密码
     */
    @Operation(summary = "修改当前用户密码", description = "修改当前用户密码")
    @PostMapping("/me/reset-password")
    public Result<Void> resetPassword(@RequestBody AuthResetPasswordReq req) {
        usersService.resetPassword(req);
        return Result.success();
    }

    /**
     * 上传当前用户头像
     */
    @Operation(summary = "上传当前用户头像", description = "上传当前用户头像")
    @PostMapping("/me/picture")
    public Result<String> uploadPicture(@RequestParam("file") MultipartFile file) {
        String url = usersService.uploadPicture(file);
        return Result.success(url);
    }

    /**
     * 重置当前用户头像
     */
    @Operation(summary = "重置当前用户头像", description = "重置当前用户头像为默认头像")
    @PutMapping("/me/reset-picture")
    public Result<String> resetPicture() {
        usersService.resetPicture();
        return Result.success(SystemConstant.USER_DEFAULT_URL);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录", description = "用户退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        usersService.logout();
        return Result.success();
    }
}