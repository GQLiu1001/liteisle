package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.center.AsyncFileProcessingCenter;
import com.liteisle.common.constant.RedisConstant;
import com.liteisle.common.domain.*;
import com.liteisle.common.dto.request.ShareCreateReq;
import com.liteisle.common.dto.request.ShareSaveReq;
import com.liteisle.common.dto.request.ShareVerifyReq;
import com.liteisle.common.dto.response.ShareCreateResp;
import com.liteisle.common.dto.response.ShareInfoResp;
import com.liteisle.common.dto.response.ShareRecordPageResp;
import com.liteisle.common.dto.response.ShareSaveAsyncResp;
import com.liteisle.common.enums.*;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.module.cache.bloom.BloomClient;
import com.liteisle.service.core.*;
import com.liteisle.mapper.ShareLinksMapper;
import com.liteisle.util.CaptchaUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.liteisle.common.constant.RedisConstant.SHARE_TOKEN_BLACKLIST_BLOOM;
import static com.liteisle.common.constant.RedisConstant.SHARE_TOKEN_BLOOM;
import static com.liteisle.common.constant.SystemConstant.MAX_TOKEN_ATTEMPTS;

/**
* @author 11965
* @description 针对表【share_links(管理公开分享链接)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class ShareLinksServiceImpl extends ServiceImpl<ShareLinksMapper, ShareLinks>
    implements ShareLinksService{

    @Resource
    private ShareLinksMapper shareLinksMapper;
    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;
    @Resource
    private StoragesService storagesService;
    @Resource
    private UsersService usersService;
    @Resource
    private AsyncFileProcessingCenter asyncFileProcessingCenter;
    @Resource
    private TransferLogService transferLogService;
    @Resource
    private BloomClient bloomClient;

    @Override
    public ShareCreateResp createShare(ShareCreateReq req) {
        //一次只能分享单个文件或者单个文件夹
        checkOnlyOneTarget(req.getFileId(), req.getFolderId());
        Long userId = UserContextHolder.getUserId();
        String token = createShareToken();
        //如果分享的是文件
        if (req.getFileId() != null) {
            //获取并判断
            Files file = filesService.getOne(new QueryWrapper<Files>()
                    .eq("id", req.getFileId())
                    .eq("user_id", userId)
            );
            if (file == null || file.getFileStatus() != FileStatusEnum.AVAILABLE) {
                throw new LiteisleException("文件不存在或不可用");
            }
            return createShareLinkP(
                    req.getFileId(), null ,token, req.getIsEncrypted(), req.getExpiresInDays(),userId);
        }else {
            //分享的文件夹
            Folders folder = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("id", req.getFolderId())
                    .eq("user_id", userId)
            );
            if (folder == null) {
                throw new LiteisleException("文件夹不存在");
            }
            return  createShareLinkP(
                   null, req.getFolderId(), token, req.getIsEncrypted(), req.getExpiresInDays(), userId);
        }
    }

    private void checkOnlyOneTarget(Long fileId, Long folderId) {
        if (fileId != null && folderId != null) {
            throw new LiteisleException("请选择要分享的文件或文件夹（只能选一个）");
        }
        if (fileId == null && folderId == null) {
            throw new LiteisleException("必须选择一个分享对象");
        }
    }

    private String createShareToken() {
        for (int i = 0; i < MAX_TOKEN_ATTEMPTS; i++) {
            String token = CaptchaUtil.generate24DigitCaptcha();
            if (!tokenExists(token)) {
                return token;
            }
        }
        throw new LiteisleException("系统繁忙，生成分享链接失败，请稍后再试");
    }

    private boolean tokenExists(String token) {
        return bloomClient.mightContain(SHARE_TOKEN_BLOOM, token,this::verifyTokenFromDb);
    }

    private boolean verifyTokenFromDb(String token) {
        return shareLinksMapper.selectOne(new QueryWrapper<ShareLinks>().eq("share_token", token)) != null;
    }

    private ShareCreateResp createShareLinkP(
            Long fileId,Long folderId, String token ,Boolean isEncrypted ,Integer expiresInDays ,Long userId) {
        //判断是否加密
        String password = null;
        if (isEncrypted) {
            password = CaptchaUtil.generate6DigitCaptcha();
        }
        ShareLinks data = new ShareLinks();
        data.setOwnerId(userId);
        data.setShareToken(token);
        data.setSharePassword(password);
        data.setFileId(fileId);
        data.setFolderId(folderId);
        data.setExpireTime(expiresInDays == null ? null : DateUtils.addDays(new Date(), expiresInDays));
        data.setCreateTime(new Date());
        data.setUpdateTime(new Date());
        int insert = shareLinksMapper.insert(data);
        if (insert <= 0){
            throw new LiteisleException("创建分享链接失败");
        }
        boolean b = bloomClient.add2Bloom(SHARE_TOKEN_BLOOM, token);
        if (!b){
            throw new LiteisleException("创建分享链接失败");
        }
        return new ShareCreateResp(token, password);
    }

    @Override
    public ShareInfoResp verifyShare(ShareVerifyReq req) {
        // 调用统一验证方法
        ShareLinks shareLink =
                validateAndGetShareLink(req.getShareToken(), req.getSharePassword());

        Long ownerUserId = shareLink.getOwnerId(); // 分享者用户ID
        ShareInfoResp shareInfoResp = new ShareInfoResp();

        if (shareLink.getFileId() != null) {
            // 分享的是文件
            Files file = filesService.getOne(new QueryWrapper<Files>()
                    .eq("id", shareLink.getFileId())
                    .eq("user_id", ownerUserId)
                    .eq("delete_time", null));
            if (file == null) {
                throw new LiteisleException("分享文件不存在或已删除");
            }
            shareInfoResp.setItemType(ItemType.FILE);
            shareInfoResp.setItemName(file.getFileName());
            shareInfoResp.setItemSize(storagesService.getById(file.getStorageId()).getFileSize());
            shareInfoResp.setTotalFiles(1L);
        } else {
            // 分享的是文件夹
            Folders folder = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("id", shareLink.getFolderId())
                    .eq("user_id", ownerUserId)
                    .eq("delete_time", null));
            if (folder == null) {
                throw new LiteisleException("分享文件夹不存在或已删除");
            }
            shareInfoResp.setItemType(ItemType.FOLDER);
            shareInfoResp.setItemName(folder.getFolderName());

            // 获取文件夹总大小，仅统计分享人自己的文件
            Long totalSize = filesService.getFileTotalSizeFromFolderId(shareLink.getFolderId(), ownerUserId);
            shareInfoResp.setItemSize(totalSize);

            // 获取文件夹下文件数量，仅统计分享人自己的文件
            Long count = filesService.count(new QueryWrapper<Files>()
                    .eq("folder_id", shareLink.getFolderId())
                    .eq("user_id", ownerUserId)
                    .eq("delete_time", null));
            shareInfoResp.setTotalFiles(count);
        }

        return shareInfoResp;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareSaveAsyncResp saveShare(ShareSaveReq req) {
        // 调用统一验证方法
        ShareLinks shareLink = validateAndGetShareLink(req.getShareToken(), req.getSharePassword());

        Long receiverId = UserContextHolder.getUserId();
        Long sharerId = shareLink.getOwnerId();

        // 2. 解析接收者要保存到的目标文件夹
        Long targetFolderId = resolveShareTargetFolderId(req.getTargetFolderId(), receiverId);

        List<Files> originalFiles;
        Long newParentFolderId = targetFolderId; // 默认保存到目标文件夹

        // 3. 根据分享类型（文件或文件夹）获取源文件列表
        if (shareLink.getFileId() != null) {
            // 分享的是单个文件
            Files file = filesService.getById(shareLink.getFileId());
            if (file == null) throw new LiteisleException("分享的文件已不存在");
            originalFiles = Collections.singletonList(file);
        } else {
            // 分享的是文件夹
            Folders originalFolder = foldersService.getById(shareLink.getFolderId());
            if (originalFolder == null) throw new LiteisleException("分享的文件夹已不存在");

            // 为接收者创建一个同名的新文件夹
            Folders newFolder = new Folders();
            newFolder.setUserId(receiverId);
            newFolder.setParentId(targetFolderId);
            newFolder.setFolderName(originalFolder.getFolderName());
            newFolder.setFolderType(originalFolder.getFolderType());
            newFolder.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()));
            foldersService.save(newFolder);
            newParentFolderId = newFolder.getId(); // 更新父文件夹ID为新创建的文件夹
            originalFiles = filesService.list(new QueryWrapper<Files>()
                    .eq("folder_id", shareLink.getFolderId()));
        }

        if (originalFiles.isEmpty()) {
            // 如果文件夹为空，直接返回成功
            return new ShareSaveAsyncResp(0, Collections.emptyList());
        }

        // 4. 检查接收者的存储空间
        long totalSizeToSave = originalFiles.stream()
                .map(f -> storagesService.getById(f.getStorageId()).getFileSize())
                .reduce(0L, Long::sum);
        Users receiver = usersService.getById(receiverId);
        if (receiver.getStorageUsed() + totalSizeToSave > receiver.getStorageQuota()) {
            throw new LiteisleException("你的存储空间不足，无法转存");
        }

        // 5. **为所有待转存文件创建初始记录 (状态为 PROCESSING)**
        List<Files> newFilesToSave = new ArrayList<>();
        List<TransferLog> logsToUpdate = new ArrayList<>();

        for (Files originalFile : originalFiles) {
            // 创建 Files 记录
            Files newFile = new Files();
            newFile.setUserId(receiverId);
            newFile.setFolderId(newParentFolderId);
            newFile.setFileName(originalFile.getFileName());
            newFile.setFileExtension(originalFile.getFileExtension());
            newFile.setFileType(originalFile.getFileType());
            newFile.setFileStatus(FileStatusEnum.PROCESSING); // 初始状态
            newFile.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()));
            filesService.save(newFile);
            newFilesToSave.add(newFile);

            // 创建 TransferLog 记录
            TransferLog log = new TransferLog();
            log.setUserId(receiverId);
            log.setTransferType(TransferTypeEnum.UPLOAD); // 转存可视为一种特殊的上传
            log.setItemName(originalFile.getFileName());
            log.setFileId(originalFile.getId()); // 【注意】这里暂时存源文件ID，用于异步任务查找
            log.setItemSize(storagesService.getById(originalFile.getStorageId()).getFileSize());
            log.setLogStatus(TransferStatusEnum.PROCESSING);
            transferLogService.save(log);
            logsToUpdate.add(log);
        }

        // 6. **触发异步任务**
        asyncFileProcessingCenter.processSharedFilesSave
                (sharerId, receiverId, newFilesToSave, logsToUpdate);

        // 7. 更新接收者已用空间
        usersService.update(new UpdateWrapper<Users>()
                .eq("id", receiverId)
                .setSql("storage_used = storage_used + " + totalSizeToSave));

        // 8. 构造并立即返回前端所需的数据
        return new ShareSaveAsyncResp(
                newFilesToSave.size(),
                newFilesToSave.stream().map(f -> {
                    ShareSaveAsyncResp.InitialFileData initialData = new ShareSaveAsyncResp.InitialFileData();
                    // ... 填充 initialData 的字段
                    initialData.setId(f.getId());
                    initialData.setName(f.getFileName());
                    initialData.setFileType(f.getFileType());
                    initialData.setFileStatus(f.getFileStatus());
                    initialData.setCreateTime(f.getCreateTime());
                    initialData.setUpdateTime(f.getUpdateTime());
                    return initialData;
                }).collect(Collectors.toList())
        );
    }

    private Long resolveShareTargetFolderId(Long folderId, Long userId) {
        if (folderId != null && folderId > 0) {
            // ... (验证逻辑同 resolveTargetFolderId)
            return folderId;
        } else {
            // 查找用户的“分享”系统文件夹
            Folders shareFolder = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("user_id", userId)
                    .eq("folder_type", FolderTypeEnum.SYSTEM)
                    .eq("folder_name", "分享")); // 依赖于初始化的文件夹名称
            if (shareFolder == null) throw new LiteisleException("无法找到默认的分享文件夹");
            return shareFolder.getId();
        }
    }

    /**
     * 统一验证分享链接并返回其实体。
     * 整合了黑名单、白名单布隆过滤器和数据库查询。
     *
     * @param shareToken 分享token
     * @param sharePassword 访问密码 (可以为null)
     * @return 验证通过的 ShareLinks 实体
     * @throws LiteisleException 如果验证失败
     */
    private ShareLinks validateAndGetShareLink(String shareToken, String sharePassword) {
        // 1. 【黑名单检查】快速拒绝无效token
        // 直接查布隆，不查DB。如果在黑名单里，说明一定无效。
        if (bloomClient.fastReqContains(SHARE_TOKEN_BLACKLIST_BLOOM, shareToken)) {
            throw new LiteisleException("分享链接不存在或已失效");
        }

        // 2. 【白名单检查】快速过滤绝大多数不存在的token
        // 直接查布隆，不查DB。如果白名单里没有，说明一定不存在。
        if (!bloomClient.fastReqContains(SHARE_TOKEN_BLOOM, shareToken)) {
            throw new LiteisleException("分享链接不存在或已失效");
        }

        // 3. 【数据库最终校验】执行唯一的一次数据库查询
        // 只有在两个布隆过滤器都无法100%确定时，才访问数据库
        ShareLinks shareLink = this.getOne(new QueryWrapper<ShareLinks>().eq("share_token", shareToken));

        // 4. 【综合验证】
        // 数据库说没有，说明是布隆误判，直接拒绝
        if (shareLink == null) {
            throw new LiteisleException("分享链接不存在或已失效");
        }

        // 检查有效期
        if (shareLink.getExpireTime() != null && new Date().after(shareLink.getExpireTime())) {
            // 可选：如果过期了，可以将其加入黑名单，加速下次判断
            bloomClient.add2Bloom(SHARE_TOKEN_BLACKLIST_BLOOM, shareToken);
            throw new LiteisleException("分享链接已过期");
        }

        // 检查密码
        if (shareLink.getSharePassword() != null && !shareLink.getSharePassword().equals(sharePassword)) {
            throw new LiteisleException("访问密码错误");
        }

        // 所有验证通过
        return shareLink;
    }

    @Override
    public IPage<ShareRecordPageResp.ShareRecord> getShareRecords(IPage<ShareRecordPageResp.ShareRecord> page) {
        Long ownerId = UserContextHolder.getUserId();
        return shareLinksMapper.getShareRecords(page,ownerId);
    }

    @Override
    public void deleteShare(Long shareId) {

        Long ownerId = UserContextHolder.getUserId();

        // 查询分享记录，校验是否存在
        ShareLinks shareLinks = this.getOne(new LambdaQueryWrapper<ShareLinks>()
                .eq(ShareLinks::getId, shareId)
                .eq(ShareLinks::getOwnerId, ownerId));

        if (shareLinks == null) {
            throw new LiteisleException("分享不存在或无权限删除");
        }

        // 删除分享记录
        boolean removed = this.removeById(shareId);
        if (!removed) {
            throw new LiteisleException("删除分享失败");
        }

        boolean success = bloomClient
                .add2Bloom(SHARE_TOKEN_BLACKLIST_BLOOM, shareLinks.getShareToken());
        if (!success) {
            throw new LiteisleException("添加分享黑名单失败");
        }
    }
}




