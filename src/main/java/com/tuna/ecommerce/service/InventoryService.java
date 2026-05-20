package com.tuna.ecommerce.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Inventory;
import com.tuna.ecommerce.domain.InventoryLog;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductVariant;
import com.tuna.ecommerce.repository.InventoryLogRepository;
import com.tuna.ecommerce.repository.InventoryRepository;
import com.tuna.ecommerce.repository.ProductVariantRepository;
import com.tuna.ecommerce.domain.response.inventory.ResInventoryDTO;
import com.tuna.ecommerce.domain.response.inventory.ResInventoryLogDTO;
import com.tuna.ecommerce.domain.request.inventory.ReqInventoryAdjustDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.tuna.ecommerce.ultil.constant.InventoryLogType;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductVariantRepository productVariantRepository;
    private final TelegramService telegramService;
    private final jakarta.persistence.EntityManager entityManager;

    public int getCurrentStock(Long productId, Long variantId) throws IdInvalidException {
        return getOrCreateInventory(productId, variantId).getStock();
    }

    private Inventory getOrCreateInventory(Long productId, Long variantId) throws IdInvalidException {
        Long targetVariantId = variantId;

        if (targetVariantId == null) {
            if (productId == null) throw new IdInvalidException("Product ID must not be null");
            // Fallback: look for DEFAULT variant first, then any active variant
            ProductVariant fallbackVariant = productVariantRepository.findFirstByProduct_IdAndDeletedFalseOrderByIdAsc(productId)
                    .orElseThrow(() -> new IdInvalidException(
                            "Hệ thống không tìm thấy kho hàng cho sản phẩm này."));
            targetVariantId = fallbackVariant.getId();
        }

        ProductVariant variant = productVariantRepository.findById(targetVariantId)
                .orElseThrow(() -> new IdInvalidException("Product Variant not found"));
        return inventoryRepository.findByProductVariant(variant)
                .orElseGet(() -> {
                    Inventory newInv = new Inventory();
                    newInv.setProductVariant(variant);
                    newInv.setStock(0);
                    return inventoryRepository.save(newInv);
                });
    }

    @Transactional
    public Inventory reserveStock(Long productId, Long variantId, int quantity) throws IdInvalidException {
        if (quantity <= 0) {
            throw new IdInvalidException("So luong giu hang phai lon hon 0.");
        }

        Inventory inventory;
        try {
            inventory = getOrCreateInventory(productId, variantId);
        } catch (IdInvalidException e) {
            log.warn("Skipping stock reservation: {} (Product ID: {}, Variant ID: {})", e.getMessage(), productId, variantId);
            return null;
        }

        int updatedRows = inventoryRepository.reserveStockAtomically(inventory.getId(), quantity);
        if (updatedRows == 0) {
            throw new IdInvalidException("Chỉ còn " + inventory.getStock() + " sản phẩm trong kho.");
        }

        entityManager.refresh(inventory);

        InventoryLog log = new InventoryLog();
        log.setInventory(inventory);
        log.setQuantityChange(-quantity);
        log.setType(InventoryLogType.RESERVE);
        log.setNote("Giữ hàng cho đơn hàng mới");
        inventoryLogRepository.save(log);
        
        checkLowStockAndNotify(inventory);

        return inventory;
    }

    @Transactional
    public void commitStock(Long productId, Long variantId, int quantity, String note) throws IdInvalidException {
        Inventory inventory;
        try {
            inventory = getOrCreateInventory(productId, variantId);
        } catch (IdInvalidException e) {
            log.warn("Skipping stock commit: {} (Product ID: {}, Variant ID: {})", e.getMessage(), productId, variantId);
            return;
        }

        if (inventory.getReservedStock() < quantity) {
            if (inventory.getStock() < quantity) {
                throw new IdInvalidException("Hết hàng trong kho để xác nhận đơn.");
            }
            inventory.setStock(inventory.getStock() - quantity);
        } else {
            inventory.setReservedStock(inventory.getReservedStock() - quantity);
        }

        inventoryRepository.save(inventory);

        InventoryLog log = new InventoryLog();
        log.setInventory(inventory);
        log.setQuantityChange(0);
        log.setType(InventoryLogType.SALE);
        log.setNote(note != null ? note : "Xác nhận đơn hàng và trừ kho chính thức");
        inventoryLogRepository.save(log);

        checkLowStockAndNotify(inventory);
    }

    @Transactional
    public void releaseStock(Long productId, Long variantId, int quantity, String note) throws IdInvalidException {
        Inventory inventory;
        try {
            inventory = getOrCreateInventory(productId, variantId);
        } catch (IdInvalidException e) {
            log.warn("Skipping stock release: {} (Product ID: {}, Variant ID: {})", e.getMessage(), productId, variantId);
            return;
        }

        inventory.setReservedStock(Math.max(0, inventory.getReservedStock() - quantity));
        inventory.setStock(inventory.getStock() + quantity);
        inventoryRepository.save(inventory);

        InventoryLog log = new InventoryLog();
        log.setInventory(inventory);
        log.setQuantityChange(quantity);
        log.setType(InventoryLogType.RELEASE);
        log.setNote(note != null ? note : "Giải phóng hàng từ đơn bị hủy");
        inventoryLogRepository.save(log);
    }

    @Transactional
    public ResInventoryDTO updateStock(Long productId, Long variantId, int quantityChange, InventoryLogType type,
            String note, Integer minStockThreshold, Integer maxStock, Double costPrice) throws IdInvalidException {
        Inventory inventory = getOrCreateInventory(productId, variantId);

        Double oldCostPrice = inventory.getCostPrice();

        if (minStockThreshold != null) {
            inventory.setMinStockThreshold(minStockThreshold);
        }
        if (maxStock != null) {
            inventory.setMaxStock(maxStock);
        }
        if (costPrice != null) {
            inventory.setCostPrice(costPrice);
        }

        int newStock = inventory.getStock() + quantityChange;
        if (newStock < 0) {
            throw new IdInvalidException("Số lượng tồn kho không đủ để thực hiện điều chỉnh.");
        }

        inventory.setStock(newStock);
        inventory = inventoryRepository.save(inventory);

        InventoryLog log = new InventoryLog();
        log.setInventory(inventory);
        log.setQuantityChange(quantityChange);
        log.setType(type);
        log.setNote(note);
        log.setOldCostPrice(oldCostPrice);
        log.setNewCostPrice(inventory.getCostPrice());
        inventoryLogRepository.save(log);

        checkLowStockAndNotify(inventory);

        return convertToResInventoryDTO(inventory);
    }
    
    private void checkLowStockAndNotify(Inventory inventory) {
        if (inventory.getStock() < inventory.getMinStockThreshold()) {
            String productName = "N/A";
            String sku = "N/A";
            if (inventory.getProductVariant() != null) {
                sku = inventory.getProductVariant().getSku();
                if (inventory.getProductVariant().getProduct() != null) {
                    productName = inventory.getProductVariant().getProduct().getName();
                }
            }
            telegramService.sendLowStockAlert(productName, sku, inventory.getStock());
        }
    }

    @Transactional
    public List<ResInventoryDTO> bulkUpdateStock(List<ReqInventoryAdjustDTO> requests) throws IdInvalidException {
        return requests.stream()
                .map(req -> {
                    try {
                        return updateStock(
                                req.getProductId(),
                                req.getVariantId(),
                                req.getQuantity(),
                                req.getType(),
                                req.getNote(),
                                req.getMinStockThreshold(),
                                req.getMaxStock(),
                                req.getCostPrice());
                    } catch (IdInvalidException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                })
                .collect(Collectors.toList());
    }

    public ResultPaginationDTO fetchInventory(Specification<Inventory> spec, Pageable pageable) {
        Page<Inventory> pageInventory = this.inventoryRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageInventory.getTotalPages());
        mt.setTotal(pageInventory.getTotalElements());

        rs.setMeta(mt);

        List<ResInventoryDTO> listInventory = pageInventory.getContent()
                .stream()
                .map(this::convertToResInventoryDTO)
                .collect(Collectors.toList());

        rs.setResult(listInventory);
        return rs;
    }

    public List<ResInventoryDTO> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .filter(inv -> inv.getProductVariant() != null && !inv.getProductVariant().isDeleted())
                .map(this::convertToResInventoryDTO)
                .collect(Collectors.toList());
    }

    public ResInventoryDTO convertToResInventoryDTO(Inventory inventory) {
        if (inventory == null)
            return null;

        ResInventoryDTO.ProductVariantDTO variantDTO = null;
        if (inventory.getProductVariant() != null) {
            ProductVariant variant = inventory.getProductVariant();
            Product product = variant.getProduct();

            String thumbnail = null;
            if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
                thumbnail = product.getImages().get(0).getImageUrl();
            }

            ResInventoryDTO.ProductDTO productDTO = ResInventoryDTO.ProductDTO.builder()
                    .id(product != null ? product.getId() : null)
                    .name(product != null ? product.getName() : "Sản phẩm đã bị xóa")
                    .thumbnail(thumbnail)
                    .categoryName(product != null && product.getCategory() != null ? product.getCategory().getName() : "N/A")
                    .build();

            variantDTO = ResInventoryDTO.ProductVariantDTO.builder()
                    .id(variant.getId())
                    .sku(variant.getSku())
                    .product(productDTO)
                    .build();
        }

        return ResInventoryDTO.builder()
                .id(inventory.getId())
                .stock(inventory.getStock())
                .reservedStock(inventory.getReservedStock())
                .costPrice(inventory.getCostPrice())
                .minStockThreshold(inventory.getMinStockThreshold())
                .maxStock(inventory.getMaxStock())
                .updatedAt(inventory.getUpdatedAt() != null ? inventory.getUpdatedAt() : inventory.getCreatedAt())
                .productVariant(variantDTO)
                .build();
    }

    public List<ResInventoryLogDTO> getHistory(Long inventoryId) throws IdInvalidException {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IdInvalidException("Inventory not found"));
        return inventoryLogRepository.findByInventoryOrderByCreatedAtDesc(inventory)
                .stream()
                .map(this::convertToResInventoryLogDTO)
                .collect(Collectors.toList());
    }

    public ResultPaginationDTO getGlobalHistory(Specification<InventoryLog> spec, Pageable pageable) {
        Page<InventoryLog> pageLogs = inventoryLogRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageLogs.getNumber() + 1);
        meta.setPageSize(pageLogs.getSize());
        meta.setPages(pageLogs.getTotalPages());
        meta.setTotal(pageLogs.getTotalElements());

        rs.setMeta(meta);
        List<ResInventoryLogDTO> list = pageLogs.getContent().stream()
                .map(this::convertToResInventoryLogDTO)
                .collect(Collectors.toList());
        rs.setResult(list);
        return rs;
    }

    public byte[] exportGlobalHistoryToExcel(Specification<InventoryLog> spec) throws IOException {
        List<InventoryLog> logs = inventoryLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo Kho hàng");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Thời gian", "Sản phẩm", "SKU", "Biến động", "Loại", "Ghi chú", "Người thực hiện"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data
            int rowIdx = 1;
            for (InventoryLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "—");
                
                String pName = "N/A";
                String sku = "N/A";
                if (log.getInventory() != null && log.getInventory().getProductVariant() != null) {
                    sku = log.getInventory().getProductVariant().getSku();
                    if (log.getInventory().getProductVariant().getProduct() != null) {
                        pName = log.getInventory().getProductVariant().getProduct().getName();
                    }
                }
                
                row.createCell(2).setCellValue(pName);
                row.createCell(3).setCellValue(sku != null ? sku : "N/A");
                row.createCell(4).setCellValue(log.getQuantityChange());
                row.createCell(5).setCellValue(log.getType() != null ? log.getType().toString() : "UNKNOWN");
                row.createCell(6).setCellValue(log.getNote() != null ? log.getNote() : "");
                row.createCell(7).setCellValue(log.getCreatedBy() != null ? log.getCreatedBy() : "System");
            }

            // Auto-size columns with fallback
            for (int i = 0; i < columns.length; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception e) {
                    sheet.setColumnWidth(i, 5000); // Fallback to fixed width
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public ResInventoryLogDTO convertToResInventoryLogDTO(InventoryLog log) {
        if (log == null) return null;

        ResInventoryLogDTO.InventoryDTO invDTO = null;
        if (log.getInventory() != null) {
            Inventory inv = log.getInventory();
            ProductVariant variant = inv.getProductVariant();

            ResInventoryLogDTO.ProductDTO productDTO = null;
            ResInventoryLogDTO.ProductVariantDTO variantDTO = null;

            if (variant != null) {
                Product product = variant.getProduct();
                if (product != null) {
                    String thumbnail = null;
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        thumbnail = product.getImages().get(0).getImageUrl();
                    }
                    productDTO = ResInventoryLogDTO.ProductDTO.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .thumbnail(thumbnail)
                            .build();
                }

                variantDTO = ResInventoryLogDTO.ProductVariantDTO.builder()
                        .id(variant.getId())
                        .sku(variant.getSku())
                        .product(productDTO)
                        .build();
            }

            invDTO = ResInventoryLogDTO.InventoryDTO.builder()
                    .id(inv.getId())
                    .productVariant(variantDTO)
                    .build();
        }

        return ResInventoryLogDTO.builder()
                .id(log.getId())
                .quantityChange(log.getQuantityChange())
                .type(log.getType())
                .note(log.getNote())
                .oldCostPrice(log.getOldCostPrice())
                .newCostPrice(log.getNewCostPrice())
                .createdAt(log.getCreatedAt())
                .createdBy(log.getCreatedBy())
                .inventory(invDTO)
                .build();
    }

    @Transactional
    public void syncInitialInventory(Product product, Map<String, Integer> variantStocks, Map<String, Double> variantCostPrices) {
        // Ensure every variant has an inventory record
        for (ProductVariant variant : product.getVariants()) {
            Inventory inv = inventoryRepository.findByProductVariant(variant).orElse(null);
            Integer initialStock = (variantStocks != null) ? variantStocks.get(variant.getSku()) : null;
            Double initialCost = (variantCostPrices != null) ? variantCostPrices.get(variant.getSku()) : null;

            if (inv == null) {
                inv = new Inventory();
                inv.setProductVariant(variant);
                inv.setStock(initialStock != null ? initialStock : 0);
                if (initialCost != null) inv.setCostPrice(initialCost);
                inventoryRepository.save(inv);
            } else if ((initialStock != null && inv.getStock() != initialStock) || (initialCost != null && inv.getCostPrice() != initialCost)) {
                Double oldCost = inv.getCostPrice();
                int diff = (initialStock != null) ? initialStock - inv.getStock() : 0;
                if (initialStock != null) inv.setStock(initialStock);
                if (initialCost != null) inv.setCostPrice(initialCost);
                inventoryRepository.save(inv);

                InventoryLog log = new InventoryLog();
                log.setInventory(inv);
                log.setQuantityChange(diff);
                log.setType(InventoryLogType.ADJUSTMENT);
                log.setNote("Đồng bộ tồn kho từ trang quản lý sản phẩm");
                log.setOldCostPrice(oldCost);
                log.setNewCostPrice(inv.getCostPrice());
                inventoryLogRepository.save(log);
            }
        }

        // Cleanup: Remove inventory for variants no longer linked or marked as deleted
        List<Inventory> existingInventories = inventoryRepository.findByProductVariantProduct(product);
        List<Inventory> variantsToRemove = existingInventories.stream()
                .filter(inv -> {
                    ProductVariant variant = inv.getProductVariant();
                    // Remove if variant is null, variant is marked as deleted, or variant is not in the current product's list
                    return variant == null || variant.isDeleted() || 
                           product.getVariants().stream().noneMatch(v -> v.getId() != null && v.getId().equals(variant.getId()));
                })
                .collect(Collectors.toList());

        if (!variantsToRemove.isEmpty()) {
            inventoryRepository.deleteAll(variantsToRemove);
        }
    }
}
