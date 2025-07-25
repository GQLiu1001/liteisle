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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.liteisle.common.constant.RedisConstant.FILE_HASH_LOCK_PREFIX;
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

    @Resource
    private RedissonClient redissonClient; // 【修改】注入 RedissonClient

    // 【关键修改 #1】注入自身的代理对象，使用 @Lazy 防止循环依赖
    @Resource
    @Lazy
    private TaskCenter self;


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
     * 【最终正确版】
     */
    @Scheduled(cron = "0 0 4 * * *")
    // 【关键修改 #2】移除这里的 @Transactional 注解
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
            if (!StringUtils.hasText(storage.getFileHash())) {
                log.warn("发现缺少 file_hash 的 storage 记录，跳过处理。ID: {}", storage.getId());
                continue;
            }

            String lockKey = FILE_HASH_LOCK_PREFIX + storage.getFileHash();
            RLock lock = redissonClient.getLock(lockKey);

            try {
                boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (isLocked) {
                    try {
                        Storages currentStorage = storagesService.getById(storage.getId());
                        if (currentStorage != null && currentStorage.getReferenceCount() <= 0) {
                            log.info("【Redisson锁成功】准备物理删除: {}", currentStorage.getStoragePath());
                            // 【关键修改 #3】使用 self (代理对象) 调用事务方法
                            self.cleanupStorageInTransaction(currentStorage);
                        }
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.error("获取 Redisson 锁时被中断", e);
                Thread.currentThread().interrupt();
            }
        }
        log.info("【定时任务】本轮孤立文件清理检查完毕。");
    }

    /**
     * 【关键修改 #4】此方法现在是每个文件独立的事务单元。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupStorageInTransaction(Storages storage) {
        try {
            minioUtil.removeFile(storage.getStoragePath());
            log.info("【物理删除】成功删除云端文件：{}", storage.getStoragePath());
            storagesService.removeById(storage.getId());
        } catch (Exception e) {
            log.error("【物理删除】删除云端文件 {} 失败！错误：{}", storage.getStoragePath(), e.getMessage());
            // 抛出异常以触发本方法的事务回滚
            throw new LiteisleException(e.getMessage());
        }
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