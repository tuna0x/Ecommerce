package com.tuna.ecommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PRODUCT_IMAGE_QUEUE = "product.image.queue";
    public static final String PRODUCT_IMAGE_EXCHANGE = "product.image.exchange";
    public static final String PRODUCT_IMAGE_ROUTING_KEY = "product.image.routing.key";

    public static final String PRODUCT_IMAGE_DLQ = "product.image.dlq";
    public static final String PRODUCT_IMAGE_DLX = "product.image.dlx";
    public static final String PRODUCT_IMAGE_DLQ_ROUTING_KEY = "product.image.dlq.routing.key";

    public static final String CHECKOUT_QUEUE = "checkout.requested.queue";
    public static final String CHECKOUT_EXCHANGE = "checkout.exchange";
    public static final String CHECKOUT_ROUTING_KEY = "checkout.requested.routing.key";

    public static final String CHECKOUT_DLQ = "checkout.requested.dlq";
    public static final String CHECKOUT_DLX = "checkout.requested.dlx";
    public static final String CHECKOUT_DLQ_ROUTING_KEY = "checkout.requested.dlq.routing.key";

    @Bean
    public Queue productImageQueue() {
        return QueueBuilder.durable(PRODUCT_IMAGE_QUEUE)
                .withArgument("x-dead-letter-exchange", PRODUCT_IMAGE_DLX)
                .withArgument("x-dead-letter-routing-key", PRODUCT_IMAGE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange productImageExchange() {
        return new DirectExchange(PRODUCT_IMAGE_EXCHANGE);
    }

    @Bean
    public Binding productImageBinding(Queue productImageQueue, DirectExchange productImageExchange) {
        return BindingBuilder.bind(productImageQueue).to(productImageExchange).with(PRODUCT_IMAGE_ROUTING_KEY);
    }

    // Dead Letter Queue and Exchange
    @Bean
    public Queue productImageDLQ() {
        return new Queue(PRODUCT_IMAGE_DLQ, true);
    }

    @Bean
    public DirectExchange productImageDLX() {
        return new DirectExchange(PRODUCT_IMAGE_DLX);
    }

    @Bean
    public Binding productImageDLQBinding(Queue productImageDLQ, DirectExchange productImageDLX) {
        return BindingBuilder.bind(productImageDLQ).to(productImageDLX).with(PRODUCT_IMAGE_DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue checkoutQueue() {
        return QueueBuilder.durable(CHECKOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", CHECKOUT_DLX)
                .withArgument("x-dead-letter-routing-key", CHECKOUT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange checkoutExchange() {
        return new DirectExchange(CHECKOUT_EXCHANGE);
    }

    @Bean
    public Binding checkoutBinding(Queue checkoutQueue, DirectExchange checkoutExchange) {
        return BindingBuilder.bind(checkoutQueue).to(checkoutExchange).with(CHECKOUT_ROUTING_KEY);
    }

    @Bean
    public Queue checkoutDLQ() {
        return new Queue(CHECKOUT_DLQ, true);
    }

    @Bean
    public DirectExchange checkoutDLX() {
        return new DirectExchange(CHECKOUT_DLX);
    }

    @Bean
    public Binding checkoutDLQBinding(Queue checkoutDLQ, DirectExchange checkoutDLX) {
        return BindingBuilder.bind(checkoutDLQ).to(checkoutDLX).with(CHECKOUT_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
