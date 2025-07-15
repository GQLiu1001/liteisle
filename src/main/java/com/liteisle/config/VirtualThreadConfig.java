package com.liteisle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {

    /**
     * 创建一个基于虚拟线程的 ExecutorService。
     * 虚拟线程是 Java 21 引入的轻量级线程，调度由 JVM 负责，
     * 比传统平台线程更节省资源，适合高并发任务。
     *
     * @return 返回一个每个任务使用一个虚拟线程的 ExecutorService。
     */
    @Bean(name = "virtualThreadPool", destroyMethod = "shutdown")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
        /*
        // 方法二：自定义虚拟线程工厂
        // 可以通过Thread.ofVirtual()来定制虚拟线程的名称、异常处理器等，
        // 然后基于该工厂创建一个按任务分配虚拟线程的Executor。
        ThreadFactory factory = Thread.ofVirtual()
                // 设置虚拟线程的名称前缀，线程编号从0开始递增
                .name("my-virtual-thread-", 0)
                // 设置未捕获异常处理器，方便线上捕获异常日志
                .uncaughtExceptionHandler((thread, throwable) ->
                    System.err.println("Uncaught exception in " + thread + ": " + throwable))
                .factory();

        // 基于自定义线程工厂创建 ExecutorService，内部每个任务对应一个虚拟线程
        return Executors.newThreadPerTaskExecutor(factory);
        */


}
