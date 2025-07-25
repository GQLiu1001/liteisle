package com.liteisle.center;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.Users;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.FoldersService;
import com.liteisle.service.core.StoragesService;
import com.liteisle.service.core.UsersService;
import com.liteisle.util.MinioUtil; // <-- 【新增】导入MinioUtil
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.liteisle.common.constant.SystemConstant.*;

/**
 * 后台定时任务中心
 */
@Slf4j
@Service
@EnableScheduling
public class TaskCenter {

    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;
    @Resource
    private StoragesService storagesService;
    @Resource
    private UsersService usersService;
    @Resource
    private MinioUtil minioUtil;

    /**
     * [新增任务] 定期清理 failed 状态文件，释放用户存储额度。
     * 策略：每天凌晨3点执行。
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupFailedFiles() {
        log.info("【定时任务】开始执行：清理失败状态的文件...");
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(FAILED_FILE_RETENTION_HOURS);

        // 1. 分批查找超过24小时的、状态为"failed"的文件
        // 假设您的Files实体有status字段，并且您有一个FileStatus枚举，其中包含FAILED状态
        List<Files> failedFiles = filesService.list(new LambdaQueryWrapper<Files>()
                .eq(Files::getFileStatus, FileStatusEnum.FAILED)
                .lt(Files::getCreateTime, expirationTime) // 按创建时间判断
                .last("LIMIT " + BATCH_SIZE));

        if (CollectionUtils.isEmpty(failedFiles)) {
            log.info("【定时任务】执行完毕：没有找到需要清理的失败文件。");
            return;
        }

        log.info("【定时任务】发现 {} 个失败文件待处理...", failedFiles.size());

        // 2.3.4.合并修改用户额度
        restoreUserStorageQuota(failedFiles);


        // 5. 永久删除文件表和存储表中的记录
        List<Long> fileIdsToDelete = failedFiles.stream().map(Files::getId).collect(Collectors.toList());
        if (!fileIdsToDelete.isEmpty()) {
            filesService.removeByIds(fileIdsToDelete);
            log.info("【定时任务】成功从数据库中永久删除了 {} 个失败的Files记录。", fileIdsToDelete.size());
        }

        log.info("【定时任务】本轮失败文件清理执行完毕。");
    }

    /**
     * [核心任务] 定时清理回收站中已过期的逻辑文件和文件夹。
     * 策略：每天凌晨2点执行。
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredRecycleBinItems() {
        log.info("【定时任务】开始执行：清理过期回收站项目...");
        LocalDateTime expirationTime = LocalDateTime.now().minusDays(RECYCLE_BIN_RETENTION_DAYS);

        // 1. 清理过期的文件夹 (注意：这里直接物理删除)
        boolean deletedFolders = foldersService.remove(new LambdaQueryWrapper<Folders>()
                .isNotNull(Folders::getDeleteTime)
                .lt(Folders::getDeleteTime, expirationTime));
        if (deletedFolders) {
            log.info("【定时任务】已清理过期文件夹。");
        }

        // 2. 分批处理过期的文件
        List<Files> expiredFiles = filesService.list(new LambdaQueryWrapper<Files>()
                .isNotNull(Files::getDeleteTime)
                .lt(Files::getDeleteTime, expirationTime)
                .last("LIMIT " + BATCH_SIZE));

        if (CollectionUtils.isEmpty(expiredFiles)) {
            log.info("【定时任务】执行完毕：没有找到需要清理的过期文件。");
            return;
        }

        log.info("【定时任务】发现 {} 个过期文件待处理...", expiredFiles.size());

        // 3.4.5. 合并修改用户额度
        restoreUserStorageQuota(expiredFiles);

        // 6. 减少物理文件的引用计数
        Map<Long, Long> refCountToDecrement = expiredFiles.stream()
                .filter(f -> f.getStorageId() != null)
                .collect(Collectors.groupingBy(Files::getStorageId, Collectors.counting()));

        for (Map.Entry<Long, Long> entry : refCountToDecrement.entrySet()) {
            storagesService.update(new UpdateWrapper<Storages>()
                    .eq("id", entry.getKey())
                    .setSql("reference_count = GREATEST(0, reference_count - " + entry.getValue() + ")"));
        }
        log.info("【定时任务】更新了 {} 个物理存储实体的引用计数。", refCountToDecrement.size());

        // 7. 永久删除文件表中的记录
        List<Long> fileIdsToDelete = expiredFiles.stream().map(Files::getId).collect(Collectors.toList());
        filesService.removeByIds(fileIdsToDelete);

        log.info("【定时任务】成功从数据库中永久删除了 {} 个文件记录。", fileIdsToDelete.size());
        log.info("【定时任务】本轮清理执行完毕。");
    }

    /**
     * [辅助任务] 定时清理物理上已无引用的文件。
     * 策略：每天凌晨4点执行，晚于回收站清理任务。
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOrphanedStorages() {
        log.info("【定时任务】开始执行：清理孤立的物理存储文件...");

        List<Storages> orphanStorages = storagesService.list(new LambdaQueryWrapper<Storages>()
                .le(Storages::getReferenceCount, 0)
                .last("LIMIT " + BATCH_SIZE));

        if (CollectionUtils.isEmpty(orphanStorages)) {
            log.info("【定时任务】执行完毕：没有找到需要清理的孤立物理文件。");
            return;
        }

        for (Storages storage : orphanStorages) {
            try {
                // 调用MinioUtil删除对应的物理文件
                minioUtil.removeFile(storage.getStoragePath());
                log.info("【物理删除】成功删除云端文件：{}", storage.getStoragePath());

                // 云端物理文件删除成功后，再删除数据库记录
                storagesService.removeById(storage.getId());

            } catch (Exception e) {
                // 如果云端删除失败，记录错误日志，本次事务会回滚，数据库记录不会被删除，等待下次重试
                log.error("【物理删除】删除云端文件 {} 失败！错误：{}", storage.getStoragePath(), e.getMessage());
                // 可选择抛出异常来触发整个批次的回滚
                throw new LiteisleException(e.getMessage());
            }
        }
        log.info("【定时任务】成功清理了 {} 个孤立的物理文件记录。", orphanStorages.size());
    }


    /**
     * 根据文件列表恢复用户已使用的存储空间
     */
    private void restoreUserStorageQuota(List<Files> files) {
        if (CollectionUtils.isEmpty(files)) return;

        Map<Long, List<Files>> filesByUser = files.stream()
                .filter(f -> f.getUserId() != null && f.getStorageId() != null)
                .collect(Collectors.groupingBy(Files::getUserId));

        List<Long> storageIds = files.stream()
                .map(Files::getStorageId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Long> storageIdToSizeMap = storagesService.listByIds(storageIds).stream()
                .collect(Collectors.toMap(Storages::getId, Storages::getFileSize));

        for (Map.Entry<Long, List<Files>> entry : filesByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Files> userFiles = entry.getValue();
            long quotaToRestore = userFiles.stream()
                    .mapToLong(file -> storageIdToSizeMap.getOrDefault(file.getStorageId(), 0L))
                    .sum();

            if (quotaToRestore > 0) {
                usersService.update(new UpdateWrapper<Users>()
                        .eq("id", userId)
                        .setSql("storage_used = GREATEST(0, storage_used - " + quotaToRestore + ")"));
                log.info("【定时任务】为用户ID {} 恢复了 {} 字节的存储空间。", userId, quotaToRestore);
            }
        }
    }

}