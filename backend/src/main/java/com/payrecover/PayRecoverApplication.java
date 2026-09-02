package com.payrecover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PayRecoverApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayRecoverApplication.class, args);
    }
}
