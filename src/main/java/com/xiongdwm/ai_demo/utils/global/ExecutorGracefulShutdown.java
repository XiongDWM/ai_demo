package com.xiongdwm.ai_demo.utils.global;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ExecutorGracefulShutdown implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(ExecutorGracefulShutdown.class);

    private final ThreadPoolExecutor executor;

    @Value("${ai.executor.shutdown-wait-seconds:30}")
    private long shutdownWaitSeconds;

    public ExecutorGracefulShutdown(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void destroy() throws Exception {
        if (executor == null) return;
        log.info("Shutting down aiExecutor, await {} seconds...", shutdownWaitSeconds);
        try {
            executor.shutdown(); // 禁止新任务
            if (!executor.awaitTermination(shutdownWaitSeconds, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in {} seconds, forcing shutdown", shutdownWaitSeconds);
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Executor still did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException ie) {
            log.warn("Interrupted while waiting for executor termination, forcing shutdown", ie);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
