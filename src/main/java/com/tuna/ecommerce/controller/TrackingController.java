package com.tuna.ecommerce.controller;

import com.tuna.ecommerce.domain.UserBehavior;
import com.tuna.ecommerce.domain.request.tracking.ReqBehaviorDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.TrackingService;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.turkraft.springfilter.boot.Filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/tracking/log")
    @APIMessage("System logged tracking event")
    public ResponseEntity<Void> logActivity(@RequestBody ReqBehaviorDTO request, HttpServletRequest httpRequest) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("anonymous");
        String ipAddress = httpRequest.getRemoteAddr();

        trackingService.logActivity(
                currentUser,
                ipAddress,
                request.getActionType(),
                request.getMetadata(),
                request.getSessionId(),
                request.getDeviceType(),
                request.getReferrer(),
                request.getPageUrl());

        // Respond immediately, processing is async
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tracking/logs")
    @com.tuna.ecommerce.ultil.anotation.APIMessage("Lấy danh sách nhật ký hành vi thành công")
    public ResponseEntity<com.tuna.ecommerce.domain.response.ResultPaginationDTO> getAllLogs(
            @com.turkraft.springfilter.boot.Filter org.springframework.data.jpa.domain.Specification<com.tuna.ecommerce.domain.UserBehavior> spec,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(this.trackingService.handleGetAll(spec, pageable));
    }

    @GetMapping("/tracking/analytics")
    @com.tuna.ecommerce.ultil.anotation.APIMessage("Lấy dữ liệu phân tích thành công")
    public ResponseEntity<com.tuna.ecommerce.domain.response.tracking.ResTrackingAnalyticsDTO> getAnalytics(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(this.trackingService.getAnalytics(days));
    }
}
