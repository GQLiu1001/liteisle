package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Folders;
import com.liteisle.service.FoldersService;
import com.liteisle.mapper.FoldersMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【folders(逻辑文件夹，统一管理歌单、文档分类等)】的数据库操作Service实现
* @createDate 2025-07-10 09:31:28
*/
@Service
public class FoldersServiceImpl extends ServiceImpl<FoldersMapper, Folders>
    implements FoldersService{

}




