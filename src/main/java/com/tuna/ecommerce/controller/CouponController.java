package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.request.coupon.ReqCreateCouponDTO;
import com.tuna.ecommerce.domain.request.coupon.ReqUpdateCouponDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.CouponService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;




@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @PostMapping("/coupons")
    @APIMessage("Create coupon successfully")
    public ResponseEntity<Coupon> createCoupon(@RequestBody ReqCreateCouponDTO reqCreateCouponDTO) {
        return ResponseEntity.ok().body(this.couponService.createCoupon(reqCreateCouponDTO));
    }

    @PutMapping("/coupons")
    @APIMessage("Update coupon successfully")
    public ResponseEntity<Coupon> updateCoupon(@RequestBody ReqUpdateCouponDTO reqUpdateCouponDTO) throws IdInvalidException {
        Coupon coupon= this.couponService.getById(reqUpdateCouponDTO.getId());
        if(coupon==null){
            throw new IdInvalidException("coupon not found");
        }
        return ResponseEntity.ok().body(this.couponService.updateCoupon(reqUpdateCouponDTO));
    }

    @GetMapping("/coupons/{id}")
    @APIMessage("Get coupon successfully")
    public ResponseEntity<Coupon> getCouponById(@PathVariable("id") Long id) throws IdInvalidException {
        Coupon coupon = this.couponService.getById(id);
        if(coupon==null){
            throw new IdInvalidException("coupon not found");
        }
        return ResponseEntity.ok().body(coupon);
    }

    @GetMapping("/coupons")
    @APIMessage("Get all coupons successfully")
    public ResponseEntity<ResultPaginationDTO> getAllCoupon(@Filter Specification<Coupon> spec, Pageable page) throws IdInvalidException {
        return ResponseEntity.ok().body(this.couponService.handleGetAll(spec, page));
    }

}
