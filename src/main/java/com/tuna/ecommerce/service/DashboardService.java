package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.response.dashboard.ResDashboardDTO;
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

    public ResDashboardDTO getStatistics() {
        ResDashboardDTO dto = new ResDashboardDTO();
        
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        dto.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        
        dto.setTotalOrders(orderRepository.countTotalOrders());
        dto.setTotalUsers(userRepository.count());
        dto.setTotalProducts(productRepository.count());
        
        // Get top 5 selling products
        List<Object[]> topSelling = orderItemRepository.findTopSellingProducts(PageRequest.of(0, 5));
        List<ResDashboardDTO.ProductStat> topSellingDTO = topSelling.stream().map(obj -> {
            ResDashboardDTO.ProductStat stat = new ResDashboardDTO.ProductStat();
            stat.setName((String) obj[0]);
            stat.setQuantity((Long) obj[1]);
            return stat;
        }).collect(Collectors.toList());
        
        dto.setTopSellingProducts(topSellingDTO);
        
        return dto;
    }
}
