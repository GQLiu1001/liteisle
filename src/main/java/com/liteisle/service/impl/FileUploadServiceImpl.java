package com.liteisle.service.impl;

import com.liteisle.common.domain.response.FileUploadAsyncResp;
import com.liteisle.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    @Override
    public String uploadMdImage(MultipartFile file, Long fileId) {
        return "";
    }

    @Override
    public FileUploadAsyncResp uploadFile(MultipartFile file, Long folderId) {
        return null;
    }
}
