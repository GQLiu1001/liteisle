package com.liteisle.service;

import com.liteisle.common.domain.UserIslands;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 11965
* @description 针对表【user_islands(记录用户已获得的岛屿表)】的数据库操作Service
* @createDate 2025-07-10 20:09:48
*/
public interface UserIslandsService extends IService<UserIslands> {
    /**
     * 获取用户已获得的岛屿列表
     * @return 获取岛屿的url列表
     */
    List<String> getIslandCollection();
}
