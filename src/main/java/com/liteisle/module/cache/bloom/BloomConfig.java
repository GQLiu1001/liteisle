package com.liteisle.module.cache.bloom;

import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomConfig {
    public static final Integer MAX_RETRY_TIMES = 3;


    public static final String NOT_FOUND_FLAG_BLOOM = "bloom:not-found:";
}
