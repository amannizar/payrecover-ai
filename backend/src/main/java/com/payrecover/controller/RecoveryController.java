package com.payrecover.controller;

import com.payrecover.dto.RecoveryDecision;
import com.payrecover.entity.RecoveryAction;
import com.payrecover.service.RecoveryAgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final RecoveryAgentService recoveryAgent;

    public RecoveryController(RecoveryAgentService recoveryAgent) {
        this.recoveryAgent = recoveryAgent;
    }

    @PostMapping("/decide/{paymentId}")
    public ResponseEntity<RecoveryDecision> decide(@PathVariable String paymentId) {
        return ResponseEntity.ok(recoveryAgent.decide(paymentId));
    }

    @PostMapping("/execute/{paymentId}")
    public ResponseEntity<RecoveryAction> execute(@PathVariable String paymentId) {
        return ResponseEntity.ok(recoveryAgent.executeRecovery(paymentId));
    }
}
