package com.tuna.ecommerce.service;

import org.springframework.stereotype.Service;
import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.domain.Order;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ShippingService {
        private final GHNService ghnService;
        private final CartService cartService;


    public Integer calculateShippingFee(String province, String district, String ward, int weight) {
        return ghnService.calculateFee(province, district, ward, weight);
    }

    public Integer calculateShippingFee(Address address, Integer weight) {
        return ghnService.calculateFee(address.getProvince(), address.getDistrict(), address.getWard(), weight);
    }

    public String createShippingOrder(Order order) {
        double weight = cartService.calculateTotalWeight(order.getItems().stream()
                .map(item -> {
                    com.tuna.ecommerce.domain.CartItem ci = new com.tuna.ecommerce.domain.CartItem();
                    ci.setProduct(item.getProduct());
                    ci.setProductVariant(item.getProductVariant());
                    ci.setQuantity(item.getQuantity());
                    return ci;
                }).collect(java.util.stream.Collectors.toList()));
        return ghnService.createOrder(order, (int) weight);
    }

}
