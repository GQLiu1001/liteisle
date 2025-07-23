package com.liteisle;

import com.liteisle.config.IslandPoolConfig;
import com.liteisle.module.chain.manager.FocusRewardChainManager;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void simulateReindexTrigger() throws InterruptedException {
        final int scale = 30;

        // 模拟排序操作的初始边界。
        // 为了清晰，我们选择0和1作为起点，它们之间的距离不影响最终的迭代次数。
        BigDecimal beforeOrder = new BigDecimal(System.currentTimeMillis()*100000);
        Thread.sleep(10);
        BigDecimal afterOrder = new BigDecimal(System.currentTimeMillis()*100000);
//        BigDecimal beforeOrder = new BigDecimal(System.currentTimeMillis());
//        Thread.sleep(10);
//        BigDecimal afterOrder = new BigDecimal(System.currentTimeMillis());
        int operationsCount = 0; // 操作计数器

        System.out.println("开始模拟排序精度耗尽过程...");
        System.out.println("初始范围: [" + beforeOrder + ", " + afterOrder + "]");
        System.out.println("-------------------------------------------------");

        while (true) {
            operationsCount++;

            // 1. 完全模拟 ItemViewServiceImpl.java 中的中点计算逻辑
            BigDecimal midPoint = beforeOrder.add(afterOrder).divide(new BigDecimal("2"));
            BigDecimal newOrder = midPoint.setScale(scale, RoundingMode.HALF_UP);

            // 打印每一次的计算过程，方便观察
            System.out.printf("第 %2d 次操作: before=%.30f, after=%.30f, newOrder=%.30f\n",
                    operationsCount, beforeOrder, afterOrder, newOrder);

            // 2. 模拟 ItemViewServiceImpl.java 中的失效条件检查
            // 如果新计算出的排序值等于其中一个边界值，说明无法再插入，精度耗尽
            if (newOrder.compareTo(beforeOrder) == 0 || newOrder.compareTo(afterOrder) == 0) {
                System.out.println("\n-------------------------------------------------");
                System.out.println("精度失效！");
                System.out.println("在第 " + operationsCount + " 次操作后，计算出的 newOrder 与边界值相等，无法再进行插入。");
                System.out.println("此时应触发重排（Re-indexing）机制。");
                break; // 结束模拟
            }

            // 3. 为了模拟“最坏情况”，我们总是在新生成的更小的区间内进行下一次插入
            // 这里我们选择更新 afterOrder，将范围缩小为 [beforeOrder, newOrder]
            afterOrder = newOrder;
        }
    }


    @Test
    void simulateInsertWithStepOnly() throws InterruptedException {
        final int scale = 30;
        final BigDecimal STEP = new BigDecimal("1e-10");

        BigDecimal beforeOrder = new BigDecimal(System.currentTimeMillis() * 100000);
        Thread.sleep(10);
        BigDecimal afterOrder = new BigDecimal(System.currentTimeMillis() * 100000);

        int insertCount = 0;

        System.out.println("开始模拟仅后者+步长插入过程...");
        System.out.println("初始区间: [" + beforeOrder + ", " + afterOrder + "]");
        System.out.println("-------------------------------------------------");

        while (true) {
            insertCount++;

            BigDecimal newOrder = afterOrder.add(STEP).setScale(scale, RoundingMode.HALF_UP);

            System.out.printf("第 %3d 次插入: afterOrder=%.30f, newOrder=%.30f\n",
                    insertCount, afterOrder, newOrder);

            // 判断是否越界（newOrder不能小于等于afterOrder）
            if (newOrder.compareTo(afterOrder) <= 0) {
                System.out.println("❌ 插入失败，无法继续插入，触发重排");
                break;
            }

            afterOrder = newOrder;
        }

        System.out.println("✅ 总插入次数：" + insertCount);
    }

    // 你的最小排序间隔常量，和生产代码保持一致
    private static final BigDecimal MIN_SORT_GAP = new BigDecimal("0.0000000001");

    // 保持与代码相同的 scale
    private static final int SCALE = 10;

    @Test
    void simulateInsertBetweenBeforeAndAfter() {
        // 模拟前后排序值，间隔为1，足够插入多次
        BigDecimal beforeOrder = new BigDecimal("1000.0000000000");
        BigDecimal afterOrder = new BigDecimal("1001.0000000000");

        int insertCount = 0;

        System.out.println("=== 模拟在两个排序值之间插入 ===");
        System.out.println("初始区间: [" + beforeOrder + ", " + afterOrder + "]");
        System.out.println("最小间隔: " + MIN_SORT_GAP);
        System.out.println("--------------------------------------");

        while (true) {
            insertCount++;

            BigDecimal gap = afterOrder.subtract(beforeOrder);

            System.out.printf("第 %3d 次插入: gap=%.10f\n", insertCount, gap);

            if (gap.compareTo(MIN_SORT_GAP) <= 0) {
                System.out.println("❌ 间隔不足，触发重排");
                break;
            }

            // 计算新排序值为 beforeOrder + 最小间隔
            BigDecimal newOrder = beforeOrder.add(MIN_SORT_GAP).setScale(SCALE, RoundingMode.HALF_UP);

            // 健壮性检查
            if (newOrder.compareTo(afterOrder) >= 0) {
                System.out.println("❌ 新排序值 >= afterOrder，停止插入");
                break;
            }

            System.out.printf("插入的新排序值: %.10f\n", newOrder);

            // 模拟插入后 newOrder 成为新的 beforeOrder
            beforeOrder = newOrder;
        }

        System.out.println("✅ 总插入次数: " + insertCount);
    }

    @Test
    void simulateInsertAtListStart() {
        // 模拟列表为空或者插入第一个元素的情况
        BigDecimal afterOrder = new BigDecimal("1000.0000000000");

        System.out.println("=== 模拟插入到列表最前面 ===");

        // 计算新排序值为 afterOrder - 1000（业务规则）
        BigDecimal newOrder = afterOrder.subtract(BigDecimal.valueOf(1000)).setScale(SCALE, RoundingMode.HALF_UP);

        System.out.println("afterOrder = " + afterOrder);
        System.out.println("新排序值 = " + newOrder);

        assert newOrder.compareTo(afterOrder) < 0;
    }

    @Test
    void simulateInsertAtListEnd() {
        // 模拟插入到列表最后面
        BigDecimal beforeOrder = new BigDecimal("1000.0000000000");

        System.out.println("=== 模拟插入到列表最后面 ===");

        // 计算新排序值为 beforeOrder + 1000（业务规则）
        BigDecimal newOrder = beforeOrder.add(BigDecimal.valueOf(1000)).setScale(SCALE, RoundingMode.HALF_UP);

        System.out.println("beforeOrder = " + beforeOrder);
        System.out.println("新排序值 = " + newOrder);

        assert newOrder.compareTo(beforeOrder) > 0;
    }

    @Test
    void simulateInsertWhenListEmpty() {
        // 模拟列表为空，首次插入，使用 System.currentTimeMillis()*100000
        BigDecimal newOrder = new BigDecimal(System.currentTimeMillis() * 100000L).setScale(SCALE, RoundingMode.HALF_UP);

        System.out.println("=== 模拟空列表首次插入 ===");
        System.out.println("新排序值 = " + newOrder);

        assert newOrder.compareTo(BigDecimal.ZERO) > 0;
    }

}
