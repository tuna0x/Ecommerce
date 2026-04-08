package com.tuna.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.response.dashboard.ResDashboardDTO;
import com.tuna.ecommerce.service.DashboardService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.tuna.ecommerce.service.DashboardExcelService;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardExcelService excelService;

    @GetMapping("/dashboard/statistics")
    @APIMessage("Lấy dữ liệu thống kê thành công")
    public ResponseEntity<ResDashboardDTO> getStatistics(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Instant startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Instant endDate) {

        if (startDate == null) {
            startDate = Instant.now().minus(180, ChronoUnit.DAYS); // Mặc định 6 tháng
        }
        if (endDate == null) {
            endDate = Instant.now();
        }

        return ResponseEntity.ok().body(dashboardService.getStatistics(startDate, endDate));
    }

    @GetMapping("/dashboard/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Instant startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Instant endDate) throws IOException {

        if (startDate == null) {
            startDate = Instant.now().minus(180, ChronoUnit.DAYS);
        }
        if (endDate == null) {
            endDate = Instant.now();
        }

        ResDashboardDTO data = dashboardService.getStatistics(startDate, endDate);
        byte[] excelContent = excelService.exportStatisticsToExcel(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dashboard-statistics.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }
}
