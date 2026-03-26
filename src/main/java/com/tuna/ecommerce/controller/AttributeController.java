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

import com.tuna.ecommerce.domain.Attribute;
import com.tuna.ecommerce.domain.request.attribute.ReqCreateAttributeDTO;
import com.tuna.ecommerce.domain.request.attribute.ReqUpdateAttributeDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.attribute.ResAttributeDTO;
import com.tuna.ecommerce.service.AttributeService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class AttributeController {
    private final AttributeService attributeService;

    @APIMessage("Create new attribute")
    @PostMapping("/attributes")
    public ResponseEntity<ResAttributeDTO> createAttribute(@RequestBody ReqCreateAttributeDTO req) throws IdInvalidException {
        Attribute attribute = this.attributeService.createAttribute(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.attributeService.convertAttributeDTO(attribute));
    }

    @APIMessage("Update attribute")
    @PutMapping("/attributes")
    public ResponseEntity<ResAttributeDTO> updateAttribute(@RequestBody ReqUpdateAttributeDTO req) throws IdInvalidException {
        Attribute updatedAttribute = this.attributeService.updateAttribute(req);
        return ResponseEntity.ok().body(this.attributeService.convertAttributeDTO(updatedAttribute));
    }

    @APIMessage("Get attribute by id")
    @GetMapping("/attributes/{id}")
    public ResponseEntity<ResAttributeDTO> getAttributeById(@PathVariable("id") Long id) throws IdInvalidException {
        Attribute attribute = this.attributeService.getAttributeById(id);
        if (attribute == null) {
            throw new IdInvalidException("Attribute not found with id: " + id);
        }
        return ResponseEntity.ok().body(this.attributeService.convertAttributeDTO(attribute));
    }

    @APIMessage("Get all attributes with filter and pagination")
    @GetMapping("/attributes")
    public ResponseEntity<ResultPaginationDTO> getAllAttributes(@Filter Specification<Attribute> spec, Pageable page) {
        return ResponseEntity.ok().body(this.attributeService.getAllAttribute(spec, page));
    }

    @APIMessage("Delete attribute")
    @DeleteMapping("/attributes/{id}")
    public ResponseEntity<Void> deleteAttributeById(@PathVariable("id") Long id) throws IdInvalidException {
        Attribute attribute = this.attributeService.getAttributeById(id);
        if (attribute == null) {
            throw new IdInvalidException("Attribute not found with id: " + id);
        }
        this.attributeService.deleteAttribute(id);
        return ResponseEntity.noContent().build();
    }
}
