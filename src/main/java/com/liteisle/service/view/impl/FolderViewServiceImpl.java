package com.liteisle.service.view.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.dto.response.FolderContentResp;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.FoldersService;
import com.liteisle.service.view.FolderViewService;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class FolderViewServiceImpl implements FolderViewService {
    @Resource
    private FoldersService foldersService;
    @Resource
    private FilesService filesService;
    @Resource
    private ExecutorService virtualThreadPool;

    // 定义合法的 sortBy 值
    private static final List<String> VALID_SORT_BY_FIELDS = Arrays.asList(
            "name", "file_size", "create_time", "update_time", "sorted_order"
    );
    // 定义合法的 sortOrder 值
    private static final List<String> VALID_SORT_ORDERS = Arrays.asList(
            "ASC", "DESC"
    );

    @Override
    public FolderContentResp getFolderContent(Long folderId, String sortBy, String sortOrder, String content) {
        // 参数校验：提前进行，如果参数无效，直接抛出异常，不进行后续的数据库操作
        checkAvailability(sortBy, sortOrder);

        //获取的是当前folderId底下的数据
        Long userId = UserContextHolder.getUserId();
        // 1. 将所有独立的IO操作全部启动为异步任务
        CompletableFuture<List<FolderContentResp.FolderItem>> folderFuture = getFolderFuture(folderId, sortBy, userId, sortOrder,content);
        CompletableFuture<List<FolderContentResp.BreadcrumbItem>> breadcrumbFuture = getBreadcrumbFuture(folderId, userId);

        // 只有在非根目录时，才需要查询文件列表
        CompletableFuture<List<FolderContentResp.FileItem>> fileFuture = (folderId != 0)
                ? getFileFuture(folderId, sortBy, userId, sortOrder,content)
                : CompletableFuture.completedFuture(Collections.emptyList());

        // 2. 使用 allOf 组合所有异步任务
        return CompletableFuture.allOf(folderFuture, fileFuture, breadcrumbFuture)
                .thenApply(v -> {
                    try {
                        return new FolderContentResp(
                                breadcrumbFuture.get(),
                                folderFuture.get(),
                                fileFuture.get()
                        );
                    } catch (Exception e) {
                        // 在实际的 get() 中，异常会被包装成 ExecutionException
                        log.error("Error getting future results", e);
                        throw new LiteisleException("获取文件夹内容失败: " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("获取文件夹内容时发生异步异常", ex);
                    // 提供一个有意义的默认空值
                    return new FolderContentResp(
                            List.of(new FolderContentResp.BreadcrumbItem(0L, "云盘")),
                            Collections.emptyList(),
                            Collections.emptyList()
                    );
                })
                .join(); // 阻塞并等待最终的组合结果
    }

    /**
     * 校验 sortBy 和 sortOrder 参数的合法性。
     * 如果参数无效，将抛出 LiteisleException。
     *
     * @param sortBy    排序字段
     * @param sortOrder 排序顺序
     */
    private void checkAvailability(String sortBy, String sortOrder) {
        // 1. 校验 sortBy
        // 如果 sortBy 为 null 或空，则使用默认排序 (sorted_order)，被认为是合法的
        if (sortBy != null && !sortBy.isEmpty() && !VALID_SORT_BY_FIELDS.contains(sortBy)) {
            throw new LiteisleException("无效的排序字段: " + sortBy);
        }

        // 2. 校验 sortOrder
        // 如果 sortOrder 为 null 或空，则使用默认排序 (DESC)，被认为是合法的
        if (sortOrder != null && !sortOrder.isEmpty() && !VALID_SORT_ORDERS.contains(sortOrder.toUpperCase())) {
            throw new LiteisleException("无效的排序顺序: " + sortOrder + ". 只能是 ASC 或 DESC。");
        }

        // 不需要返回 boolean，如果校验不通过直接抛异常
    }


    /**
     * 【重构】将 getBreadcrumb 包装成异步方法
     */
    private CompletableFuture<List<FolderContentResp.BreadcrumbItem>> getBreadcrumbFuture(Long folderId, Long userId) {
        // 使用 supplyAsync 将耗时的数据库操作提交到线程池
        return CompletableFuture.supplyAsync(() -> getBreadcrumbSync(folderId, userId), virtualThreadPool);
    }

    private List<FolderContentResp.BreadcrumbItem> getBreadcrumbSync(Long folderId, Long userId) {
        List<FolderContentResp.BreadcrumbItem> breadcrumb = new ArrayList<>();
        breadcrumb.add(new FolderContentResp.BreadcrumbItem(0L, "云盘"));
        if (folderId == 0) {
            //根目录 不显示其他
            return breadcrumb;
        } else {
            //不为根目录 两个情况，
            //首先获得当前文件夹信息
            Folders currentFolder = foldersService.getOne(new QueryWrapper<Folders>()
                    .eq("id", folderId).eq("user_id", userId));
            if (currentFolder == null) {
                return breadcrumb;
            }
            Long parentId = currentFolder.getParentId();
            Long id = currentFolder.getId();
            String folderName = currentFolder.getFolderName();
            if (parentId != 0) {
                //说明父级不是根目录 是 云盘/歌单/我喜欢的
                Folders parentFolder = foldersService.getOne(new QueryWrapper<Folders>()
                        .eq("id", parentId).eq("user_id", userId));
                // 【安全锁 2】确保父级存在才添加，避免崩溃
                if (parentFolder != null) {
                    breadcrumb.add(new FolderContentResp.BreadcrumbItem(parentFolder.getId(), parentFolder.getFolderName()));
                }
                breadcrumb.add(new FolderContentResp.BreadcrumbItem(id, folderName));
                return breadcrumb;
            } else {
                //说明父级是根目录 是 云盘/歌单
                breadcrumb.add(new FolderContentResp.BreadcrumbItem(id, folderName));
                return breadcrumb;
            }
        }
    }


    private CompletableFuture<List<FolderContentResp.FolderItem>> getFolderFuture(
            Long folderId, String sortBy, Long userId, String sortOrder, String content) {
        return foldersService.getFolderContentWithSort(folderId, sortBy, userId, sortOrder,content);
    }

    private CompletableFuture<List<FolderContentResp.FileItem>> getFileFuture(
            Long folderId, String sortBy, Long userId, String sortOrder, String  content) {
        return filesService.getFolderContentWithSort(folderId, sortBy, userId, sortOrder,content);
    }

}
