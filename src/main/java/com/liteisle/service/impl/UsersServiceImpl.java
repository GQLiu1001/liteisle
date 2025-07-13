package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.constant.RedisConstant;
import com.liteisle.common.constant.SystemConstant;
import com.liteisle.common.domain.Users;
import com.liteisle.common.domain.request.AuthForgotPasswordReq;
import com.liteisle.common.domain.request.AuthLoginReq;
import com.liteisle.common.domain.request.AuthRegisterReq;
import com.liteisle.common.domain.request.AuthResetPasswordReq;
import com.liteisle.common.domain.response.AuthCurrentUserResp;
import com.liteisle.common.domain.response.AuthInfoResp;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.UsersService;
import com.liteisle.mapper.UsersMapper;
import com.liteisle.util.CaptchaUtil;
import com.liteisle.util.JwtUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
* @author 11965
* @description 针对表【users(用户账户与基本信息表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users>
    implements UsersService{

    @Resource
    private  PasswordEncoder passwordEncoder; // 注入密码编码器
    @Resource
    private UsersMapper usersMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public AuthInfoResp login(AuthLoginReq req) {
        Users user = this.getOne(new LambdaQueryWrapper<Users>()
                .eq(Users::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new LiteisleException("用户名或密码错误");
        }
        AuthInfoResp authInfoResp = new AuthInfoResp();
        authInfoResp.setUsername(user.getUsername());
        authInfoResp.setEmail(user.getEmail());
        authInfoResp.setAvatar(user.getAvatar());
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        authInfoResp.setToken(token);
        return authInfoResp;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AuthInfoResp register(AuthRegisterReq req) {
        if (isValidEmail(req.getEmail())) {
            throw new LiteisleException("邮箱格式不正确");
        }
        String redisVCode = stringRedisTemplate.opsForValue().get(RedisConstant.USER_EMAIL + req.getEmail());
        if (!req.getVcode().equals(redisVCode)){
            throw new LiteisleException("验证码错误");
        }
        LambdaQueryWrapper<Users> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(Users::getUsername, req.getUsername());
        if (this.exists(usernameWrapper)) {
            throw new LiteisleException("用户名已被占用");
        }
        Users user = new Users();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword())); // 加密存储
        user.setAvatar(SystemConstant.USER_DEFAULT_URL);
        user.setStorageQuota(SystemConstant.USER_DEFAULT_STORAGE_QUOTA);
        user.setStorageUsed(0L);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        this.save(user);
        stringRedisTemplate.delete(RedisConstant.USER_EMAIL + req.getEmail());
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        //TODO 创建默认用户文件夹
        return new AuthInfoResp(user.getUsername(), user.getEmail(), user.getAvatar(), token);
    }

    @Override
    public void sendVcode(String email) {
        if (isValidEmail(email)) {
            throw new LiteisleException("邮箱格式不正确");
        }
        String vCode = CaptchaUtil.generate6DigitCaptcha();
        stringRedisTemplate.opsForValue().set(RedisConstant.USER_EMAIL + email, vCode, 5, TimeUnit.MINUTES);
        //TODO 发送邮件
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void forgotPassword(AuthForgotPasswordReq req) {
        if (isValidEmail(req.getEmail())) {
            throw new LiteisleException("邮箱格式不正确");
        }
        if (!Objects.equals(req.getConfirmPassword(), req.getNewPassword())){
            throw new LiteisleException("密码不一致");
        }
        String redisVCode = stringRedisTemplate.opsForValue().get(RedisConstant.USER_EMAIL + req.getEmail());
        if (redisVCode == null || !redisVCode.equals(req.getVcode())) {
            throw new LiteisleException("验证码错误或已过期");
        }
        Users user = usersMapper.selectOne(new QueryWrapper<Users>()
                .eq("email", req.getEmail())
                .eq("username", req.getUsername())
        );
        if (user == null) {
            throw new LiteisleException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        usersMapper.updateById(user);
        stringRedisTemplate.delete(RedisConstant.USER_EMAIL + req.getEmail());
    }

    @Override
    public AuthCurrentUserResp getCurrentUser() {
        Long userId = UserContextHolder.getUserId();
        Users user = usersMapper.selectOne(new QueryWrapper<Users>()
            .select("username", "email", "avatar", "storage_used", "storage_quota")
            .eq("id", userId)
        );
        if (user == null){
            throw new LiteisleException("用户不存在");
        }
        return new AuthCurrentUserResp(
            user.getUsername(),
            user.getEmail(),
            user.getAvatar(),
            user.getStorageUsed(),
            user.getStorageQuota()
        );
    }

    @Override
    public void resetPassword(AuthResetPasswordReq req) {
        Long userId = UserContextHolder.getUserId();
        Users user = usersMapper.selectOne(new QueryWrapper<Users>().eq("id", userId));
        boolean matches = passwordEncoder.matches(req.getOldPassword(), user.getPassword());
        if (!matches) {
            throw new LiteisleException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        int row = usersMapper.updateById(user);
        if (row == 0) {
            throw new LiteisleException("修改密码失败");
        }
    }

    @Override
    public String uploadPicture(MultipartFile file) {
        //TODO 集成MINIO
        return "";
    }

    @Override
    public void resetPicture() {
        Long userId = UserContextHolder.getUserId();
        Users user = usersMapper.selectOne(new QueryWrapper<Users>().eq("id", userId));
        user.setAvatar(SystemConstant.USER_DEFAULT_URL);
        int row = usersMapper.updateById(user);
        if (row == 0) {
            throw new LiteisleException("重置头像失败");
        }
    }

    @Override
    public void logout(Long userId, String token) {
        String userToken = UserContextHolder.getUserToken();
        if (!token.equals(userToken)) {
            throw new LiteisleException("用户未登录");
        }
        Long id = UserContextHolder.getUserId();
        if (!id.equals(userId)) {
            throw new LiteisleException("用户未登录");
        }
        stringRedisTemplate.opsForValue()
                .set(RedisConstant.BLACK_LIST_TOKEN + token, "invalid", 7, TimeUnit.DAYS);
    }

    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(regex);
    }
}




