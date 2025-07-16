package com.liteisle;

import com.liteisle.config.IslandPoolConfig;
import com.liteisle.module.chain.manager.FocusRewardChainManager;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Random;

@SpringBootTest
class LiteisleApplicationTests {
    @Resource
    private FocusRewardChainManager focusRewardChainManager;


    @Test
    void contextLoads() {
    }

    @Test
    void test() {
        IslandPoolConfig islandPoolConfig = new IslandPoolConfig();
        for (int i = 0; i < 100; i++) {
            int randomIslandId = islandPoolConfig.getRandomIslandId();
            System.out.println(randomIslandId);
        }
    }

    @Test
    void test2() {
        for (int i = 0; i < 100; i++) {
            String s = focusRewardChainManager.executeChain(i);
            System.out.println(s);
        }
    }

    @Test
    void test3() {
        for (int i = 0; i < 10; i++) {
            System.out.println(BigDecimal.valueOf(System.currentTimeMillis()));
        }
    }
}
