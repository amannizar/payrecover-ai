package com.payrecover.dto;

import java.util.Map;

public class RecoveryDecision {
    private String paymentId;
    private String failureCategory;
    private String selectedAction;
    private double probability;
    private double expectedRecovery;
    private Map<String, Double> allProbabilities;
    private String stoppingReason;

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getFailureCategory() { return failureCategory; }
    public void setFailureCategory(String failureCategory) { this.failureCategory = failureCategory; }
    public String getSelectedAction() { return selectedAction; }
    public void setSelectedAction(String selectedAction) { this.selectedAction = selectedAction; }
    public double getProbability() { return probability; }
    public void setProbability(double probability) { this.probability = probability; }
    public double getExpectedRecovery() { return expectedRecovery; }
    public void setExpectedRecovery(double expectedRecovery) { this.expectedRecovery = expectedRecovery; }
    public Map<String, Double> getAllProbabilities() { return allProbabilities; }
    public void setAllProbabilities(Map<String, Double> allProbabilities) { this.allProbabilities = allProbabilities; }
    public String getStoppingReason() { return stoppingReason; }
    public void setStoppingReason(String stoppingReason) { this.stoppingReason = stoppingReason; }
}
