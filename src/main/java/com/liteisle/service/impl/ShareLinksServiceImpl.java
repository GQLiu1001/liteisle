package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.ShareLinks;
import com.liteisle.common.dto.request.ShareCreateReq;
import com.liteisle.common.dto.request.ShareSaveReq;
import com.liteisle.common.dto.request.ShareVerifyReq;
import com.liteisle.common.dto.response.ShareCreateResp;
import com.liteisle.common.dto.response.ShareInfoResp;
import com.liteisle.common.dto.response.ShareRecordPageResp;
import com.liteisle.common.dto.response.ShareSaveAsyncResp;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.ItemType;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.FilesService;
import com.liteisle.service.FoldersService;
import com.liteisle.service.ShareLinksService;
import com.liteisle.mapper.ShareLinksMapper;
import com.liteisle.service.StoragesService;
import com.liteisle.util.CaptchaUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

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
    private ShareLinksService shareLinksService;
    @Resource
    private StoragesService storagesService;
    @Override
    public ShareCreateResp createShare(ShareCreateReq req) {
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
            return createShare(
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
            return  createShare(
                   null, req.getFolderId(), token, req.getIsEncrypted(), req.getExpiresInDays(), userId);
        }
    }

    private ShareCreateResp createShare(
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
    public ShareSaveAsyncResp saveShare(ShareSaveReq req) {
        //注意要再验证一次有效期 密码 token ,verifyShare 只是展示
        //用户保存分享的文件/文件夹 输入token 密码 以及 用户自定义保存路径
        boolean flag = validateGetShareRequest(req.getShareToken(), req.getSharePassword());
        if (!flag){
            throw new LiteisleException("验证失败");
        }
        //主要影响：file -》 storages 引用次数变多 且保存在用户自定义路径（默认为用户的分享文件夹下）
        // folder -》 file -》 storages 引用次数变多 且保存在用户自定义路径（默认为用户的分享文件夹下）
        //链接： transfer log 与 websocket 通信
        //TODO 开启传输任务链
        return null;
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




