package com.liteisle.service.business;

import com.liteisle.common.dto.response.FileUploadAsyncResp;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    /**
     * 上传文件
     * @param file 需要上传的文件
     * @param folderId 需要上传的文件所在的文件夹id
     * @return 文件的访问URL
     */
    FileUploadAsyncResp uploadFile(MultipartFile file, Long folderId);
}
