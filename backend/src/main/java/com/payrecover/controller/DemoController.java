package com.payrecover.controller;

import com.payrecover.repository.AuditLogRepository;
import com.payrecover.repository.RecoveryActionRepository;
import com.payrecover.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Development-only endpoint to reset the demo database to a clean state.
 * Only enabled when spring.profiles.active includes "dev" or "demo".
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final RecoveryActionRepository actionRepo;
    private final AuditLogRepository auditRepo;
    private final PaymentService paymentService;

    @Value("${payrecover.demo.reset-enabled:false}")
    private boolean resetEnabled;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public DemoController(RecoveryActionRepository actionRepo,
                          AuditLogRepository auditRepo,
                          PaymentService paymentService) {
        this.actionRepo = actionRepo;
        this.auditRepo = auditRepo;
        this.paymentService = paymentService;
    }

    /**
     * POST /api/demo/reset — Reset the demo database to a clean state.
     * 
     * 1. Clear all recovery actions
     * 2. Clear all audit logs
     * 3. Reset all payment statuses to FAILED and retry counts to 0
     * 4. Re-seed if payments table is empty
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        // Safety: only allow reset when explicitly enabled
        if (!resetEnabled) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Demo reset is disabled. Enable with payrecover.demo.reset-enabled=true",
                "hint", "Set spring.profiles.active=dev or add DEMO_RESET_ENABLED=true to env"
            ));
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Clear recovery actions
        long actionsDeleted = actionRepo.count();
        actionRepo.deleteAllInBatch();

        // 2. Clear audit logs
        long logsDeleted = auditRepo.count();
        auditRepo.deleteAllInBatch();

        // 3. Reset all payments to FAILED state with retry count 0
        int paymentsReset = paymentService.resetAllToFailed();

        // 4. Re-seed if empty
        int seeded = 0;
        if (paymentService.getAll().isEmpty()) {
            seeded = paymentService.seedFromCsv("/data/payments.csv");
        }

        result.put("status", "ok");
        result.put("recoveryActionsDeleted", actionsDeleted);
        result.put("auditLogsDeleted", logsDeleted);
        result.put("paymentsReset", paymentsReset);
        result.put("paymentsSeeded", seeded);
        result.put("totalPayments", paymentService.getAll().size());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/demo/status — Check current demo database state.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalPayments", paymentService.getAll().size());
        status.put("failedPayments", paymentService.countFailed());
        status.put("recoveredPayments", paymentService.countRecovered());
        status.put("totalRecoveryActions", actionRepo.count());
        status.put("totalAuditLogs", auditRepo.count());
        status.put("datasourceType", datasourceUrl.contains("h2:") ? "H2 (dev)" : "Production DB");
        return ResponseEntity.ok(status);
    }
}
