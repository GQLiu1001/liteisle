package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.ShareLinks;
import com.liteisle.common.domain.request.ShareCreateReq;
import com.liteisle.common.domain.request.ShareSaveReq;
import com.liteisle.common.domain.request.ShareVerifyReq;
import com.liteisle.common.domain.response.ShareCreateResp;
import com.liteisle.common.domain.response.ShareInfoResp;
import com.liteisle.common.domain.response.ShareRecordPageResp;
import com.liteisle.common.domain.response.ShareSaveAsyncResp;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.FilesService;
import com.liteisle.service.FoldersService;
import com.liteisle.service.ShareLinksService;
import com.liteisle.mapper.ShareLinksMapper;
import com.liteisle.util.CaptchaUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private ShareLinksService shareLinksService;

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
        data.setUserId(userId);
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
        //验证 有效期 密码 token
        //用户输入验证 验证成功会展示的项目为ShareInfoResp
        return null;
    }

    @Override
    public ShareSaveAsyncResp saveShare(ShareSaveReq req) {
        //注意要再验证一次有效期 密码 token ,verifyShare 只是展示
        //用户保存分享的文件/文件夹 输入token 密码 以及 用户自定义保存路径

        //主要影响：file -》 storages 引用次数变多 且保存在用户自定义路径（默认为用户的分享文件夹下）
        // folder -》 file -》 storages 引用次数变多 且保存在用户自定义路径（默认为用户的分享文件夹下）
        return null;
    }

    @Override
    public IPage<ShareRecordPageResp.ShareRecord> getShareRecords(IPage<ShareRecordPageResp.ShareRecord> page) {
        Long userId = UserContextHolder.getUserId();
        return shareLinksMapper.getShareRecords(page,userId);
    }

    @Override
    public void deleteShare(Long shareId) {
        //用户取消分享
        Long userId = UserContextHolder.getUserId();
        boolean remove = this.remove(new QueryWrapper<ShareLinks>().eq("id", shareId).eq("user_id", userId));
        if (!remove){
            throw new LiteisleException("删除分享失败");
        }
        //TODO redis
    }
}




