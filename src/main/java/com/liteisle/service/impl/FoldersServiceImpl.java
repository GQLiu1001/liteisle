package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Folders;
import com.liteisle.service.FoldersService;
import com.liteisle.mapper.FoldersMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class FoldersServiceImpl extends ServiceImpl<FoldersMapper, Folders>
    implements FoldersService{

}




