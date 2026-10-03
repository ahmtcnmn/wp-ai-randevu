package com.appointflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BackendApplication {
    public static void main(String[] args) {
        // Reactor Netty DNS resolver IPv6/IPv4 sorgularken bazen takiliyor (macOS),
        // OpenAI cagrilarinda UnknownHostException atilmasinin onune gec
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        SpringApplication.run(BackendApplication.class, args);
    }
}
