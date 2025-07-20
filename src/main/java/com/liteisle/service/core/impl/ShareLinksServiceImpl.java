package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.center.AsyncFileProcessingCenter;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 11965
* @description 针对表【share_links(管理公开分享链接)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class ShareLinksServiceImpl extends ServiceImpl<ShareLinksMapper, ShareLinks>
    implements ShareLinksService{

    @Resource
    private StringRedisTemplate stringRedisTemplate;
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

    @Override
    public ShareCreateResp createShare(ShareCreateReq req) {
        //TODO 加入布隆cache
        //一次只能分享单个文件或者单个文件夹
        checkOnlyOneTarget(req.getFileId(), req.getFolderId());
        Long userId = UserContextHolder.getUserId();
        String token = getShareToken();
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
        return new ShareCreateResp(token, password);
    }

    private boolean tokenExists(String token) {
        //TODO redis布隆
        return shareLinksMapper
                .selectOne(new QueryWrapper<ShareLinks>().eq("share_token", token)) != null;
    }

    private String getShareToken() {
        String token = CaptchaUtil.generate24DigitCaptcha();
        while (tokenExists(token)) {
            token = CaptchaUtil.generate24DigitCaptcha();
        }
        return token;
    }

    private void checkOnlyOneTarget(Long fileId, Long folderId) {
        if (fileId != null && folderId != null) {
            throw new LiteisleException("请选择要分享的文件或文件夹（只能选一个）");
        }
        if (fileId == null && folderId == null) {
            throw new LiteisleException("必须选择一个分享对象");
        }
    }


    @Override
    public ShareInfoResp verifyShare(ShareVerifyReq req) {
        // 验证 token 和密码
        boolean flag = validateGetShareRequest(req.getShareToken(), req.getSharePassword());
        if (!flag){
            throw new LiteisleException("验证失败");
        }

        // 获取分享信息
        ShareLinks shareLink = this.getOne(new QueryWrapper<ShareLinks>()
                .eq("share_token", req.getShareToken()));
        if (shareLink == null) {
            throw new LiteisleException("分享链接无效");
        }

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
        // 1. 再次验证分享链接的有效性
        boolean flag = validateGetShareRequest(req.getShareToken(), req.getSharePassword());
        if (!flag) {
            throw new LiteisleException("分享链接无效或密码错误");
        }

        Long receiverId = UserContextHolder.getUserId();
        ShareLinks shareLink = this.getOne(new QueryWrapper<ShareLinks>()
                .eq("share_token", req.getShareToken()));
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

    private boolean validateGetShareRequest(String shareToken, String sharePassword) {
        ShareLinks shareLink = this.getOne(new QueryWrapper<ShareLinks>().eq("share_token", shareToken));
        if (shareLink == null) return false;
        // 检查有效期
        if (shareLink.getExpireTime() != null && new Date().after(shareLink.getExpireTime())) {
            return false;
        }
        // 如果设置了密码，需校验
        return shareLink.getSharePassword() == null || shareLink.getSharePassword().equals(sharePassword);
    }



    @Override
    public IPage<ShareRecordPageResp.ShareRecord> getShareRecords(IPage<ShareRecordPageResp.ShareRecord> page) {
        Long ownerId = UserContextHolder.getUserId();
        return shareLinksMapper.getShareRecords(page,ownerId);
    }

    @Override
    public void deleteShare(Long shareId) {
        //用户取消分享
        Long ownerId = UserContextHolder.getUserId();
        boolean remove = this.remove(new QueryWrapper<ShareLinks>()
                .eq("id", shareId)
                .eq("owner_id", ownerId));
        if (!remove){
            throw new LiteisleException("删除分享失败");
        }
        //TODO redis
    }
}




