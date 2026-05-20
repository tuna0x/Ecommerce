package com.tuna.ecommerce.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.config.RabbitMQConfig;
import com.tuna.ecommerce.domain.message.CheckoutMessage;
import com.tuna.ecommerce.service.CheckoutAsyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutConsumer {
    private final CheckoutAsyncService checkoutAsyncService;

    @RabbitListener(
            queues = RabbitMQConfig.CHECKOUT_QUEUE,
            concurrency = "${checkout.rabbitmq.consumer.concurrency:2-5}")
    public void processCheckout(CheckoutMessage message) {
        log.info("Received checkout request {}", message.getRequestId());
        checkoutAsyncService.process(message.getRequestId());
    }
}
