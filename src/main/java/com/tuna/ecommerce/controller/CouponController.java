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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    @APIMessage("Get coupons successfully")
    public ResponseEntity<ResultPaginationDTO> getAllCoupons(@Filter Specification<Coupon> spec, Pageable page) {
        return ResponseEntity.ok(this.couponService.handleGetAll(spec, page));
    }

    @DeleteMapping("/coupons/{id}")
    @APIMessage("Delete coupon successfully")
    public ResponseEntity<Void> deleteCoupon(@PathVariable("id") Long id) throws IdInvalidException {
        if (this.couponService.getById(id) == null) {
            throw new IdInvalidException("coupon not found");
        }
        this.couponService.deleteCoupon(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/coupons/{id}/active")
    @APIMessage("Toggle coupon active status")
    public ResponseEntity<Void> toggleStatus(@PathVariable("id") Long id, @RequestParam("active") boolean active)
            throws IdInvalidException {
        Coupon coupon = this.couponService.getById(id);
        if (coupon == null) {
            throw new IdInvalidException("coupon not found");
        }
        this.couponService.toggleStatus(id,
                active ? com.tuna.ecommerce.ultil.constant.CouponStatus.ACTIVE
                        : com.tuna.ecommerce.ultil.constant.CouponStatus.DISABLED);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/coupons/{id}/public")
    @APIMessage("Toggle coupon public status")
    public ResponseEntity<Void> togglePublic(@PathVariable("id") Long id, @RequestParam("isPublic") boolean isPublic)
            throws IdInvalidException {
        Coupon coupon = this.couponService.getById(id);
        if (coupon == null) {
            throw new IdInvalidException("coupon not found");
        }
        this.couponService.togglePublic(id, isPublic);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/coupons/validate")
    @APIMessage("Validate coupon successfully")
    public ResponseEntity<Coupon> validateCoupon(@RequestParam("code") String code) throws IdInvalidException {
        String email = com.tuna.ecommerce.ultil.SecurityUtil.getCurrentUserLogin().orElse(null);
        return ResponseEntity.ok().body(this.couponService.validateCoupon(code, email));
    }
}
