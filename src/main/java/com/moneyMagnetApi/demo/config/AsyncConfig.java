package com.moneyMagnetApi.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "webhookTaskExecutor")
    public Executor webhookTaskExecutor() {
        return executor("webhook-", 2, 8, 100);
    }

    @Bean(name = "transactionSyncExecutor")
    @Primary
    public Executor transactionSyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        int coreSize = Math.max(4, processors);
        return executor("transaction-sync-", coreSize, coreSize * 2, 200);
    }

    private ThreadPoolTaskExecutor executor(
            String threadNamePrefix,
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
