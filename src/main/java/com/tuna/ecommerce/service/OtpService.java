package com.tuna.ecommerce.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    public OtpService(StringRedisTemplate redisTemplate, EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    private static final String OTP_PREFIX = "OTP_";

    public String generateAndSendOtp(String email) {
        // 1. Generate 6-digit code
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2. Save to Redis (TTL 5 minutes)
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, Duration.ofMinutes(5));

        // 3. Send via Email
        emailService.sendOtpEmail(email, otp);

        return otp;
    }

    public boolean verifyOtp(String email, String otpEntered) {
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
