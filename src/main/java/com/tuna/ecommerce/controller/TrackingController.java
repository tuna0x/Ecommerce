package com.tuna.ecommerce.controller;

import com.tuna.ecommerce.domain.UserActivityLog;
import com.tuna.ecommerce.domain.request.tracking.ReqTrackingDTO;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/tracking/log")
    @APIMessage("System logged tracking event")
    public ResponseEntity<Void> logActivity(@RequestBody ReqTrackingDTO request, HttpServletRequest httpRequest) {
        String currentUser = SecurityUtil.getCurrentUserLogin().orElse("anonymous");
        String ipAddress = httpRequest.getRemoteAddr();

        trackingService.logActivity(currentUser, ipAddress, request.getActionType(), request.getMetadata());

        // Respond immediately, processing is async
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tracking/logs")
    @APIMessage("Get all tracking logs successfully")
    public ResponseEntity<ResultPaginationDTO> getAllTracking(
            @Filter Specification<UserActivityLog> spec,
            Pageable pageable) {
        return ResponseEntity.ok().body(this.trackingService.handleGetAll(spec, pageable));
    }
}
