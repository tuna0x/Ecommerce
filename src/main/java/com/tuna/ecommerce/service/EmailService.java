package com.tuna.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${tuna.backend-url}")
    private String backendUrl;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.api.url}")
    private String brevoApiUrl;

    @Value("${tuna.email.sender.name}")
    private String senderName;

    @Value("${tuna.email.sender.email}")
    private String senderEmail;

    private final RestTemplate restTemplate;
    private final SpringTemplateEngine templateEngine;

    public EmailService(RestTemplate restTemplate, SpringTemplateEngine templateEngine) {
        this.restTemplate = restTemplate;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmailSync(String to, String subject, String content, boolean isHtml) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            // Xây dựng body JSON cho Brevo API
            Map<String, Object> body = new HashMap<>();
            
            // Người gửi
            Map<String, String> sender = new HashMap<>();
            sender.put("name", senderName);
            sender.put("email", senderEmail);
            body.put("sender", sender);

            // Người nhận
            Map<String, String> receiver = new HashMap<>();
            receiver.put("email", to);
            body.put("to", Collections.singletonList(receiver));

            body.put("subject", subject);
            body.put("htmlContent", content);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info(">>> Connecting to Brevo API to send email to {}...", to);
            restTemplate.postForEntity(brevoApiUrl, request, String.class);
            log.info("SUCCESS: Email sent to {} via Brevo API", to);
            
        } catch (Exception e) {
            log.error("FAILED to send email to {} via Brevo API. Error: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendOtpEmail(String to, String otp) {
        String subject = "M\u00E3 x\u00E1c th\u1EF1c OTP - B\u00F4ng Cosmetic";
        
        Context context = new Context();
        context.setVariable("otp", otp);
        String content = templateEngine.process("email/otp-email", context);
        
        this.sendEmailSync(to, subject, content, true);
    }

    @Async
    public void sendOrderConfirmationEmail(com.tuna.ecommerce.domain.Order order, boolean isCod) {
        String subject = isCod ? "Vui l\u00F2ng x\u00E1c nh\u1EADn \u0111\u01A1n h\u00E0ng c\u1EE7a b\u1EA1n - B\u00F4ng Cosmetic" : "Th\u00F4ng tin \u0111\u01A1n h\u00E0ng c\u1EE7a b\u1EA1n - B\u00F4ng Cosmetic";
        
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("isCod", isCod);
        
        if (isCod) {
            String confirmationUrl = backendUrl + "/api/v1/public/order/confirm?token=" + order.getConfirmationToken();
            context.setVariable("confirmationUrl", confirmationUrl);
        }
        
        String content = templateEngine.process("email/order-confirmation", context);
        
        this.sendEmailSync(order.getUser().getEmail(), subject, content, true);
    }

    @Async
    public void sendWelcomeNewsletterEmail(String to) {
        String subject = "Chào mừng bạn đến với Bông Cosmetic - Nhận ngay ưu đãi 20%";
        
        Context context = new Context();
        // Bạn có thể truyền thêm các biến vào đây nếu template cần
        
        String content = templateEngine.process("email/welcome-newsletter", context);
        
        this.sendEmailSync(to, subject, content, true);
    }

    @Async
    public void sendRefundNotificationEmail(com.tuna.ecommerce.domain.Order order, java.math.BigDecimal amount) {
        String subject = "Thông báo hoàn tiền đơn hàng #" + order.getId() + " - Bông Cosmetic";
        
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("amount", amount);
        
        String content = templateEngine.process("email/refund-notification", context);
        
        this.sendEmailSync(order.getUser().getEmail(), subject, content, true);
    }

    @Async
    public void sendOrderCancellationEmail(com.tuna.ecommerce.domain.Order order) {
        String subject = "Thông báo hủy đơn hàng #" + order.getId() + " - Bông Cosmetic";
        
        Context context = new Context();
        context.setVariable("order", order);
        
        String content = templateEngine.process("email/order-cancelled", context);
        
        this.sendEmailSync(order.getUser().getEmail(), subject, content, true);
    }
}
