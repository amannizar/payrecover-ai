package com.payrecover.config;

import com.payrecover.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final PaymentService paymentService;

    public DataSeeder(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (paymentService.getAll().isEmpty()) {
            log.info("Database empty — seeding from CSV…");
            int count = paymentService.seedFromCsv("/data/payments.csv");
            log.info("Seeded {} payments", count);
        } else {
            log.info("Database already has {} payments — skipping seed", paymentService.getAll().size());
        }
    }
}
