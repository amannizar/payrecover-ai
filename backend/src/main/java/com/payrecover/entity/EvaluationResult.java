package com.payrecover.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_results")
public class EvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String strategy;

    private int totalPayments;
    private BigDecimal revenueAtRisk;
    private BigDecimal recoveredRevenue;
    private double recoveryRate;
    private double revenueRecoveryRate;
    private int totalAttempts;
    private double efficiency;
    private BigDecimal improvementOverBlindRetry;
    private BigDecimal improvementOverRuleBased;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public int getTotalPayments() { return totalPayments; }
    public void setTotalPayments(int totalPayments) { this.totalPayments = totalPayments; }

    public BigDecimal getRevenueAtRisk() { return revenueAtRisk; }
    public void setRevenueAtRisk(BigDecimal revenueAtRisk) { this.revenueAtRisk = revenueAtRisk; }

    public BigDecimal getRecoveredRevenue() { return recoveredRevenue; }
    public void setRecoveredRevenue(BigDecimal recoveredRevenue) { this.recoveredRevenue = recoveredRevenue; }

    public double getRecoveryRate() { return recoveryRate; }
    public void setRecoveryRate(double recoveryRate) { this.recoveryRate = recoveryRate; }

    public double getRevenueRecoveryRate() { return revenueRecoveryRate; }
    public void setRevenueRecoveryRate(double revenueRecoveryRate) { this.revenueRecoveryRate = revenueRecoveryRate; }

    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }

    public double getEfficiency() { return efficiency; }
    public void setEfficiency(double efficiency) { this.efficiency = efficiency; }

    public BigDecimal getImprovementOverBlindRetry() { return improvementOverBlindRetry; }
    public void setImprovementOverBlindRetry(BigDecimal improvementOverBlindRetry) { this.improvementOverBlindRetry = improvementOverBlindRetry; }

    public BigDecimal getImprovementOverRuleBased() { return improvementOverRuleBased; }
    public void setImprovementOverRuleBased(BigDecimal improvementOverRuleBased) { this.improvementOverRuleBased = improvementOverRuleBased; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
