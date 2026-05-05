package com.tuna.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

import com.tuna.ecommerce.ultil.err.IdInvalidException;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    public OtpService(StringRedisTemplate redisTemplate, EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    private static final String OTP_PREFIX = "OTP_";
    private static final String OTP_COOLDOWN_PREFIX = "OTP_COOLDOWN_";

    public String generateAndSendOtp(String email) throws IdInvalidException {
        if (email != null) email = email.trim().toLowerCase();
        
        // 0. Check Cooldown (Anti-spam)
        String cooldownKey = OTP_COOLDOWN_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new IdInvalidException("Vui lòng đợi 1 phút trước khi yêu cầu mã mới.");
        }

        // 1. Generate 6-digit code
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2. Save to Redis (TTL 5 minutes for OTP, 1 minute for Cooldown)
        try {
            redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, Duration.ofMinutes(5));
            redisTemplate.opsForValue().set(cooldownKey, "lock", Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("FAILED to save OTP/Cooldown to Redis: {}", e.getMessage());
        }

        // 3. Send via Email
        emailService.sendOtpEmail(email, otp);

        return otp;
    }

    public boolean verifyOtp(String email, String otpEntered) {
        if (email != null) email = email.trim().toLowerCase();
        if (otpEntered != null) otpEntered = otpEntered.trim();
        
        String key = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otpEntered)) {
            // Success: Remove OTP from Redis
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
