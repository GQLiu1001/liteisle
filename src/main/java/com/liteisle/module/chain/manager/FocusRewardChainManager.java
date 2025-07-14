package com.liteisle.module.chain.manager;

import com.liteisle.module.chain.AbstractFocusRewardHandler;
import com.liteisle.module.chain.FocusRewardContext;
import com.liteisle.module.chain.handler.HighTierHandler;
import com.liteisle.module.chain.handler.LowTierHandler;
import com.liteisle.module.chain.handler.MidTierHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


@Service
public class FocusRewardChainManager {

    @Resource
    private HighTierHandler highTierHandler;

    @Resource
    private MidTierHandler midTierHandler;

    @Resource
    private LowTierHandler lowTierHandler;

    // 责任链的起点
    private AbstractFocusRewardHandler chainStart;

    /**
     * 使用 @PostConstruct 注解，在Spring容器初始化该Bean后，自动构建责任链。
     */
    @PostConstruct
    public void buildChain() {
        // 设定链条顺序: High -> Mid -> Low
        highTierHandler.setNext(midTierHandler);
        midTierHandler.setNext(lowTierHandler);

        // 指定链条的起点
        this.chainStart = highTierHandler;
    }

    /**
     * 执行责任链
     * @param focusMinutes 专注时长
     * @return 奖励的岛屿URL或null
     */
    public String executeChain(Integer focusMinutes) {
        FocusRewardContext context = new FocusRewardContext(focusMinutes);
        chainStart.process(context);
        return context.getAwardedIslandUrl();
    }
}