package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.Transaction;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.TransactionRepository;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;
import com.tuna.ecommerce.ultil.constant.TransactionStatusEnum;

import jakarta.persistence.criteria.Predicate;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction handleLogTransaction(Order order, BigDecimal amount, PaymentMethodEnum method,
            TransactionStatusEnum status, String externalId, String rawData) {
        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setAmount(amount);
        transaction.setPaymentMethod(method);
        transaction.setStatus(status);
        transaction.setExternalId(externalId);
        transaction.setRawData(rawData);
        return this.transactionRepository.save(transaction);
    }

    public ResultPaginationDTO fetchAllTransactions(Pageable pageable, TransactionStatusEnum status, Instant startDate,
            Instant endDate, String externalId) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                // If crossed, we return a predicate that always fails (id < 0)
                predicates.add(cb.lessThan(root.get("id"), 0));
            } else {
                if (startDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
                }
                if (endDate != null) {
                    Instant adjustedEndDate = endDate.truncatedTo(ChronoUnit.DAYS)
                            .plus(23, ChronoUnit.HOURS)
                            .plus(59, ChronoUnit.MINUTES)
                            .plus(59, ChronoUnit.SECONDS);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), adjustedEndDate));
                }
            }
            if (externalId != null && !externalId.isEmpty()) {
                predicates.add(cb.like(root.get("externalId"), "%" + externalId + "%"));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Transaction> pageTransaction = this.transactionRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageTransaction.getTotalPages());
        mt.setTotal(pageTransaction.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageTransaction.getContent());

        return rs;
    }
}
