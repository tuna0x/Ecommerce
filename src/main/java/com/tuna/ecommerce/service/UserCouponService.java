package com.tuna.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserCoupon;
import com.tuna.ecommerce.repository.CouponRepository;
import com.tuna.ecommerce.repository.UserCouponRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.domain.response.usercoupon.ResUserCouponDTO;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class UserCouponService {
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    public ResUserCouponDTO collectCoupon(Long couponId) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) throw new IdInvalidException("User not authenticated");

        User user = userRepository.findByEmail(email);
        Coupon coupon = couponRepository.findById(couponId).orElse(null);

        if (user == null) throw new IdInvalidException("User not found");
        if (coupon == null) throw new IdInvalidException("Coupon not found");

        if (userCouponRepository.existsByUserAndCoupon(user, coupon)) {
            throw new IdInvalidException("You have already collected this coupon");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUser(user);
        userCoupon.setCoupon(coupon);
        UserCoupon saved = userCouponRepository.saveAndFlush(userCoupon);
        return this.mapToDTO(saved);
    }

    public List<ResUserCouponDTO> getMyCoupons() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) throw new IdInvalidException("User not authenticated");

        User user = userRepository.findByEmail(email);
        if (user == null) throw new IdInvalidException("User not found");

        return userCouponRepository.findByUserOrderByCollectedAtDesc(user)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<Coupon> getAvailableCoupons() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) throw new IdInvalidException("User not authenticated");

        User user = userRepository.findByEmail(email);
        if (user == null) throw new IdInvalidException("User not found");

        // Get all public active coupons
        List<Coupon> allPublicCoupons = couponRepository.findByIsPublicTrue();
        
        // Filter out coupons user already has
        List<Long> collectedCouponIds = userCouponRepository.findByUserOrderByCollectedAtDesc(user)
                .stream().map(uc -> uc.getCoupon().getId()).collect(Collectors.toList());

        return allPublicCoupons.stream()
                .filter(c -> !collectedCouponIds.contains(c.getId()))
                .filter(c -> c.getStatus() != null && c.getStatus() == com.tuna.ecommerce.ultil.constant.CouponStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    private ResUserCouponDTO mapToDTO(UserCoupon userCoupon) {
        ResUserCouponDTO dto = new ResUserCouponDTO();
        dto.setId(userCoupon.getId());
        dto.setUsed(userCoupon.isUsed());
        dto.setCollectedAt(userCoupon.getCollectedAt());
        dto.setUsedAt(userCoupon.getUsedAt());

        if (userCoupon.getCoupon() != null) {
            Coupon c = userCoupon.getCoupon();
            ResUserCouponDTO.CouponInner couponInner = new ResUserCouponDTO.CouponInner();
            couponInner.setId(c.getId());
            couponInner.setCode(c.getCode());
            couponInner.setName(c.getName());
            couponInner.setDescription(c.getDescription());
            couponInner.setType(c.getType());
            couponInner.setDiscountValue(c.getDiscountValue());
            couponInner.setStartDate(c.getStartDate());
            couponInner.setEndDate(c.getEndDate());
            couponInner.setMinOrderValue(c.getMinOrderValue());
            couponInner.setMaxDiscountValue(c.getMaxDiscountValue());
            couponInner.setUsageLimit(c.getUsageLimit());
            couponInner.setUsedCount(c.getUsedCount());
            dto.setCoupon(couponInner);
        }

        return dto;
    }
}
