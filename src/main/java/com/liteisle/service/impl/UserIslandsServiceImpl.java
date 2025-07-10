package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.UserIslands;
import com.liteisle.service.UserIslandsService;
import com.liteisle.mapper.UserIslandsMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【user_islands(记录用户已解锁的岛屿表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class UserIslandsServiceImpl extends ServiceImpl<UserIslandsMapper, UserIslands>
    implements UserIslandsService{

}




