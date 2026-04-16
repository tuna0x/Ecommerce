package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.response.dashboard.ResDashboardDTO;
import com.tuna.ecommerce.repository.InventoryRepository;
import com.tuna.ecommerce.repository.OrderItemRepository;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public ResDashboardDTO getStatistics(Instant startDate, Instant endDate) {
        // 1. Overview Stats
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(startDate, endDate);
        long totalOrders = orderRepository.countTotalOrders(startDate, endDate);
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        Double aov = orderRepository.calculateAverageOrderValue(startDate, endDate);
        long newUsers = userRepository.countNewUsers(startDate, endDate);
        long returningUsers = orderRepository.countReturningUsers(startDate, endDate);

        // 2. Growth Rate Calculation
        long durationDays = java.time.Duration.between(startDate, endDate).toDays();
        Instant prevStartDate = startDate.minus(durationDays, java.time.temporal.ChronoUnit.DAYS);
        BigDecimal prevRevenue = orderRepository.calculateTotalRevenue(prevStartDate, startDate);
        long prevOrders = orderRepository.countTotalOrders(prevStartDate, startDate);
        Double prevAov = orderRepository.calculateAverageOrderValue(prevStartDate, startDate);

        double growthRate = 0;
        if (prevRevenue != null && prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthRate = (totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                    .subtract(prevRevenue)
                    .divide(prevRevenue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100)).doubleValue();
        }

        double aovGrowth = 0;
        if (prevAov != null && prevAov > 0 && aov != null) {
            aovGrowth = ((aov - prevAov) / prevAov) * 100;
        }

        // 3. Distributions
        List<Object[]> statusData = orderRepository.countOrdersByStatus(startDate, endDate);
        Map<String, Long> statusDist = statusData.stream()
                .collect(Collectors.toMap(row -> row[0].toString(), row -> ((Number) row[1]).longValue()));

        List<Object[]> catData = orderRepository.findCategoryOrderDistribution(startDate, endDate);
        List<ResDashboardDTO.CategoryStat> catDist = catData.stream()
                .map(row -> {
                    long count = ((Number) row[1]).longValue();
                    BigDecimal totalValue = new BigDecimal(row[2].toString());
                    BigDecimal catAov = count > 0 ? totalValue.divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    return ResDashboardDTO.CategoryStat.builder()
                        .category((String) row[0])
                        .count(count)
                        .value(totalValue)
                        .aov(catAov)
                        .build();
                })
                .collect(Collectors.toList());

        // 4. Monthly Revenue (Chart)
        List<Object[]> monthlyData = orderRepository.findMonthlyRevenue(startDate, endDate);
        List<ResDashboardDTO.MonthlyRevenue> monthlyRevenue = monthlyData.stream()
                .map(row -> ResDashboardDTO.MonthlyRevenue.builder()
                        .month((String) row[0])
                        .revenue(new BigDecimal(row[1].toString()))
                        .orderCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // 5. Top Selling Products
        List<Object[]> topProductData = orderItemRepository.findTopSellingProducts(startDate, endDate, PageRequest.of(0, 5));
        List<ResDashboardDTO.ProductStat> topProducts = topProductData.stream()
                .map(row -> ResDashboardDTO.ProductStat.builder()
                        .name((String) row[0])
                        .quantity(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        // 6. Inventory Logic
        List<Object[]> topProdByValueData = inventoryRepository.findTopProductsByValue();
        List<ResDashboardDTO.ProductValueStat> topProductsByValue = topProdByValueData.stream()
                .limit(5)
                .map(row -> ResDashboardDTO.ProductValueStat.builder()
                        .name((String) row[0])
                        .value(new BigDecimal(row[1].toString()))
                        .build())
                .collect(Collectors.toList());

        ResDashboardDTO.InventorySummary invSummary = ResDashboardDTO.InventorySummary.builder()
                .totalCapitalValue(inventoryRepository.calculateTotalCapitalValue())
                .totalItems(inventoryRepository.count())
                .lowStockCount(inventoryRepository.countLowStock())
                .outOfStockCount(inventoryRepository.countOutOfStock())
                .topProductsByValue(topProductsByValue)
                .build();

        List<Object[]> lowStockData = inventoryRepository.findLowStockItems();
        List<ResDashboardDTO.LowStockProduct> lowStockProducts = lowStockData.stream()
                .map(row -> ResDashboardDTO.LowStockProduct.builder()
                        .id(((Number) row[0]).longValue())
                        .name((String) row[1])
                        .image((String) row[2])
                        .stock(((Number) row[3]).intValue())
                        .build())
                .collect(Collectors.toList());

        // 7. Recent Activity
        List<com.tuna.ecommerce.domain.Order> recentOrdersList = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10)).getContent();
        List<ResDashboardDTO.RecentOrder> recentOrders = recentOrdersList.stream()
                .map(order -> ResDashboardDTO.RecentOrder.builder()
                        .id(order.getId())
                        .transactionId(order.getPayment() != null ? order.getPayment().getTransactionId() : "N/A")
                        .customerName(order.getReceiverName())
                        .total(order.getFinalPrice())
                        .status(order.getStatus().toString())
                        .createdAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResDashboardDTO.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .averageOrderValue(aov != null ? aov : 0.0)
                .revenueGrowthRate(growthRate)
                .aovGrowthRate(aovGrowth)
                .newUsersCount(newUsers)
                .returningUsersCount(returningUsers)
                .orderStatusDistribution(statusDist)
                .monthlyRevenue(monthlyRevenue)
                .categoryDistribution(catDist)
                .topSellingProducts(topProducts)
                .inventorySummary(invSummary)
                .lowStockProducts(lowStockProducts)
                .recentOrders(recentOrders)
                .build();
    }

    public String getQuickStatsForTelegram() {
        java.time.Instant end = java.time.Instant.now();
        java.time.Instant start = end.atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        
        java.math.BigDecimal revenue = orderRepository.calculateTotalRevenue(start, end);
        long orders = orderRepository.countTotalOrders(start, end);
        Double aov = orderRepository.calculateAverageOrderValue(start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("<b>📊 BÁO CÁO NHANH HÔM NAY</b>\n\n");
        sb.append("<b>💰 Doanh thu:</b> ").append(String.format("%,.0f VNĐ", revenue != null ? revenue.doubleValue() : 0)).append("\n");
        sb.append("<b>📦 Tổng đơn hàng:</b> ").append(orders).append("\n");
        sb.append("<b>💎 AOV (Trung bình đơn):</b> ").append(String.format("%,.0f VNĐ", aov != null ? aov : 0)).append("\n\n");
        sb.append("👉 <i>Dữ liệu tính đến: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))).append("</i>");
        
        return sb.toString();
    }

    public String getTopProductsForTelegram() {
        java.time.Instant end = java.time.Instant.now();
        java.time.Instant start = end.minus(7, java.time.temporal.ChronoUnit.DAYS); // Top 7 ngày qua
        
        List<Object[]> topData = orderItemRepository.findTopSellingProducts(start, end, org.springframework.data.domain.PageRequest.of(0, 5));
        
        StringBuilder sb = new StringBuilder();
        sb.append("<b>🏆 TOP 5 SẢN PHẨM BÁN CHẠY (7 NGÀY QUA)</b>\n\n");
        int rank = 1;
        for (Object[] row : topData) {
            sb.append(rank++).append(". ").append(row[0]).append(" (Đã bán: ").append(row[1]).append(")\n");
        }
        
        if (topData.isEmpty()) sb.append("<i>Chưa có dữ liệu bán hàng trong 7 ngày qua.</i>");
        
        return sb.toString();
    }
}
