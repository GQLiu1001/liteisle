package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.constant.FolderConstant;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.request.FolderCreateReq;
import com.liteisle.common.domain.response.DocumentViewResp;
import com.liteisle.common.domain.response.FolderHierarchyResp;
import com.liteisle.common.domain.response.MusicViewResp;
import com.liteisle.common.domain.response.RecycleBinContentResp;
import com.liteisle.common.enums.FolderTypeEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.FoldersService;
import com.liteisle.mapper.FoldersMapper;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author 11965
 * @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Service实现
 * @createDate 2025-07-10 20:09:48
 */
@Service
public class FoldersServiceImpl extends ServiceImpl<FoldersMapper, Folders>
        implements FoldersService {

    @Resource
    private ExecutorService virtualThreadPool;
    @Resource
    private FoldersMapper foldersMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createUserDefaultFolder(Long userId) {
        for (String folderName : FolderConstant.DEFAULT_SYSTEM_FOLDERS) {
            createSystemFolder(userId, folderName);
        }
    }

    @Override
    public CompletableFuture<List<DocumentViewResp.Booklist>> getDocumentViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<Folders> list;
            if (content != null && !content.isEmpty()) {
                // 有搜索关键词：执行复杂查询，计算符合条件的文件数
                list = foldersMapper.selectFoldersWithCount(userId, content, "booklist");
            } else {
                // 无搜索关键词：执行简单查询，直接返回总文件数
                list = foldersMapper.selectFolders(userId, "booklist");
            }
            return list.stream()
                    .map(this::convertToBooklist)
                    .toList();
        }, virtualThreadPool);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createFolder(FolderCreateReq req) {
        // 基础验证
        validateBaseRequest(req);

        // 获取并验证父文件夹
        Folders parentFolder = validateParentFolder(req.getParentId());

        // 验证文件夹类型与父文件夹的匹配性
        validateFolderTypeCompatibility(parentFolder, req.getFolderType());

        // 构建并保存新文件夹
        Folders folder = buildFolderEntity(req);
        boolean saveResult = this.save(folder);

        if (!saveResult) {
            throw new LiteisleException("创建文件夹失败");
        }
    }

//    @Override
//    public List<FolderHierarchyResp> getFolderHierarchy() {
//        Long userId = UserContextHolder.getUserId();
//        List<FolderHierarchyResp> resp = new ArrayList<>();
//        List<Folders> list = this.list(new QueryWrapper<Folders>().eq("user_id", userId));
//        if (list.isEmpty()){
//            throw new LiteisleException("获取文件夹层级失败");
//        }
//        for (Folders folders : list) {
//            FolderHierarchyResp folderHierarchyResp = new FolderHierarchyResp();
//            folderHierarchyResp.setId(folders.getId());
//            folderHierarchyResp.setFolderName(folders.getFolderName());
//            folderHierarchyResp.setFolderType(folders.getFolderType());
//            folderHierarchyResp.setParentId(folders.getParentId());
//            resp.add(folderHierarchyResp);
//        }
//        return resp;
//    }

    // 使用 Stream API 优化转换
    @Override
    public List<FolderHierarchyResp> getFolderHierarchy() {
        Long userId = UserContextHolder.getUserId();
        List<Folders> list = this.list(new QueryWrapper<Folders>().eq("user_id", userId));
        // 注意：如果 list 为空，这里会返回一个空List，而不是抛异常，这通常是更好的行为
        return list.stream()
                .map(folder -> {
                    FolderHierarchyResp dto = new FolderHierarchyResp();
                    BeanUtils.copyProperties(folder, dto); // 如果字段名一致，可以用 BeanUtils
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<MusicViewResp.Playlist>> getMusicViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<Folders> list;
            if (content != null && !content.isEmpty()) {
                // 有搜索关键词：执行复杂查询，计算符合条件的文件数
                list = foldersMapper.selectFoldersWithCount(userId, content, "playlist");
            } else {
                // 无搜索关键词：执行简单查询，直接返回总文件数
                list = foldersMapper.selectFolders(userId, "playlist");
            }
            return list.stream()
                    .map(this::convertToPlaylist)
                    .toList();
        }, virtualThreadPool);
    }

    @Override
    public CompletableFuture<List<RecycleBinContentResp.FolderItem>> getRecycleBinViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return CompletableFuture.supplyAsync(() -> foldersMapper.getRecycleBinViewWithContent(userId, content), virtualThreadPool);
    }

    private MusicViewResp.Playlist convertToPlaylist(Folders item) {
        MusicViewResp.Playlist playlist = new MusicViewResp.Playlist();
        playlist.setId(item.getId());
        playlist.setFolderName(item.getFolderName());
        playlist.setFolderType(item.getFolderType());
        playlist.setSortedOrder(item.getSortedOrder());
        playlist.setSubCount(item.getSubCount());
        return playlist;
    }

    private DocumentViewResp.Booklist convertToBooklist(Folders item) {
        DocumentViewResp.Booklist booklist = new DocumentViewResp.Booklist();
        booklist.setId(item.getId());
        booklist.setFolderName(item.getFolderName());
        booklist.setFolderType(item.getFolderType());
        booklist.setSortedOrder(item.getSortedOrder());
        booklist.setSubCount(item.getSubCount());
        return booklist;
    }

    private void createSystemFolder(Long userId, String folderName) {
        Folders folder = new Folders();
        folder.setUserId(userId);
        folder.setParentId(0L);
        folder.setFolderName(folderName);
        folder.setFolderType(FolderTypeEnum.SYSTEM);
        folder.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()));
        Date now = new Date();
        folder.setCreateTime(now);
        folder.setUpdateTime(now);
        this.save(folder);
    }

    private void validateBaseRequest(FolderCreateReq req) {
        if (req == null) {
            throw new LiteisleException("文件夹信息不能为空");
        }
        if (req.getParentId() == 0) {
            throw new LiteisleException("文件夹不能创建在根目录下");
        }
    }

    private Folders validateParentFolder(Long parentId) {
        Folders parentFolder = this.getById(parentId);
        if (parentFolder == null) {
            throw new LiteisleException("父级文件夹不存在");
        }
        if (parentFolder.getParentId() != 0) {
            throw new LiteisleException("父级文件夹不能创建子文件夹");
        }
        return parentFolder;
    }

    private void validateFolderTypeCompatibility(Folders parentFolder, FolderTypeEnum folderType) {
        // 定义父文件夹名称与允许的文件夹类型映射
        Map<String, FolderTypeEnum> allowedTypeMap = Map.of(
                "歌单", FolderTypeEnum.PLAYLIST,
                "笔记", FolderTypeEnum.BOOKLIST,
                "分享", FolderTypeEnum.SYSTEM,
                "上传", FolderTypeEnum.SYSTEM
        );

        // 检查父文件夹是否有特定类型限制
        allowedTypeMap.forEach((folderName, allowedType) -> {
            if (Objects.equals(parentFolder.getFolderName(), folderName) && !Objects.equals(folderType, allowedType)) {
                throw new LiteisleException("禁止在" + folderName + "文件夹下创建" + allowedType.getValue() + "类型文件夹");
            }
        });
    }

    private Folders buildFolderEntity(FolderCreateReq req) {
        Folders folder = new Folders();
        folder.setUserId(UserContextHolder.getUserId());
        folder.setParentId(req.getParentId());
        folder.setFolderName(req.getName());
        folder.setFolderType(req.getFolderType());
        folder.setCreateTime(new Date());
        folder.setUpdateTime(new Date());
        folder.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()));
        return folder;
    }
}




