package com.payrecover.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentId;

    private String customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String paymentMethod;
    private String bank;
    private String merchantCategory;

    @Column(nullable = false)
    private String failureCode;

    private String failureCategory;
    private int retryCount;
    private String status;

    private int gatewayLatencyMs;
    private double customerSuccessRate;
    private int previousFailures30d;
    private int previousSuccessfulRetries;
    private int customerTenureDays;
    private String subscriptionStatus;
    @Column(name = "\"hour\"")
    private int hour;
    private String dayOfWeek;
    private String deviceType;
    private String networkType;
    private String locationZone;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureCategory() { return failureCategory; }
    public void setFailureCategory(String failureCategory) { this.failureCategory = failureCategory; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getGatewayLatencyMs() { return gatewayLatencyMs; }
    public void setGatewayLatencyMs(int gatewayLatencyMs) { this.gatewayLatencyMs = gatewayLatencyMs; }

    public double getCustomerSuccessRate() { return customerSuccessRate; }
    public void setCustomerSuccessRate(double customerSuccessRate) { this.customerSuccessRate = customerSuccessRate; }

    public int getPreviousFailures30d() { return previousFailures30d; }
    public void setPreviousFailures30d(int previousFailures30d) { this.previousFailures30d = previousFailures30d; }

    public int getPreviousSuccessfulRetries() { return previousSuccessfulRetries; }
    public void setPreviousSuccessfulRetries(int previousSuccessfulRetries) { this.previousSuccessfulRetries = previousSuccessfulRetries; }

    public int getCustomerTenureDays() { return customerTenureDays; }
    public void setCustomerTenureDays(int customerTenureDays) { this.customerTenureDays = customerTenureDays; }

    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getNetworkType() { return networkType; }
    public void setNetworkType(String networkType) { this.networkType = networkType; }

    public String getLocationZone() { return locationZone; }
    public void setLocationZone(String locationZone) { this.locationZone = locationZone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
