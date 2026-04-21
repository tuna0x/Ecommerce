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
                    + vnp_TransactionNo;
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
        System.out.println(">>> PayOS Callback: orderCode=" + orderCode + ", code=" + code + ", status=" + status + ", cancel=" + cancel + ", id=" + id);

        String frontendRedirectUrl = frontendUrl + "/payment-result";

        if (orderCode == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(frontendRedirectUrl + "?status=failed&message=Missing_orderCode"))
                .build();
        }

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

        // PayOS logic: cancel can be "true" string, status can be "CANCELLED"
        boolean isCancelled = "true".equals(cancel) || "CANCELLED".equals(status);
        boolean isSuccess = "00".equals(code) && "PAID".equals(status);

        if (isSuccess) {
            // Thanh toán thành công
            String transactionId = (id != null) ? id : "PAYOS-" + orderCode;
            String rawData = "code=" + code + "&id=" + id + "&status=" + status + "&orderCode=" + orderCode;
            this.paymentService.processPaymentSuccess(payment, transactionId, rawData);

            String redirectUrl = frontendRedirectUrl + "?status=success&orderId=" + order.getId()
                    + "&transactionId=" + transactionId;
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        } else {
            // Thanh toán thất bại hoặc bị hủy
            // Cập nhật trạng thái payment trước
            payment.setStatus(OrderStatusEnum.CANCELLED);
            this.paymentService.save(payment);

            // Log failed transaction
            String rawData = "code=" + code + "&id=" + id + "&status=" + status + "&orderCode=" + orderCode + "&cancel=" + cancel;
            this.transactionService.handleLogTransaction(
                    order,
                    payment.getAmount(),
                    payment.getMethod(),
                    com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.FAIL,
                    id,
                    rawData);

            // Xử lý hủy đơn hàng nếu đang PENDING
            if (order.getStatus() == OrderStatusEnum.PENDING) {
                order.setPaymentStatus(PaymentStatusEnum.UNPAID);
                // logic handleUpdateStatus sẽ: đổi trạng thái order thành CANCELLED, releaseStock, gửi notification
                this.orderService.handleUpdateStatus(order.getId(), OrderStatusEnum.CANCELLED,
                        isCancelled ? "Khách hàng hủy thanh toán PayOS" : "Giao dịch PayOS thất bại");
            }

            // Redirect về frontend kèm theo message cụ thể nếu là hủy
            String redirectUrl = frontendRedirectUrl + "?status=failed&orderId=" + order.getId();
            if (isCancelled) {
                redirectUrl += "&message=cancelled";
            }
            
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
        }
    }
}
