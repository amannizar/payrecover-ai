package com.payrecover.service;

import com.payrecover.entity.Payment;
import com.payrecover.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    

    private final PaymentRepository paymentRepo;

    public PaymentService(PaymentRepository paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    public List<Payment> getAll() {
        return paymentRepo.findAll();
    }

    public Optional<Payment> getByPaymentId(String paymentId) {
        return paymentRepo.findByPaymentId(paymentId);
    }

    public List<Payment> getFailed() {
        return paymentRepo.findByStatus("FAILED");
    }

    public List<Payment> getFailedPayments() {
        return paymentRepo.findByStatus("FAILED");
    }

    public List<Payment> search(String q) {
        return paymentRepo.search(q);
    }

    public long countFailed() {
        return paymentRepo.countByStatus("FAILED");
    }

    public long countRecovered() {
        return paymentRepo.countByStatus("RECOVERED");
    }

    @Transactional
    public int resetAllToFailed() {
        List<Payment> all = paymentRepo.findAll();
        int count = 0;
        for (Payment p : all) {
            if (!"FAILED".equals(p.getStatus())) {
                p.setStatus("FAILED");
                p.setRetryCount(0);
                paymentRepo.save(p);
                count++;
            }
        }
        // Also reset any FAILED payments that had retries incremented
        for (Payment p : all) {
            if ("FAILED".equals(p.getStatus()) && p.getRetryCount() > 0) {
                p.setRetryCount(0);
                paymentRepo.save(p);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public int seedFromCsv(String resourcePath) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream(resourcePath), StandardCharsets.UTF_8))) {

            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", -1);
                if (cols.length < 22) continue;

                Payment p = new Payment();
                p.setPaymentId(cols[0]);
                p.setCustomerId(cols[1]);
                p.setAmount(new BigDecimal(cols[2]));
                p.setPaymentMethod(cols[3]);
                p.setBank(cols[4]);
                p.setMerchantCategory(cols[6]);
                p.setFailureCode(cols[8]);
                p.setFailureCategory(cols[9]);
                p.setRetryCount(Integer.parseInt(cols[10]));
                p.setGatewayLatencyMs(Integer.parseInt(cols[11]));
                p.setCustomerSuccessRate(Double.parseDouble(cols[12]));
                p.setPreviousFailures30d(Integer.parseInt(cols[13]));
                p.setPreviousSuccessfulRetries(Integer.parseInt(cols[14]));
                p.setCustomerTenureDays(Integer.parseInt(cols[15]));
                p.setSubscriptionStatus(cols[16]);
                p.setHour(Integer.parseInt(cols[17]));
                p.setDayOfWeek(cols[18]);
                p.setDeviceType(cols[19]);
                p.setNetworkType(cols[20]);
                p.setLocationZone(cols[21]);
                p.setStatus("FAILED");

                paymentRepo.save(p);
                count++;
            }
            log.info("Seeded {} payments from {}", count, resourcePath);
        } catch (Exception e) {
            log.error("Failed to seed payments", e);
        }
        return count;
    }
}
