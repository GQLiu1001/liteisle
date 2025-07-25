package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.DocumentMetadata;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.dto.request.MarkdownCreateReq;
import com.liteisle.common.dto.request.MarkdownUpdateReq;
import com.liteisle.common.dto.response.*;
import com.liteisle.common.enums.FileStatusEnum;
import com.liteisle.common.enums.FileTypeEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.core.DocumentMetadataService;
import com.liteisle.service.core.FilesService;
import com.liteisle.mapper.FilesMapper;
import com.liteisle.service.core.StoragesService;
import com.liteisle.util.MinioUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author 11965
 * @description 针对表【files(文件通用基础信息表)】的数据库操作Service实现
 * @createDate 2025-07-10 20:09:48
 */
@Service
public class FilesServiceImpl extends ServiceImpl<FilesMapper, Files>
        implements FilesService {

    @Resource
    private ExecutorService virtualThreadPool;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private StoragesService storagesService;
    @Resource
    private DocumentMetadataService documentMetadataService;
    @Resource
    private FilesMapper filesMapper;

    @Override
    public CompletableFuture<List<DocumentViewResp.DocumentFile>> getDocumentViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<Files> list;
            if (content == null) {
                list = this.list(new LambdaQueryWrapper<Files>()
                        .eq(Files::getUserId, userId)
                        .eq(Files::getFileType, FileTypeEnum.DOCUMENT)
                        .eq(Files::getFileStatus, FileStatusEnum.AVAILABLE)
                        .isNull(Files::getDeleteTime) // 过滤已删除文件
                );
            } else {
                list = this.list(new LambdaQueryWrapper<Files>()
                        .eq(Files::getUserId, userId)
                        .eq(Files::getFileStatus, FileStatusEnum.AVAILABLE)
                        .like(Files::getFileName, content)
                        .isNull(Files::getDeleteTime) // 过滤已删除文件
                );
            }
            return list.stream()
                    .map(this::convertToDocumentFile)
                    .toList();
        }, virtualThreadPool);
    }

    @Override
    public String getDocumentViewUrl(Long fileId) {
        Long userId = UserContextHolder.getUserId();
        Files file = this.getOne(new LambdaQueryWrapper<Files>()
                .eq(Files::getId, fileId)
                .eq(Files::getUserId, userId)
                .isNull(Files::getDeleteTime)); // 过滤已删除文件
        if (file == null) {
            throw new LiteisleException("文件不存在");
        }
        Long storageId = file.getStorageId();
        Storages storage = storagesService.getOne(new QueryWrapper<Storages>().eq("id", storageId));
        if (storage == null) {
            throw new LiteisleException("文件存储信息不存在");
        }
        try {
            return minioUtil.getPresignedObjectUrl(storage.getStoragePath(), 1, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new LiteisleException(e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createMarkdown(MarkdownCreateReq req) {
        Long userId = UserContextHolder.getUserId();
        Files file = new Files();
        file.setUserId(userId);
        file.setFolderId(req.getFolderId());
        file.setStorageId(null);
        file.setFileName(req.getName());
        file.setFileExtension("md");
        file.setFileType(FileTypeEnum.DOCUMENT);
        file.setFileStatus(FileStatusEnum.AVAILABLE);
        file.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()*100000));
        file.setCreateTime(new Date());
        file.setUpdateTime(new Date());
        boolean saveFile = this.save(file);
        if (!saveFile) {
            throw new LiteisleException("创建文件失败");
        }
        DocumentMetadata documentMetadata = new DocumentMetadata();
        documentMetadata.setFileId(file.getId());
        documentMetadata.setContent("");
        documentMetadata.setVersion(0L);
        boolean saveMeta = documentMetadataService.save(documentMetadata);
        if (!saveMeta) {
            throw new LiteisleException("创建文件元数据失败");
        }
        return file.getId();
    }

    @Override
    public MarkdownContentResp getMarkdownContent(Long fileId) {
        DocumentMetadata documentMetadata = documentMetadataService.getOne(new QueryWrapper<DocumentMetadata>().eq("file_id", fileId));
        if (documentMetadata == null) {
            throw new LiteisleException("文件元数据不存在");
        }
        return new MarkdownContentResp(documentMetadata.getContent(), documentMetadata.getVersion());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateMarkdown(Long fileId, MarkdownUpdateReq req) {

        Long userId = UserContextHolder.getUserId();
        // 检查文件是否该用户名下存在
        Files one = this.getOne(new QueryWrapper<Files>().eq("id", fileId).eq("user_id", userId));
        if (one == null) {
            throw new LiteisleException("文件不存在");
        }
        DocumentMetadata documentMetadata = documentMetadataService
                .getOne(new QueryWrapper<DocumentMetadata>().eq("file_id", fileId));
        if (documentMetadata == null) {
            throw new LiteisleException("文件元数据不存在");
        }
        documentMetadata.setContent(req.getContent());
        //判断version字段
        if (!Objects.equals(documentMetadata.getVersion(), req.getVersion())) {
            throw new LiteisleException("文件修改失败");
        }
        boolean update = documentMetadataService.updateById(documentMetadata);
        if (!update) {
            throw new LiteisleException("更新文件元数据失败");
        }
    }

    @Override
    public CompletableFuture<List<MusicViewResp.MusicFile>> getMusicViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return CompletableFuture.supplyAsync(() -> filesMapper.getMusicViewWithContent(content, userId), virtualThreadPool);
    }

    @Override
    public String getMusicPlayUrl(Long fileId) {
        Long userId = UserContextHolder.getUserId();
        Files file = this.getOne(new QueryWrapper<Files>().eq("id", fileId).eq("user_id", userId));
        if (file == null) {
            throw new LiteisleException("文件不存在");
        }
        Storages storages = storagesService.getById(file.getStorageId());
        if (storages == null) {
            throw new LiteisleException("文件存储信息不存在");
        }
        try {
            return minioUtil.getPresignedObjectUrl(storages.getStoragePath(), 1, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new LiteisleException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<List<RecycleBinContentResp.FileItem>> getRecycleBinViewWithContent(String content) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return CompletableFuture.supplyAsync(() -> filesMapper.getRecycleBinViewWithContent(content, userId), virtualThreadPool);
    }

    @Override
    public CompletableFuture<List<FolderContentResp.FileItem>> getFolderContentWithSort(
            Long folderId, String sortBy, Long userId,String sortOrder,String content) {
        if (userId == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return CompletableFuture.supplyAsync(() -> filesMapper.getFolderContentWithSort(
                folderId,sortBy, userId,sortOrder,content), virtualThreadPool);
    }

    @Override
    public Long getFileTotalSizeFromFolderId(Long folderId , Long userId) {
        return filesMapper.getTotalSizeByFolderId(folderId,userId);
    }

    @Override
    public Long getMdVersion(Long fileId) {
        DocumentMetadata one = documentMetadataService.getOne(new LambdaQueryWrapper<DocumentMetadata>()
                .eq(DocumentMetadata::getFileId, fileId)
                .select(DocumentMetadata::getVersion));
        if (one == null){
            throw new LiteisleException("获取MD文档版本号失败");
        }
        return one.getVersion();
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




