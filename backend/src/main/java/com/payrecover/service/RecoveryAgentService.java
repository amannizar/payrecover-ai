package com.payrecover.service;

import com.payrecover.dto.ProbabilityResponse;
import com.payrecover.dto.RecoveryDecision;
import com.payrecover.entity.AuditLog;
import com.payrecover.entity.Payment;
import com.payrecover.entity.RecoveryAction;
import com.payrecover.repository.AuditLogRepository;
import com.payrecover.repository.PaymentRepository;
import com.payrecover.repository.RecoveryActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecoveryAgentService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryAgentService.class);
    private static final int MAX_RETRIES = 2;
    private static final double MIN_RECOVERY_PROB = 0.20;

    private static final Map<String, Double> ACTION_COSTS = Map.of(
        "RETRY_NOW", 2.0, "RETRY_LATER", 2.0,
        "SWITCH_METHOD", 5.0, "SEND_NOTIFICATION", 8.0, "STOP", 0.0
    );

    private static final List<String> ACTIONS = List.of(
        "RETRY_NOW", "RETRY_LATER", "SWITCH_METHOD", "SEND_NOTIFICATION", "STOP"
    );

    private final AiClientService aiClient;
    private final SimulatorService simulator;
    private final PaymentRepository paymentRepo;
    private final RecoveryActionRepository actionRepo;
    private final AuditLogRepository auditRepo;

    public RecoveryAgentService(AiClientService aiClient, SimulatorService simulator,
                                 PaymentRepository paymentRepo,
                                 RecoveryActionRepository actionRepo,
                                 AuditLogRepository auditRepo) {
        this.aiClient = aiClient;
        this.simulator = simulator;
        this.paymentRepo = paymentRepo;
        this.actionRepo = actionRepo;
        this.auditRepo = auditRepo;
    }

    /**
     * Get AI decision for a payment without executing it.
     */
    public RecoveryDecision decide(String paymentId) {
        Payment payment = paymentRepo.findByPaymentId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if ("RECOVERED".equals(payment.getStatus())) {
            throw new IllegalStateException("Payment " + paymentId + " is already recovered");
        }

        ProbabilityResponse probs = aiClient.getRecoveryProbabilities(payment);
        int retryCount = payment.getRetryCount();
        String stopReason = checkStoppingRules(payment, retryCount);

        RecoveryDecision decision = new RecoveryDecision();
        decision.setPaymentId(paymentId);
        decision.setFailureCategory(payment.getFailureCategory());

        // Build probability map
        Map<String, Double> probMap = new LinkedHashMap<>();
        for (String action : ACTIONS) {
            probMap.put(action, probs.getProb(action));
        }
        decision.setAllProbabilities(probMap);

        if (stopReason != null) {
            decision.setSelectedAction("STOP");
            decision.setStoppingReason(stopReason);
            decision.setProbability(probs.getStop());
            decision.setExpectedRecovery(0.0);
            return decision;
        }

        // Select best action by expected value
        double amount = payment.getAmount().doubleValue();
        String bestAction = "STOP";
        double bestEv = 0.0;

        for (String action : ACTIONS) {
            if ("STOP".equals(action)) continue;
            double p = probs.getProb(action);
            double cost = ACTION_COSTS.getOrDefault(action, 0.0);
            double ev = amount * p - cost;

            if (action.contains("RETRY") && p < MIN_RECOVERY_PROB) continue;
            if (ev > bestEv) {
                bestEv = ev;
                bestAction = action;
            }
        }

        decision.setSelectedAction(bestAction);
        decision.setProbability(probs.getProb(bestAction));
        decision.setExpectedRecovery(amount * probs.getProb(bestAction));
        return decision;
    }

    /**
     * Execute a recovery attempt: decide → simulate → record.
     */
    @Transactional
    public RecoveryAction executeRecovery(String paymentId) {
        Payment payment = paymentRepo.findByPaymentId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if ("RECOVERED".equals(payment.getStatus())) {
            throw new IllegalStateException("Payment " + paymentId + " is already recovered");
        }

        RecoveryDecision decision = decide(paymentId);

        RecoveryAction action = new RecoveryAction();
        action.setPaymentId(paymentId);
        action.setSelectedAction(decision.getSelectedAction());
        action.setExpectedRecovery(BigDecimal.valueOf(decision.getExpectedRecovery()));
        action.setTimestampStep(payment.getRetryCount());

        AuditLog logEntry = new AuditLog();
        logEntry.setPaymentId(paymentId);
        logEntry.setAction(decision.getSelectedAction());
        logEntry.setExpectedRecovery(decision.getExpectedRecovery());

        if ("STOP".equals(decision.getSelectedAction())) {
            action.setActualResult("STOPPED");
            action.setActualRecovery(BigDecimal.ZERO);
            action.setStoppingReason(decision.getStoppingReason());
            logEntry.setResult("STOPPED");
            logEntry.setRecoveredAmount(0.0);
            logEntry.setStoppingReason(decision.getStoppingReason());
        } else {
            boolean success = simulator.simulate(payment, decision.getSelectedAction());
            if (success) {
                action.setActualResult("SUCCESS");
                action.setActualRecovery(payment.getAmount());
                payment.setStatus("RECOVERED");
                logEntry.setResult("SUCCESS");
                logEntry.setRecoveredAmount(payment.getAmount().doubleValue());
            } else {
                action.setActualResult("FAILED");
                action.setActualRecovery(BigDecimal.ZERO);
                payment.setRetryCount(payment.getRetryCount() + 1);
                logEntry.setResult("FAILED");
                logEntry.setRecoveredAmount(0.0);
            }
            logEntry.setDetails("Attempt #" + (payment.getRetryCount()));
        }

        actionRepo.save(action);
        paymentRepo.save(payment);
        auditRepo.save(logEntry);

        log.info("Recovery {} for {}: result={}", decision.getSelectedAction(), paymentId, action.getActualResult());
        return action;
    }

    private String checkStoppingRules(Payment payment, int retryCount) {
        if (retryCount >= MAX_RETRIES) return "MAX_RETRIES_REACHED";
        if ("HARD".equals(payment.getFailureCategory())) return "HARD_FAILURE_STOP";
        return null;
    }
}
