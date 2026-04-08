package com.tuna.ecommerce.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.response.usercoupon.ResUserCouponDTO;
import com.tuna.ecommerce.service.UserCouponService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/user-coupons")
@AllArgsConstructor
public class UserCouponController {
    private final UserCouponService userCouponService;

    @PostMapping("/collect/{id}")
    @APIMessage("Collect coupon successfully")
    public ResponseEntity<ResUserCouponDTO> collectCoupon(@PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(userCouponService.collectCoupon(id));
    }

    @GetMapping("/my")
    @APIMessage("Get my coupons successfully")
    public ResponseEntity<List<ResUserCouponDTO>> getMyCoupons() throws IdInvalidException {
        return ResponseEntity.ok(userCouponService.getMyCoupons());
    }

    @GetMapping("/available")
    @APIMessage("Get available coupons successfully")
    public ResponseEntity<List<Coupon>> getAvailableCoupons() throws IdInvalidException {
        return ResponseEntity.ok(userCouponService.getAvailableCoupons());
    }
}
