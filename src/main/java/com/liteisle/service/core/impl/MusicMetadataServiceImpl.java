package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.MusicMetadata;
import com.liteisle.service.core.MusicMetadataService;
import com.liteisle.mapper.MusicMetadataMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【music_metadata(音乐文件专属元数据表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class MusicMetadataServiceImpl extends ServiceImpl<MusicMetadataMapper, MusicMetadata>
    implements MusicMetadataService{

}




