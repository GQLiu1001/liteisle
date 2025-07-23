package com.liteisle.service.view.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liteisle.center.ReindexCenter;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.Users;
import com.liteisle.common.dto.request.ItemsDeleteReq;
import com.liteisle.common.dto.request.ItemsOperationReq;
import com.liteisle.common.dto.request.ItemsRenameReq;
import com.liteisle.common.dto.request.SetOrderReq;
import com.liteisle.common.dto.response.ItemDetailResp;
import com.liteisle.common.enums.ItemType;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.FoldersService;
import com.liteisle.service.core.StoragesService;
import com.liteisle.service.core.UsersService;
import com.liteisle.service.view.ItemViewService;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
@Slf4j
@Service
public class ItemViewServiceImpl implements ItemViewService {
    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;
    @Resource
    private StoragesService storagesService;
    @Resource
    private UsersService usersService;
    @Resource
    private ReindexCenter reindexCenter;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void renameItem(ItemsRenameReq req) {
        if (req.getFileId() == null && req.getFolderId() == null) {
            throw new LiteisleException("必须提供要重命名的文件或文件夹ID");
        }
        if (req.getFileId() != null && req.getFolderId() != null) {
            throw new LiteisleException("只能选择一个项目进行重命名");
        }
        Long userId = UserContextHolder.getUserId();
        if (req.getFolderId() != null && req.getFolderId() > 0) {
            // 重命名文件夹
            foldersService.update(new UpdateWrapper<Folders>()
                    .eq("id", req.getFolderId())
                    .eq("user_id", userId)
                    .set("folder_name", req.getNewName()));
        }
        if (req.getFileId() != null && req.getFileId() > 0) {
            // 重命名文件
            filesService.update(new UpdateWrapper<Files>()
                    .eq("id", req.getFileId())
                    .eq("user_id", userId)
                    .set("file_name", req.getNewName()));
        }
    }

    // 重构后的 moveItems 方法
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void moveItems(ItemsOperationReq req) {
        Long targetFolderId = req.getTargetFolderId();
        if ((req.getFileIds() == null || req.getFileIds().isEmpty()) && (req.getFolderIds() == null || req.getFolderIds().isEmpty())) {
            throw new LiteisleException("请选择要移动的文件或文件夹");
        }
        if (targetFolderId == null || targetFolderId <= 0) {
            throw new LiteisleException("必须选择一个有效的目标文件夹");
        }
        Long userId = UserContextHolder.getUserId();

        // 1. 获取并校验目标文件夹
        Folders targetFolder = foldersService.getOne(new LambdaQueryWrapper<Folders>()
                .eq(Folders::getId, targetFolderId)
                .eq(Folders::getUserId, userId));
        if (targetFolder == null) {
            throw new LiteisleException("目标文件夹不存在或无权限");
        }

        // 2. 移动文件夹的逻辑
        if (req.getFolderIds() != null && !req.getFolderIds().isEmpty()) {
            // 规则: 文件夹只能移动到根目录下的系统文件夹 (parent_id=0)
            if (targetFolder.getParentId() != 0) {
                throw new LiteisleException("文件夹只能移动到 '歌单', '文档' 等一级分类下");
            }
            // 检查是否移动到自身或子孙文件夹
            for (Long folderIdToMove : req.getFolderIds()) {
                if (Objects.equals(folderIdToMove, targetFolderId)) {
                    throw new LiteisleException("不能将文件夹移动到其自身");
                }
                // 递归检查是否是子孙文件夹（需要一个辅助方法）
                if (isSubfolder(folderIdToMove, targetFolderId, userId)) {
                    throw new LiteisleException("不能将文件夹移动到其子文件夹中");
                }
            }
            // 执行更新
            foldersService.update(new UpdateWrapper<Folders>()
                    .in("id", req.getFolderIds())
                    .eq("user_id", userId)
                    .set("parent_id", targetFolderId));
        }

        // 3. 移动文件的逻辑
        if (req.getFileIds() != null && !req.getFileIds().isEmpty()) {
            // 规则: 文件只能移动到二级文件夹 (parent_id != 0)
            if (targetFolder.getParentId() == 0) {
                throw new LiteisleException("文件必须放入一个具体的歌单或文档分类中，不能直接放在一级分类下");
            }
            // 执行更新
            filesService.update(new UpdateWrapper<Files>()
                    .in("id", req.getFileIds())
                    .eq("user_id", userId)
                    .set("folder_id", targetFolderId));
        }
    }



