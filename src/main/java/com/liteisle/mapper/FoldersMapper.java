package com.liteisle.mapper;

import com.liteisle.common.domain.Folders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 11965
* @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Mapper
* @createDate 2025-07-10 20:09:48
* @Entity com.liteisle.common.domain.Folders
*/
public interface FoldersMapper extends BaseMapper<Folders> {


    List<Folders> selectFoldersWithFilteredFileCount(Long userId, String content);

    List<Folders> selectFoldersWithTotalFileCount(Long userId);
}




