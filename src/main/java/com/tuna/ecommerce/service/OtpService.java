package com.tuna.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

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

    public String generateAndSendOtp(String email) {
        log.info("START: Generating OTP for email: {}", email);
        // 1. Generate 6-digit code
        String otp = String.format("%06d", new Random().nextInt(999999));
        log.info("GENERATED OTP: {}", otp);

        // 2. Save to Redis (TTL 5 minutes)
        try {
            log.info("SAVING OTP to Redis for key: {}", OTP_PREFIX + email);
            redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, Duration.ofMinutes(5));
            log.info("SUCCESS: OTP saved to Redis");
        } catch (Exception e) {
            log.error("FAILED to save OTP to Redis: {}", e.getMessage());
            // We still proceed to send email, or maybe throw error based on your preference
            // For now, let's just log it to see if this is the cause
        }

        // 3. Send via Email
        log.info("CALLING EmailService to send OTP to: {}", email);
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
