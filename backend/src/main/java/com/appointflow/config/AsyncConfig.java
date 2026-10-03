package com.appointflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    /**
     * Hatirlatma ve bildirim gonderimleri icin paralel executor.
     * Her tenant'in WhatsApp gonderimi ayri thread'de calisir;
     * tek bir tenant'in API timeout'u digerlerini bloke etmez.
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(8);
        exec.setMaxPoolSize(16);
        exec.setQueueCapacity(500);
        exec.setThreadNamePrefix("notif-");
        exec.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}
