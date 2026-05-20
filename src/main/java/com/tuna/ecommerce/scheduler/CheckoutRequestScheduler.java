package com.tuna.ecommerce.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tuna.ecommerce.domain.CheckoutRequest;
import com.tuna.ecommerce.repository.CheckoutRequestRepository;
import com.tuna.ecommerce.service.CheckoutAsyncService;
import com.tuna.ecommerce.ultil.constant.CheckoutStatusEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutRequestScheduler {
    private final CheckoutRequestRepository checkoutRequestRepository;
    private final CheckoutAsyncService checkoutAsyncService;

    @Value("${checkout.async.processing-retry-after-minutes:2}")
    private long processingRetryAfterMinutes;

    @Scheduled(fixedDelayString = "${checkout.async.recovery-scan-ms:60000}")
    public void republishStaleProcessingRequests() {
        Instant cutoff = Instant.now().minus(processingRetryAfterMinutes, ChronoUnit.MINUTES);
        List<CheckoutRequest> staleRequests =
                checkoutRequestRepository.findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                        CheckoutStatusEnum.PROCESSING,
                        cutoff);

        if (staleRequests.isEmpty()) {
            return;
        }

        log.warn("Found {} stale checkout requests still PROCESSING. Republishing...", staleRequests.size());
        for (CheckoutRequest request : staleRequests) {
            try {
                checkoutAsyncService.republish(request.getRequestId());
                log.info("Republished checkout request {}", request.getRequestId());
            } catch (Exception e) {
                log.error("Failed to republish checkout request {}", request.getRequestId(), e);
            }
        }
    }
}
