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

        // Category Distribution (based on Orders)
        List<Object[]> categoryData = orderRepository.findCategoryOrderDistribution(startDate, endDate);
        List<ResDashboardDTO.CategoryStat> allCategoryStats = categoryData.stream().map(obj -> {
            ResDashboardDTO.CategoryStat stat = new ResDashboardDTO.CategoryStat();
            stat.setCategory((String) obj[0]);
            stat.setCount(((Number) obj[1]).longValue());
            stat.setValue(obj[2] != null ? (BigDecimal) obj[2] : BigDecimal.ZERO);
            return stat;
        }).sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
          .collect(Collectors.toList());

        List<ResDashboardDTO.CategoryStat> categoryStats = allCategoryStats.stream().limit(5).collect(Collectors.toList());
        if (allCategoryStats.size() > 5) {
            long otherCount = allCategoryStats.stream().skip(5).mapToLong(ResDashboardDTO.CategoryStat::getCount).sum();
            BigDecimal otherValue = allCategoryStats.stream().skip(5).map(ResDashboardDTO.CategoryStat::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            ResDashboardDTO.CategoryStat otherStat = new ResDashboardDTO.CategoryStat();
            otherStat.setCategory("Khác");
            otherStat.setCount(otherCount);
            otherStat.setValue(otherValue);
            categoryStats.add(otherStat);
        }
        dto.setCategoryDistribution(categoryStats);
        
        // Populate Recent Orders
        List<com.tuna.ecommerce.domain.Order> recentOrdersData = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)).getContent();
        List<ResDashboardDTO.RecentOrder> recentOrdersDTO = recentOrdersData.stream().map(order -> {
            ResDashboardDTO.RecentOrder ro = new ResDashboardDTO.RecentOrder();
            ro.setId(order.getId());
            ro.setTransactionId(order.getPayment() != null ? order.getPayment().getTransactionId() : "N/A");
            ro.setCustomerName(order.getReceiverName());
            ro.setTotal(order.getFinalPrice());
            ro.setStatus(order.getStatus().toString());
            ro.setCreatedAt(order.getCreatedAt());
            return ro;
        }).collect(Collectors.toList());
        dto.setRecentOrders(recentOrdersDTO);

        // Populate Low Stock Products
        List<Object[]> lowStockData = inventoryRepository.findLowStockItems();
        List<ResDashboardDTO.LowStockProduct> lowStockDTO = lowStockData.stream().limit(5).map(obj -> {
            ResDashboardDTO.LowStockProduct lsp = new ResDashboardDTO.LowStockProduct();
            lsp.setId(((Number) obj[0]).longValue());
            lsp.setName((String) obj[1]);
            lsp.setImage((String) obj[2]);
            lsp.setStock(((Number) obj[3]).longValue());
            return lsp;
        }).collect(Collectors.toList());
        dto.setLowStockProducts(lowStockDTO);
        
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
