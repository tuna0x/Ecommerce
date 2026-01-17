package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.attribute.ReqCreateAttributeDTO;
import com.tuna.ecommerce.domain.request.attribute.ReqUpdateAttributeDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.AttributeService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class AttributeController {
    private final AttributeService attributeService;

    @APIMessage("Create new attribute")
    @PostMapping("/attributes")
    public ResponseEntity<Attribute> postMethodName(@RequestBody ReqCreateAttributeDTO attribute) throws IdInvalidException {
        if (this.attributeService.existsByName(attribute.getName())) {
            throw new IdInvalidException("attribute name is exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.attributeService.createAttribute(attribute));
    }

    @APIMessage("Update attribute")
    @PutMapping("/attributes")
    public ResponseEntity<Attribute> updateAttribute(@RequestBody ReqUpdateAttributeDTO attribute) throws IdInvalidException {
        Attribute updatedAttribute = this.attributeService.getAttributeById(attribute.getId());
        if (updatedAttribute == null) {
            throw new IdInvalidException("attribute id is invalid");
        }
        if (this.attributeService.existsByName(attribute.getName())) {
            throw new IdInvalidException("attribute name is exists");
        }
        return ResponseEntity.ok().body(this.attributeService.updateAttribute(attribute));
    }

    @GetMapping("/attributes/{id}")
    @APIMessage("Get attribute by id")
    public ResponseEntity<Attribute> getAttributeById(@PathVariable ("id") Long id) throws IdInvalidException {
        Attribute attribute = this.attributeService.getAttributeById(id); 
        if (attribute==null) {
            throw new IdInvalidException("attribute id is invalid");
        }
        return ResponseEntity.ok().body(this.attributeService.getAttributeById(id));
    }
    
    @GetMapping("/attributes")
    @APIMessage("Get all attributes with filter and pagination")
     public ResponseEntity<ResultPaginationDTO> getAllUser(@Filter Specification<Attribute> spec,Pageable page) {
        return ResponseEntity.ok().body(this.attributeService.getAllAttribute(spec, page));
    }
    
}
