package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.repository.AddressRepository;
import com.tuna.ecommerce.service.GHTKService;
import com.tuna.ecommerce.service.ShippingService;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class ShippingController {
    private final ShippingService service;
    private final AddressRepository addressRepository;

    @GetMapping("/fee/{addressId}")
    public Integer calculateFee(@PathVariable Long addressId) throws IdInvalidException {
        Address address= this.addressRepository.findById(addressId).orElse(null);
        if (address==null) {
            throw new IdInvalidException("address not found");
        }
        return service.calculateShippingFee(address, 500);
    }
    
}
