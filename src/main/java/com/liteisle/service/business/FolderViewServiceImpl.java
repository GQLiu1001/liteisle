package com.liteisle.service.business;

import com.liteisle.common.domain.response.FolderContentResp;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FolderViewServiceImpl implements  FolderViewService{
    @Override
    public FolderContentResp getFolderContent(Long folderId, String sortBy) {
        //TODO 获取文件夹内容
        List<FolderContentResp.BreadcrumbItem> breadcrumb = new ArrayList<>();
        List<FolderContentResp.FolderItem> folders = new ArrayList<>();
        List<FolderContentResp.FileItem> files = new ArrayList<>();






        return new FolderContentResp(breadcrumb, folders, files);
    }
}
