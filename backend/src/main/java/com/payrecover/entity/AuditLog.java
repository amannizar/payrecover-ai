package com.payrecover.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String paymentId;

    private String action;
    private String result;
    private String details;
    private String stoppingReason;
    private double recoveredAmount;
    private double expectedRecovery;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStoppingReason() { return stoppingReason; }
    public void setStoppingReason(String stoppingReason) { this.stoppingReason = stoppingReason; }

    public double getRecoveredAmount() { return recoveredAmount; }
    public void setRecoveredAmount(double recoveredAmount) { this.recoveredAmount = recoveredAmount; }

    public double getExpectedRecovery() { return expectedRecovery; }
    public void setExpectedRecovery(double expectedRecovery) { this.expectedRecovery = expectedRecovery; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
