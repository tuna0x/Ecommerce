package com.tuna.ecommerce.controller;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.tuna.ecommerce.service.TransactionService;
import com.tuna.ecommerce.ultil.VNPayUtil;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
 
 import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class PaymentController {

    @Value("${secretKey}")
    private String secretKey;

    @Value("${tuna.frontend-url}")
    private String frontendUrl;

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final com.tuna.ecommerce.service.OrderService orderService;
    private final TransactionService transactionService;
    private final com.tuna.ecommerce.service.PayOSService payOSService;

    public PaymentController(OrderRepository orderRepository, PaymentService paymentService,
            PaymentRepository paymentRepository, com.tuna.ecommerce.service.OrderService orderService,
            TransactionService transactionService, com.tuna.ecommerce.service.PayOSService payOSService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.transactionService = transactionService;
        this.payOSService = payOSService;
    }

    @PostMapping("/payment/confirm")
    @APIMessage("Confirm payment as paid")
    public ResponseEntity<Payment> confirmPayment(@RequestBody ReqTransactionIdDTO req) throws IdInvalidException {
        return ResponseEntity.ok().body(this.paymentService.markAsPaid(req));
    }

    @PostMapping("/payment/{orderId}/refund")
    @APIMessage("Refund VNPay payment manually")
    public ResponseEntity<Void> refundPayment(@PathVariable("orderId") Long orderId) throws IdInvalidException {
        Order order = this.orderRepository.findById(orderId)
            .orElseThrow(() -> new IdInvalidException("Order not found with id: " + orderId));
        
        if (order.getPayment() == null || order.getPayment().getMethod() != com.tuna.ecommerce.ultil.constant.PaymentMethodEnum.VNPAY) {
            throw new IdInvalidException("Manual refund is only available for VNPay payments.");
        }
        
        boolean success = this.paymentService.refundVNPayPayment(order.getPayment(), "Admin");
        if (!success) {
            throw new IdInvalidException("Refund failed. Please check logs for more details.");
        }
        
        return ResponseEntity.ok().build();
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
        String rawData = fields.toString();

        if ("00".equals(vnp_ResponseCode)) {
            this.paymentService.processPaymentSuccess(payment, vnp_TransactionNo, rawData);

            String redirectUrl = frontendRedirectUrl + "?status=success&orderId=" + payment.getOrder().getId() + "&transactionId="
                    + vnp_TransactionNo + "&method=vnpay";
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        } else {
            // Thanh toán thất bại hoặc bị hủy bởi khách hàng
            payment.setStatus(OrderStatusEnum.CANCELLED);
            this.paymentService.save(payment);

            // Hủy đơn hàng + giải phóng stock + gửi thông báo
            Order order = payment.getOrder();

            // Log failed transaction
            this.transactionService.handleLogTransaction(
                    order,
                    payment.getAmount(),
                    payment.getMethod(),
                    com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.FAIL,
                    vnp_TransactionNo,
                    rawData);

            if (order.getStatus() == OrderStatusEnum.PENDING) {
                order.setPaymentStatus(PaymentStatusEnum.UNPAID);
                this.orderRepository.save(order);
                // handleUpdateStatus sẽ: đổi trạng thái, releaseStock, gửi notification
                this.orderService.handleUpdateStatus(order.getId(), OrderStatusEnum.CANCELLED, "Giao dịch thất bại / Bị hủy bởi khách hàng");
            }

            String redirectUrl = frontendRedirectUrl + "?status=failed&orderId=" + order.getId() + "&transactionId="
                    + (vnp_TransactionNo != null ? vnp_TransactionNo : "");
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        }
    }

    @GetMapping("/payment/payos-callback")
    @APIMessage("Handle PayOS callback")
    public ResponseEntity<Void> payosCallbackHandler(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String cancel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long orderCode) throws IdInvalidException {

        // Log the incoming callback for debugging
        log.info(">>> PayOS Callback Redirect: orderCode={}, code={}, status={}, cancel={}, id={}", orderCode, code, status, cancel, id);

        String frontendRedirectUrl = frontendUrl + "/payment-result";

        if (orderCode == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Missing_orderCode"))
                .build();
        }

        // --- SECURITY IMPROVEMENT: Verify status with PayOS API ---
        com.fasterxml.jackson.databind.JsonNode paymentInfo = this.payOSService.getPaymentLinkInfo(orderCode);
        if (paymentInfo == null || !"00".equals(paymentInfo.get("code").asText())) {
             return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Payment_verification_failed"))
                .build();
        }

        com.fasterxml.jackson.databind.JsonNode data = paymentInfo.get("data");
        String actualStatus = data.get("status").asText();
        // --- END SECURITY IMPROVEMENT ---

        Order order = this.orderRepository.findById(orderCode).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Order_not_found"))
                .build();
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Payment_not_found"))
                .build();
        }

        if (payment.getStatus() == OrderStatusEnum.CONFIRMED) {
             String redirectUrl = frontendRedirectUrl + "?status=success&orderId=" + order.getId()
                    + "&transactionId=" + (payment.getTransactionId() != null ? payment.getTransactionId() : id);
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        }

        // Trust 'cancel=true' parameter from PayOS redirect, or check API status
        boolean isCancelledByUser = "true".equals(cancel) || "CANCELLED".equals(actualStatus);
        boolean isSuccess = "PAID".equals(actualStatus);

        if (isSuccess) {
            String transactionId = (id != null) ? id : "PAYOS-" + orderCode;
            String rawData = "verified_via_api=true&" + data.toString();
            this.paymentService.processPaymentSuccess(payment, transactionId, rawData);

            String redirectUrl = frontendRedirectUrl + "?status=success&orderId=" + order.getId()
                    + "&transactionId=" + transactionId + "&method=payos";
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        } else {
            payment.setStatus(OrderStatusEnum.CANCELLED);
            this.paymentRepository.save(payment);

            String rawData = "verified_via_api=true&status=" + actualStatus + "&orderCode=" + orderCode + "&paramCancel=" + cancel;
            this.transactionService.handleLogTransaction(
                    order,
                    payment.getAmount(),
                    payment.getMethod(),
                    com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.FAIL,
                    id,
                    rawData);

            // Cập nhật trạng thái đơn hàng sang CANCELLED nếu đang PENDING
            if (order.getStatus() == OrderStatusEnum.PENDING) {
                order.setPaymentStatus(PaymentStatusEnum.UNPAID);
                this.orderService.handleUpdateStatus(order.getId(), OrderStatusEnum.CANCELLED,
                        isCancelledByUser ? "Khách hàng hủy thanh toán PayOS" : "Giao dịch PayOS thất bại hoặc hết hạn (Status: " + actualStatus + ")");
            }

            String redirectUrl = frontendRedirectUrl + "?status=failed&orderId=" + order.getId();
            if (isCancelledByUser) {
                redirectUrl += "&message=cancelled";
            }
            
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        }
    }
}
