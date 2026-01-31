package com.tuna.ecommerce.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.request.attributeValue.ReqCreateAttributesValueDTO;
import com.tuna.ecommerce.domain.request.attributeValue.ReqUpdateAttributesValueDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.AttributeService;
import com.tuna.ecommerce.service.AttributeValueService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;




@RequestMapping("/api/v1")
@RestController
@AllArgsConstructor
public class AttributeValueController {
    private final AttributeValueService attributeValueService;
    private final AttributeService attributeService;

    @PostMapping("/attributes-values")
    @APIMessage("Attribute value created successfully")
    public ResponseEntity<AttributeValue> createAttributeValue(@RequestBody ReqCreateAttributesValueDTO attributeValue) throws IdInvalidException{ 
       if (this.attributeValueService.existsByAttributeIdAndValue(attributeValue.getAttributeId(), attributeValue.getValue())) {
           throw new IdInvalidException("Attribute value already exists for this attribute");
       }
        return ResponseEntity.ok(this.attributeValueService.createAttributeValue(attributeValue));
    }

    @PutMapping("/attributes-values")
    @APIMessage("Attribute value updated successfully")
    public ResponseEntity<AttributeValue> updateAttributeValue(@RequestBody ReqUpdateAttributesValueDTO attributeValue) throws IdInvalidException {
        AttributeValue updatedAttributeValue = this.attributeValueService.getAttributeValueById(attributeValue.getId());

        if (this.attributeValueService.existsByAttributeIdAndValue(attributeValue.getAttributeId(),
                attributeValue.getValue())) {
            throw new IdInvalidException("Attribute value already exists for this attribute");

        }

        if (updatedAttributeValue == null) {
            throw new IdInvalidException("attribute value id is invalid");
        }
        return ResponseEntity.ok(this.attributeValueService.updateAttributeValue(attributeValue));
    }

    @GetMapping("/attributes-values")
    @APIMessage("Get all attribute values successfully")
    public ResponseEntity<ResultPaginationDTO> getAllUser(@Filter Specification<AttributeValue> spec,Pageable page) {
        return ResponseEntity.ok().body(this.attributeValueService.getAllAttributeValue(spec, page));
    }

    @GetMapping("/attributes-values/{id}")
    @APIMessage("Get attribute value by id successfully")
    public ResponseEntity<AttributeValue> getAttributeValueById(@PathVariable("id") Long id) throws IdInvalidException {
        AttributeValue attributeValue = this.attributeValueService.getAttributeValueById(id);
        if (attributeValue == null) {
            throw new IdInvalidException("attribute value id is invalid");
        }
        return ResponseEntity.ok().body(attributeValue);
    }

}