    // 修正后的 deleteItems 方法
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteItems(ItemsDeleteReq req) {
        if ((req.getFileIds() == null || req.getFileIds().isEmpty()) &&
                (req.getFolderIds() == null || req.getFolderIds().isEmpty())) {
            throw new LiteisleException("请选择要删除的文件或文件夹");
        }
        Long userId = UserContextHolder.getUserId();

        // 1. 软删除文件夹及其下的所有文件
        if (req.getFolderIds() != null && !req.getFolderIds().isEmpty()) {
            // 1.1 软删除文件夹本身
            foldersService.update(new UpdateWrapper<Folders>()
                    .in("id", req.getFolderIds())
                    .eq("user_id", userId)
                    .set("delete_time", new Date()));

            // 1.2 【新增逻辑】软删除这些文件夹下的所有文件
            filesService.update(new UpdateWrapper<Files>()
                    .in("folder_id", req.getFolderIds()) // 根据 folder_id 查找
                    .eq("user_id", userId)            // 确保是该用户的文件
                    .set("delete_time", new Date())); // 设置删除时间
        }

        // 2. 软删除指定的文件
        if (req.getFileIds() != null && !req.getFileIds().isEmpty()) {
            filesService.update(new UpdateWrapper<Files>()
                    .in("id", req.getFileIds())
                    .eq("user_id", userId)
                    .set("delete_time", new Date()));
        }
    }


