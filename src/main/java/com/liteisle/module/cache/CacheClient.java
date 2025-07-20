package com.liteisle.module.cache;

import java.util.function.Supplier;

public interface CacheClient {

    <T> T getWithLock
            (String combinedKey, long waitTimeMillis, long leaseTimeMillis, Supplier<T> action);

    boolean set(String key, String value , long expireTimeMillis);


}
