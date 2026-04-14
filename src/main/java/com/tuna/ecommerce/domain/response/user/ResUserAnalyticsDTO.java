package com.tuna.ecommerce.domain.response.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResUserAnalyticsDTO {
    private Long userId;
    private String email;
    
    // Financials
    private BigDecimal lifetimeValue;
    private long totalOrders;
    
    // Stats
    private Map<String, Long> orderStatusDistribution;
    
    // Security & Status
    private Instant lastLoginAt;
    private String lastIpAddress;
    
    // Insights
    private List<String> autoTags;
    private List<String> customTags;
    private String adminNotes;
    
    // Recent Activity (Timeline)
    private List<ActivityDTO> recentActivities;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActivityDTO {
        private String action;
        private String metadata;
        private String pageUrl;
        private Instant timestamp;
    }
}
