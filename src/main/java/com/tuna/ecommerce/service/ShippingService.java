package com.tuna.ecommerce.service;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Address;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ShippingService {
        private final GHTKService ghtkService;
        private final CartService cartService;

    public Integer calculateShippingFee(Address address, Integer weight) {

        return ghtkService.calculateFee(
                address.getProvince(),
                address.getDistrict(),
                weight
        );
    }
}
