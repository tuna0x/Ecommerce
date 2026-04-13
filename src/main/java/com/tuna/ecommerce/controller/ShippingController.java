package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.repository.AddressRepository;
import com.tuna.ecommerce.service.ShippingService;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
@Slf4j
public class ShippingController {
    private final ShippingService service;
    private final AddressRepository addressRepository;

    @GetMapping("/fee/{addressId}")
    public Integer calculateFee(@PathVariable Long addressId, @RequestParam(defaultValue = "500") int weight) throws IdInvalidException {
        Address address = this.addressRepository.findById(addressId).orElse(null);
        if (address == null) {
            throw new IdInvalidException("address not found");
        }
        return service.calculateShippingFee(address, weight);
    }

    @GetMapping("/fee/preview")
    public Integer getFeePreview(
            @RequestParam String province,
            @RequestParam String district,
            @RequestParam String ward,
            @RequestParam(defaultValue = "500") int weight) {
        log.info(">>> GET FEE PREVIEW (GHN): province={}, district={}, ward={}, weight={}", province, district, ward, weight);
        return service.calculateShippingFee(province, district, ward, weight);
    }
}
