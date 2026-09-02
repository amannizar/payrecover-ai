package com.payrecover.dto;

/**
 * Request to the FastAPI AI service for diagnosis / probability prediction.
 * Fields use camelCase to match the Python API contract.
 */
public class AiRequest {
    private String paymentMethod;
    private String bank;
    private String merchantCategory;
    private String failureCode;
    private String deviceType;
    private String networkType;
    private String locationZone;
    private String dayOfWeek;
    private String subscriptionStatus;
    private double amount;
    private int retryCount;
    private int gatewayLatencyMs;
    private double customerSuccessRate;
    private int previousFailures30d;
    private int previousSuccessfulRetries;
    private int customerTenureDays;
    private int hour;

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }
    public String getMerchantCategory() { return merchantCategory; }
    public void setMerchantCategory(String merchantCategory) { this.merchantCategory = merchantCategory; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getNetworkType() { return networkType; }
    public void setNetworkType(String networkType) { this.networkType = networkType; }
    public String getLocationZone() { return locationZone; }
    public void setLocationZone(String locationZone) { this.locationZone = locationZone; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
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
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
}
