package com.tuna.ecommerce.controller;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.Payment;
import com.tuna.ecommerce.domain.request.Payment.ReqTransactionIdDTO;
import com.tuna.ecommerce.domain.response.payment.ResPaymentVNPAYDTO;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.PaymentRepository;
import com.tuna.ecommerce.service.PaymentService;
import com.tuna.ecommerce.ultil.VNPayUtil;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    @Value("${secretKey}")
    private String secretKey;

    @Value("${tuna.frontend-url}")
    private String frontendUrl;

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final com.tuna.ecommerce.service.OrderService orderService;

    public PaymentController(OrderRepository orderRepository, PaymentService paymentService,
            PaymentRepository paymentRepository, com.tuna.ecommerce.service.OrderService orderService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
    }

    @PostMapping("/payment/confirm")
    @APIMessage("Confirm payment as paid")
    public ResponseEntity<Payment> confirmPayment(@RequestBody ReqTransactionIdDTO req) throws IdInvalidException {
        return ResponseEntity.ok().body(this.paymentService.markAsPaid(req));
    }

    @GetMapping("/payment/vn-pay")
    @APIMessage("Generate VNPay payment URL")
    public ResponseEntity<ResPaymentVNPAYDTO> pay(HttpServletRequest req, @RequestParam Long paymentId) {
        return ResponseEntity.ok().body(this.paymentService.createVnPayPayment(req, paymentId));
    }

    @GetMapping("/payment/vn-pay-callback")
    @APIMessage("Handle VNPay callback")
    public ResponseEntity<Void> payCallbackHandler(HttpServletRequest req) throws IdInvalidException {
        // Verify signature
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = req.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = req.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = req.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        String signValue = VNPayUtil.hashAllFields(fields, secretKey);
        String vnp_TxnRef = req.getParameter("vnp_TxnRef");
        String vnp_ResponseCode = req.getParameter("vnp_ResponseCode");
        String vnp_TransactionNo = req.getParameter("vnp_TransactionNo");

        // Frontend URL from application.properties (tuna.frontend-url)
        String frontendRedirectUrl = frontendUrl + "/payment-result";

        if (!signValue.equals(vnp_SecureHash)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Invalid_signature"))
                    .build();
        }

        if (vnp_TxnRef == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Missing_vnp_TxnRef"))
                .build();
        }

        Long paymentId = Long.valueOf(vnp_TxnRef);
        Payment payment = this.paymentService.findById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Payment_not_found"))
                .build();
        }

        payment.setTransactionId(vnp_TransactionNo);
        if ("00".equals(vnp_ResponseCode)) {
            payment.setStatus(OrderStatusEnum.CONFIRMED);
            Order order = payment.getOrder();
            order.setPaymentStatus(PaymentStatusEnum.PAID);
            order.setStatus(OrderStatusEnum.CONFIRMED);
            this.orderRepository.save(order);
            this.paymentService.save(payment);
            this.orderService.handleClearCart(order);
            
            String redirectUrl = frontendRedirectUrl + "?status=success&orderId=" + order.getId() + "&transactionId=" + vnp_TransactionNo;
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        } else {
            // Thanh toán thất bại hoặc bị hủy bởi khách hàng
            payment.setStatus(OrderStatusEnum.CANCELLED);
            this.paymentService.save(payment);
            
            // Hủy đơn hàng + giải phóng stock + gửi thông báo
            Order order = payment.getOrder();
            if (order.getStatus() == OrderStatusEnum.PENDING) {
                order.setPaymentStatus(PaymentStatusEnum.UNPAID);
                this.orderRepository.save(order);
                // handleUpdateStatus sẽ: đổi trạng thái, releaseStock, gửi notification
                this.orderService.handleUpdateStatus(order.getId(), OrderStatusEnum.CANCELLED);
            }

            String redirectUrl = frontendRedirectUrl + "?status=failed&orderId=" + order.getId() + "&transactionId=" + (vnp_TransactionNo != null ? vnp_TransactionNo : "");
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        }
    }
}
