package com.payrecover.dto;

public class ProbabilityResponse {
    private double retryNow;
    private double retryLater;
    private double switchMethod;
    private double notification;
    private double stop;

    public double getRetryNow() { return retryNow; }
    public void setRetryNow(double retryNow) { this.retryNow = retryNow; }
    public double getRetryLater() { return retryLater; }
    public void setRetryLater(double retryLater) { this.retryLater = retryLater; }
    public double getSwitchMethod() { return switchMethod; }
    public void setSwitchMethod(double switchMethod) { this.switchMethod = switchMethod; }
    public double getNotification() { return notification; }
    public void setNotification(double notification) { this.notification = notification; }
    public double getStop() { return stop; }
    public void setStop(double stop) { this.stop = stop; }

    public double getProb(String action) {
        return switch (action) {
            case "RETRY_NOW" -> retryNow;
            case "RETRY_LATER" -> retryLater;
            case "SWITCH_METHOD" -> switchMethod;
            case "SEND_NOTIFICATION" -> notification;
            case "STOP" -> stop;
            default -> 0.0;
        };
    }
}
