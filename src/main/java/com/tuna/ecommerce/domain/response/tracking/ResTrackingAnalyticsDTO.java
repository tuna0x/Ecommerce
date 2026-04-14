package com.tuna.ecommerce.domain.response.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResTrackingAnalyticsDTO {
    private long totalEvents;
    private long totalSessions;
    private long activeUsers;
    private long totalPurchases;
    private long totalAddToCart;
    private double conversionRate;

    private List<ActionCount> actionDistribution;
    private List<DailyCount> activityTrend;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActionCount {
        private String action;
        private long count;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyCount {
        private String date;
        private long count;
    }
}
