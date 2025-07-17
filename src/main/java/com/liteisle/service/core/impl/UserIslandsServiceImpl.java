package com.liteisle.service.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.UserIslands;
import com.liteisle.service.core.UserIslandsService;
import com.liteisle.mapper.UserIslandsMapper;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author 11965
* @description 针对表【user_islands(记录用户已解锁的岛屿表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class UserIslandsServiceImpl extends ServiceImpl<UserIslandsMapper, UserIslands>
    implements UserIslandsService{

    @Resource
    private UserIslandsMapper userIslandsMapper;

    @Override
    public List<String> getIslandCollection() {
        Long userId = UserContextHolder.getUserId();
        List<String> resp = new ArrayList<>();
        for (UserIslands island : this.list(new QueryWrapper<UserIslands>().eq("user_id", userId))) {
            resp.add(island.getIslandUrl());
        }
        return resp;
    }
}




