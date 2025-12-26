package com.xiongdwm.ai_demo.utils.global;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AsyncConfig {
    @Value("${ai.executor.core-size:2}")
    private int coreSize;

    @Value("${ai.executor.max-size:8}")
    private int maxSize;

    @Value("${ai.executor.queue-capacity:50}")
    private int queueCapacity;

    @Value("${ai.executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${ai.executor.thread-name-prefix:ai-exec-}")
    private String threadNamePrefix;

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);


    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiExecutor(){
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(Math.max(1, queueCapacity));
        ThreadFactory tf = new ThreadFactory() {
            private final AtomicInteger idx = new AtomicInteger(0);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r, threadNamePrefix + idx.incrementAndGet());
                t.setDaemon(false);
                t.setUncaughtExceptionHandler((thread, ex) -> log.error("Uncaught exception in thread {}:", thread.getName(), ex));
                return t;
            }
        };

        RejectedExecutionHandler rejectHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        ThreadPoolExecutor exec = new ThreadPoolExecutor(
                Math.max(1, coreSize),
                Math.max(coreSize, maxSize),
                Math.max(0, keepAliveSeconds),
                TimeUnit.SECONDS,
                queue,
                tf,
                rejectHandler
        );

        exec.allowCoreThreadTimeOut(true);
        // 预启动核心线程
        try {
            exec.prestartAllCoreThreads();
        } catch (Exception e) {
            log.warn("prestartAllCoreThreads failed", e);
        }
        log.info("Created aiExecutor: core={}, max={}, queue={}, keepAlive={}, namePrefix={}",
                coreSize, maxSize, queueCapacity, keepAliveSeconds, threadNamePrefix);
        return exec;
    }

    @Bean
    public Scheduler aiScheduler(ExecutorService aiExecutor){
        return reactor.core.scheduler.Schedulers.fromExecutorService(aiExecutor);
    }
}
