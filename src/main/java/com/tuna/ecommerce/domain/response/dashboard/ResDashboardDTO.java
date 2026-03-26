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

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductStat {
        private String name;
        private long quantity;
    }
}
