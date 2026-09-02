package com.payrecover.controller;

import com.payrecover.entity.Payment;
import com.payrecover.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments")
    public String payments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            Model model) {
        List<Payment> payments;
        if (q != null && !q.isBlank()) {
            payments = paymentService.search(q);
        } else if (status != null && !status.isBlank()) {
            payments = paymentService.getFailed();
        } else {
            payments = paymentService.getAll();
        }
        model.addAttribute("payments", payments);
        model.addAttribute("query", q);
        return "payments";
    }

    @GetMapping("/payments/{paymentId}")
    public String paymentDetail(@PathVariable String paymentId, Model model) {
        Payment payment = paymentService.getByPaymentId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        model.addAttribute("payment", payment);
        return "payment-detail";
    }
}
