package com.tuna.ecommerce.controller;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.TransactionService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.constant.TransactionStatusEnum;

@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/transactions")
    @APIMessage("Fetch all transactions with pagination and filters")
    public ResponseEntity<ResultPaginationDTO> getAllTransactions(
            Pageable pageable,
            @RequestParam(required = false) TransactionStatusEnum status,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) String externalId) {
        return ResponseEntity.ok()
                .body(this.transactionService.fetchAllTransactions(pageable, status, startDate, endDate, externalId));
    }
}
