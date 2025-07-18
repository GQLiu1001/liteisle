package com.liteisle.service.view.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.Storages;
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
import com.liteisle.service.view.ItemViewService;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ItemViewServiceImpl implements ItemViewService {
    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;
    @Resource
    private StoragesService storagesService;


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
        //TODO redis简化 不能移动系统根目录文件夹 代码review
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
        int scale = 10; // 数据库小数位数
        BigDecimal half = new BigDecimal("0.5");

        if (beforeOrder != null && afterOrder != null) {
            // 移动到中间
            BigDecimal mid = beforeOrder.add(afterOrder).multiply(half);
            newOrder = mid.setScale(scale, RoundingMode.HALF_UP);

            // 4. 核心：检查精度是否耗尽
            if (newOrder.compareTo(beforeOrder) == 0 || newOrder.compareTo(afterOrder) == 0) {
                // 精度耗尽，需要重新分配排序值（Re-indexing）
                // 此处简化处理，抛出异常提示前端重试或提示系统繁忙。
                // 生产环境应触发一个 re-indexing 逻辑。
                //TODO reindex操作
                throw new LiteisleException("排序空间不足，请稍后重试");
            }
        } else if (afterOrder != null) {
            // 移动到最前
            newOrder = afterOrder.subtract(BigDecimal.valueOf(1000)); // 减去一个较大的固定值，而不是1，以留出更多空间
        } else if (beforeOrder != null) {
            // 移动到最后
            newOrder = beforeOrder.add(BigDecimal.valueOf(1000)); // 加上一个较大的固定值
        } else {
            // 理论上不应该发生，因为列表至少有一个元素（被移动的那个）
            // 如果是创建新元素，可以给一个默认值
            newOrder = BigDecimal.valueOf(System.currentTimeMillis());
        }

        // 5. 更新目标项目的排序值
        orderUpdater.accept(itemId, newOrder);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void copyItems(ItemsOperationReq req) {
        //TODO 增加用户额度

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
            Folders parent = foldersService.getById(currentParentId);
            if (parent == null) return null;
            if (parent.getParentId() == 0) return parent;
            currentParentId = parent.getParentId();
        }
        return null;
    }

    /**
     * 优化：使用批量操作复制文件
     */
    private void copyFilesBatch(List<Long> fileIds, Long targetFolderId, Long userId) {
        // 1. 一次性查询所有原始文件
        List<Files> originalFiles = filesService.list(new LambdaQueryWrapper<Files>()
                .in(Files::getId, fileIds)
                .eq(Files::getUserId, userId));

        if (originalFiles.isEmpty()) return;

        // 2. 在内存中准备好所有要插入的新文件对象
        List<Files> newFilesToSave = originalFiles.stream().map(original -> {
            Files copy = new Files();
            // 复制属性，但不复制主键和时间戳
            BeanUtils.copyProperties(original, copy, "id", "createTime", "updateTime");
            copy.setFolderId(targetFolderId);
            // 你可能需要为复制的文件设置一个新的 sorted_order
            copy.setSortedOrder(new BigDecimal(System.currentTimeMillis()));
            return copy;
        }).collect(Collectors.toList());

        // 3. 一次性批量保存所有新文件
        filesService.saveBatch(newFilesToSave);

        // 4. 批量更新引用计数
        Map<Long, Long> storageIdCountMap = originalFiles.stream()
                .filter(f -> f.getStorageId() != null)
                .collect(Collectors.groupingBy(Files::getStorageId, Collectors.counting()));

        storageIdCountMap.forEach((storageId, count) -> {
            storagesService.update(new UpdateWrapper<Storages>()
                    .eq("id", storageId)
                    .setSql("reference_count = reference_count + " + count));
        });
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
        newFolder.setSortedOrder(new BigDecimal(System.currentTimeMillis()));
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
