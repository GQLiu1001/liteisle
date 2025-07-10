package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.AuthForgotPasswordReq;
import com.liteisle.common.domain.request.AuthLoginReq;
import com.liteisle.common.domain.request.AuthRegisterReq;
import com.liteisle.common.domain.request.AuthResetPasswordReq;
import com.liteisle.common.domain.response.AuthCurrentUserResp;
import com.liteisle.common.domain.response.AuthInfoResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@Tag(name = "用户认证接口")
public class AuthController {

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录接口")
    @PostMapping("/login")
    public Result<AuthInfoResp> login(@RequestBody AuthLoginReq req) {
        return Result.success();
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "用户注册接口")
    @PostMapping("/register")
    public Result<AuthInfoResp> register(@RequestBody AuthRegisterReq req) {
        return Result.success();
    }

    /**
     * 发送验证码
     */
    @Operation(summary = "发送验证码", description = "发送邮箱验证码")
    @PostMapping("/send-vcode")
    public Result<Void> sendVcode(@RequestParam String email) {
        return Result.success();
    }

    /**
     * 忘记密码
     */
    @Operation(summary = "忘记密码", description = "忘记密码重置接口")
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody AuthForgotPasswordReq req) {
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<AuthCurrentUserResp> getCurrentUser() {
        return Result.success();
    }

    /**
     * 修改当前用户密码
     */
    @Operation(summary = "修改当前用户密码", description = "修改当前用户密码")
    @PostMapping("/me/reset-password")
    public Result<Void> resetPassword(@RequestBody AuthResetPasswordReq req) {
        return Result.success();
    }

    /**
     * 上传当前用户头像
     */
    @Operation(summary = "上传当前用户头像", description = "上传当前用户头像")
    @PostMapping("/me/picture")
    public Result<String> uploadPicture(@RequestParam("file") MultipartFile file) {
        return Result.success();
    }

    /**
     * 重置当前用户头像
     */
    @Operation(summary = "重置当前用户头像", description = "重置当前用户头像为默认头像")
    @PutMapping("/me/reset-picture")
    public Result<String> resetPicture() {
        return Result.success();
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录", description = "用户退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}