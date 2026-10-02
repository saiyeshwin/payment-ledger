package com.paymentledger.reporting.controller;

import com.paymentledger.reporting.dto.ReportSummaryResponse;
import com.paymentledger.reporting.security.UserPrincipal;
import com.paymentledger.reporting.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryResponse> getSummary(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReportSummaryResponse summary = reportingService.getReportSummary(userPrincipal.userId());
        return ResponseEntity.ok(summary);
    }
}
