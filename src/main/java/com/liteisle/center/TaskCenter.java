package com.liteisle.center;


import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;


@Service
@EnableScheduling
public class TaskCenter {
    //TODO 定时检查回收站文件是否已过期，过期则将其从回收站中彻底删除（恢复用户额度）。
    @Resource
    private EmailCenter emailCenter;

}
