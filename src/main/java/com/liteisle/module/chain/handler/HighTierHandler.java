package com.liteisle.module.chain.handler;

import com.liteisle.config.IslandPoolConfig;
import com.liteisle.module.chain.AbstractFocusRewardHandler;
import com.liteisle.module.chain.FocusRewardContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class HighTierHandler extends AbstractFocusRewardHandler {

    @Resource
    private IslandPoolConfig islandPoolConfig;
    private final Random random = new Random();

    @Override
    protected void handle(FocusRewardContext context) {
        if (context.getFocusMinutes() >= 45) {
            // 30% 的概率
            if (random.nextDouble() < 0.3) {
                context.setAwardedIslandUrl(islandPoolConfig.getRandomIslandUrl());
            }
            // 无论是否中奖，该档位已经处理完毕，标记为 handled
            context.setHandled(true);
        }
    }
}