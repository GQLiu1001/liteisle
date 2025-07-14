package com.liteisle.service;

import com.liteisle.common.domain.Folders;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 11965
* @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Service
* @createDate 2025-07-10 20:09:48
*/
public interface FoldersService extends IService<Folders> {
    /**
     * 创建用户默认文件夹
     * @param userId
     */
    void createUserDefaultFolder(Long userId);
}
