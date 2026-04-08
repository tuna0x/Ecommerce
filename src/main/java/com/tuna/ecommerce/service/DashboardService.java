package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
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
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ResDashboardDTO getStatistics(java.time.Instant startDate, java.time.Instant endDate) {
        ResDashboardDTO dto = new ResDashboardDTO();

        // Basic KPIs
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(startDate, endDate);
        dto.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        dto.setTotalOrders(orderRepository.countTotalOrders(startDate, endDate));
        dto.setTotalUsers(userRepository.count());
        dto.setTotalProducts(productRepository.count());

        // Monthly Revenue Trend
        List<Object[]> monthlyData = orderRepository.findMonthlyRevenue(startDate, endDate);
        List<ResDashboardDTO.MonthlyRevenue> monthlyRevenue = monthlyData.stream().map(obj -> {
            ResDashboardDTO.MonthlyRevenue rev = new ResDashboardDTO.MonthlyRevenue();
            rev.setMonth((String) obj[0]);
            rev.setRevenue(obj[1] != null ? (BigDecimal) obj[1] : BigDecimal.ZERO);
            rev.setOrderCount(obj[2] != null ? ((Number) obj[2]).longValue() : 0L);
            return rev;
        }).collect(Collectors.toList());
        dto.setMonthlyRevenue(monthlyRevenue);

        // Top 5 selling products
        List<Object[]> topSelling = orderItemRepository.findTopSellingProducts(startDate, endDate, PageRequest.of(0, 5));
        List<ResDashboardDTO.ProductStat> topSellingDTO = topSelling.stream().map(obj -> {
            ResDashboardDTO.ProductStat stat = new ResDashboardDTO.ProductStat();
            stat.setName((String) obj[0]);
            stat.setQuantity(((Number) obj[1]).longValue());
            return stat;
        }).collect(Collectors.toList());
        dto.setTopSellingProducts(topSellingDTO);

        // Inventory Analytics
        ResDashboardDTO.InventorySummary invSummary = new ResDashboardDTO.InventorySummary();
        BigDecimal capitalValue = inventoryRepository.calculateTotalCapitalValue();
        invSummary.setTotalCapitalValue(capitalValue != null ? capitalValue : BigDecimal.ZERO);
        invSummary.setTotalItems(inventoryRepository.count());
        invSummary.setLowStockCount(inventoryRepository.countLowStock());
        invSummary.setOutOfStockCount(inventoryRepository.countOutOfStock());

        // Top 8 Products by Value
        List<Object[]> topValueData = inventoryRepository.findTopProductsByValue();
        List<ResDashboardDTO.ProductValueStat> topValueDTO = topValueData.stream().limit(8).map(obj -> {
            ResDashboardDTO.ProductValueStat stat = new ResDashboardDTO.ProductValueStat();
            stat.setName((String) obj[0]);
            stat.setValue(obj[1] != null ? (BigDecimal) obj[1] : BigDecimal.ZERO);
            return stat;
        }).collect(Collectors.toList());
        invSummary.setTopProductsByValue(topValueDTO);

        dto.setInventorySummary(invSummary);

        // Category Distribution
        List<Object[]> categoryData = productRepository.findCategoryDistribution();
        List<ResDashboardDTO.CategoryStat> categoryStats = categoryData.stream().map(obj -> {
            ResDashboardDTO.CategoryStat stat = new ResDashboardDTO.CategoryStat();
            stat.setCategory((String) obj[0]);
            stat.setCount(((Number) obj[1]).longValue());
            stat.setValue(obj[2] != null ? (BigDecimal) obj[2] : BigDecimal.ZERO);
            return stat;
        }).collect(Collectors.toList());
        dto.setCategoryDistribution(categoryStats);
        
        // --- NEW STATISTICS ---
        
        // 1. Order Status Distribution
        List<Object[]> statusData = orderRepository.countOrdersByStatus(startDate, endDate);
        java.util.Map<String, Long> statusDistribution = new java.util.HashMap<>();
        for (Object[] obj : statusData) {
            statusDistribution.put(obj[0].toString(), ((Number) obj[1]).longValue());
        }
        dto.setOrderStatusDistribution(statusDistribution);
        
        // 2. Customer Loyalty
        dto.setNewUsersCount(userRepository.countNewUsers(startDate, endDate));
        dto.setReturningUsersCount(orderRepository.countReturningUsers(startDate, endDate));
        
        // 3. Average Order Value
        Double aov = orderRepository.calculateAverageOrderValue(startDate, endDate);
        dto.setAverageOrderValue(aov != null ? BigDecimal.valueOf(aov) : BigDecimal.ZERO);
        
        // 4. Revenue Growth Rate
        java.time.Duration duration = java.time.Duration.between(startDate, endDate);
        java.time.Instant prevStartDate = startDate.minus(duration);
        BigDecimal currentRevenue = dto.getTotalRevenue();
        BigDecimal prevRevenue = orderRepository.calculateTotalRevenue(prevStartDate, startDate);
        
        if (prevRevenue == null || prevRevenue.equals(BigDecimal.ZERO)) {
            dto.setRevenueGrowthRate(currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);
        } else {
            double growth = currentRevenue.subtract(prevRevenue)
                                         .divide(prevRevenue, 4, java.math.RoundingMode.HALF_UP)
                                         .multiply(new BigDecimal(100))
                                         .doubleValue();
            dto.setRevenueGrowthRate(growth);
        }

        return dto;
    }
}
