package com.tuna.ecommerce.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.Payment;
import com.tuna.ecommerce.domain.request.Payment.ReqTransactionIdDTO;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.PaymentRepository;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    // private final VNPAYConfig vnpayConfig;

    public Payment createCODPayment(Long orderId){
        Order order= this.orderService.getOrder(orderId);
        Payment payment=new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethodEnum.COD);
        payment.setStatus(OrderStatusEnum.PENDDING);
        payment.setAmount(order.getFinalPrice());
        order.setPayment(payment);

        return this.paymentRepository.save(payment);
    }

    public Payment createPendingVNPayPayment(Long orderId){
        Order order= this.orderService.getOrder(orderId);
        Payment payment=new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethodEnum.VNPAY);
        payment.setStatus(OrderStatusEnum.PENDDING);
        payment.setAmount(order.getFinalPrice());
        payment.setTransactionId(null);
        order.setPayment(payment);
         return this.paymentRepository.save(payment);
    }

    public Payment markAsPaid(ReqTransactionIdDTO req) throws IdInvalidException{
        Payment payment=this.paymentRepository.findByTransactionId(req.getTransactionId()).orElseThrow(()-> new IdInvalidException("transaction not found"));
        payment.setStatus(OrderStatusEnum.COMPLETED);

        Order order= payment.getOrder();
        order.setPaymentStatus(PaymentStatusEnum.PAID);
        order.setStatus(OrderStatusEnum.COMPLETED);
        this.orderRepository.save(order);
        payment=this.paymentRepository.save(payment);

        return payment;
    }

    public Payment findById(long id){
        Optional <Payment> payOptional=this.paymentRepository.findById(id) ;
        return payOptional.isPresent() ? payOptional.get() : null;
    }

    // public ResPaymentVNPAYDTO createVnPayPayment(HttpServletRequest request, Long paymentId) {
    //     Payment payment= this.paymentRepository.findById(paymentId).isPresent() ? this.paymentRepository.findById(paymentId).get() : null;
    //     Order order=payment.getOrder();

    //     long amount = (long) ((payment.getAmount()) * 100L);
    //     String bankCode = request.getParameter("bankCode");
    //     Map<String, String> vnpParamsMap = vnpayConfig.getVNPayConfig();
    //     vnpParamsMap.put("vnp_TxnRef",  String.valueOf(payment.getId()));
    //     vnpParamsMap.put("vnp_OrderInfo", "Thanh toan don hang:" +  order.getId());
    //     vnpParamsMap.put("vnp_Amount", String.valueOf(amount));
    //     if (bankCode != null && !bankCode.isEmpty()) {
    //         vnpParamsMap.put("vnp_BankCode", bankCode);
    //     }
    //     vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
    //     //build query url
    //     String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
    //     String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
    //     String vnpSecureHash = VNPayUtil.hmacSHA512(vnpayConfig.getSecretKey(), hashData);
    //     queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
    //     String paymentUrl = vnpayConfig.getVnp_PayUrl() + "?" + queryUrl;
    //     return new ResPaymentVNPAYDTO("ok","success",paymentUrl);

    // }

}
