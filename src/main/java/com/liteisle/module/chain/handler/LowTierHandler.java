package com.liteisle.module.chain.handler;

import com.liteisle.config.IslandPoolConfig;
import com.liteisle.module.chain.AbstractFocusRewardHandler;
import com.liteisle.module.chain.FocusRewardContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class LowTierHandler extends AbstractFocusRewardHandler {

    @Resource
    private IslandPoolConfig islandPoolConfig;
    private final Random random = new Random();

    @Override
    protected void handle(FocusRewardContext context) {
        if (context.getFocusMinutes() >= 15) { // 注意这里是 >= 15
            // 20% 的概率
            if (random.nextDouble() < 0.2) {
                context.setAwardedIslandUrl(islandPoolConfig.getRandomIslandUrl());
            }
            context.setHandled(true);
        }
    }
}
