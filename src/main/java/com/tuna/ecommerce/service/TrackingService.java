package com.tuna.ecommerce.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserBehavior;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.UserBehaviorRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.constant.ActionTypeEnum;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserRepository userRepository;

    public long countAll() {
        return this.userBehaviorRepository.count();
    }

    @Async
    @org.springframework.transaction.annotation.Transactional
    public void logActivity(String email, String ip, String actionTypeStr, String metadata,
            String sessionId, String deviceType, String referrer, String pageUrl) {
        try {
            if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
                return;
            }

            ActionTypeEnum actionType;
            try {
                actionType = ActionTypeEnum.valueOf(actionTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return;
            }

            UserBehavior behavior = new UserBehavior();
            behavior.setUserEmail(email);
            behavior.setIpAddress(ip);
            behavior.setActionType(actionType);
            behavior.setMetadata(metadata);
            behavior.setSessionId(sessionId);
            behavior.setDeviceType(deviceType);
            behavior.setReferrer(referrer);
            behavior.setPageUrl(pageUrl);

            if (email != null && !email.equals("anonymous")) {
                User user = userRepository.findByEmail(email);
                if (user != null) {
                    behavior.setUser(user);
                }
            }

            userBehaviorRepository.save(behavior);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ResultPaginationDTO handleGetAll(Specification<UserBehavior> spec, Pageable pageable) {
        Page<UserBehavior> pLogs = this.userBehaviorRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pLogs.getTotalPages());
        mt.setTotal(pLogs.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pLogs.getContent());

        return rs;
    }

    public com.tuna.ecommerce.domain.response.tracking.ResTrackingAnalyticsDTO getAnalytics(int days) {
        java.time.Instant startDate = java.time.Instant.now().minus(java.time.Duration.ofDays(days));

        long totalEvents = this.userBehaviorRepository.count();
        long totalSessions = this.userBehaviorRepository.countDistinctSessions();
        long activeUsers = this.userBehaviorRepository.countDistinctUsers();
        long totalPurchases = this.userBehaviorRepository.countByActionType(ActionTypeEnum.PURCHASE);
        long totalAddToCart = this.userBehaviorRepository.countByActionType(ActionTypeEnum.ADD_CART);

        // Tính Conversion Rate: Sessions with Purchase / Total Sessions
        long sessionsWithPurchase = this.userBehaviorRepository.countDistinctSessionsByActionType(ActionTypeEnum.PURCHASE);
        double conversionRate = totalSessions > 0 ? (double) sessionsWithPurchase / totalSessions * 100 : 0;

        // Action Distribution
        List<Object[]> distributionRaw = this.userBehaviorRepository.findActionDistribution();
        List<com.tuna.ecommerce.domain.response.tracking.ResTrackingAnalyticsDTO.ActionCount> distribution = distributionRaw.stream()
                .map(obj -> new com.tuna.ecommerce.domain.response.tracking.ResTrackingAnalyticsDTO.ActionCount(
                        obj[0].toString(), 
                        ((Number) obj[1]).longValue()))
                .toList();

        // Activity Trend
        List<Object[]> trendRaw = this.userBehaviorRepository.findActivityTrend(startDate);
        List<com.tuna.ecommerce.domain.response.tracking.ResTrackingAnalyticsDTO.DailyCount> trend = trendRaw.stream()
                .map(obj -> new com.tuna.ecommerce.domain.response.tracking.ResTrackingAnalyticsDTO.DailyCount(
                        obj[0].toString(), 
                        ((Number) obj[1]).longValue()))
                .toList();

        return com.tuna.ecommerce.domain.response.tracking.ResTrackingAnalyticsDTO.builder()
                .totalEvents(totalEvents)
                .totalSessions(totalSessions)
                .activeUsers(activeUsers)
                .totalPurchases(totalPurchases)
                .totalAddToCart(totalAddToCart)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .actionDistribution(distribution)
                .activityTrend(trend)
                .build();
    }
}
