package com.paymentledger.ledger.controller;

import com.paymentledger.ledger.dto.LedgerEntryResponse;
import com.paymentledger.ledger.security.UserPrincipal;
import com.paymentledger.ledger.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/")
    public ResponseEntity<List<LedgerEntryResponse>> getMyLedgerEntries(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<LedgerEntryResponse> entries = ledgerService.getLedgerEntriesByUser(userPrincipal.userId());
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/expense/{expenseId}")
    public ResponseEntity<List<LedgerEntryResponse>> getLedgerEntriesForExpense(@PathVariable UUID expenseId) {
        List<LedgerEntryResponse> entries = ledgerService.getLedgerEntriesByExpense(expenseId);
        return ResponseEntity.ok(entries);
    }
}
