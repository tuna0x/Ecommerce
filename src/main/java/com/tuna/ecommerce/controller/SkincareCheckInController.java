package com.tuna.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.SkincareCheckIn;
import com.tuna.ecommerce.service.SkincareCheckInService;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.anotation.APIMessage;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class SkincareCheckInController {

    private final SkincareCheckInService skincareCheckInService;

    @GetMapping("/skincare-checkin")
    @APIMessage("Get skincare check-in state")
    public ResponseEntity<SkincareCheckIn> getCheckInState() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập để xem thông tin điểm danh"));
        return ResponseEntity.ok(skincareCheckInService.getOrCreateCheckIn(email));
    }

    @PostMapping("/skincare-checkin")
    @APIMessage("Perform daily skincare check-in")
    public ResponseEntity<SkincareCheckIn> checkIn() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập để thực hiện điểm danh"));
        return ResponseEntity.ok(skincareCheckInService.checkIn(email));
    }

    @PostMapping("/skincare-checkin/claim/{milestoneId}")
    @APIMessage("Claim milestone reward voucher")
    public ResponseEntity<SkincareCheckIn> claimMilestone(@PathVariable("milestoneId") String milestoneId) {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập để nhận thưởng"));
        return ResponseEntity.ok(skincareCheckInService.claimMilestone(email, milestoneId));
    }
}
