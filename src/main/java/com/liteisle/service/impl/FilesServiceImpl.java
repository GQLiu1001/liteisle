package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.response.DocumentViewResp;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.config.VirtualThreadConfig;
import com.liteisle.service.FilesService;
import com.liteisle.mapper.FilesMapper;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
* @author 11965
* @description 针对表【files(文件通用基础信息表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class FilesServiceImpl extends ServiceImpl<FilesMapper, Files>
    implements FilesService{

    @Resource
    private ExecutorService virtualThreadPool;

    @Override
    public CompletableFuture<List<DocumentViewResp.DocumentFile>> getDocumentViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<Files> list;
            if (content == null){
                list = this.list(new LambdaQueryWrapper<Files>()
                        .eq(Files::getUserId, userId)
                        .isNull(Files::getDeleteTime) // 过滤已删除文件
                );
            }else {
                list = this.list(new LambdaQueryWrapper<Files>()
                        .eq(Files::getUserId, userId)
                        .like(Files::getFileName, content)
                        .isNull(Files::getDeleteTime) // 过滤已删除文件
                );
            }
            return list.stream()
                    .map(this::convertToDocumentFile)
                    .toList();
        }, virtualThreadPool);
    }

    private DocumentViewResp.DocumentFile convertToDocumentFile(Files item) {
        DocumentViewResp.DocumentFile docFile = new DocumentViewResp.DocumentFile();
        docFile.setId(item.getId());
        docFile.setFolderId(item.getFolderId());
        docFile.setFileName(item.getFileName());
        docFile.setFileType(item.getFileType());
        docFile.setSortedOrder(item.getSortedOrder());
        return docFile;
    }

}




