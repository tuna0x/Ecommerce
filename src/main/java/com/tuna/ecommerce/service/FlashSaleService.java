package com.tuna.ecommerce.service;

import lombok.extern.slf4j.Slf4j;

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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import org.springframework.context.annotation.Lazy;

@Service
@Transactional(readOnly = true)
@Slf4j
public class FlashSaleService {
    private final FlashSaleCampaignRepository flashSaleCampaignRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final com.tuna.ecommerce.repository.InventoryRepository inventoryRepository;
    private final ProductService productService;

    public FlashSaleService(
            FlashSaleCampaignRepository flashSaleCampaignRepository,
            FlashSaleItemRepository flashSaleItemRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            com.tuna.ecommerce.repository.InventoryRepository inventoryRepository,
            @Lazy ProductService productService) {
        this.flashSaleCampaignRepository = flashSaleCampaignRepository;
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryRepository = inventoryRepository;
        this.productService = productService;
    }

    @Cacheable(value = "active_flash_sale_item", key = "'product_' + #productId")
    public Optional<FlashSaleItem> findActiveFlashSaleForItem(Long productId) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        List<FlashSaleItem> items = flashSaleItemRepository.findActiveFlashSaleItem(productId, now);
        if (items.isEmpty()) {
            log.info(">>> No active FlashSale found for product {}. Current system time: {}", productId, now);
            return Optional.empty();
        }
        log.info(">>> Found active FlashSale for product {}. Time: {}", productId, now);
        return Optional.of(items.get(0));
    }

    @Cacheable(value = "active_flash_sale_item", key = "'variant_' + #variantId")
    public Optional<FlashSaleItem> findActiveFlashSaleItemByVariant(Long variantId) {
        List<FlashSaleItem> items = flashSaleItemRepository.findActiveFlashSaleItemByVariant(variantId,
                LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Transactional
    @CacheEvict(value = "active_flash_sale_item", key = "'product_' + #productId")
    public void incrementSoldQuantity(Long productId, int quantity) {
        findActiveFlashSaleForItem(productId).ifPresent(item -> {
            if (item.getSoldQuantity() + quantity <= item.getLimitQuantity()) {
                item.setSoldQuantity(item.getSoldQuantity() + quantity);
                flashSaleItemRepository.save(item);
            } else {
                item.setSoldQuantity(item.getLimitQuantity());
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        List<FlashSaleCampaign> overlaps = flashSaleCampaignRepository.findOverlappingCampaigns(start, end, currentId);
        if (!overlaps.isEmpty()) {
            FlashSaleCampaign conflict = overlaps.get(0);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khung giờ này bị trùng với chiến dịch: " + conflict.getName() +
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
                    String name = (variantId != null) ? "bién thể " + variant.getSku()
                            : "sản phẩm " + product.getName();
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Số lượng Flash Sale cho " + name + " (" + limitQuantity + ") vượt quá tồn kho ("
                                    + inv.getStock() + ")");
                }
            });
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "active_flash_sale_item", allEntries = true),
            @CacheEvict(value = "active_flash_sale_campaign", allEntries = true),
            @CacheEvict(value = "flash_sale_products", allEntries = true)
    })
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
                            BigDecimal basePrice = (v.getPrice() == null
                                    || v.getPrice().compareTo(BigDecimal.ZERO) == 0)
                                            ? product.getOriginalPrice()
                                            : v.getPrice();
                            if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 ||
                                    itemReq.getFlashSalePrice().compareTo(basePrice) >= 0) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá Flash Sale cho biến thể "
                                        + v.getSku() + " không hợp lệ (phải >= 0 và < giá gốc)");
                            }
                        });
                    } else {
                        // Validate price against product price
                        if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 ||
                                itemReq.getFlashSalePrice().compareTo(product.getOriginalPrice()) >= 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Giá Flash Sale cho sản phẩm " + product.getName()
                                            + " không hợp lệ (phải >= 0 và < " + product.getOriginalPrice() + ")");
                        }
                    }

                    if (itemReq.getLimitQuantity() == null || itemReq.getLimitQuantity() <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Số lượng Sale cho sản phẩm " + product.getName() + " phải lớn hơn 0");
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
    @Caching(evict = {
            @CacheEvict(value = "active_flash_sale_item", allEntries = true),
            @CacheEvict(value = "active_flash_sale_campaign", allEntries = true),
            @CacheEvict(value = "flash_sale_products", allEntries = true)
    })
    public void deleteCampaign(Long id) {
        flashSaleCampaignRepository.deleteById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "active_flash_sale_item", allEntries = true),
            @CacheEvict(value = "active_flash_sale_campaign", allEntries = true),
            @CacheEvict(value = "flash_sale_products", allEntries = true)
    })
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
                            BigDecimal basePrice = (v.getPrice() == null
                                    || v.getPrice().compareTo(BigDecimal.ZERO) == 0)
                                            ? product.getOriginalPrice()
                                            : v.getPrice();
                            if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 ||
                                    itemReq.getFlashSalePrice().compareTo(basePrice) >= 0) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá Flash Sale cho biến thể "
                                        + v.getSku() + " không hợp lệ (phải >= 0 và < giá gốc)");
                            }
                        });
                    } else {
                        // Validate price against product price
                        if (itemReq.getFlashSalePrice().compareTo(BigDecimal.ZERO) < 0 ||
                                itemReq.getFlashSalePrice().compareTo(product.getOriginalPrice()) >= 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Giá Flash Sale cho sản phẩm " + product.getName()
                                            + " không hợp lệ (phải >= 0 và < " + product.getOriginalPrice() + ")");
                        }
                    }

                    if (itemReq.getLimitQuantity() == null || itemReq.getLimitQuantity() <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Số lượng Sale cho sản phẩm " + product.getName() + " phải lớn hơn 0");
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
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        log.info(">>> Checking for active/upcoming FlashSale campaign at: {}", now);

        // 1. Check for active campaigns
        List<FlashSaleCampaign> activeCampaigns = flashSaleCampaignRepository.findActiveCampaigns(now);
        if (!activeCampaigns.isEmpty()) {
            log.info(">>> Active campaign found: {}", activeCampaigns.get(0).getName());
            return convertToResDTO(activeCampaigns.get(0));
        }

        // 2. If no active, check for the nearest upcoming campaign
        List<FlashSaleCampaign> upcomingCampaigns = flashSaleCampaignRepository.findUpcomingCampaigns(now);
        if (!upcomingCampaigns.isEmpty()) {
            log.info(">>> Upcoming campaign found: {}", upcomingCampaigns.get(0).getName());
            return convertToResDTO(upcomingCampaigns.get(0));
        }

        log.info(">>> No active or upcoming campaign found.");
        return null;
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

    public ResultPaginationDTO getActiveFlashSaleProducts(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

        List<FlashSaleCampaign> activeCampaigns = flashSaleCampaignRepository.findActiveCampaigns(now);
        if (activeCampaigns.isEmpty()) {
            ResultPaginationDTO emptyRs = new ResultPaginationDTO();
            ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
            mt.setPage(pageable.getPageNumber() + 1);
            mt.setPageSize(pageable.getPageSize());
            emptyRs.setMeta(mt);
            emptyRs.setResult(new ArrayList<>());
            return emptyRs;
        }

        List<Long> campaignProductIds = activeCampaigns.stream()
                .flatMap(c -> c.getItems().stream())
                .map(item -> item.getProduct().getId())
                .distinct()
                .collect(Collectors.toList());

        if (campaignProductIds.isEmpty()) {
            ResultPaginationDTO emptyRs = new ResultPaginationDTO();
            ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
            mt.setPage(pageable.getPageNumber() + 1);
            mt.setPageSize(pageable.getPageSize());
            emptyRs.setMeta(mt);
            emptyRs.setResult(new ArrayList<>());
            return emptyRs;
        }

        Page<Product> productPage = productRepository.findByDeletedFalseAndActiveTrueAndIdIn(campaignProductIds,
                pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(productPage.getNumber() + 1);
        mt.setPageSize(productPage.getSize());
        mt.setPages(productPage.getTotalPages());
        mt.setTotal(productPage.getTotalElements());
        rs.setMeta(mt);

        rs.setResult(productPage.getContent().stream()
                .map(productService::convertToResProductDTO)
                .collect(Collectors.toList()));

        return rs;
    }

    public String getFlashSaleSummaryForChatbot() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        StringBuilder sb = new StringBuilder();

        // 1. Active Campaigns
        List<FlashSaleCampaign> active = flashSaleCampaignRepository.findActiveCampaigns(now);
        if (!active.isEmpty()) {
            sb.append("\n🔥 ĐANG DIỄN RA FLASH SALE CỰC SỐC:\n");
            for (FlashSaleCampaign c : active) {
                sb.append("--- Chiến dịch: ").append(c.getName()).append(" ---\n");
                if (c.getItems() != null) {
                    for (FlashSaleItem item : c.getItems()) {
                        sb.append("- ").append(item.getProduct().getName())
                                .append(" chỉ còn: ").append(item.getFlashSalePrice()).append("đ")
                                .append(" (Số lượng còn ít!)\n");
                    }
                }
            }
        }

        // 2. Upcoming Campaigns
        List<FlashSaleCampaign> upcoming = flashSaleCampaignRepository.findUpcomingCampaigns(now);
        if (!upcoming.isEmpty()) {
            sb.append("\n⏰ SẮP DIỄN RA (Nàng nhớ canh giờ nhé):\n");
            for (int i = 0; i < Math.min(upcoming.size(), 2); i++) { // Lấy tối đa 2 cái sắp tới
                FlashSaleCampaign c = upcoming.get(i);
                sb.append("- ").append(c.getName()).append(" bắt đầu lúc: ")
                        .append(c.getStartAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM")))
                        .append("\n");
            }
        }

        if (sb.length() == 0) {
            return "Hiện tại chưa có chương trình Flash Sale nào đang diễn ra. Nàng theo dõi shop thường xuyên để nhận tin nhé! 🌸";
        }
        return sb.toString();
    }
}
