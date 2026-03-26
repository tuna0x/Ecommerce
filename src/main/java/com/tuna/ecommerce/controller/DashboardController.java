package com.tuna.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.response.dashboard.ResDashboardDTO;
import com.tuna.ecommerce.service.DashboardService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @APIMessage("Lấy dữ liệu thống kê thành công")
    public ResponseEntity<ResDashboardDTO> getStatistics() {
        return ResponseEntity.ok().body(dashboardService.getStatistics());
    }
}
