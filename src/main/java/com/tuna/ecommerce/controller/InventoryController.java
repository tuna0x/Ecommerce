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
import com.tuna.ecommerce.domain.response.inventory.ResInventoryLogDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.turkraft.springfilter.boot.Filter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import java.io.IOException;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventory")
@AllArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @APIMessage("Fetch inventory with pagination successfully")
    public ResponseEntity<ResultPaginationDTO> getAll(
            @Filter Specification<com.tuna.ecommerce.domain.Inventory> spec,
            Pageable pageable) {
        return ResponseEntity.ok(inventoryService.fetchInventory(spec, pageable));
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
                req.getMaxStock(),
                req.getCostPrice()));
    }

    @PostMapping("/bulk-adjust")
    @APIMessage("Bulk adjust inventory successfully")
    public ResponseEntity<List<ResInventoryDTO>> bulkAdjust(@RequestBody List<ReqInventoryAdjustDTO> requests)
            throws IdInvalidException {
        return ResponseEntity.ok(inventoryService.bulkUpdateStock(requests));
    }

    @GetMapping("/{id}/logs")
    @APIMessage("Get inventory logs successfully")
    public ResponseEntity<List<ResInventoryLogDTO>> getLogs(@PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok(inventoryService.getHistory(id));
    }

    @GetMapping("/logs")
    @APIMessage("Get all inventory logs successfully")
    public ResponseEntity<ResultPaginationDTO> getAllLogs(
            @Filter Specification<InventoryLog> spec,
            Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getGlobalHistory(spec, pageable));
    }

    @GetMapping("/logs/export")
    @APIMessage("Export inventory logs successfully")
    public ResponseEntity<Resource> exportLogs(@Filter Specification<InventoryLog> spec) throws IOException {
        byte[] data = inventoryService.exportGlobalHistoryToExcel(spec);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_logs.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(data.length)
                .body(resource);
    }
}
