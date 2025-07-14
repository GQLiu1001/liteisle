package com.liteisle.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liteisle.mapper.UserFocusRecordsMapper;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.liteisle.common.constant.RedisConstant.USER_SIGN_IN_KEY_PREFIX;

@Component
public class SignUtil {
    @Resource
    private UserFocusRecordsMapper userFocusRecordsMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean doSign(Long userId) {
        // 1. 获取当前日期
        LocalDate today = LocalDate.now();
        YearMonth yearMonth = YearMonth.of(today.getYear(), today.getMonth());
        // 2. 构造Redis Key，格式为 "sign:用户ID:年月"，例如 "sign:101:202507"
        String key = buildSignKey(userId, yearMonth);
        // 3. 计算今天是本月的第几天，作为 bit 的偏移量 (offset)。注意，offset从0开始，所以要-1。
        int offset = today.getDayOfMonth() - 1;

        // 4. 执行 SETBIT 操作。该命令会返回指定位原来的值（0或1）。
        // 如果返回 true(1)，说明今天已经签到过了。
        // 如果返回 false(0)，说明今天是第一次签到。
        Boolean wasSigned = stringRedisTemplate.opsForValue().setBit(key, offset, true);

        // 如果 `wasSigned` 不为 null 且为 false，说明签到成功且是今日首次
        return wasSigned != null && !wasSigned;
    }

    /**
     * 获取用户在指定月份的总签到次数。
     * @param userId 用户ID
     * @param month 要查询的年月
     * @return 指定月份的总签到次数
     */
    public long getSignCount(Long userId, YearMonth month) {
        String key = buildSignKey(userId, month); // <-- 使用传入的 month
        Long count = stringRedisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.stringCommands().bitCount(key.getBytes())
        );
        return count == null ? 0L : count;
    }


    /**
     * 获取用户在指定月份的所有签到日期。
     */
    public List<Integer> getSignDaysForMonth(Long userId, YearMonth month) {
        if (userId == null || month == null) {
            return Collections.emptyList(); // 参数为空，返回空列表
        }

        // 1. 构建该月份的 Redis Key
        String key = buildSignKey(userId, month);

        // 2. 从 Redis 获取整个 key 对应的原始字节数组 (Bitmap 的底层存储)
        byte[] bitmapBytes = stringRedisTemplate.execute(
                (RedisCallback<byte[]>) connection -> connection.stringCommands().get(key.getBytes())
        );

        if (bitmapBytes == null) {
            // 如果 key 不存在，说明该月没有任何签到记录
            return Collections.emptyList();
        }

        return getDayIntegers(month, bitmapBytes);
    }

    @NotNull
    private static List<Integer> getDayIntegers(YearMonth month, byte[] bitmapBytes) {
        List<Integer> signedDays = new ArrayList<>();
        int lengthOfMonth = month.lengthOfMonth();

        for (int byteIndex = 0; byteIndex < bitmapBytes.length; byteIndex++) {
            byte currentByte = bitmapBytes[byteIndex];

            // 从最高位(7)到最低位(0)处理每个bit
            for (int bitIndex = 7; bitIndex >= 0; bitIndex--) {
                if (((currentByte >> bitIndex) & 1) == 1) {
                    int dayOfMonth = (byteIndex * 8) + (7 - bitIndex) + 1;

                    if (dayOfMonth <= lengthOfMonth) {
                        signedDays.add(dayOfMonth);
                    }
                }
            }
        }
        return signedDays;
    }

    /**
     * 辅助方法：构建签到用的Redis Key
     */
    private String buildSignKey(Long userId, YearMonth date) {
        return USER_SIGN_IN_KEY_PREFIX + userId + ":" + date.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
