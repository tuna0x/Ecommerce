package com.tuna.ecommerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Payment;
import com.tuna.ecommerce.domain.request.Payment.ReqTransactionIdDTO;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.PaymentRepository;
import com.tuna.ecommerce.service.PaymentService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class PaymentController {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @PostMapping("/payment/confirm")
    @APIMessage("confirm")
    public ResponseEntity<Payment> confirmPayment(@RequestBody ReqTransactionIdDTO req) throws IdInvalidException {
        return ResponseEntity.ok().body(this.paymentService.markAsPaid(req));
    }

    //     @GetMapping("/payment/vn-pay")
    // public ResponseEntity<ResPaymentVNPAYDTO> pay(HttpServletRequest req,@RequestParam Long paymentId) {
    //     return ResponseEntity.ok().body(this.paymentService.createVnPayPayment(req,paymentId));
    // }

    //         @GetMapping("/payment/vn-pay-callback")
    // public ResponseEntity<ResPaymentVNPAYDTO> payCallbackHandler(HttpServletRequest req, @RequestParam String vnp_ResponseCode) throws IdInvalidException {
    //     Long paymentId = Long.valueOf(req.getParameter("vnp_TxnRef"));
    //     String status=req.getParameter("vnp_ResponseCode");
    //     String transactionId = req.getParameter("vnp_TransactionNo");
    //     Payment payment= this.paymentService.findById(paymentId);
    //     if (payment==null) {
    //         throw new IdInvalidException("Payment not found");
    //     }
    //     payment.setTransactionId(transactionId);
    //     if (status.equals("00")) {
    //     payment.setStatus(StatusEnum.COMPLETED);
    //     payment.getOrder().setPaymentStatus(PaymentStatusEnum.PAID);
    //     this.paymentRepository.save(payment);
    //     this.orderRepository.save(payment.getOrder());
    //     return ResponseEntity.ok().body(new ResPaymentVNPAYDTO("00", "Success", ""));
    //     }
    //     else{
    //     payment.setStatus(StatusEnum.FAILED);
    //     this.paymentRepository.save(payment);
    //     this.orderRepository.save(payment.getOrder());
    //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResPaymentVNPAYDTO("99", "Failed", ""));
    //     }

    // }
}
