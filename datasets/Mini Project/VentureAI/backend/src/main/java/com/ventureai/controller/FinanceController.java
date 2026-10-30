package com.ventureai.controller;

import com.ventureai.dto.common.MessageResponse;
import com.ventureai.dto.finance.FinancialAssumptionsRequest;
import com.ventureai.dto.finance.FinancialDashboardResponse;
import com.ventureai.dto.finance.FinancialDataRequest;
import com.ventureai.service.FinanceQueryService;
import com.ventureai.util.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceQueryService financeQueryService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{startupId}")
    public ResponseEntity<MessageResponse> update(@PathVariable Long startupId, @RequestBody FinancialDataRequest request) {
        financeQueryService.updateFinancialData(currentUserProvider.getCurrentUser(), startupId, request);
        return ResponseEntity.ok(new MessageResponse("Financial data updated."));
    }

    @GetMapping("/{startupId}")
    public ResponseEntity<FinancialDashboardResponse> get(@PathVariable Long startupId) {
        return ResponseEntity.ok(financeQueryService.getDashboard(currentUserProvider.getCurrentUser(), startupId));
    }

    @PostMapping("/{startupId}/recalculate")
    public ResponseEntity<FinancialDashboardResponse> recalculate(@PathVariable Long startupId, @RequestBody FinancialAssumptionsRequest request) {
        return ResponseEntity.ok(financeQueryService.recalculate(currentUserProvider.getCurrentUser(), startupId, request));
    }
}