    @Override
    public ItemDetailResp getItemDetail(Long itemId, String itemType) {
        if (itemId == null || itemType == null) {
            throw new LiteisleException("请求文件类型错误");
        }
        Long userId = UserContextHolder.getUserId();
//        if (itemType.equals("folder")) {
//            Folders one = foldersService.getOne(new QueryWrapper<Folders>()
//                    .eq("id", itemId)
//                    .eq("user_id", userId)
//            );
//            long count = filesService.count(new QueryWrapper<Files>().eq("folder_id", itemId));
//            String path = getRelativePath(one.getId(), one.getFolderName());
//            return new ItemDetailResp(
//                    one.getId(),
//                    one.getFolderName(),
//                    ItemType.FOLDER,
//                    count,
//                    path,
//                    one.getCreateTime(),
//                    one.getUpdateTime()
//            );
//        }
        if (itemType.equals("folder")) {
            Folders one = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("id", itemId)
                    .eq("user_id", userId)
            );

            // 分别统计文件和文件夹的数量，并确保都加上了 user_id 条件
            long fileCount = filesService.count(new LambdaQueryWrapper<Files>()
                    .eq(Files::getFolderId, itemId)
                    .eq(Files::getUserId, userId));

            long folderCount = foldersService.count(new LambdaQueryWrapper<Folders>()
                    .eq(Folders::getParentId, itemId)
                    .eq(Folders::getUserId, userId));

            // 计算总数
            long totalCount = fileCount + folderCount;

            String path = getRelativePath(one.getId(), one.getFolderName());
            return new ItemDetailResp(
                    one.getId(),
                    one.getFolderName(),
                    ItemType.FOLDER,
                    totalCount, // <-- 使用修正后的总数
                    path,
                    one.getCreateTime(),
                    one.getUpdateTime()
            );
        }
        if (itemType.equals("file")) {
            Files one = filesService.getById(itemId);
            Storages storages = storagesService.getById(one.getStorageId());
            String path = getRelativePath(one.getFolderId(), one.getFileName());
            return new ItemDetailResp(
                    one.getId(),
                    one.getFileName(),
                    ItemType.FILE,
                    storages.getFileSize(),
                    path,
                    one.getCreateTime(),
                    one.getUpdateTime()
            );
        }
        throw new LiteisleException("获取详细信息失败");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setItemOrder(Long itemId, SetOrderReq req, String itemType) {

        Long userId = UserContextHolder.getUserId();

        if ("folder".equals(itemType)) {
            Folders folderToSort = foldersService.getOne(new LambdaQueryWrapper<Folders>()
                    .select(Folders::getParentId)
                    .eq(Folders::getId, itemId)
                    .eq(Folders::getUserId, userId));

            if (folderToSort == null) {
                throw new LiteisleException("要排序的文件夹不存在");
            }
            if (folderToSort.getParentId() == 0) {
                // 通常系统一级文件夹是不允许用户移动排序的
                throw new LiteisleException("不能对系统一级文件夹进行排序");
            }
        }

        if (itemId == null || req == null || itemType == null) {
            throw new LiteisleException("请求参数错误");
        }

        // 1. 消除代码重复：使用 Function 抽象化数据访问
        Function<Long, BigDecimal> orderFetcher;
        BiConsumer<Long, BigDecimal> orderUpdater;

        switch (itemType) {
            case "file" -> {
                orderFetcher = id -> {
                    Files file = filesService.getOne(new QueryWrapper<Files>()
                            .select("sorted_order")
                            .eq("id", id)
                            .eq("user_id", userId));
                    if (file == null) throw new LiteisleException("文件不存在或无权限：" + id);
                    return file.getSortedOrder();
                };
                orderUpdater = (id, order) -> {
                    boolean success = filesService.update(new UpdateWrapper<Files>()
                            .eq("id", id)
                            .eq("user_id", userId)
                            .set("sorted_order", order));
                    if (!success) throw new LiteisleException("更新文件排序失败");
                };
            }
            case "folder" -> {
                orderFetcher = id -> {
                    Folders folder = foldersService.getOne(new QueryWrapper<Folders>()
                            .select("sorted_order")
                            .eq("id", id)
                            .eq("user_id", userId));
                    if (folder == null) throw new LiteisleException("文件夹不存在或无权限：" + id);
                    return folder.getSortedOrder();
                };
                orderUpdater = (id, order) -> {
                    boolean success = foldersService.update(new UpdateWrapper<Folders>()
                            .eq("id", id)
                            .eq("user_id", userId)
                            .set("sorted_order", order));
                    if (!success) throw new LiteisleException("更新文件夹排序失败");
                };
            }
            default -> throw new LiteisleException("未知类型：" + itemType);
        }

        BigDecimal beforeOrder = (req.getBeforeId() == null) ? null : orderFetcher.apply(req.getBeforeId());
        BigDecimal afterOrder = (req.getAfterId() == null) ? null : orderFetcher.apply(req.getAfterId());

        // 2. 增加健壮性检查
        if (beforeOrder != null && afterOrder != null && beforeOrder.compareTo(afterOrder) >= 0) {
            // 如果前一个item的order大于等于后一个，说明数据可能已错乱或传入ID有误
            // 此时可以触发一次 re-indexing (重排序) 来修复数据
            throw new LiteisleException("排序位置无效，前后项目顺序错误");
            // 或者在这里调用 re-indexing 方法
        }

        // 3. 计算新的排序值
        BigDecimal newOrder;
        // 最小允许的排序间隔，对应于DECIMAL(30,10)的最小正数0.0000000001
        BigDecimal MIN_SORT_GAP = new BigDecimal("0.0000000001");


        // 情况一：在两个现有项之间插入
        if (beforeOrder != null && afterOrder != null) {
            BigDecimal actualGap = afterOrder.subtract(beforeOrder);

            // 核心：如果间隔不足，触发重排
            if (actualGap.compareTo(MIN_SORT_GAP) <= 0) {
                // 获取父文件夹ID以进行重排
                Long parentFolderId = getParentFolderId(itemId, itemType, userId);

                log.warn("排序精度耗尽或间隔过小 ({} <= {}), 对文件夹ID: {} 进行重新索引",
                        actualGap, MIN_SORT_GAP, parentFolderId);
                reindexCenter.reindexItemsInFolder(parentFolderId, userId);

                // 重排后，重新获取最新的排序值
                beforeOrder = orderFetcher.apply(req.getBeforeId());
                afterOrder = orderFetcher.apply(req.getAfterId());
            }

            // 无论是正常情况还是重排后，都使用微增法
            newOrder = beforeOrder.add(MIN_SORT_GAP);

            // 最终的健壮性检查，确保计算结果在区间内。
            if (newOrder.compareTo(afterOrder) >= 0) {
                log.error("即使在重排后，微增法计算的newOrder({})也超出了afterOrder({})的范围。" +
                        "系统可能存在严重的数据排序问题。", newOrder, afterOrder);
                throw new LiteisleException("排序计算失败，请联系管理员或重试。");
            }

            // 情况二：移动到列表最前面
        } else if (afterOrder != null) {
            newOrder = afterOrder.subtract(MIN_SORT_GAP);

            // 情况三：移动到列表最后面
        } else if (beforeOrder != null) {
            newOrder = beforeOrder.add(MIN_SORT_GAP);

            // 情况四：列表为空，插入第一个元素 (或创建新项)
        } else {
            newOrder = BigDecimal.valueOf(System.currentTimeMillis() * 100000L);
        }

        // 4. 更新目标项目的排序值
        orderUpdater.accept(itemId, newOrder);
    }

