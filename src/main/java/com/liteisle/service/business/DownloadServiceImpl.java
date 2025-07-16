package com.liteisle.service.business;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liteisle.common.domain.Files;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.domain.Storages;
import com.liteisle.common.domain.TransferLog;
import com.liteisle.common.domain.request.ItemsSelectionReq;
import com.liteisle.common.domain.response.DownloadSessionResp;
import com.liteisle.common.enums.TransferStatusEnum;
import com.liteisle.common.enums.TransferTypeEnum;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.FilesService;
import com.liteisle.service.FoldersService;
import com.liteisle.service.StoragesService;
import com.liteisle.service.TransferLogService;
import com.liteisle.util.MinioUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DownloadServiceImpl implements DownloadService {
    @Resource
    private FilesService filesService;
    @Resource
    private StoragesService storagesService;
    @Resource
    private TransferLogService transferLogService;
    @Resource
    private HttpServletRequest httpServletRequest;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private FoldersService foldersService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DownloadSessionResp registerDownload(ItemsSelectionReq req) {
        Long userId = UserContextHolder.getUserId();
        Long totalSize = 0L;
        Long totalFiles = 0L;
        DownloadSessionResp.FolderD folderD = new DownloadSessionResp.FolderD();
        List<DownloadSessionResp.FilesD> filesD = new ArrayList<>();
        // 验证请求有效性
        if ((req.getFolderId() == null || req.getFolderId() <= 0) &&
                (req.getFileIds() == null || req.getFileIds().isEmpty())) {
            throw new LiteisleException("请选择要下载的文件或文件夹");
        }
        //进行分流
        //文件(单个或多个)下载
        if (req.getFolderId() == null) {
            for (Long fileId : req.getFileIds()) {
                Files file = filesService.getOne(new QueryWrapper<Files>().eq("id", fileId));
                DownloadSessionResp.FilesD fileD = buildFileDownloadInfo(file, userId);
                filesD.add(fileD);
                totalFiles++;
                totalSize += fileD.getSize(); // 从封装对象中取
            }
        }
        //文件夹下载 文件为空（下载一个文件夹）
        if (req.getFileIds().isEmpty()) {
            //获取文件夹信息 填充 folderD
            Folders folder = foldersService.getById(req.getFolderId());
            folderD.setFolderId(folder.getId());
            folderD.setFolderName(folder.getFolderName());
            folderD.setRelativePath(getRelativePath(folder.getId(), folder.getFolderName()));
            //填充 FilesD
            List<Files> list = filesService.list(new QueryWrapper<Files>().eq("folder_id", req.getFolderId()));
            for (Files file : list) {
                DownloadSessionResp.FilesD fileD = buildFileDownloadInfo(file, userId);
                filesD.add(fileD);
                totalFiles++;
                totalSize += fileD.getSize(); // 从封装对象中取
            }
        }
        return new DownloadSessionResp(
                totalSize,
                totalFiles,
                folderD,
                filesD
        );
    }


    private TransferLog getTransferLog(Long fileId, Long userId, Storages optById) {
        TransferLog transferLog = new TransferLog();
        transferLog.setUserId(userId);
        transferLog.setTransferType(TransferTypeEnum.DOWNLOAD);
        transferLog.setLogStatus(TransferStatusEnum.PROCESSING);
        transferLog.setFileId(fileId);
        transferLog.setItemName(optById.getFileHash());
        transferLog.setItemSize(optById.getFileSize());
        transferLog.setClientIp(httpServletRequest.getRemoteAddr());
        transferLog.setCreateTime(new Date());
        transferLog.setUpdateTime(new Date());
        return transferLog;
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

    private DownloadSessionResp.FilesD buildFileDownloadInfo(Files file, Long userId) {
        Storages storage = storagesService.getById(file.getStorageId());
        if (storage == null) {
            throw new LiteisleException("获取文件存储信息失败");
        }
        TransferLog log = getTransferLog(file.getId(), userId, storage);
        if (!transferLogService.save(log)) {
            throw new LiteisleException("创建下载日志失败");
        }
        String relativePath = getRelativePath(file.getFolderId(), file.getFileName());
        try {
            return new DownloadSessionResp.FilesD(
                    log.getId(),
                    file.getId(),
                    file.getFileName(),
                    relativePath,
                    storage.getFileSize(),
                    minioUtil.getPresignedObjectUrl(storage.getStoragePath(), 1, TimeUnit.DAYS)
            );
        } catch (Exception e) {
            throw new LiteisleException("生成下载链接失败: " + e.getMessage());
        }
    }


}
