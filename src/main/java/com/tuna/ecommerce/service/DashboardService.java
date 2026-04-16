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
        double growthRate = 0;
        if (prevRevenue != null && prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthRate = (totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                    .subtract(prevRevenue)
                    .divide(prevRevenue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100)).doubleValue();
        }

        // 3. Distributions
        List<Object[]> statusData = orderRepository.countOrdersByStatus(startDate, endDate);
        Map<String, Long> statusDist = statusData.stream()
                .collect(Collectors.toMap(row -> row[0].toString(), row -> ((Number) row[1]).longValue()));

        List<Object[]> catData = orderRepository.findCategoryOrderDistribution(startDate, endDate);
        List<ResDashboardDTO.CategoryStat> catDist = catData.stream()
                .map(row -> ResDashboardDTO.CategoryStat.builder()
                        .category((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .value(new BigDecimal(row[2].toString()))
                        .build())
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
}
