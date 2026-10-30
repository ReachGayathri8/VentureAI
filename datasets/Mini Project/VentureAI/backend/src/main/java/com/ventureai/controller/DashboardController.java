package com.ventureai.controller;

import com.ventureai.dto.dashboard.DashboardResponse;
import com.ventureai.service.DashboardService;
import com.ventureai.util.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<DashboardResponse> get() {
        return ResponseEntity.ok(dashboardService.getDashboard(currentUserProvider.getCurrentUser()));
    }
}
