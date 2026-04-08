package com.tuna.ecommerce.domain.response.dashboard;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResDashboardDTO {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalUsers;
    private long totalProducts;
    private List<ProductStat> topSellingProducts;
    private List<MonthlyRevenue> monthlyRevenue;
    private List<CategoryStat> categoryDistribution;
    private InventorySummary inventorySummary;
    
    // New Statistics fields
    private java.util.Map<String, Long> orderStatusDistribution;
    private long newUsersCount;
    private long returningUsersCount;
    private BigDecimal averageOrderValue;
    private Double revenueGrowthRate;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductStat {
        private String name;
        private long quantity;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal revenue;
        private long orderCount;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryStat {
        private String category;
        private long count;
        private BigDecimal value;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InventorySummary {
        private BigDecimal totalCapitalValue;
        private long totalItems;
        private long lowStockCount;
        private long outOfStockCount;
        private List<ProductValueStat> topProductsByValue;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductValueStat {
        private String name;
        private BigDecimal value;
    }
}
