package com.liteisle.service;

import com.liteisle.common.domain.response.FileUploadAsyncResp;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadMdImage(MultipartFile file, Long fileId);

    FileUploadAsyncResp uploadFile(MultipartFile file, Long folderId);
}
