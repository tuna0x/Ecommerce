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
import org.springframework.transaction.annotation.Transactional;

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
    @Value("${vnp_ApiUrl}")
    private String vnp_ApiUrl;
    @Value("${orderType}")
    private String orderType;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final TransactionService transactionService;
    private final EmailService emailService;
    private final TelegramService telegramService;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository,
            @Lazy OrderService orderService, NotificationService notificationService,
            TransactionService transactionService, EmailService emailService,
            TelegramService telegramService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.transactionService = transactionService;
        this.emailService = emailService;
        this.telegramService = telegramService;
    }

    public Payment createCODPayment(Long orderId) {
        Order order = this.orderService.getOrder(orderId);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethodEnum.COD);
        payment.setStatus(OrderStatusEnum.PENDING);
        payment.setAmount(order.getFinalPrice());
        order.setPayment(payment);

        Payment savedPayment = this.paymentRepository.save(payment);

        // Log COD transaction as PENDING
        this.transactionService.handleLogTransaction(
                order,
                payment.getAmount(),
                PaymentMethodEnum.COD,
                com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.PENDING,
                savedPayment.getTransactionId(),
                "Initial COD Payment Created");

        return savedPayment;
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
        Payment savedPayment = this.paymentRepository.save(payment);

        // Log VNPay initial attempt as PENDING
        this.transactionService.handleLogTransaction(
                order,
                payment.getAmount(),
                PaymentMethodEnum.VNPAY,
                com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.PENDING,
                null,
                "Initial VNPay Payment Intent Created");

        return savedPayment;
    }

    public Payment createPendingPayOSPayment(Long orderId) {
        Order order = this.orderService.getOrder(orderId);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethodEnum.PAYOS);
        payment.setStatus(OrderStatusEnum.PENDING);
        payment.setAmount(order.getFinalPrice());
        payment.setTransactionId(null);
        order.setPayment(payment);
        Payment savedPayment = this.paymentRepository.save(payment);

        // Log PayOS initial attempt as PENDING
        this.transactionService.handleLogTransaction(
                order,
                payment.getAmount(),
                PaymentMethodEnum.PAYOS,
                com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.PENDING,
                null,
                "Initial PayOS Payment Intent Created");

        return savedPayment;
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

    @Transactional
    public void processPaymentSuccess(Payment payment, String transactionId, String rawData) {
        payment.setStatus(OrderStatusEnum.CONFIRMED);
        payment.setTransactionId(transactionId);
        
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatusEnum.PAID);
        order.setStatus(OrderStatusEnum.CONFIRMED);
        
        this.orderRepository.save(order);
        this.paymentRepository.save(payment);
        
        // Clear cart for the user
        this.orderService.handleClearCart(order);
        
        // Log transaction
        this.transactionService.handleLogTransaction(
            order,
            payment.getAmount(),
            payment.getMethod(),
            com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.SUCCESS,
            transactionId,
            rawData
        );
        
        // Push notification
        this.notificationService.createNotification(
            order.getUser(),
            "Thanh toán thành công",
            "Đơn hàng #" + order.getId() + " của bạn đã được xác nhận thanh toán thành công.",
            "PAYMENT_SUCCESS"
        );
        
        // Send Thank You Email
        this.orderService.forceLoadOrder(order);
        this.emailService.sendOrderSuccessEmail(order);

        // Send Telegram Notification to Admin on successful online payment (VNPay/PayOS)
        this.telegramService.sendOrderNotification(order);
    }

    @Transactional
    public Payment markAsPaid(ReqTransactionIdDTO req) throws IdInvalidException {
        Payment payment = this.paymentRepository.findByTransactionId(req.getTransactionId())
                .orElseThrow(() -> new IdInvalidException("transaction not found"));
        
        this.processPaymentSuccess(payment, req.getTransactionId(), "Manual Payment Confirmation");
        return payment;
    }

    public Payment findById(long id) {
        Optional<Payment> payOptional = this.paymentRepository.findById(id);
        return payOptional.isPresent() ? payOptional.get() : null;
    }

    public void save(Payment payment) {
        this.paymentRepository.save(payment);
    }

    public boolean refundVNPayPayment(Payment payment, String createdBy) {
        try {
            Object searchResult = this.transactionService.fetchAllTransactions(
                    org.springframework.data.domain.PageRequest.of(0, 1),
                    com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.SUCCESS, null, null, payment.getTransactionId())
                    .getResult();
            
            com.tuna.ecommerce.domain.Transaction txn = null;
            if (searchResult instanceof java.util.List) {
                java.util.List<?> listResult = (java.util.List<?>) searchResult;
                if (!listResult.isEmpty() && listResult.get(0) instanceof com.tuna.ecommerce.domain.Transaction) {
                    txn = (com.tuna.ecommerce.domain.Transaction) listResult.get(0);
                }
            }

            String vnp_TxnRef = String.valueOf(payment.getId());
            String vnp_TransactionNo = payment.getTransactionId();
            String vnp_TransactionDate = "";
            
            if (txn != null && txn.getRawData() != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("vnp_PayDate=([0-9]{14})").matcher(txn.getRawData());
                if (m.find()) {
                    vnp_TransactionDate = m.group(1);
                }
            }
            if (vnp_TransactionDate.isEmpty()) {
                // Fallback to payment created at if not found
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
                vnp_TransactionDate = formatter.format(payment.getCreatedAt());
            }

            String vnp_RequestId = java.util.UUID.randomUUID().toString();
            String vnp_Command = "refund";
            String vnp_TransactionType = "02"; // Full refund
            long amount = payment.getAmount().multiply(new java.math.BigDecimal(100)).longValue();
            
            java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String vnp_CreateDate = formatter.format(cld.getTime());
            
            String vnp_IpAddr = "127.0.0.1"; // Can be dynamic
            String vnp_OrderInfo = "Hoan tien don hang " + payment.getOrder().getId();

            com.google.gson.JsonObject vnp_Params = new com.google.gson.JsonObject();
            vnp_Params.addProperty("vnp_RequestId", vnp_RequestId);
            vnp_Params.addProperty("vnp_Version", vnp_Version);
            vnp_Params.addProperty("vnp_Command", vnp_Command);
            vnp_Params.addProperty("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.addProperty("vnp_TransactionType", vnp_TransactionType);
            vnp_Params.addProperty("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.addProperty("vnp_Amount", amount);
            vnp_Params.addProperty("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.addProperty("vnp_TransactionNo", vnp_TransactionNo);
            vnp_Params.addProperty("vnp_TransactionDate", vnp_TransactionDate);
            vnp_Params.addProperty("vnp_CreateBy", createdBy);
            vnp_Params.addProperty("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.addProperty("vnp_IpAddr", vnp_IpAddr);

            String hashData = vnp_RequestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" + 
                              vnp_TransactionType + "|" + vnp_TxnRef + "|" + amount + "|" + vnp_TransactionNo + "|" + 
                              vnp_TransactionDate + "|" + createdBy + "|" + vnp_CreateDate + "|" + vnp_IpAddr + "|" + vnp_OrderInfo;
                              
            String vnp_SecureHash = VNPayUtil.hmacSHA512(secretKey, hashData);
            vnp_Params.addProperty("vnp_SecureHash", vnp_SecureHash);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(vnp_Params.toString(), headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(vnp_ApiUrl, entity, String.class);
            com.google.gson.JsonObject jsonResponse = com.google.gson.JsonParser.parseString(response.getBody()).getAsJsonObject();
            
            String responseCode = jsonResponse.has("vnp_ResponseCode") ? jsonResponse.get("vnp_ResponseCode").getAsString() : "99";
            
            if ("00".equals(responseCode)) {
                payment.setStatus(OrderStatusEnum.CANCELLED);
                payment.getOrder().setPaymentStatus(PaymentStatusEnum.REFUNDED);
                this.paymentRepository.save(payment);
                
                this.transactionService.handleLogTransaction(
                    payment.getOrder(),
                    payment.getAmount(),
                    payment.getMethod(),
                    com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.REFUNDED,
                    vnp_TransactionNo,
                    response.getBody()
                );
                
                this.notificationService.createNotification(
                    payment.getOrder().getUser(),
                    "Đã tiếp nhận yêu cầu hoàn tiền",
                    "Yêu cầu hoàn tiền cho đơn hàng #" + payment.getOrder().getId() + " đã được xử lý thành công. Tiền sẽ được hoàn lại vào tài khoản của bạn trong vòng 7-14 ngày làm việc tùy theo chính sách ngân hàng.",
                    "REFUND_SUCCESS"
                );

                this.emailService.sendRefundNotificationEmail(payment.getOrder(), payment.getAmount());
                return true;
            } else {
                this.transactionService.handleLogTransaction(
                    payment.getOrder(),
                    payment.getAmount(),
                    payment.getMethod(),
                    com.tuna.ecommerce.ultil.constant.TransactionStatusEnum.FAIL,
                    vnp_TransactionNo,
                    "Lỗi hoàn tiền: " + response.getBody()
                );
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
