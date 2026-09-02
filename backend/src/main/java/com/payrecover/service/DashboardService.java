package com.payrecover.service;

import com.payrecover.repository.AuditLogRepository;
import com.payrecover.repository.PaymentRepository;
import com.payrecover.repository.RecoveryActionRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final PaymentRepository paymentRepo;
    private final RecoveryActionRepository actionRepo;
    private final AuditLogRepository auditRepo;

    public DashboardService(PaymentRepository paymentRepo,
                            RecoveryActionRepository actionRepo,
                            AuditLogRepository auditRepo) {
        this.paymentRepo = paymentRepo;
        this.actionRepo = actionRepo;
        this.auditRepo = auditRepo;
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();

        long totalFailed = paymentRepo.countByStatus("FAILED");
        long totalRecovered = paymentRepo.countByStatus("RECOVERED");
        long totalPayments = paymentRepo.count();

        m.put("totalPayments", totalPayments);
        m.put("totalFailed", totalFailed);
        m.put("totalRecovered", totalRecovered);
        m.put("paymentRecoveryRate", totalPayments > 0
            ? Math.round(totalRecovered * 1000.0 / totalPayments) / 10.0 : 0.0);

        // Revenue metrics
        Double totalAmount = paymentRepo.sumAllAmounts();
        Double recoveredAmount = paymentRepo.sumAmountByStatus("RECOVERED");
        m.put("revenueAtRisk", totalAmount != null ? totalAmount : 0.0);
        m.put("revenueRecovered", recoveredAmount != null ? recoveredAmount : 0.0);
        m.put("revenueRecoveryRate", totalAmount != null && totalAmount > 0
            ? Math.round(recoveredAmount * 1000.0 / totalAmount) / 10.0 : 0.0);

        // Total actions executed
        long totalActions = actionRepo.count();
        m.put("totalActions", totalActions);

        // Action distribution for charts
        List<Object[]> actionCounts = actionRepo.countBySelectedAction();
        Map<String, Long> actionDist = new LinkedHashMap<>();
        for (Object[] row : actionCounts) {
            actionDist.put((String) row[0], (Long) row[1]);
        }
        m.put("actionDistribution", actionDist);

        // Stopping reasons
        List<Object[]> stopReasons = actionRepo.countByStoppingReason();
        Map<String, Long> stopMap = new LinkedHashMap<>();
        for (Object[] row : stopReasons) {
            stopMap.put((String) row[0], (Long) row[1]);
        }
        m.put("stoppingReasons", stopMap);

        // Audit log for recent activity
        m.put("recentLogs", auditRepo.findTop20OrderByCreatedAtDesc());

        return m;
    }
}
