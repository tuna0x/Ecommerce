package com.tuna.ecommerce.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.FlashSaleCampaign;
import com.tuna.ecommerce.domain.FlashSaleItem;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.request.flashsale.ReqFlashSaleCampaignDTO;
import com.tuna.ecommerce.repository.FlashSaleCampaignRepository;
import com.tuna.ecommerce.repository.FlashSaleItemRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.domain.ProductVariant;
import com.tuna.ecommerce.repository.ProductVariantRepository;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FlashSaleService {
    private final FlashSaleCampaignRepository flashSaleCampaignRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

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

    @Transactional
    public FlashSaleCampaign createCampaign(ReqFlashSaleCampaignDTO req) {
        FlashSaleCampaign campaign = new FlashSaleCampaign();
        campaign.setName(req.getName());
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
                                throw new RuntimeException("Giá Flash Sale cho biến thể " + v.getSku() + " không hợp lệ (phải >= 0 và < giá gốc)");
                            }
                        });
                    } else {
                        // Validate price against product price
                        if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 || 
                            itemReq.getFlashSalePrice().compareTo(product.getOriginalPrice()) >= 0) {
                            throw new RuntimeException("Giá Flash Sale cho sản phẩm " + product.getName() + " không hợp lệ (phải >= 0 và < " + product.getOriginalPrice() + ")");
                        }
                    }
                    
                    item.setFlashSalePrice(itemReq.getFlashSalePrice());
                    item.setLimitQuantity(itemReq.getLimitQuantity());
                    item.setSoldQuantity(0);
                    items.add(item);
                }
            }
        }
        campaign.setItems(items);
        return flashSaleCampaignRepository.save(campaign);
    }

    public List<FlashSaleCampaign> getAllCampaigns() {
        return flashSaleCampaignRepository.findAll();
    }

    @Transactional
    public void deleteCampaign(Long id) {
        flashSaleCampaignRepository.deleteById(id);
    }

    @Transactional
    public FlashSaleCampaign updateCampaign(Long id, ReqFlashSaleCampaignDTO req) {
        FlashSaleCampaign campaign = flashSaleCampaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flash Sale Campaign not found"));

        campaign.setName(req.getName());
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
                                throw new RuntimeException("Giá Flash Sale cho biến thể " + v.getSku() + " không hợp lệ (phải >= 0 và < giá gốc)");
                            }
                        });
                    } else {
                        // Validate price against product price
                        if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 || 
                            itemReq.getFlashSalePrice().compareTo(product.getOriginalPrice()) >= 0) {
                            throw new RuntimeException("Giá Flash Sale cho sản phẩm " + product.getName() + " không hợp lệ (phải >= 0 và < " + product.getOriginalPrice() + ")");
                        }
                    }
                    
                    item.setFlashSalePrice(itemReq.getFlashSalePrice());
                    item.setLimitQuantity(itemReq.getLimitQuantity());
                    item.setSoldQuantity(0); 
                    campaign.getItems().add(item);
                }
            }
        }
        return flashSaleCampaignRepository.save(campaign);
    }
}
