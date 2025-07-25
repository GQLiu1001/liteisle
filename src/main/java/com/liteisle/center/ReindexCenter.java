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

    /**
     * 对指定父文件夹下的子文件夹进行重新排序。
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @param sortOrderGenerator 统一的排序值生成器
     */
    private void reindexFolders(Long parentId, Long userId, AtomicLong sortOrderGenerator) {
        List<Folders> folders = foldersService.list(new LambdaQueryWrapper<Folders>()
                .eq(Folders::getParentId, parentId)
                .eq(Folders::getUserId, userId)
                .orderByAsc(Folders::getSortedOrder)); // 按现有顺序排序

        if (folders.isEmpty()) {
            return;
        }

        // 【核心修改】使用 getAndAdd 方法，每次增加一个巨大的步长，而不是+1
        folders.forEach(folder -> {
            // getAndAdd会原子性地返回当前值，并加上步长值
            folder.setSortedOrder(new BigDecimal(sortOrderGenerator.getAndAdd(REINDEX_STEP)));
        });

        foldersService.updateBatchById(folders);
    }

    /**
     * 对指定文件夹下的文件进行重新排序。
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @param sortOrderGenerator 统一的排序值生成器
     */
    private void reindexFiles(Long folderId, Long userId, AtomicLong sortOrderGenerator) {
        List<Files> files = filesService.list(new LambdaQueryWrapper<Files>()
                .eq(Files::getFolderId, folderId)
                .eq(Files::getUserId, userId)
                .orderByAsc(Files::getSortedOrder)); // 按现有顺序排序

        if (files.isEmpty()) {
            return;
        }

        // 【核心修改】同样使用 getAndAdd 方法，确保文件和文件夹都使用大步长策略
        files.forEach(file -> file.setSortedOrder(new BigDecimal(sortOrderGenerator.getAndAdd(REINDEX_STEP))));

        filesService.updateBatchById(files);
    }
}