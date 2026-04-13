package com.tuna.ecommerce.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender javaMailSender, SpringTemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmailSync(String to, String subject, String content, boolean isHtml) {
        log.info("PREPARING MAIL to: {}, subject: {}", to, subject);
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, 
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                StandardCharsets.UTF_8.name());

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            javaMailSender.send(message);
            log.info("SUCCESS: Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("FAILED to send email to {}. Error: {}", to, e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendOtpEmail(String to, String otp) {
        log.info("GENERATING OTP Email for: {}", to);
        String subject = "Mã xác thực OTP - Bông Cosmetic";
        
        Context context = new Context();
        context.setVariable("otp", otp);
        String content = templateEngine.process("email/otp-email", context);
        
        log.info("TEMPLATE RENDERED for OTP email to: {}", to);
        this.sendEmailSync(to, subject, content, true);
    }
}
