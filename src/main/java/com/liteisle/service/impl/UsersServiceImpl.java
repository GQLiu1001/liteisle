package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.center.EmailCenter;
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
import com.liteisle.config.MinioConfig;
import com.liteisle.service.FoldersService;
import com.liteisle.service.UsersService;
import com.liteisle.mapper.UsersMapper;
import com.liteisle.util.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.liteisle.common.constant.SystemConstant.*;

/**
* @author 11965
* @description 针对表【users(用户账户与基本信息表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Slf4j
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
    @Resource
    private FoldersService foldersService;
    @Resource
    private EmailCenter emailCenter;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private MinioConfig minioConfig;

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
        // 1. 参数校验
        if (isInvalidEmail(req.getEmail())) {
            throw new LiteisleException("邮箱格式不正确");
        }
        // 2. 验证码校验
        String redisVCode = stringRedisTemplate.opsForValue().get(RedisConstant.USER_EMAIL + req.getEmail());
        if (redisVCode == null) {
            throw new LiteisleException("验证码已过期，请重新获取");
        }
        if (!req.getVcode().equals(redisVCode)){
            throw new LiteisleException("验证码错误");
        }
        // 3. 检查用户名是否已存在 (作为初步检查，对用户友好)
        if (this.exists(new LambdaQueryWrapper<Users>().eq(Users::getUsername, req.getUsername()))) {
            throw new LiteisleException("用户名已被占用");
        }
        // 4. 创建并保存用户
        Users user = createNewUser(req);
        try {
            this.save(user); // 依赖数据库唯一约束来处理并发
        } catch (DuplicateKeyException e) {
            throw new LiteisleException("用户名已被占用"); // 捕获并发导致的重复键异常
        }
        // 5. 创建用户关联资源（如默认文件夹）
        foldersService.createUserDefaultFolder(user.getId());
        // 6. 所有事务性操作成功后，清理非事务性资源（如Redis验证码）
        stringRedisTemplate.delete(RedisConstant.USER_EMAIL + req.getEmail());
        // 7. 生成Token并返回结果
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new AuthInfoResp(user.getUsername(), user.getEmail(), user.getAvatar(), token);
    }

    @Override
    public void sendVcode(String email) {
        if (stringRedisTemplate.opsForValue().get(RedisConstant.USER_EMAIL + email) != null) {
            throw new LiteisleException("请勿重复发送验证码");
        }
        if (isInvalidEmail(email)) {
            throw new LiteisleException("邮箱格式不正确");
        }
        String vCode = CaptchaUtil.generate6DigitCaptcha();
        emailCenter.sendEmail(email, "Liteisle 邮箱验证码", "您的验证码是：" + vCode);
        stringRedisTemplate.opsForValue().set(RedisConstant.USER_EMAIL + email, vCode, 5, TimeUnit.MINUTES);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void forgotPassword(AuthForgotPasswordReq req) {
        if (isInvalidEmail(req.getEmail())) {
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
    @Transactional(rollbackFor = Exception.class)
    public String uploadPicture(MultipartFile file) {
        // 1. 基本校验
        if (file.isEmpty()) {
            throw new LiteisleException("上传文件不能为空");
        }
        Long userId = UserContextHolder.getUserId();

        // 2. 校验新文件的后缀是否合法
        String originalFilename = file.getOriginalFilename();
        String newExtension = getExtension(originalFilename);
        if (newExtension.isEmpty() || !isImageExtension(newExtension)) {
            throw new LiteisleException("不支持的头像文件格式");
        }

        try {
            // 3. 【核心优化】上传前，删除所有可能的旧头像，无需查询数据库
            removeOldAvatars(userId);

            // 4. 构建新的、唯一的 MinIO 对象名
            String objectName = String.format(USER_AVATAR_FOLDER_PREFIX, userId) + "/" + AVATAR_FILE_NAME + newExtension;

            // 5. 上传新文件到 MinIO
            minioUtil.uploadFile(file, objectName);

            // 6. 构建新头像的永久访问 URL
            String newAvatarUrl = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;

            // 7. 将新 URL 更新到数据库
            Users userToUpdate = new Users();
            userToUpdate.setId(userId);
            userToUpdate.setAvatar(newAvatarUrl);
            boolean updatedRows = this.updateById(userToUpdate); // 使用 this.updateById()
            if (!updatedRows) {
                throw new LiteisleException("更新用户头像失败，用户可能不存在");
            }

            // 8. 返回新 URL 给前端
            return newAvatarUrl;

        } catch (Exception e) {
            // 修正日志记录方式
            log.error("上传头像失败, 用户ID: {}", userId, e);
            throw new LiteisleException("上传头像失败，请稍后重试");
        }
    }

    /**
     * 尝试删除一个用户所有可能的旧头像。
     * 这个方法会忽略 "对象不存在" 的错误，因为这是预期行为。
     * @param userId 用户ID
     */
    private void removeOldAvatars(Long userId) {
        String avatarFolderPath = String.format(USER_AVATAR_FOLDER_PREFIX, userId);
        for (String ext : SUPPORTED_AVATAR_EXTENSIONS) {
            String oldObjectName = avatarFolderPath + "/" + AVATAR_FILE_NAME + ext;
            try {
                minioUtil.removeFile(oldObjectName);
                log.info("成功删除旧头像: {}", oldObjectName);
            } catch (io.minio.errors.ErrorResponseException e) {
                // 这是 MinIO 特定的、包含错误响应的异常
                if (!"NoSuchKey".equals(e.errorResponse().code())) {
                    // 如果错误码不是 "NoSuchKey"（即“对象不存在”），说明是其他问题，值得记录
                    log.warn("删除旧头像 {} 时发生非预期的 MinIO 错误: {}", oldObjectName, e.errorResponse());
                }
                // 如果是 NoSuchKey，我们就忽略它，这是预期行为
            } catch (Exception e) {
                // 捕获其他所有非 MinIO 的异常，如网络问题
                log.warn("删除旧头像 {} 时发生通用异常: {}", oldObjectName, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPicture() {
        Long userId = UserContextHolder.getUserId();

        // 1. 先删除云存储上的所有旧头像
        removeOldAvatars(userId);

        // 2. 再更新数据库中的记录为默认 URL
        Users userToUpdate = new Users();
        userToUpdate.setId(userId);
        userToUpdate.setAvatar(SystemConstant.USER_DEFAULT_URL);
        boolean updated = this.updateById(userToUpdate);
        if (!updated) {
            throw new LiteisleException("重置头像失败");
        }
    }

    @Override
    public void logout() {
        String token = UserContextHolder.getUserToken();
        if (token == null) {
            log.warn("登出操作没有取得token");
            return;
        }

        stringRedisTemplate.opsForValue()
                .set(RedisConstant.BLACK_LIST_TOKEN + token, "invalid", 7, TimeUnit.DAYS);
    }

    private boolean isInvalidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return !email.matches(regex);
    }

    private Users createNewUser(AuthRegisterReq req) {
        Users user = new Users();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setAvatar(SystemConstant.USER_DEFAULT_URL);
        user.setStorageQuota(SystemConstant.USER_DEFAULT_STORAGE_QUOTA);
        user.setStorageUsed(0L);
        Date now = new Date();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        return user;
    }

    // 从文件名获取后缀的工具方法（之前已提供）
    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex);
    }

    private boolean isImageExtension(String extension) {
        return SUPPORTED_AVATAR_EXTENSIONS.contains(extension.toLowerCase());
    }


}




