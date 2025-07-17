package com.liteisle.mapper;

import com.liteisle.common.domain.Folders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteisle.common.domain.response.FolderContentResp;
import com.liteisle.common.domain.response.RecycleBinContentResp;

import java.util.List;

/**
* @author 11965
* @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Mapper
* @createDate 2025-07-10 20:09:48
* @Entity com.liteisle.common.domain.Folders
*/
public interface FoldersMapper extends BaseMapper<Folders> {


    List<Folders> selectFoldersWithCount(Long userId, String content, String type);

    List<Folders> selectFolders(Long userId, String type);

    List<RecycleBinContentResp.FolderItem> getRecycleBinViewWithContent(Long userId, String content);

    List<FolderContentResp.FolderItem> getFolderContentWithSort(Long folderId, String sortBy, Long userId, String sortOrder,String content);

}




