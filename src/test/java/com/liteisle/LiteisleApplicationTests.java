package com.liteisle;

import com.liteisle.config.IslandPoolConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;

@SpringBootTest
class LiteisleApplicationTests {

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
}
