package com.tuna.ecommerce.service;

import javax.naming.spi.DirStateFactory.Result;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.request.coupon.ReqCreateCouponDTO;
import com.tuna.ecommerce.domain.request.coupon.ReqUpdateCouponDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.CouponRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;

    public boolean existsByCode(String code) {
        return this.couponRepository.existsByCode(code);
    }

    public Coupon getByCode(String code) {
        return this.couponRepository.findByCode(code).orElse(null);
    }

    public Coupon createCoupon(ReqCreateCouponDTO coupon) {
        Coupon newCoupon=new Coupon();
        newCoupon.setType(coupon.getType());
        newCoupon.setValue(coupon.getValue());
        newCoupon.setStartDate(coupon.getStartDate());
        newCoupon.setEndDate(coupon.getEndDate());
        newCoupon.setMinOrderValue(coupon.getMinOrderValue());
        newCoupon.setMaxDiscountValue(coupon.getMaxDiscountValue());
        newCoupon.setUsageLimit(coupon.getUsageLimit());
        newCoupon.setUsedCount(0);
        newCoupon.setStatus(coupon.getStatus());
        newCoupon.setPublic(coupon.isPublic());
        return this.couponRepository.save(newCoupon);
    }
    public void deleteCoupon(Long id) {
        this.couponRepository.deleteById(id);
    }
    public Coupon getById(Long id) {
        return this.couponRepository.findById(id).orElse(null);
    }

    public Coupon updateCoupon(ReqUpdateCouponDTO coupon) {
        Coupon cur=this.getById(coupon.getId());
        if(cur==null){
            cur.setType(coupon.getType());
            cur.setValue(coupon.getValue());
            cur.setStartDate(coupon.getStartDate());
            cur.setEndDate(coupon.getEndDate());
            cur.setMinOrderValue(coupon.getMinOrderValue());
            cur.setMaxDiscountValue(coupon.getMaxDiscountValue());
            cur.setUsageLimit(coupon.getUsageLimit());
            cur.setStatus(coupon.getStatus());
            cur.setPublic(coupon.isPublic());
        }
        return this.couponRepository.save(cur);
    }

     public ResultPaginationDTO handleGetAll(Specification<Coupon> spec,Pageable page){
         Page<Coupon> coupon= this.couponRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(coupon.getNumber() + 1);
        meta.setPageSize(coupon.getSize());
        meta.setPages(coupon.getTotalPages());
        meta.setTotal(coupon.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(coupon.getContent());
        return rs;
    }

}
