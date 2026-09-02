package com.payrecover.controller;

import com.payrecover.service.DashboardService;
import com.payrecover.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final PaymentService paymentService;

    @Value("${payrecover.demo.reset-enabled:false}")
    private boolean resetEnabled;

    public DashboardController(DashboardService dashboardService, PaymentService paymentService) {
        this.dashboardService = dashboardService;
        this.paymentService = paymentService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> metrics = dashboardService.getMetrics();
        model.addAllAttributes(metrics);
        model.addAttribute("resetEnabled", resetEnabled);
        return "dashboard";
    }

    @GetMapping("/evaluation")
    public String evaluation(Model model) {
        model.addAttribute("totalPayments", 1500);
        return "evaluation";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        return "audit";
    }
}
