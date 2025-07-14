package com.liteisle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.constant.FolderConstant;
import com.liteisle.common.domain.Folders;
import com.liteisle.common.enums.FolderTypeEnum;
import com.liteisle.service.FoldersService;
import com.liteisle.mapper.FoldersMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
* @author 11965
* @description 针对表【folders(用户逻辑文件夹结构表)】的数据库操作Service实现
* @createDate 2025-07-10 20:09:48
*/
@Service
public class FoldersServiceImpl extends ServiceImpl<FoldersMapper, Folders>
    implements FoldersService{

    @Override
    public void createUserDefaultFolder(Long userId) {
        for (String folderName : FolderConstant.DEFAULT_SYSTEM_FOLDERS) {
            createSystemFolder(userId, folderName);
        }
    }

    public void createSystemFolder(Long userId, String folderName) {
        Folders folder = new Folders();
        folder.setUserId(userId);
        folder.setParentId(0L);
        folder.setFolderName(folderName);
        folder.setFolderType(FolderTypeEnum.SYSTEM);
        folder.setSortedOrder(BigDecimal.valueOf(System.currentTimeMillis()));
        Date now = new Date();
        folder.setCreateTime(now);
        folder.setUpdateTime(now);
        this.save(folder);
    }
}




