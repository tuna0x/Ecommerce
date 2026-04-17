package com.tuna.ecommerce.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import com.tuna.ecommerce.domain.FlashSaleCampaign;
import com.tuna.ecommerce.domain.FlashSaleItem;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductVariant;
import com.tuna.ecommerce.domain.request.flashsale.ReqFlashSaleCampaignDTO;
import com.tuna.ecommerce.domain.response.flashsale.ResFlashSaleCampaignDTO;
import com.tuna.ecommerce.repository.FlashSaleCampaignRepository;
import com.tuna.ecommerce.repository.FlashSaleItemRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.ProductVariantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashSaleService {
    private final FlashSaleCampaignRepository flashSaleCampaignRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final com.tuna.ecommerce.repository.InventoryRepository inventoryRepository;

    public Optional<FlashSaleItem> findActiveFlashSaleForItem(Long productId) {
        List<FlashSaleItem> items = flashSaleItemRepository.findActiveFlashSaleItem(productId, LocalDateTime.now());
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    public Optional<FlashSaleItem> findActiveFlashSaleItemByVariant(Long variantId) {
        List<FlashSaleItem> items = flashSaleItemRepository.findActiveFlashSaleItemByVariant(variantId, LocalDateTime.now());
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Transactional
    public void incrementSoldQuantity(Long productId) {
        findActiveFlashSaleForItem(productId).ifPresent(item -> {
            if (item.getSoldQuantity() < item.getLimitQuantity()) {
                item.setSoldQuantity(item.getSoldQuantity() + 1);
                flashSaleItemRepository.save(item);
            }
        });
    }

    public boolean isFlashSaleAvailable(Long productId) {
        return findActiveFlashSaleForItem(productId)
                .map(item -> item.getSoldQuantity() < item.getLimitQuantity())
                .orElse(false);
    }

    private void validateTimeConflict(LocalDateTime start, LocalDateTime end, Long currentId) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        
        List<FlashSaleCampaign> overlaps = flashSaleCampaignRepository.findOverlappingCampaigns(start, end, currentId);
        if (!overlaps.isEmpty()) {
            FlashSaleCampaign conflict = overlaps.get(0);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khung giờ này bị trùng với chiến dịch: " + conflict.getName() + 
                " (" + conflict.getStartAt() + " - " + conflict.getEndAt() + ")");
        }
    }

    private void validateStock(Product product, Long variantId, int limitQuantity) {
        ProductVariant variant;
        if (variantId != null) {
            variant = productVariantRepository.findById(variantId).orElse(null);
        } else {
            variant = product.getVariants().stream()
                .filter(v -> v.getSku().startsWith("DEFAULT-")).findFirst().orElse(null);
        }

        if (variant != null) {
            inventoryRepository.findByProductVariant(variant).ifPresent(inv -> {
                if (limitQuantity > inv.getStock()) {
                    String name = (variantId != null) ? "bién thể " + variant.getSku() : "sản phẩm " + product.getName();
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Số lượng Flash Sale cho " + name + " (" + limitQuantity + ") vượt quá tồn kho (" + inv.getStock() + ")");
                }
            });
        }
    }

    @Transactional
    public ResFlashSaleCampaignDTO createCampaign(ReqFlashSaleCampaignDTO req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chiến dịch không được để trống");
        }
        
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chiến dịch phải có ít nhất một sản phẩm");
        }

        validateTimeConflict(req.getStartAt(), req.getEndAt(), null);
        
        FlashSaleCampaign campaign = new FlashSaleCampaign();
        campaign.setName(req.getName().trim());
        campaign.setDescription(req.getDescription());
        campaign.setStartAt(req.getStartAt());
        campaign.setEndAt(req.getEndAt());
        campaign.setActive(true);

        List<FlashSaleItem> items = new ArrayList<>();
        if (req.getItems() != null) {
            for (ReqFlashSaleCampaignDTO.FlashSaleItemRequest itemReq : req.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId()).orElse(null);
                if (product != null) {
                    FlashSaleItem item = new FlashSaleItem();
                    item.setCampaign(campaign);
                    item.setProduct(product);
                    
                    if (itemReq.getVariantId() != null) {
                        productVariantRepository.findById(itemReq.getVariantId()).ifPresent(v -> {
                            item.setVariant(v);
                            // Validate price against variant price
                            BigDecimal basePrice = (v.getPrice() == null || v.getPrice().compareTo(BigDecimal.ZERO) == 0) 
                                ? product.getOriginalPrice() : v.getPrice();
                            if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 || 
                                itemReq.getFlashSalePrice().compareTo(basePrice) >= 0) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá Flash Sale cho biến thể " + v.getSku() + " không hợp lệ (phải >= 0 và < giá gốc)");
                            }
                        });
                    } else {
                        // Validate price against product price
                        if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 || 
                            itemReq.getFlashSalePrice().compareTo(product.getOriginalPrice()) >= 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá Flash Sale cho sản phẩm " + product.getName() + " không hợp lệ (phải >= 0 và < " + product.getOriginalPrice() + ")");
                        }
                    }
                    
                    if (itemReq.getLimitQuantity() == null || itemReq.getLimitQuantity() <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng Sale cho sản phẩm " + product.getName() + " phải lớn hơn 0");
                    }
                    
                    validateStock(product, itemReq.getVariantId(), itemReq.getLimitQuantity());
                    
                    item.setFlashSalePrice(itemReq.getFlashSalePrice());
                    item.setLimitQuantity(itemReq.getLimitQuantity());
                    item.setSoldQuantity(0);
                    items.add(item);
                }
            }
        }
        campaign.setItems(items);
        FlashSaleCampaign saved = flashSaleCampaignRepository.save(campaign);
        return convertToResDTO(saved);
    }

    public List<ResFlashSaleCampaignDTO> getAllCampaigns() {
        return flashSaleCampaignRepository.findAll().stream()
                .map(this::convertToResDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCampaign(Long id) {
        flashSaleCampaignRepository.deleteById(id);
    }

    @Transactional
    public ResFlashSaleCampaignDTO updateCampaign(Long id, ReqFlashSaleCampaignDTO req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chiến dịch không được để trống");
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chiến dịch phải có ít nhất một sản phẩm");
        }

        validateTimeConflict(req.getStartAt(), req.getEndAt(), id);
        
        FlashSaleCampaign campaign = flashSaleCampaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flash Sale Campaign not found"));

        campaign.setName(req.getName().trim());
        campaign.setDescription(req.getDescription());
        campaign.setStartAt(req.getStartAt());
        campaign.setEndAt(req.getEndAt());

        // Clear and update items
        campaign.getItems().clear();
        
        if (req.getItems() != null) {
            for (ReqFlashSaleCampaignDTO.FlashSaleItemRequest itemReq : req.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId()).orElse(null);
                if (product != null) {
                    FlashSaleItem item = new FlashSaleItem();
                    item.setCampaign(campaign);
                    item.setProduct(product);
                    
                    if (itemReq.getVariantId() != null) {
                        productVariantRepository.findById(itemReq.getVariantId()).ifPresent(v -> {
                            item.setVariant(v);
                            // Validate price against variant price
                            BigDecimal basePrice = (v.getPrice() == null || v.getPrice().compareTo(BigDecimal.ZERO) == 0) 
                                ? product.getOriginalPrice() : v.getPrice();
                            if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 || 
                                itemReq.getFlashSalePrice().compareTo(basePrice) >= 0) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá Flash Sale cho biến thể " + v.getSku() + " không hợp lệ (phải >= 0 và < giá gốc)");
                            }
                        });
                    } else {
                        // Validate price against product price
                        if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 || 
                            itemReq.getFlashSalePrice().compareTo(product.getOriginalPrice()) >= 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá Flash Sale cho sản phẩm " + product.getName() + " không hợp lệ (phải >= 0 và < " + product.getOriginalPrice() + ")");
                        }
                    }
                    
                    if (itemReq.getLimitQuantity() == null || itemReq.getLimitQuantity() <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng Sale cho sản phẩm " + product.getName() + " phải lớn hơn 0");
                    }
                    
                    validateStock(product, itemReq.getVariantId(), itemReq.getLimitQuantity());
                    
                    item.setFlashSalePrice(itemReq.getFlashSalePrice());
                    item.setLimitQuantity(itemReq.getLimitQuantity());
                    item.setSoldQuantity(0); 
                    campaign.getItems().add(item);
                }
            }
        }
        FlashSaleCampaign saved = flashSaleCampaignRepository.save(campaign);
        return convertToResDTO(saved);
    }

    public ResFlashSaleCampaignDTO getActiveCampaign() {
        LocalDateTime now = LocalDateTime.now();
        List<FlashSaleCampaign> activeCampaigns = flashSaleCampaignRepository.findActiveCampaigns(now);
        if (activeCampaigns.isEmpty()) {
            return null; // Return null instead of throwing 404
        }
        return convertToResDTO(activeCampaigns.get(0));
    }

    public ResFlashSaleCampaignDTO convertToResDTO(FlashSaleCampaign campaign) {
        ResFlashSaleCampaignDTO dto = new ResFlashSaleCampaignDTO();
        dto.setId(campaign.getId());
        dto.setName(campaign.getName());
        dto.setDescription(campaign.getDescription());
        dto.setStartAt(campaign.getStartAt());
        dto.setEndAt(campaign.getEndAt());
        dto.setActive(campaign.getActive());

        if (campaign.getItems() != null) {
            List<ResFlashSaleCampaignDTO.ResFlashSaleItemDTO> itemDTOs = campaign.getItems().stream()
                    .map(item -> {
                        ResFlashSaleCampaignDTO.ResFlashSaleItemDTO itemDTO = new ResFlashSaleCampaignDTO.ResFlashSaleItemDTO();
                        itemDTO.setId(item.getId());
                        if (item.getProduct() != null) {
                            itemDTO.setProductId(item.getProduct().getId());
                            itemDTO.setProductName(item.getProduct().getName());
                            // Handle images safely
                            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                                itemDTO.setProductImage(item.getProduct().getImages().get(0).getImageUrl());
                            }
                        }
                        if (item.getVariant() != null) {
                            itemDTO.setVariantId(item.getVariant().getId());
                            itemDTO.setVariantSku(item.getVariant().getSku());
                        }
                        itemDTO.setFlashSalePrice(item.getFlashSalePrice());
                        itemDTO.setLimitQuantity(item.getLimitQuantity());
                        itemDTO.setSoldQuantity(item.getSoldQuantity());
                        return itemDTO;
                    }).collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }
        return dto;
    }
}
