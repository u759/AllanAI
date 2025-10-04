package com.backend.backend.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    private final ProcessingProperties processingProperties;

    public AsyncConfig(ProcessingProperties processingProperties) {
        this.processingProperties = processingProperties;
    }

    @Bean(name = "matchProcessingExecutor")
    public Executor matchProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        ProcessingProperties.Threads threads = processingProperties.getThreads();
        executor.setCorePoolSize(threads.getCore());
        executor.setMaxPoolSize(threads.getMax());
        executor.setQueueCapacity(threads.getQueueCapacity());
        executor.setThreadNamePrefix("MatchProc-");
        executor.initialize();
        return executor;
    }
}
