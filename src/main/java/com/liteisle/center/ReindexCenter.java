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

@Slf4j
@Component
public class ReindexCenter {

    @Resource
    private FilesService filesService;

    @Resource
    private FoldersService foldersService;

    /**
     * 对指定用户和文件夹下的所有项目（文件和文件夹）进行排序重建。
     * 当排序精度耗尽时，可以调用此方法来重新分配排序值。
     *
     * @param folderId 需要重建索引的文件夹ID
     * @param userId   用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void reindexItemsInFolder(Long folderId, Long userId) {
        log.warn("开始为文件夹ID: {} 和用户ID: {} 执行重新索引操作", folderId, userId);

        try {
            // 1. 对文件夹进行重新索引
            reindexFolders(folderId, userId);

            // 2. 对文件进行重新索引
            reindexFiles(folderId, userId);

            log.info("成功完成文件夹ID: {} 和用户ID: {} 的重新索引", folderId, userId);
        } catch (Exception e) {
            log.error("为文件夹ID: {} 和用户ID: {} 执行重新索引时失败", folderId, userId, e);
            // 抛出运行时异常以确保事务回滚
            throw new RuntimeException("重新索引操作失败", e);
        }
    }

    /**
     * 对指定父文件夹下的子文件夹进行重新排序。
     */
    private void reindexFolders(Long parentId, Long userId) {
        // 查询出所有需要重新排序的文件夹
        List<Folders> folders = foldersService.list(new LambdaQueryWrapper<Folders>()
                .eq(Folders::getParentId, parentId)
                .eq(Folders::getUserId, userId)
                .orderByAsc(Folders::getSortedOrder)); // 按现有顺序排序

        if (folders.isEmpty()) {
            return;
        }

        // 使用原子长整数确保即使在同一毫秒内也能生成唯一的排序值
        AtomicLong currentTime = new AtomicLong(System.currentTimeMillis() * 100000);

        // 为每个文件夹分配新的排序值
        folders.forEach(folder -> {
            folder.setSortedOrder(new BigDecimal(currentTime.getAndIncrement()));
        });

        // 批量更新
        foldersService.updateBatchById(folders);
    }

    /**
     * 对指定文件夹下的文件进行重新排序。
     */
    private void reindexFiles(Long folderId, Long userId) {
        // 查询出所有需要重新排序的文件
        List<Files> files = filesService.list(new LambdaQueryWrapper<Files>()
                .eq(Files::getFolderId, folderId)
                .eq(Files::getUserId, userId)
                .orderByAsc(Files::getSortedOrder)); // 按现有顺序排序

        if (files.isEmpty()) {
            return;
        }

        AtomicLong currentTime = new AtomicLong(System.currentTimeMillis() * 100000);

        // 为每个文件分配新的排序值
        files.forEach(file -> {
            file.setSortedOrder(new BigDecimal(currentTime.getAndIncrement()));
        });

        // 批量更新
        filesService.updateBatchById(files);
    }
}