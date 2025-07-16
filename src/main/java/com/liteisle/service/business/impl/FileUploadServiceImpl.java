package com.liteisle.service.business.impl;

import com.liteisle.common.domain.response.FileUploadAsyncResp;
import com.liteisle.service.business.FileUploadService;
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