    /**
     * 辅助方法：获取给定项的父文件夹ID
     */
    private Long getParentFolderId(Long itemId, String itemType, Long userId) {
        if ("file".equals(itemType)) {
            Files file = filesService.getOne(new LambdaQueryWrapper<Files>()
                    .select(Files::getFolderId)
                    .eq(Files::getId, itemId)
                    .eq(Files::getUserId, userId));
            if (file == null) {
                throw new LiteisleException("文件不存在或无权限：" + itemId);
            }
            return file.getFolderId();
        } else { // itemType == "folder"
            Folders folder = foldersService.getOne(new LambdaQueryWrapper<Folders>()
                    .select(Folders::getParentId)
                    .eq(Folders::getId, itemId)
                    .eq(Folders::getUserId, userId));
            if (folder == null) {
                throw new LiteisleException("文件夹不存在或无权限：" + itemId);
            }
            return folder.getParentId();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void copyItems(ItemsOperationReq req) {
        // 强制一次只复制一种类型，避免二义性
        if (!req.getFileIds().isEmpty() && !req.getFolderIds().isEmpty()) {
            throw new LiteisleException("不支持同时复制文件和文件夹，请分开操作");
        }
        Long targetFolderId = req.getTargetFolderId();
        if (targetFolderId == null || targetFolderId <= 0) {
            throw new LiteisleException("请选择目标文件夹");
        }
        Long userId = UserContextHolder.getUserId();

        // 处理文件夹复制
        if (req.getFolderIds() != null && !req.getFolderIds().isEmpty()) {
            // 文件夹的规则：目标必须是一级目录
            Folders target = foldersService.getById(targetFolderId);
            if (target == null || target.getParentId() != 0) {
                throw new LiteisleException("文件夹只能复制到一级分类下");
            }
            for (Long sourceFolderId : req.getFolderIds()) {
                copySingleLayerFolderBatch(sourceFolderId, targetFolderId, userId);
            }
        }

        // 处理文件复制
        if (req.getFileIds() != null && !req.getFileIds().isEmpty()) {
            // 文件的规则：目标必须是二级目录
            Folders target = foldersService.getById(targetFolderId);
            if (target == null || target.getParentId() == 0) {
                // 这里需要考虑特例：如果目标是 "上传/分享" 文件夹，是允许的
                Folders rootParent = null; // 需要这个辅助方法
                if (target != null) {
                    rootParent = getRootParentFolder(target, userId);
                }
                boolean isFlexibleZone = rootParent != null &&
                        ("上传".equals(rootParent.getFolderName()) || "分享".equals(rootParent.getFolderName()));

                if (!isFlexibleZone) { // 如果不是上传/分享区，则必须是二级目录
                    throw new LiteisleException("文件必须复制到一个具体的歌单或文档分类中");
                }
            }
            copyFilesBatch(req.getFileIds(), targetFolderId, userId);
        }
    }

    private Folders getRootParentFolder(Folders folder, Long userId) {
        if (folder.getParentId() == 0) {
            return folder; // 它自己就是一级目录
        }
        // 循环向上查找，直到 parent_id == 0
        Long currentParentId = folder.getParentId();
        while (currentParentId != null && currentParentId != 0) {
            Folders parent = foldersService.getOne(new LambdaQueryWrapper<Folders>()
                    .eq(Folders::getId, currentParentId)
                    .eq(Folders::getUserId, userId));
            if (parent == null) return null;
            if (parent.getParentId() == 0) return parent;
            currentParentId = parent.getParentId();
        }
        return null;
    }

    /**
     * 优化：使用批量操作复制文件，并增加存储配额检查 (修正版)
     */
    private void copyFilesBatch(List<Long> fileIds, Long targetFolderId, Long userId) {
        // 1. 一次性查询所有原始文件
        List<Files> originalFiles = filesService.list(new LambdaQueryWrapper<Files>()
                .in(Files::getId, fileIds)
                .eq(Files::getUserId, userId));

        if (originalFiles.isEmpty()) return;

        // --- 【第一步：前置检查】---

        // 2. 计算需要增加的总文件大小
        List<Long> storageIds = originalFiles.stream()
                .map(Files::getStorageId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        long totalSizeToAdd = 0;
        if (!storageIds.isEmpty()) {
            List<Storages> storagesToCopy = storagesService.listByIds(storageIds);
            Map<Long, Long> storageIdToSizeMap = storagesToCopy.stream()
                    .collect(Collectors.toMap(Storages::getId, Storages::getFileSize));
            totalSizeToAdd = originalFiles.stream()
                    .mapToLong(file -> storageIdToSizeMap.getOrDefault(file.getStorageId(), 0L))
                    .sum();
        }

        // 3. 检查用户配额是否充足
        if (totalSizeToAdd > 0) {
            // 只查询一次数据库获取User对象
            Users user = usersService.getById(userId);
            if (user == null) {
                throw new LiteisleException("用户信息不存在");
            }
            // 假设字段名为 getStorageSize() 和 getStorageQuota()
            if (user.getStorageUsed() + totalSizeToAdd > user.getStorageQuota()) {
                throw new LiteisleException("存储空间不足，复制失败");
            }
        }

        // --- 【第二步：执行写入】---
        // 检查通过后，才开始真正地向数据库写入数据

        // 4. 在内存中准备好所有要插入的新文件对象
        List<Files> newFilesToSave = originalFiles.stream().map(original -> {
            Files copy = new Files();
            BeanUtils.copyProperties(original, copy, "id", "createTime", "updateTime");
            copy.setFolderId(targetFolderId);
            copy.setSortedOrder(new BigDecimal(System.currentTimeMillis()*100000));
            return copy;
        }).collect(Collectors.toList());

        // 5. 批量保存新文件记录
        filesService.saveBatch(newFilesToSave);

        // 6. 更新用户的已用存储空间
        if (totalSizeToAdd > 0) {
            usersService.update(new UpdateWrapper<Users>()
                    .eq("id", userId)
                    .setSql("storage_size = storage_size + " + totalSizeToAdd));
        }

        // 7. 批量更新文件实体的引用计数
        Map<Long, Long> storageIdCountMap = originalFiles.stream()
                .filter(f -> f.getStorageId() != null)
                .collect(Collectors.groupingBy(Files::getStorageId, Collectors.counting()));

        storageIdCountMap.forEach((storageId, count) -> storagesService.update(new UpdateWrapper<Storages>()
                .eq("id", storageId)
                .setSql("reference_count = reference_count + " + count)));
    }

    /**
     * 优化：重命名并使用批量操作复制单层文件夹
     */
    private void copySingleLayerFolderBatch(Long sourceFolderId, Long parentFolderId, Long userId) {
        // 1. 查询源文件夹，并校验是否是叶子节点
        Folders originalFolder = foldersService.getOne(new LambdaQueryWrapper<Folders>()
                .eq(Folders::getId, sourceFolderId).eq(Folders::getUserId, userId));
        if (originalFolder == null) return;

        long subFolderCount = foldersService.count(new LambdaQueryWrapper<Folders>()
                .eq(Folders::getParentId, sourceFolderId).eq(Folders::getUserId, userId));
        if (subFolderCount > 0) {
            throw new LiteisleException("只能复制不包含子文件夹的文件夹（禁止嵌套）");
        }

        // 2. 创建新文件夹
        Folders newFolder = new Folders();
        BeanUtils.copyProperties(originalFolder, newFolder, "id", "createTime", "updateTime");
        newFolder.setParentId(parentFolderId);
        newFolder.setSortedOrder(new BigDecimal(System.currentTimeMillis()*100000));
        foldersService.save(newFolder); // 保存新文件夹以获取ID

        // 3. 找出所有源文件ID
        List<Long> sourceFileIds = filesService.list(new LambdaQueryWrapper<Files>()
                        .select(Files::getId)
                        .eq(Files::getFolderId, sourceFolderId)
                        .eq(Files::getUserId, userId)
                        .isNull(Files::getDeleteTime))
                .stream().map(Files::getId).collect(Collectors.toList());

        if (!sourceFileIds.isEmpty()) {
            // 4. 复用批量复制文件的方法
            copyFilesBatch(sourceFileIds, newFolder.getId(), userId);
        }
    }

    private String getRelativePath(Long folderId, String fileName) {
        Folders currentFolder = foldersService.getById(folderId);
        String folderName = currentFolder.getFolderName();
        if (currentFolder.getParentId() == 0) {
            //最后一层递归 拼接最终的folderName字符串
            return folderName + "/" + fileName;
        }
        return getRelativePath(currentFolder.getParentId(), folderName + "/" + fileName);
    }

    // 辅助方法：检查 childFolderId 是否是 parentFolderId 的子孙文件夹
    private boolean isSubfolder(Long parentFolderId, Long childFolderId, Long userId) {
        Long currentParentId = childFolderId;
        while (currentParentId != null && currentParentId != 0) {
            Folders currentFolder = foldersService.getOne(new LambdaQueryWrapper<Folders>()
                    .select(Folders::getParentId)
                    .eq(Folders::getId, currentParentId)
                    .eq(Folders::getUserId, userId));

            if (currentFolder == null) return false;

            currentParentId = currentFolder.getParentId();
            if (Objects.equals(currentParentId, parentFolderId)) {
                return true;
            }
        }
        return false;
    }
}
