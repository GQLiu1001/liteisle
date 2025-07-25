package com.liteisle.center;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.FoldersService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.liteisle.common.constant.SystemConstant.REINDEX_STEP;

@Slf4j
@Component
public class ReindexCenter {

    @Resource
    private FilesService filesService;

    @Resource
    private FoldersService foldersService;

    @Transactional(rollbackFor = Exception.class)
    public void reindexItemsInFolder(Long folderId, Long userId) {
        log.warn("开始为文件夹ID: {} 和用户ID: {} 执行重新索引操作", folderId, userId);

        try {
            // 基准值依然是当前时间戳，作为排序的“天花板”
            long baseSortOrder = System.currentTimeMillis() * 100000;
            AtomicLong sortOrderGenerator = new AtomicLong(baseSortOrder);

            // 1. 对文件夹进行重新索引
            reindexFolders(folderId, userId, sortOrderGenerator);

            // 2. 对文件进行重新索引
            reindexFiles(folderId, userId, sortOrderGenerator);

            log.info("成功完成文件夹ID: {} 和用户ID: {} 的重新索引", folderId, userId);
        } catch (Exception e) {
            log.error("为文件夹ID: {} 和用户ID: {} 执行重新索引时失败", folderId, userId, e);
            throw new RuntimeException("重新索引操作失败", e);
        }
    }

    private void reindexFolders(Long parentId, Long userId, AtomicLong sortOrderGenerator) {
        // 【核心修改 1】查询顺序必须与最终展示顺序一致，改为 DESC
        List<Folders> folders = foldersService.list(new LambdaQueryWrapper<Folders>()
                .eq(Folders::getParentId, parentId)
                .eq(Folders::getUserId, userId)
                .orderByDesc(Folders::getSortedOrder)); // 按现有顺序降序排序

        if (folders.isEmpty()) {
            return;
        }

        // 【核心修改 2】要生成递减序列，步长应为负数
        folders.forEach(folder -> folder.setSortedOrder(new BigDecimal(sortOrderGenerator.getAndAdd(-REINDEX_STEP))));

        foldersService.updateBatchById(folders);
    }

    private void reindexFiles(Long folderId, Long userId, AtomicLong sortOrderGenerator) {
        // 【核心修改 1】查询顺序必须与最终展示顺序一致，改为 DESC
        List<Files> files = filesService.list(new LambdaQueryWrapper<Files>()
                .eq(Files::getFolderId, folderId)
                .eq(Files::getUserId, userId)
                .orderByDesc(Files::getSortedOrder)); // 按现有顺序降序排序

        if (files.isEmpty()) {
            return;
        }

        // 【核心修改 2】要生成递减序列，步长应为负数
        files.forEach(file -> file.setSortedOrder(new BigDecimal(sortOrderGenerator.getAndAdd(-REINDEX_STEP))));

        filesService.updateBatchById(files);
    }
}