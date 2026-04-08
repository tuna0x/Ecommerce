package com.tuna.ecommerce.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.InventoryLog;
import com.tuna.ecommerce.domain.request.inventory.ReqInventoryAdjustDTO;
import com.tuna.ecommerce.domain.response.inventory.ResInventoryDTO;
import com.tuna.ecommerce.service.InventoryService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventory")
@AllArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @APIMessage("Get all inventory successfully")
    public ResponseEntity<List<ResInventoryDTO>> getAll() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @PostMapping("/adjust")
    @APIMessage("Adjust inventory successfully")
    public ResponseEntity<ResInventoryDTO> adjust(@RequestBody ReqInventoryAdjustDTO req) throws IdInvalidException {
        return ResponseEntity.ok(inventoryService.updateStock(
                req.getProductId(),
                req.getVariantId(),
                req.getQuantity(),
                req.getType(),
                req.getNote(),
                req.getMinStockThreshold(),
                req.getMaxStock()));
    }

    @PostMapping("/bulk-adjust")
    @APIMessage("Bulk adjust inventory successfully")
    public ResponseEntity<List<ResInventoryDTO>> bulkAdjust(@RequestBody List<ReqInventoryAdjustDTO> requests)
            throws IdInvalidException {
        return ResponseEntity.ok(inventoryService.bulkUpdateStock(requests));
    }

    @GetMapping("/{id}/logs")
    @APIMessage("Get inventory logs successfully")
    public ResponseEntity<List<InventoryLog>> getLogs(@PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(inventoryService.getHistory(id));
    }
}
