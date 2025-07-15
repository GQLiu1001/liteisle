package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.constant.FolderConstant;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.response.DocumentViewResp;
import com.liteisle.common.enums.FolderTypeEnum;
import com.liteisle.service.FoldersService;
import com.liteisle.mapper.FoldersMapper;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
* @author 11965
* @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class FoldersServiceImpl extends ServiceImpl<FoldersMapper, Folders>
    implements FoldersService{

    @Resource
    private ExecutorService virtualThreadPool;

    @Resource
    private FoldersMapper foldersMapper;

    @Override
    public void createUserDefaultFolder(Long userId) {
        for (String folderName : FolderConstant.DEFAULT_SYSTEM_FOLDERS) {
            createSystemFolder(userId, folderName);
        }
    }

    @Override
    public CompletableFuture<List<DocumentViewResp.Notebook>> getDocumentViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<Folders> list;
            if (content != null && !content.isEmpty()) {
                // 有搜索关键词：执行复杂查询，计算符合条件的文件数
                list = baseMapper.selectFoldersWithFilteredFileCount(userId, content);
            } else {
                // 无搜索关键词：执行简单查询，直接返回总文件数
                list = baseMapper.selectFoldersWithTotalFileCount(userId);
            }
            return list.stream()
                    .map(this::convertToNotebook)
                    .toList();
        }, virtualThreadPool);
    }

    private DocumentViewResp.Notebook convertToNotebook(Folders item) {
        DocumentViewResp.Notebook notebook = new DocumentViewResp.Notebook();
        notebook.setId(item.getId());
        notebook.setFolderName(item.getFolderName());
        notebook.setFolderType(item.getFolderType());
        notebook.setSortedOrder(item.getSortedOrder());
        notebook.setDocumentCount(item.getDocumentCount());
        return notebook;
    }

    public void createSystemFolder(Long userId, String folderName) {
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
}




