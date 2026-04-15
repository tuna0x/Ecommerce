package com.tuna.ecommerce.service;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Subscriber;
import com.tuna.ecommerce.repository.SubscriberRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SubscriberService {
    private final SubscriberRepository subscriberRepository;
    private final EmailService emailService;

    public Subscriber handleCreate(Subscriber subscriber) throws IdInvalidException {
        if (this.subscriberRepository.existsByEmail(subscriber.getEmail())) {
            throw new IdInvalidException("Email này đã đăng ký trước đó rồi!");
        }
        Subscriber res = this.subscriberRepository.save(subscriber);
        this.emailService.sendWelcomeNewsletterEmail(res.getEmail());
        return res;
    }
}
