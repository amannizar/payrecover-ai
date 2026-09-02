package com.payrecover.service;

import com.payrecover.entity.Payment;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates payment recovery outcomes.
 * Uses ground-truth recovery probabilities baked into the synthetic data.
 */
@Service
public class SimulatorService {

    private static final Map<String, Map<String, Double>> BASE_PROBS = Map.of(
        "TEMPORARY", Map.of(
            "RETRY_NOW", 0.55, "RETRY_LATER", 0.82,
            "SWITCH_METHOD", 0.61, "SEND_NOTIFICATION", 0.20, "STOP", 0.00
        ),
        "CUSTOMER", Map.of(
            "RETRY_NOW", 0.18, "RETRY_LATER", 0.44,
            "SWITCH_METHOD", 0.58, "SEND_NOTIFICATION", 0.72, "STOP", 0.05
        ),
        "HARD", Map.of(
            "RETRY_NOW", 0.02, "RETRY_LATER", 0.03,
            "SWITCH_METHOD", 0.10, "SEND_NOTIFICATION", 0.12, "STOP", 0.99
        )
    );

    /**
     * Simulate whether a recovery action succeeds for this payment.
     * Returns true if recovery succeeds, false otherwise.
     */
    public boolean simulate(Payment payment, String action) {
        String category = payment.getFailureCategory();
        double baseProb = BASE_PROBS
            .getOrDefault(category, Map.of())
            .getOrDefault(action, 0.0);

        // Customer quality boost
        double customerBoost = (payment.getCustomerSuccessRate() - 0.70) * 0.15;
        double finalProb = Math.min(0.99, Math.max(0.0, baseProb + customerBoost));

        return ThreadLocalRandom.current().nextDouble() < finalProb;
    }
}
