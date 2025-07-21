package com.liteisle.module.cache.bloom;

import com.liteisle.common.exception.LiteisleException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.liteisle.module.cache.bloom.BloomConfig.*;

@Slf4j
@Component
public class BloomClientImpl implements BloomClient {

    private final RedissonClient redissonClient;

    private final Map<String, RBloomFilter<String>> bloomFilterCache =
            new ConcurrentHashMap<>();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public BloomClientImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private RBloomFilter<String> getOrInitFilter(String bloomKey) {
        return bloomFilterCache.computeIfAbsent(bloomKey, key -> {
            RBloomFilter<String> filter = redissonClient.getBloomFilter(key);
            filter.tryInit(1_000_000L, 0.001);
            return filter;
        });
    }

    @Override
    public boolean add2Bloom(String bloomKey, String value) {
        return getOrInitFilter(bloomKey).add(value);
    }

    @Override
    public boolean fastReqContains(String bloomKey, String value) {
        return getOrInitFilter(bloomKey).contains(value);
    }

    @Override
    public boolean mightContain(String bloomKey, String value, Function<String, Boolean> verifyFunction) {
        return mightContainWithRetry(bloomKey, value, verifyFunction, 0);
    }

    private boolean mightContainWithRetry
            (String bloomKey, String value, Function<String, Boolean> verifyFunction, int retryCount) {
        // 获取布隆过滤器
        RBloomFilter<String> orInitFilter = getOrInitFilter(bloomKey);
        boolean contains = orInitFilter.contains(value);
        if (!contains) {
            // BloomFilter判断不存在，直接返回 false
            return false;
        }

        String combinedKey = bloomKey + value;
        String notFoundKey = NOT_FOUND_FLAG_BLOOM + combinedKey;

        // 判断是否之前已经确认为不存在
        if (stringRedisTemplate.hasKey(notFoundKey)) {
            return false;
        }

        RLock lock = redissonClient.getLock(combinedKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(100, 5000, TimeUnit.MILLISECONDS);
            if (locked) {
                //仅会有一个线程进入
                // 执行数据库校验
                boolean dbExists = verifyFunction.apply(value);
                if (!dbExists) {
                    // 写防穿透缓存（短暂标记）
                    stringRedisTemplate.opsForValue().set(notFoundKey, "0", 1, TimeUnit.MINUTES);
                }
                return dbExists;

            } else {
                // 获取锁失败，Sleep 重试
                if (retryCount >= MAX_RETRY_TIMES) {
                    log.warn("线程 :{} mightContain 获取锁重试超限", Thread.currentThread().getName());
                    return false;
                }
                Thread.sleep(1000);
                return mightContainWithRetry(bloomKey, value, verifyFunction, retryCount + 1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LiteisleException(e.getMessage());
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}

