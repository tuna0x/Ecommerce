package com.tuna.ecommerce.domain.response.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResDashboardDTO {
    // Overview Stats
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalUsers;
    private long totalProducts;
    private Double averageOrderValue;
    private double revenueGrowthRate;
    private long newUsersCount;
    private long returningUsersCount;
    private double aovGrowthRate; // Added for AOV trend

    // Distributions & Charts
    private Map<String, Long> orderStatusDistribution;
    private List<ProductStat> topSellingProducts;
    private List<CategoryStat> categoryDistribution;
    private List<MonthlyRevenue> monthlyRevenue; // Renamed from revenueChart for FE compatibility

    // Inventory Section
    private InventorySummary inventorySummary;
    private List<LowStockProduct> lowStockProducts;

    // Activity Section
    private List<RecentOrder> recentOrders;

    @Getter
    @Setter
    @Builder
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal revenue;
        private long orderCount;
    }

    @Getter
    @Setter
    @Builder
    public static class ProductStat {
        private String name;
        private long quantity;
    }

    @Getter
    @Setter
    @Builder
    public static class CategoryStat {
        private String category;
        private long count;
        private BigDecimal value;
        private BigDecimal aov; // Average Order Value per category
    }

    @Getter
    @Setter
    @Builder
    public static class InventorySummary {
        private BigDecimal totalCapitalValue;
        private long totalItems;
        private long lowStockCount;
        private long outOfStockCount;
        private List<ProductValueStat> topProductsByValue;
    }

    @Getter
    @Setter
    @Builder
    public static class ProductValueStat {
        private String name;
        private BigDecimal value;
    }

    @Getter
    @Setter
    @Builder
    public static class LowStockProduct {
        private Long id;
        private String name;
        private String image; // Added for FE product cards
        private int stock;
    }

    @Getter
    @Setter
    @Builder
    public static class RecentOrder {
        private Long id;
        private String transactionId;
        private String customerName;
        private BigDecimal total;
        private String status;
        private Instant createdAt;
    }
}
