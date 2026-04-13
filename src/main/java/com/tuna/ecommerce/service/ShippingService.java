package com.tuna.ecommerce.service;

import org.springframework.stereotype.Service;
import com.tuna.ecommerce.domain.Address;
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

}
