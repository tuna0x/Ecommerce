package com.tuna.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.request.attributeValue.ReqCreateAttributesValueDTO;
import com.tuna.ecommerce.domain.request.attributeValue.ReqUpdateAttributesValueDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.attributevalue.ResAttributeValueDTO;
import com.tuna.ecommerce.service.AttributeValueService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class AttributeValueController {
    private final AttributeValueService attributeValueService;

    @APIMessage("Attribute value created successfully")
    @PostMapping("/attributes-values")
    public ResponseEntity<ResAttributeValueDTO> createAttributeValue(@RequestBody ReqCreateAttributesValueDTO req) throws IdInvalidException {
        if (this.attributeValueService.existsByAttributeIdAndValue(req.getAttributeId(), req.getValue())) {
            throw new IdInvalidException("Attribute value already exists for this attribute");
        }
        AttributeValue value = this.attributeValueService.createAttributeValue(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.attributeValueService.convertToAttributeValueDTO(value));
    }

    @APIMessage("Attribute value updated successfully")
    @PutMapping("/attributes-values")
    public ResponseEntity<ResAttributeValueDTO> updateAttributeValue(@RequestBody ReqUpdateAttributesValueDTO req) throws IdInvalidException {
        if (this.attributeValueService.existsByAttributeIdAndValue(req.getAttributeId(), req.getValue())) {
            // Need to ensure it's not the same row being updated to the same value
            AttributeValue existing = this.attributeValueService.getAttributeValueById(req.getId());
            if (existing != null && !existing.getValue().equals(req.getValue())) {
                throw new IdInvalidException("Attribute value already exists for this attribute");
            }
        }
        AttributeValue updatedValue = this.attributeValueService.updateAttributeValue(req);
        return ResponseEntity.ok(this.attributeValueService.convertToAttributeValueDTO(updatedValue));
    }

    @APIMessage("Get all attribute values successfully")
    @GetMapping("/attributes-values")
    public ResponseEntity<ResultPaginationDTO> getAllAttributeValues(@Filter Specification<AttributeValue> spec, Pageable page) {
        return ResponseEntity.ok().body(this.attributeValueService.getAllAttributeValue(spec, page));
    }

    @APIMessage("Get attribute value by id successfully")
    @GetMapping("/attributes-values/{id}")
    public ResponseEntity<ResAttributeValueDTO> getAttributeValueById(@PathVariable("id") Long id) throws IdInvalidException {
        AttributeValue attributeValue = this.attributeValueService.getAttributeValueById(id);
        if (attributeValue == null) {
            throw new IdInvalidException("Attribute value not found with id: " + id);
        }
        return ResponseEntity.ok().body(this.attributeValueService.convertToAttributeValueDTO(attributeValue));
    }

    @APIMessage("Delete attribute value successfully")
    @DeleteMapping("/attributes-values/{id}")
    public ResponseEntity<Void> deleteAttributeValueById(@PathVariable("id") Long id) throws IdInvalidException {
        AttributeValue attributeValue = this.attributeValueService.getAttributeValueById(id);
        if (attributeValue == null) {
            throw new IdInvalidException("Attribute value not found with id: " + id);
        }
        this.attributeValueService.deleteAttributeValue(id);
        return ResponseEntity.noContent().build();
    }
}
