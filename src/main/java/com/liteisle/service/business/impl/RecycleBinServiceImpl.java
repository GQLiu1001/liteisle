package com.liteisle.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.Users;
import com.liteisle.common.domain.request.RecycleBinReq;
import com.liteisle.common.domain.response.RecycleBinContentResp;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.FilesService;
import com.liteisle.service.FoldersService;
import com.liteisle.service.StoragesService;
import com.liteisle.service.UsersService;
import com.liteisle.service.business.RecycleBinService;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecycleBinServiceImpl implements RecycleBinService {
    @Resource
    private FoldersService foldersService;
    @Resource
    private FilesService filesService;
    @Resource
    private StoragesService storagesService;
    @Resource
    private UsersService usersService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RecycleBinContentResp getRecycleBinContent(String content) {
        // 并行执行两个异步查询
        CompletableFuture<List<RecycleBinContentResp.FileItem>> fileFuture = filesService.getRecycleBinViewWithContent(content);
        CompletableFuture<List<RecycleBinContentResp.FolderItem>> folderFuture = foldersService.getRecycleBinViewWithContent(content);

        return CompletableFuture.allOf(fileFuture, folderFuture)
                .thenApply(v -> {
                    try {
                        return new RecycleBinContentResp(
                                folderFuture.get(),  // 获取文件夹列表结果
                                fileFuture.get()     // 获取文件列表结果
                        );
                    } catch (Exception e) {
                        throw new LiteisleException("获取文档信息失败" + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("获取文档页面信息失败", ex);
                    return new RecycleBinContentResp(Collections.emptyList(), Collections.emptyList());
                })
                .join();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void restoreItems(RecycleBinReq req) {
        // 2. 统一获取 userID
        Long userId = UserContextHolder.getUserId();

        // 3. 参数校验
        if ((req.getFileIds() == null || req.getFileIds().isEmpty()) &&
                (req.getFolderIds() == null || req.getFolderIds().isEmpty())) {
            throw new LiteisleException("请选择要恢复的文件或文件夹");
        }

        // 4. 批量恢复文件
        if (req.getFileIds() != null && !req.getFileIds().isEmpty()) {
            filesService.update(new UpdateWrapper<Files>()
                    .in("id", req.getFileIds())
                    .eq("user_id", userId) //  增加用户ID校验，防止越权
                    .set("delete_time", null));
        }

        // 5. 批量恢复文件夹及其下的所有文件
        if (req.getFolderIds() != null && !req.getFolderIds().isEmpty()) {
            // 5.1 恢复文件夹本身
            foldersService.update(new UpdateWrapper<Folders>()
                    .in("id", req.getFolderIds())
                    .eq("user_id", userId) //  增加用户ID校验
                    .set("delete_time", null));

            // 5.2  恢复这些文件夹下的所有文件 直接按 folder_id 更新
            filesService.update(new UpdateWrapper<Files>()
                    .in("folder_id", req.getFolderIds()) //  按 folder_id 批量查找
                    .eq("user_id", userId)
                    .isNotNull("delete_time")
                    .set("delete_time", null));
        }
    }

    /**
     * 彻底删除回收站中选定的项目
     * @param req 包含要删除的文件和文件夹ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void purgeItems(RecycleBinReq req) {
        Long userId = UserContextHolder.getUserId();

        // 1. 参数校验
        if ((req.getFileIds() == null || req.getFileIds().isEmpty()) &&
                (req.getFolderIds() == null || req.getFolderIds().isEmpty())) {
            // 如果请求为空，直接返回，不抛异常
            return;
        }

        long totalSizeToDecrease = 0L;

        // 2. 彻底删除文件夹及其下的所有文件
        if (req.getFolderIds() != null && !req.getFolderIds().isEmpty()) {
            // 2.1 找出这些文件夹下的所有文件
            List<Files> filesInFolders = filesService.list(new LambdaQueryWrapper<Files>()
                    .in(Files::getFolderId, req.getFolderIds())
                    .eq(Files::getUserId, userId));

            // 2.2 计算这些文件占用的总空间大小，并减少引用计数
            if (!filesInFolders.isEmpty()) {
                totalSizeToDecrease += decreaseStorageReferenceAndGetSize(filesInFolders);

                // 2.3 物理删除这些文件记录
                List<Long> fileIdsInFolders =
                        filesInFolders.stream().map(Files::getId).collect(Collectors.toList());
                filesService.removeByIds(fileIdsInFolders);
            }

            // 2.4 物理删除文件夹记录
            foldersService.removeByIds(req.getFolderIds());
        }

        // 3. 彻底删除选中的独立文件
        if (req.getFileIds() != null && !req.getFileIds().isEmpty()) {
            // 3.1 找出这些文件的信息
            List<Files> standaloneFiles = filesService.listByIds(req.getFileIds());

            // 3.2 计算这些文件占用的总空间大小，并减少引用计数
            if (!standaloneFiles.isEmpty()) {
                totalSizeToDecrease += decreaseStorageReferenceAndGetSize(standaloneFiles);

                // 3.3 物理删除这些文件记录
                filesService.removeByIds(req.getFileIds());
            }
        }

        // 4. 更新用户已用空间
        if (totalSizeToDecrease > 0) {
            updateUserStorageUsed(userId, -totalSizeToDecrease); // 传负数表示减少
        }
    }


    /**
     * 清空回收站
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void clearRecycleBin() {
        Long userId = UserContextHolder.getUserId();

        // 1. 查找回收站中所有的文件
        List<Files> allFilesInRecycleBin = filesService.list(new LambdaQueryWrapper<Files>()
                .eq(Files::getUserId, userId)
                .isNotNull(Files::getDeleteTime));

        long totalSizeToDecrease = 0L;

        // 2. 处理所有待删除文件
        if (!allFilesInRecycleBin.isEmpty()) {
            // 2.1 计算总空间大小，并减少引用计数
            totalSizeToDecrease += decreaseStorageReferenceAndGetSize(allFilesInRecycleBin);

            // 2.2 物理删除所有文件记录
            List<Long> allFileIds =
                    allFilesInRecycleBin.stream().map(Files::getId).collect(Collectors.toList());
            filesService.removeByIds(allFileIds);
        }

        // 3. 查找并物理删除回收站中所有的文件夹
        List<Long> allFolderIdsInRecycleBin = foldersService.list(new LambdaQueryWrapper<Folders>()
                        .select(Folders::getId)
                        .eq(Folders::getUserId, userId)
                        .isNotNull(Folders::getDeleteTime))
                .stream().map(Folders::getId).collect(Collectors.toList());

        if (!allFolderIdsInRecycleBin.isEmpty()) {
            foldersService.removeByIds(allFolderIdsInRecycleBin);
        }

        // 4. 更新用户已用空间
        if (totalSizeToDecrease > 0) {
            updateUserStorageUsed(userId, -totalSizeToDecrease);
        }
    }


    /**
     * [辅助方法] 减少存储引用计数，并返回释放的总空间大小
     * @param filesToDelete 要处理的文件列表
     * @return 释放的总空间大小 (in bytes)
     */
    private long decreaseStorageReferenceAndGetSize(List<Files> filesToDelete) {
        if (filesToDelete == null || filesToDelete.isEmpty()) {
            return 0L;
        }

        // 1. 筛选出有有效 storageId 的文件
        List<Files> validFiles = filesToDelete.stream()
                .filter(f -> f.getStorageId() != null)
                .toList();

        if (validFiles.isEmpty()) {
            return 0L;
        }

        // 2. 批量获取这些文件对应的存储信息 (storages)
        List<Long> storageIds = validFiles.stream().map(Files::getStorageId).distinct().collect(Collectors.toList());
        Map<Long, Storages> storageMap = storagesService.listByIds(storageIds)
                .stream().collect(Collectors
                        //创建一个 Map。
                        // 对于流里的每一个 Storages 对象，用它的 getId() 方法的结果作为 key，并用这个对象本身 (s -> s 的结果) 作为 value。
                        .toMap(Storages::getId,
                        //身份函数 (Identity Function)，因为它返回的永远是它自己。
                        s -> s));

        // 3. 计算每个 storageId 需要减少的引用次数
        Map<Long, Long> refCountMap = validFiles.stream()
                .collect(Collectors.groupingBy(Files::getStorageId, Collectors.counting()));

        // 4. 批量减少引用计数
        refCountMap.forEach((storageId, countToDecrease) -> {
            storagesService.update(new UpdateWrapper<Storages>()
                    .eq("id", storageId)
                    .setSql("reference_count = reference_count - " + countToDecrease));
        });

        // 5. 计算释放的总空间大小
        long totalFreedSize = 0L;
        for (Files file : validFiles) {
            Storages storage = storageMap.get(file.getStorageId());
            if (storage != null) {
                totalFreedSize += storage.getFileSize();
            }
        }

        return totalFreedSize;
    }


    /**
     * [辅助方法] 更新用户已用空间
     * @param userId 用户ID
     * @param sizeChange 空间变化量 (正数表示增加, 负数表示减少)
     */
    private void updateUserStorageUsed(Long userId, long sizeChange) {
        if (sizeChange == 0) return;

        usersService.update(new UpdateWrapper<Users>()
                .eq("id", userId)
                .setSql("storage_used = storage_used + " + sizeChange));
    }

}
