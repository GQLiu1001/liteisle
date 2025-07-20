package com.liteisle.module.cache.bloom;

import java.util.function.Function;

/**
 * 通用布隆过滤器客户端接口，用于统一封装不同布隆键的增删查操作。
 */
public interface BloomClient {

    /**
     * 向指定布隆过滤器中添加一个值。
     *
     * @param bloomKey 布隆过滤器名称（Redis Key）
     * @param value    要添加的值
     * @return 成功返回 true，失败返回 false
     */
    boolean add2Bloom(String bloomKey, String value);

    /**
     * 判断指定布隆过滤器中是否存在一个值，
     * 并且通过传入的自定义校验函数对value做进一步校验。
     *
     * @param bloomKey 布隆过滤器名称
     * @param value    要判断的值
     * @param verifyFunction 用户自定义的校验函数，通过输入方法判断数据库是否存在，返回boolean
     * @return 存在且通过校验返回true，否则false
     */
    boolean mightContain(String bloomKey, String value, Function<String, Boolean> verifyFunction);
}
