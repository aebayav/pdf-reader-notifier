package com.pdfFileReader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Evrak isleme kuyrugu: TEK isci thread - isler teslim sirasina gore
 * (FIFO) islenir; kullanici yukleme yaptigi sirada beklemez.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "pdfProcessingExecutor")
    public ThreadPoolTaskExecutor pdfProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pdf-job-");
        executor.initialize();
        return executor;
    }
}
