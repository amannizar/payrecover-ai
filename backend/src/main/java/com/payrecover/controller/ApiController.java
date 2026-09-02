package com.payrecover.controller;

import com.payrecover.entity.AuditLog;
import com.payrecover.entity.Payment;
import com.payrecover.repository.AuditLogRepository;
import com.payrecover.service.PaymentService;
import com.payrecover.service.RecoveryAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final PaymentService paymentService;
    private final AuditLogRepository auditRepo;
    private final RecoveryAgentService recoveryAgent;

    public ApiController(PaymentService paymentService, AuditLogRepository auditRepo,
                         RecoveryAgentService recoveryAgent) {
        this.paymentService = paymentService;
        this.auditRepo = auditRepo;
        this.recoveryAgent = recoveryAgent;
    }

    @GetMapping("/payments")
    public List<Payment> getPayments() {
        return paymentService.getAll();
    }

    @GetMapping("/payments/{paymentId}")
    public Payment getPayment(@PathVariable String paymentId) {
        return paymentService.getByPaymentId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    @GetMapping("/audit")
    public List<AuditLog> getAuditLogs() {
        return auditRepo.findAllOrderByCreatedAtDesc();
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return Map.of(
            "totalPayments", paymentService.getAll().size(),
            "totalFailed", paymentService.countFailed(),
            "totalRecovered", paymentService.countRecovered()
        );
    }

    /**
     * One-time data seed endpoint: loads the CSV test set into the database.
     * Call POST /api/seed once after startup.
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed() {
        int count = paymentService.seedFromCsv("/data/payments.csv");
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "paymentsSeeded", count
        ));
    }

    /**
     * Batch recovery: process multiple failed payments at once.
     * POST /api/recovery/batch?limit=100
     */
    @PostMapping("/recovery/batch")
    public ResponseEntity<Map<String, Object>> batchRecovery(
            @RequestParam(defaultValue = "100") int limit) {

        // Input validation
        if (limit <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid limit",
                "message", "Limit must be a positive integer, got: " + limit
            ));
        }
        final int MAX_BATCH = 10000;
        if (limit > MAX_BATCH) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Limit too large",
                "message", "Maximum batch size is " + MAX_BATCH + ", got: " + limit
            ));
        }

        // Only process actually-failed payments (not recovered/stopped)
        List<Payment> failedPayments = paymentService.getFailedPayments();
        int toProcess = Math.min(limit, failedPayments.size());

        if (toProcess == 0) {
            return ResponseEntity.ok(Map.of(
                "processed", 0,
                "success", 0,
                "failed", 0,
                "stopped", 0,
                "message", "No failed payments to process",
                "results", java.util.List.of()
            ));
        }

        int successCount = 0;
        int failedCount = 0;
        int stoppedCount = 0;
        double totalRecovered = 0;
        double totalAttempted = 0;

        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < toProcess; i++) {
            Payment p = failedPayments.get(i);
            try {
                var action = recoveryAgent.executeRecovery(p.getPaymentId());
                String result = action.getActualResult();
                double recovered = action.getActualRecovery() != null
                    ? action.getActualRecovery().doubleValue() : 0;

                if ("SUCCESS".equals(result)) {
                    successCount++;
                    totalRecovered += recovered;
                } else if ("STOPPED".equals(result)) {
                    stoppedCount++;
                } else {
                    failedCount++;
                }
                totalAttempted += p.getAmount().doubleValue();

                results.add(Map.of(
                    "paymentId", p.getPaymentId(),
                    "amount", p.getAmount(),
                    "action", action.getSelectedAction(),
                    "result", result,
                    "recovered", recovered
                ));
            } catch (Exception e) {
                failedCount++;
                results.add(Map.of(
                    "paymentId", p.getPaymentId(),
                    "error", e.getMessage()
                ));
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("processed", toProcess);
        summary.put("success", successCount);
        summary.put("failed", failedCount);
        summary.put("stopped", stoppedCount);
        summary.put("totalAttempted", totalAttempted);
        summary.put("totalRecovered", totalRecovered);
        summary.put("recoveryRate", toProcess > 0
            ? Math.round(successCount * 1000.0 / toProcess) / 10.0 : 0);
        summary.put("results", results);

        return ResponseEntity.ok(summary);
    }
}
