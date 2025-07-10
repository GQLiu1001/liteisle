package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.Users;
import com.liteisle.service.UsersService;
import com.liteisle.mapper.UsersMapper;
import org.springframework.stereotype.Service;

/**
* @author 11965
* @description 针对表【users(用户账户与基本信息)】的数据库操作Service实现
* @createDate 2025-07-10 09:31:28
*/
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users>
    implements UsersService{

}




