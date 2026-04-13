package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.Payment;
import com.tuna.ecommerce.domain.request.Payment.ReqTransactionIdDTO;
import com.tuna.ecommerce.domain.response.payment.ResPaymentVNPAYDTO;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.PaymentRepository;
import com.tuna.ecommerce.ultil.VNPayUtil;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PaymentService {

    @Value("${vnp_PayUrl}")
    private String vnp_PayUrl;
    @Value("${vnp_ReturnUrl}")
    private String vnp_ReturnUrl;
    @Value("${vnp_TmnCode}")
    private String vnp_TmnCode;
    @Value("${secretKey}")
    private String secretKey;
    @Value("${vnp_Version}")
    private String vnp_Version;
    @Value("${vnp_Command}")
    private String vnp_Command;
    @Value("${orderType}")
    private String orderType;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository,
            @Lazy OrderService orderService, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    public Payment createCODPayment(Long orderId) {
        Order order = this.orderService.getOrder(orderId);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethodEnum.COD);
        payment.setStatus(OrderStatusEnum.PENDING);
        payment.setAmount(order.getFinalPrice());
        order.setPayment(payment);

        return this.paymentRepository.save(payment);
    }

    public Payment createPendingVNPayPayment(Long orderId) {
        Order order = this.orderService.getOrder(orderId);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethodEnum.VNPAY);
        payment.setStatus(OrderStatusEnum.PENDING);
        payment.setAmount(order.getFinalPrice());
        payment.setTransactionId(null);
        order.setPayment(payment);
        return this.paymentRepository.save(payment);
    }

    public ResPaymentVNPAYDTO createVnPayPayment(HttpServletRequest req, Long paymentId) {
        Payment payment = this.paymentRepository.findById(paymentId).orElse(null);
        if (payment == null)
            return new ResPaymentVNPAYDTO("99", "Payment not found", null);

        String paymentUrl = VNPayUtil.createVnPayPayment(
                req,
                payment.getId(),
                vnp_PayUrl,
                vnp_ReturnUrl,
                vnp_TmnCode,
                secretKey,
                vnp_Version,
                vnp_Command,
                orderType,
                payment.getAmount());

        return new ResPaymentVNPAYDTO("00", "Success", paymentUrl);
    }

    public Payment markAsPaid(ReqTransactionIdDTO req) throws IdInvalidException {
        Payment payment = this.paymentRepository.findByTransactionId(req.getTransactionId())
                .orElseThrow(() -> new IdInvalidException("transaction not found"));
        payment.setStatus(OrderStatusEnum.CONFIRMED);

        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatusEnum.PAID);
        order.setStatus(OrderStatusEnum.CONFIRMED);
        this.orderRepository.save(order);
        payment = this.paymentRepository.save(payment);
        this.orderService.handleClearCart(order);

        // Gửi thông báo thanh toán thành công
        this.notificationService.createNotification(
                order.getUser(),
                "Thanh toán thành công",
                "Giao dịch cho đơn hàng #" + order.getId() + " của bạn đã được xác nhận thành công.",
                "PAYMENT_SUCCESS");

        return payment;
    }

    public Payment findById(long id) {
        Optional<Payment> payOptional = this.paymentRepository.findById(id);
        return payOptional.isPresent() ? payOptional.get() : null;
    }

    public void save(Payment payment) {
        this.paymentRepository.save(payment);
    }
}
