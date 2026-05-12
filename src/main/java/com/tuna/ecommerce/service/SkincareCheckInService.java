package com.tuna.ecommerce.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.SkincareCheckIn;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserCoupon;
import com.tuna.ecommerce.repository.CouponRepository;
import com.tuna.ecommerce.repository.SkincareCheckInRepository;
import com.tuna.ecommerce.repository.UserCouponRepository;
import com.tuna.ecommerce.repository.UserRepository;

@Service
@Transactional
public class SkincareCheckInService {

    private final SkincareCheckInRepository skincareCheckInRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public SkincareCheckInService(SkincareCheckInRepository skincareCheckInRepository, 
                                  UserRepository userRepository,
                                  CouponRepository couponRepository,
                                  UserCouponRepository userCouponRepository) {
        this.skincareCheckInRepository = skincareCheckInRepository;
        this.userRepository = userRepository;
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    public SkincareCheckIn getOrCreateCheckIn(String email) {
        Optional<SkincareCheckIn> existing = skincareCheckInRepository.findByUserEmail(email);
        if (existing.isPresent()) {
            SkincareCheckIn checkIn = existing.get();
            validateAndResetStreak(checkIn);
            return checkIn;
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }

        SkincareCheckIn checkIn = new SkincareCheckIn();
        checkIn.setUser(user);
        checkIn.setStreak(0);
        checkIn.setLastCheckIn(null);
        checkIn.setHistory(new ArrayList<>());
        checkIn.setClaimedMilestones(new ArrayList<>());

        return skincareCheckInRepository.save(checkIn);
    }

    private void validateAndResetStreak(SkincareCheckIn checkIn) {
        if (checkIn.getLastCheckIn() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (!checkIn.getLastCheckIn().equals(today) && !checkIn.getLastCheckIn().equals(yesterday)) {
            // Broken streak!
            checkIn.setStreak(0);
            skincareCheckInRepository.save(checkIn);
        }
    }

    public SkincareCheckIn checkIn(String email) {
        SkincareCheckIn checkIn = getOrCreateCheckIn(email);
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        if (checkIn.getLastCheckIn() != null && checkIn.getLastCheckIn().equals(today)) {
            return checkIn; // Already checked in today
        }

        checkIn.getHistory().add(todayStr);
        checkIn.setStreak(checkIn.getStreak() + 1);
        checkIn.setLastCheckIn(today);

        return skincareCheckInRepository.save(checkIn);
    }

    public SkincareCheckIn claimMilestone(String email, String milestoneId) {
        SkincareCheckIn checkIn = getOrCreateCheckIn(email);
        if (checkIn.getClaimedMilestones().contains(milestoneId)) {
            return checkIn; // Already claimed
        }

        // Map milestone ID to specific coupon code
        String couponCode = null;
        if ("streak_3".equals(milestoneId)) {
            couponCode = "BONGBUDS";
        } else if ("streak_7".equals(milestoneId)) {
            couponCode = "BONGXINH10";
        } else if ("streak_15".equals(milestoneId)) {
            couponCode = "BONGFREESHIP";
        } else if ("streak_30".equals(milestoneId)) {
            couponCode = "BONGVIP50K";
        }

        // Automatically assign coupon to user's wallet if it exists
        if (couponCode != null) {
            Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);
            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                User user = checkIn.getUser();
                if (user != null && !userCouponRepository.existsByUserAndCoupon(user, coupon)) {
                    UserCoupon userCoupon = new UserCoupon();
                    userCoupon.setUser(user);
                    userCoupon.setCoupon(coupon);
                    userCouponRepository.save(userCoupon);
                }
            }
        }

        checkIn.getClaimedMilestones().add(milestoneId);
        return skincareCheckInRepository.save(checkIn);
    }
}
