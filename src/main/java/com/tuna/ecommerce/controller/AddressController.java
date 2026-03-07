package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.domain.ProductDetail;
import com.tuna.ecommerce.domain.request.address.ReqCreateAddressDTO;
import com.tuna.ecommerce.domain.request.address.ReqUpdateAddressDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.address.ResAddressDTO;
import com.tuna.ecommerce.service.AddressService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @PostMapping("/addresses")
    public ResponseEntity<ResAddressDTO> createAddress(@RequestBody ReqCreateAddressDTO req) {
        Address address= this.addressService.createAddress(req);

        return ResponseEntity.status(HttpStatus.CREATED).body(this.addressService.convertToAddressDTO(address));
    }
    @PutMapping("/addresses")
    public ResponseEntity<ResAddressDTO> updateAddress(@RequestBody ReqUpdateAddressDTO req) throws IdInvalidException {
        Address address= this.addressService.getAddressById(req.getId());
        if (address==null) {
            throw new IdInvalidException("address not found");
        }
        address=this.addressService.UpdateAddress(req);
        return ResponseEntity.ok().body(this.addressService.convertToAddressDTO(address));
    }

    @DeleteMapping("/addresses")
    public ResponseEntity<Void> deleteAddress(@PathVariable ("id") Long id) throws IdInvalidException{
        Address address= this.addressService.getAddressById(id);
        if (address==null) {
            throw new IdInvalidException("address not found");
        }
        this.addressService.deleteById(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/addresses")
    @APIMessage("Get all address")
    public ResponseEntity<ResultPaginationDTO> getAllCategory(@Filter Specification<Address> spec, Pageable page) {
        return ResponseEntity.ok().body(this.addressService.getAllAddress(spec, page));
    }

    @PutMapping("addresses/{id}/default")
    public ResponseEntity<?> setAddressDefault(@PathVariable("id") Long id) {
        this.addressService.setDefaultAddress(id);
        return ResponseEntity.ok().body("Set default success");
    }
}
