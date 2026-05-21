package com.tuna.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("Ecommerce-Async-");
        executor.setRejectedExecutionHandler((task, executorService) -> {
            if (executorService.isShutdown()) {
                throw new RejectedExecutionException("Async executor is shutting down");
            }
            log.warn("Dropping non-critical async task because async executor is saturated. active={}, queued={}",
                    executorService.getActiveCount(),
                    executorService.getQueue().size());
        });
        executor.initialize();
        return executor;
    }
}
