package com.payrecover.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_actions")
public class RecoveryAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String paymentId;

    private int retryCount;
    private String aiDiagnosis;
    private String selectedAction;
    private BigDecimal expectedRecovery;
    private BigDecimal actualRecovery;
    private String actualResult;
    private String stoppingReason;
    private int timestampStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id_fk", insertable = false, updatable = false)
    private Payment payment;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getAiDiagnosis() { return aiDiagnosis; }
    public void setAiDiagnosis(String aiDiagnosis) { this.aiDiagnosis = aiDiagnosis; }

    public String getSelectedAction() { return selectedAction; }
    public void setSelectedAction(String selectedAction) { this.selectedAction = selectedAction; }

    public BigDecimal getExpectedRecovery() { return expectedRecovery; }
    public void setExpectedRecovery(BigDecimal expectedRecovery) { this.expectedRecovery = expectedRecovery; }

    public BigDecimal getActualRecovery() { return actualRecovery; }
    public void setActualRecovery(BigDecimal actualRecovery) { this.actualRecovery = actualRecovery; }

    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }

    public String getStoppingReason() { return stoppingReason; }
    public void setStoppingReason(String stoppingReason) { this.stoppingReason = stoppingReason; }

    public int getTimestampStep() { return timestampStep; }
    public void setTimestampStep(int timestampStep) { this.timestampStep = timestampStep; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
