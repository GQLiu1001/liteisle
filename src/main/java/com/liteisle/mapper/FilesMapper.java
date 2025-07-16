package com.liteisle.mapper;

import com.liteisle.common.domain.Files;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteisle.common.domain.response.MusicViewResp;

import java.util.List;

/**
* @author 11965
* @description 针对表【files(文件通用基础信息表)】的数据库操作Mapper
* @createDate 2025-07-10 20:09:48
* @Entity com.liteisle.common.domain.Files
*/
public interface FilesMapper extends BaseMapper<Files> {

    List<MusicViewResp.MusicFile> getMusicViewWithContent(String content, Long userId);
}




