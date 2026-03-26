package com.tuna.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.request.coupon.ReqCreateCouponDTO;
import com.tuna.ecommerce.domain.request.coupon.ReqUpdateCouponDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.CouponRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class CouponService {
    private final CouponRepository couponRepository;

    public boolean existsByCode(String code) {
        return this.couponRepository.existsByCode(code);
    }

    public Coupon getByCode(String code) {
        return this.couponRepository.findByCode(code).orElse(null);
    }

    public Coupon createCoupon(ReqCreateCouponDTO req) {
        Coupon newCoupon = new Coupon();
        newCoupon.setType(req.getType());
        newCoupon.setValue(req.getValue());
        newCoupon.setStartDate(req.getStartDate());
        newCoupon.setEndDate(req.getEndDate());
        newCoupon.setMinOrderValue(req.getMinOrderValue());
        newCoupon.setMaxDiscountValue(req.getMaxDiscountValue());
        newCoupon.setUsageLimit(req.getUsageLimit());
        newCoupon.setUsedCount(0);
        newCoupon.setStatus(req.getStatus());
        newCoupon.setPublic(req.isPublic());
        
        // If code is provided in DTO, use it. Otherwise, @PrePersist will handle it.
        if (req.getCode() != null && !req.getCode().isBlank()) {
            newCoupon.setCode(req.getCode().toUpperCase());
        }
        
        return this.couponRepository.save(newCoupon);
    }

    public void deleteCoupon(Long id) {
        this.couponRepository.deleteById(id);
    }

    public Coupon getById(Long id) {
        return this.couponRepository.findById(id).orElse(null);
    }

    public Coupon updateCoupon(ReqUpdateCouponDTO req) {
        Coupon cur = this.getById(req.getId());
        if (cur != null) {
            cur.setType(req.getType());
            cur.setValue(req.getValue());
            cur.setStartDate(req.getStartDate());
            cur.setEndDate(req.getEndDate());
            cur.setMinOrderValue(req.getMinOrderValue());
            cur.setMaxDiscountValue(req.getMaxDiscountValue());
            cur.setUsageLimit(req.getUsageLimit());
            cur.setStatus(req.getStatus());
            cur.setPublic(req.isPublic());
            if (req.getCode() != null && !req.getCode().isBlank()) {
                cur.setCode(req.getCode().toUpperCase());
            }
            return this.couponRepository.save(cur);
        }
        return null;
    }

    public ResultPaginationDTO handleGetAll(Specification<Coupon> spec, Pageable page) {
        Page<Coupon> couponPage = this.couponRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(couponPage.getNumber() + 1);
        meta.setPageSize(couponPage.getSize());
        meta.setPages(couponPage.getTotalPages());
        meta.setTotal(couponPage.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(couponPage.getContent());
        return rs;
    }
}
