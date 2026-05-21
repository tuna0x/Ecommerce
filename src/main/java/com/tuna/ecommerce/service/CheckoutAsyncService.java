package com.tuna.ecommerce.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tuna.ecommerce.config.RabbitMQConfig;
import com.tuna.ecommerce.domain.CheckoutRequest;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.message.CheckoutMessage;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.domain.response.order.ResCheckoutAsyncDTO;
import com.tuna.ecommerce.domain.response.order.ResGetOrderDTO;
import com.tuna.ecommerce.repository.AddressRepository;
import com.tuna.ecommerce.repository.CheckoutRequestRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.CheckoutStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutAsyncService {
    private final CheckoutRequestRepository checkoutRequestRepository;
    private final UserService userService;
    private final AddressRepository addressRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ResCheckoutAsyncDTO submit(ReqCheckoutDTO req) throws IdInvalidException {
        if (req == null) {
            throw new IdInvalidException("Du lieu checkout khong hop le.");
        }
        if (req.getAddressId() == null) {
            throw new IdInvalidException("Dia chi giao hang khong duoc de trong.");
        }
        if (req.getCartItemId() == null || req.getCartItemId().isEmpty()) {
            throw new IdInvalidException("Gio hang cua ban dang trong.");
        }

        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        User user = userService.findByUsernameForAuth(email);
        if (user == null) {
            throw new IdInvalidException("Nguoi dung khong hop le.");
        }

        if (!addressRepository.existsByIdAndUserId(req.getAddressId(), user.getId())) {
            throw new IdInvalidException("Dia chi giao hang khong hop le.");
        }

        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setRequestId(UUID.randomUUID().toString());
        checkoutRequest.setUser(user);
        checkoutRequest.setAddressId(req.getAddressId());
        checkoutRequest.setCartItemIds(serializeCartItemIds(req.getCartItemId()));
        checkoutRequest.setCouponCode(req.getCouponCode());
        checkoutRequest.setShippingFee(req.getShippingFee());
        checkoutRequest.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethodEnum.COD);
        checkoutRequest.setStatus(CheckoutStatusEnum.PROCESSING);
        checkoutRequest = checkoutRequestRepository.save(checkoutRequest);

        CheckoutMessage message = new CheckoutMessage(checkoutRequest.getRequestId());
        publishAfterCommit(message);

        return toResponse(checkoutRequest);
    }

    @Transactional(readOnly = true)
    public ResCheckoutAsyncDTO getStatus(String requestId) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        User user = userService.findByUsernameForAuth(email);
        if (user == null) {
            throw new IdInvalidException("Nguoi dung khong hop le.");
        }

        CheckoutRequest checkoutRequest = checkoutRequestRepository.findByRequestIdAndUserId(requestId, user.getId())
                .orElseThrow(() -> new IdInvalidException("Checkout request not found."));
        return toResponse(checkoutRequest);
    }

    public void process(String requestId) {
        CheckoutRequest checkoutRequest = checkoutRequestRepository.findByRequestId(requestId).orElse(null);
        if (checkoutRequest == null) {
            log.warn("Checkout request {} not found, skipping.", requestId);
            return;
        }
        if (checkoutRequest.getStatus() != CheckoutStatusEnum.PROCESSING) {
            log.info("Checkout request {} already processed with status {}", requestId, checkoutRequest.getStatus());
            return;
        }

        ReqCheckoutDTO req = toCheckoutDTO(checkoutRequest);
        try {
            ResGetOrderDTO order = orderService.createOrderAsync(req, checkoutRequest.getUser().getEmail());
            checkoutRequest.setOrderId(order.getId());
            checkoutRequest.setPaymentUrl(order.getPaymentUrl());
            checkoutRequest.setTransactionId(order.getTransactionID());
            checkoutRequest.setStatus(order.getPaymentUrl() != null
                    ? CheckoutStatusEnum.PAYMENT_REQUIRED
                    : CheckoutStatusEnum.SUCCESS);
            checkoutRequest.setErrorMessage(null);
        } catch (IdInvalidException e) {
            checkoutRequest.setStatus(isOutOfStock(e.getMessage()) ? CheckoutStatusEnum.OUT_OF_STOCK : CheckoutStatusEnum.FAILED);
            checkoutRequest.setErrorMessage(e.getMessage());
            log.warn("Checkout request {} failed: {}", requestId, e.getMessage());
        } catch (Exception e) {
            checkoutRequest.setStatus(CheckoutStatusEnum.FAILED);
            checkoutRequest.setErrorMessage(e.getMessage());
            log.error("Checkout request {} failed unexpectedly", requestId, e);
        }

        checkoutRequest = checkoutRequestRepository.save(checkoutRequest);
        notifyStatus(checkoutRequest);
    }

    public void republish(String requestId) {
        publish(new CheckoutMessage(requestId));
    }

    private void publishAfterCommit(CheckoutMessage message) {
        Runnable publish = () -> publish(message);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private void publish(CheckoutMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHECKOUT_EXCHANGE,
                RabbitMQConfig.CHECKOUT_ROUTING_KEY,
                message);
    }

    private void notifyStatus(CheckoutRequest checkoutRequest) {
        messagingTemplate.convertAndSend(
                "/topic/checkout/" + checkoutRequest.getRequestId(),
                toResponse(checkoutRequest));
    }

    private ResCheckoutAsyncDTO toResponse(CheckoutRequest checkoutRequest) {
        return ResCheckoutAsyncDTO.builder()
                .checkoutId(checkoutRequest.getRequestId())
                .status(checkoutRequest.getStatus())
                .orderId(checkoutRequest.getOrderId())
                .paymentUrl(checkoutRequest.getPaymentUrl())
                .message(checkoutRequest.getErrorMessage())
                .transactionId(checkoutRequest.getTransactionId())
                .build();
    }

    private ReqCheckoutDTO toCheckoutDTO(CheckoutRequest checkoutRequest) {
        ReqCheckoutDTO req = new ReqCheckoutDTO();
        req.setAddressId(checkoutRequest.getAddressId());
        req.setCartItemId(deserializeCartItemIds(checkoutRequest.getCartItemIds()));
        req.setCouponCode(checkoutRequest.getCouponCode());
        req.setShippingFee(checkoutRequest.getShippingFee());
        req.setPaymentMethod(checkoutRequest.getPaymentMethod());
        return req;
    }

    private String serializeCartItemIds(List<Long> ids) {
        return ids.stream()
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Long> deserializeCartItemIds(String ids) {
        return Arrays.stream(ids.split(","))
                .filter(item -> !item.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private boolean isOutOfStock(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("stock")
                || lower.contains("hang")
                || lower.contains("kho")
                || lower.contains("flash sale")
                || lower.contains("suat");
    }
}
