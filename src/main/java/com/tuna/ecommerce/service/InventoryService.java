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
import com.tuna.ecommerce.domain.request.inventory.ReqInventoryAdjustDTO;
import com.tuna.ecommerce.ultil.constant.InventoryLogType;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductVariantRepository productVariantRepository;

    public int getCurrentStock(Long productId, Long variantId) throws IdInvalidException {
        return getOrCreateInventory(productId, variantId).getStock();
    }

    private Inventory getOrCreateInventory(Long productId, Long variantId) throws IdInvalidException {
        Long targetVariantId = variantId;

        if (targetVariantId == null) {
            if (productId == null) throw new IdInvalidException("Product ID must not be null");
            // Fallback for simple products: look for the DEFAULT variant
            ProductVariant defaultVariant = productVariantRepository.findAll().stream()
                    .filter(v -> v.getProduct() != null && v.getProduct().getId().equals(productId) && v.getSku().contains("DEFAULT-"))
                    .findFirst()
                    .orElseThrow(() -> new IdInvalidException(
                            "Hệ thống không tìm thấy kho hàng mặc định cho sản phẩm này."));
            targetVariantId = defaultVariant.getId();
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
        Inventory inventory = getOrCreateInventory(productId, variantId);
        if (inventory.getStock() < quantity) {
            throw new IdInvalidException("Chỉ còn " + inventory.getStock() + " sản phẩm trong kho.");
        }

        inventory.setStock(inventory.getStock() - quantity);
        inventory.setReservedStock(inventory.getReservedStock() + quantity);
        inventory = inventoryRepository.save(inventory);

        InventoryLog log = new InventoryLog();
        log.setInventory(inventory);
        log.setQuantityChange(-quantity);
        log.setType(InventoryLogType.RESERVE);
        log.setNote("Giữ hàng cho đơn hàng mới");
        inventoryLogRepository.save(log);

        return inventory;
    }

    @Transactional
    public void commitStock(Long productId, Long variantId, int quantity, String note) throws IdInvalidException {
        Inventory inventory = getOrCreateInventory(productId, variantId);
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
    }

    @Transactional
    public void releaseStock(Long productId, Long variantId, int quantity, String note) throws IdInvalidException {
        Inventory inventory = getOrCreateInventory(productId, variantId);
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
            String note, Integer minStockThreshold, Integer maxStock) throws IdInvalidException {
        Inventory inventory = getOrCreateInventory(productId, variantId);

        if (minStockThreshold != null) {
            inventory.setMinStockThreshold(minStockThreshold);
        }
        if (maxStock != null) {
            inventory.setMaxStock(maxStock);
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
        inventoryLogRepository.save(log);

        return convertToResInventoryDTO(inventory);
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
                                req.getMaxStock());
                    } catch (IdInvalidException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                })
                .collect(Collectors.toList());
    }

    public List<ResInventoryDTO> getAllInventory() {
        return inventoryRepository.findAll().stream()
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
                .minStockThreshold(inventory.getMinStockThreshold())
                .maxStock(inventory.getMaxStock())
                .updatedAt(inventory.getUpdatedAt() != null ? inventory.getUpdatedAt() : inventory.getCreatedAt())
                .productVariant(variantDTO)
                .build();
    }

    public List<InventoryLog> getHistory(Long inventoryId) throws IdInvalidException {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IdInvalidException("Inventory not found"));
        return inventoryLogRepository.findByInventoryOrderByCreatedAtDesc(inventory);
    }

    @Transactional
    public void syncInitialInventory(Product product, Map<String, Integer> variantStocks) {
        // Ensure every variant has an inventory record
        for (ProductVariant variant : product.getVariants()) {
            Inventory inv = inventoryRepository.findByProductVariant(variant).orElse(null);
            Integer initialStock = (variantStocks != null) ? variantStocks.get(variant.getSku()) : null;

            if (inv == null) {
                inv = new Inventory();
                inv.setProductVariant(variant);
                inv.setStock(initialStock != null ? initialStock : 0);
                inventoryRepository.save(inv);
            } else if (initialStock != null && inv.getStock() != initialStock) {
                int diff = initialStock - inv.getStock();
                inv.setStock(initialStock);
                inventoryRepository.save(inv);

                InventoryLog log = new InventoryLog();
                log.setInventory(inv);
                log.setQuantityChange(diff);
                log.setType(InventoryLogType.ADJUSTMENT);
                log.setNote("Đồng bộ tồn kho từ trang quản lý sản phẩm");
                inventoryLogRepository.save(log);
            }
        }

        // Cleanup: Remove inventory for variants no longer linked to this product
        // We find all inventory records where variant.product == this product, but
        // variant is not in product.getVariants()
        List<Inventory> variantsToRemove = inventoryRepository.findAll().stream()
                .filter(inv -> inv.getProductVariant() != null &&
                        inv.getProductVariant().getProduct() != null &&
                        inv.getProductVariant().getProduct().getId().equals(product.getId()) &&
                        !product.getVariants().contains(inv.getProductVariant()))
                .collect(Collectors.toList());

        if (!variantsToRemove.isEmpty()) {
            inventoryRepository.deleteAll(variantsToRemove);
        }
    }
}
