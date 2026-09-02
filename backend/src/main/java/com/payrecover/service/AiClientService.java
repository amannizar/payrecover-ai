package com.payrecover.service;

import com.payrecover.config.AppConfig;
import com.payrecover.dto.AiRequest;
import com.payrecover.dto.DiagnosisResponse;
import com.payrecover.dto.ProbabilityResponse;
import com.payrecover.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiClientService {

    private static final Logger log = LoggerFactory.getLogger(AiClientService.class);
    private final RestTemplate restTemplate;
    private final AppConfig appConfig;

    public AiClientService(RestTemplate restTemplate, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
    }

    public DiagnosisResponse diagnose(Payment payment) {
        AiRequest req = buildRequest(payment);
        String url = appConfig.getAiServiceUrl() + "/api/ai/diagnose";
        log.info("Calling AI diagnose for {}", payment.getPaymentId());
        return restTemplate.postForObject(url, req, DiagnosisResponse.class);
    }

    public ProbabilityResponse getRecoveryProbabilities(Payment payment) {
        AiRequest req = buildRequest(payment);
        String url = appConfig.getAiServiceUrl() + "/api/ai/recovery-probability";
        log.info("Calling AI recovery-probability for {}", payment.getPaymentId());
        return restTemplate.postForObject(url, req, ProbabilityResponse.class);
    }

    private AiRequest buildRequest(Payment p) {
        AiRequest req = new AiRequest();
        req.setPaymentMethod(p.getPaymentMethod());
        req.setBank(p.getBank());
        req.setMerchantCategory(p.getMerchantCategory());
        req.setFailureCode(p.getFailureCode());
        req.setDeviceType(p.getDeviceType());
        req.setNetworkType(p.getNetworkType());
        req.setLocationZone(p.getLocationZone());
        req.setDayOfWeek(p.getDayOfWeek());
        req.setSubscriptionStatus(p.getSubscriptionStatus());
        req.setAmount(p.getAmount().doubleValue());
        req.setRetryCount(p.getRetryCount());
        req.setGatewayLatencyMs(p.getGatewayLatencyMs());
        req.setCustomerSuccessRate(p.getCustomerSuccessRate());
        req.setPreviousFailures30d(p.getPreviousFailures30d());
        req.setPreviousSuccessfulRetries(p.getPreviousSuccessfulRetries());
        req.setCustomerTenureDays(p.getCustomerTenureDays());
        req.setHour(p.getHour());
        return req;
    }
}
