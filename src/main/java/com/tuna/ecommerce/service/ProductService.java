package com.tuna.ecommerce.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.tuna.ecommerce.domain.response.promotion.ResPriceResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.Brand;
import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductAttributeValue;
import com.tuna.ecommerce.domain.ProductImage;
import com.tuna.ecommerce.domain.Inventory;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.request.product.ReqCreateProductDTO;
import com.tuna.ecommerce.domain.request.product.ReqUpdateProductDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.repository.ProductAttributeValueRepository;
import com.tuna.ecommerce.repository.ProductImageRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.ReviewRepository;
import com.tuna.ecommerce.domain.ProductVariant;
import com.tuna.ecommerce.repository.ProductVariantRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.tuna.ecommerce.domain.ProductPromotion;
import com.tuna.ecommerce.repository.ProductPromotionRepository;
import com.tuna.ecommerce.repository.PromotionRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import com.tuna.ecommerce.repository.InventoryRepository;
import com.tuna.ecommerce.repository.FlashSaleCampaignRepository;
import com.tuna.ecommerce.repository.FlashSaleItemRepository;
import com.tuna.ecommerce.domain.FlashSaleCampaign;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryService categoryService;
    private final CloudinaryService cloudinaryService;
    private final ProductImageRepository productImageRepository;
    private final BrandService brandService;
    private final PricingService pricingService;
    private final ProductVariantRepository productVariantRepository;
    private final ProductPromotionRepository productPromotionRepository;
    private final PromotionRepository promotionRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final FlashSaleCampaignRepository flashSaleCampaignRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final FlashSaleService flashSaleService;

    public ProductService(
            ProductRepository productRepository,
            ReviewRepository reviewRepository,
            AttributeValueRepository attributeValueRepository,
            ProductAttributeValueRepository productAttributeValueRepository,
            CategoryService categoryService,
            CloudinaryService cloudinaryService,
            ProductImageRepository productImageRepository,
            BrandService brandService,
            PricingService pricingService,
            ProductVariantRepository productVariantRepository,
            ProductPromotionRepository productPromotionRepository,
            PromotionRepository promotionRepository,
            @Lazy InventoryService inventoryService,
            InventoryRepository inventoryRepository,
            FlashSaleCampaignRepository flashSaleCampaignRepository,
            FlashSaleItemRepository flashSaleItemRepository,
            FlashSaleService flashSaleService) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.categoryService = categoryService;
        this.cloudinaryService = cloudinaryService;
        this.productImageRepository = productImageRepository;
        this.brandService = brandService;
        this.pricingService = pricingService;
        this.productVariantRepository = productVariantRepository;
        this.productPromotionRepository = productPromotionRepository;
        this.promotionRepository = promotionRepository;
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
        this.flashSaleCampaignRepository = flashSaleCampaignRepository;
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.flashSaleService = flashSaleService;
    }

    @jakarta.annotation.PostConstruct
    public void initPrices() {
        // Full sync on startup to ensure all filtered prices are accurate
        this.syncAllProductsPrice();
    }

    public Product handleCreate(ReqCreateProductDTO product, List<MultipartFile> files)
            throws IdInvalidException, IOException {
        Category category = this.categoryService.handleGetById(product.getCategoryId());
        if (category == null) {
            throw new IdInvalidException("Category not found with id: " + product.getCategoryId());
        }

        Product newProduct = new Product();
        newProduct.setName(product.getName());
        newProduct.setOriginalPrice(product.getOriginalPrice());
        newProduct.setCategory(category);

        Brand brand = this.brandService.handleGetById(product.getBrandId());
        if (brand != null) {
            newProduct.setBrand(brand);
        }

        // Save first to get ID for images and attributes
        newProduct = this.productRepository.save(newProduct);

        List<ProductImage> uploadedImages = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
                ProductImage image = new ProductImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                image.setProduct(newProduct);
                this.productImageRepository.save(image);
                newProduct.addImage(image);
                uploadedImages.add(image);
            }
        }

        if (product.getAttributeValue() != null) {
            for (Long id : product.getAttributeValue()) {
                AttributeValue attributeValue = this.attributeValueRepository.findById(id).orElse(null);
                if (attributeValue != null) {
                    ProductAttributeValue pav = new ProductAttributeValue();
                    pav.setProduct(newProduct);
                    pav.setAttributeValue(attributeValue);
                    this.productAttributeValueRepository.save(pav);
                    newProduct.getProductAttributeValues().add(pav);
                }
            }
        }

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (ReqCreateProductDTO.VariantDTO vDto : product.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(newProduct);
                variant.setSku(vDto.getSku());
                variant.setPrice(vDto.getPrice());
                variant.setWeight(vDto.getWeight());

                if (vDto.getProductImageId() != null) {
                    this.productImageRepository.findById(vDto.getProductImageId()).ifPresent(variant::setProductImage);
                } else if (vDto.getProductImageIndex() != null && vDto.getProductImageIndex() < uploadedImages.size()) {
                    variant.setProductImage(uploadedImages.get(vDto.getProductImageIndex()));
                }

                if (vDto.getAttributeValues() != null) {
                    List<AttributeValue> avs = vDto.getAttributeValues().stream()
                            .map(id -> this.attributeValueRepository.findById(id).orElse(null))
                            .filter(av -> av != null)
                            .collect(Collectors.toList());
                    variant.setAttributeValues(avs);
                }
                this.productVariantRepository.save(variant);
                newProduct.getVariants().add(variant);
            }
        } else {
            // Force a default variant for simple products
            ProductVariant defaultVariant = new ProductVariant();
            defaultVariant.setProduct(newProduct);
            defaultVariant.setSku("DEFAULT-" + newProduct.getId());
            defaultVariant.setPrice(newProduct.getOriginalPrice());
            this.productVariantRepository.save(defaultVariant);
            newProduct.getVariants().add(defaultVariant);
        }

        this.syncProductWithVariants(newProduct);
        this.updateProductPrice(newProduct);
        newProduct = this.productRepository.save(newProduct);

        Map<String, Integer> variantStocks = new HashMap<>();
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            variantStocks = product.getVariants().stream()
                    .collect(Collectors.toMap(ReqCreateProductDTO.VariantDTO::getSku,
                            ReqCreateProductDTO.VariantDTO::getStock));
        } else {
            variantStocks.put("DEFAULT-" + newProduct.getId(), product.getStock());
        }
        this.inventoryService.syncInitialInventory(newProduct, variantStocks);
        return newProduct;
    }

    public Product handleGetById(long id) {
        Product product = this.productRepository.findById(id).orElse(null);
        if (product != null && product.isDeleted()) {
            return null;
        }
        return product;
    }

    public Product handleUpdate(ReqUpdateProductDTO product, List<MultipartFile> files)
            throws IdInvalidException, IOException {
        Product cur = this.handleGetById(product.getId());
        if (cur == null) {
            throw new IdInvalidException("Product not found with id: " + product.getId());
        }

        Category category = this.categoryService.handleGetById(product.getCategoryId());
        if (category == null) {
            throw new IdInvalidException("Category not found with id: " + product.getCategoryId());
        }

        cur.setName(product.getName());
        cur.setOriginalPrice(product.getOriginalPrice());
        cur.setCategory(category);

        Brand brand = this.brandService.handleGetById(product.getBrandId());
        cur.setBrand(brand);

        if (product.getActive() != null) {
            cur.setActive(product.getActive());
        }

        // Update Attributes
        if (product.getAttributeValue() != null) {
            // Remove old ones
            cur.getProductAttributeValues().clear();

            // Add new ones
            for (Long id : product.getAttributeValue()) {
                AttributeValue attributeValue = this.attributeValueRepository.findById(id).orElse(null);
                if (attributeValue != null) {
                    ProductAttributeValue pav = new ProductAttributeValue();
                    pav.setProduct(cur);
                    pav.setAttributeValue(attributeValue);
                    cur.getProductAttributeValues().add(pav);
                }
            }
        }

        List<ProductImage> newlyUploadedImages = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
                ProductImage image = new ProductImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                image.setProduct(cur);
                this.productImageRepository.save(image);
                cur.addImage(image);
                newlyUploadedImages.add(image);
            }
        }

        // Update Variants
        if (product.getVariants() != null) {
            Map<String, ProductVariant> existingVariants = cur.getVariants().stream()
                    .collect(Collectors.toMap(ProductVariant::getSku, v -> v));

            List<ProductVariant> updatedVariants = new ArrayList<>();

            for (ReqUpdateProductDTO.VariantDTO vDto : product.getVariants()) {
                ProductVariant variant = existingVariants.get(vDto.getSku());
                if (variant == null) {
                    variant = new ProductVariant();
                    variant.setProduct(cur);
                    variant.setSku(vDto.getSku());
                }

                variant.setPrice(vDto.getPrice());
                variant.setWeight(vDto.getWeight());

                if (vDto.getProductImageId() != null) {
                    this.productImageRepository.findById(vDto.getProductImageId()).ifPresent(variant::setProductImage);
                } else if (vDto.getProductImageIndex() != null
                        && vDto.getProductImageIndex() < newlyUploadedImages.size()) {
                    variant.setProductImage(newlyUploadedImages.get(newlyUploadedImages.size() - 1));
                }

                if (vDto.getAttributeValues() != null) {
                    List<AttributeValue> avs = vDto.getAttributeValues().stream()
                            .map(id -> this.attributeValueRepository.findById(id).orElse(null))
                            .filter(av -> av != null)
                            .collect(Collectors.toList());
                    variant.setAttributeValues(avs);
                }
                updatedVariants.add(variant);
            }

            // Soft delete removed variants instead of physically removing them (Avoiding FK Error)
            for (ProductVariant v : cur.getVariants()) {
                boolean stillExists = product.getVariants().stream()
                        .anyMatch(vDto -> vDto.getSku().equals(v.getSku()));
                if (!stillExists) {
                    v.setDeleted(true);
                } else {
                    v.setDeleted(false); // Restore if it was deleted before
                }
            }

            for (ProductVariant updated : updatedVariants) {
                if (!cur.getVariants().contains(updated)) {
                    cur.getVariants().add(updated);
                }
            }
        }

        // Update Images (Selective Deletion)
        if (product.getImage() != null) {
            List<String> keepUrls = product.getImage();
            List<ProductImage> toDelete = cur.getImages().stream()
                    .filter(img -> !keepUrls.contains(img.getImageUrl()))
                    .collect(Collectors.toList());

            for (ProductImage img : toDelete) {
                try {
                    this.cloudinaryService.deleteFile(img.getPublicId());
                } catch (Exception e) {
                    // Log or handle deletion error
                }
                this.productImageRepository.delete(img);
                cur.getImages().remove(img);
            }
        } else if (files != null && !files.isEmpty()) {
            // Legacy behavior: if no keep-list provided but new files are, clear all
            for (ProductImage img : new ArrayList<>(cur.getImages())) {
                try {
                    this.cloudinaryService.deleteFile(img.getPublicId());
                } catch (Exception e) {
                }
                this.productImageRepository.delete(img);
            }
            cur.getImages().clear();
        }

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
                ProductImage image = new ProductImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                image.setProduct(cur);
                this.productImageRepository.save(image);
                cur.addImage(image);
            }
        }

        this.syncProductWithVariants(cur);
        this.updateProductPrice(cur);
        cur = this.productRepository.save(cur);

        Map<String, Integer> variantStocks = new HashMap<>();
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            variantStocks = product.getVariants().stream()
                    .collect(Collectors.toMap(ReqUpdateProductDTO.VariantDTO::getSku,
                            ReqUpdateProductDTO.VariantDTO::getStock));
        } else {
            variantStocks.put("DEFAULT-" + cur.getId(), product.getStock());
        }
        this.inventoryService.syncInitialInventory(cur, variantStocks);
        return cur;
    }

    private void syncProductWithVariants(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return;
        }

        BigDecimal minPrice = null;

        for (ProductVariant variant : product.getVariants()) {
            // Price synchronization (using originalPrice as the base display price)
            if (variant.getPrice() != null) {
                if (minPrice == null || variant.getPrice().compareTo(minPrice) < 0) {
                    minPrice = variant.getPrice();
                }
            }
        }

        if (minPrice != null) {
            product.setOriginalPrice(minPrice);
        }
        this.updateProductPrice(product);
    }

    public void updateProductPrice(Product product) {
        if (product == null || product.getOriginalPrice() == null) {
            return;
        }
        // Calculate the current active price with promotions
        ResPriceResultDTO priceResult = this.pricingService.calculatePrice(product);
        if (priceResult != null) {
            product.setPrice(priceResult.getFinalPrice());
        } else {
            product.setPrice(product.getOriginalPrice());
        }
    }

    @Transactional
    public void syncAllProductsPrice() {
        List<Product> products = this.productRepository.findAll();
        for (Product p : products) {
            this.updateProductPrice(p);
            this.productRepository.save(p);
        }
    }

    public void handleDelete(long id) throws IOException {
        Product product = this.handleGetById(id);
        if (product != null) {
            product.setDeleted(true);
            this.productRepository.save(product);
        }
    }

    public ResultPaginationDTO handleGetAll(Specification<Product> spec, Long categoryId, String search,
            Pageable page, boolean isPublic) {
        // Default filter: never show deleted products
        Specification<Product> softDeleteSpec = (root, query, cb) -> cb.equal(root.get("deleted"), false);
        spec = (spec == null) ? softDeleteSpec : spec.and(softDeleteSpec);

        // Public filter: only show active products
        if (isPublic) {
            Specification<Product> activeSpec = (root, query, cb) -> cb.equal(root.get("active"), true);
            spec = spec.and(activeSpec);
        }

        if (categoryId != null) {
            List<Long> categoryIds = this.categoryService.getAllIdsInHierarchy(categoryId);
            Specification<Product> categorySpec = (root, query, cb) -> root.get("category").get("id")
                    .in(categoryIds);
            spec = (spec == null) ? categorySpec : spec.and(categorySpec);
        }

        // Unaccented search: if search param exists, add OR condition for nameUnsigned
        if (search != null && !search.trim().isEmpty()) {
            String unsignedSearch = Product.removeVietnameseAccents(search.trim());
            Specification<Product> unsignedSpec = (root, query, cb) -> cb.like(cb.lower(root.get("nameUnsigned")),
                    "%" + unsignedSearch.toLowerCase() + "%");
            // AND with the original spec from springfilter (which searches `name`)
            if (spec != null) {
                spec = spec.and(unsignedSpec);
            } else {
                spec = unsignedSpec;
            }
        }

        Page<Product> product = this.productRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(product.getNumber() + 1);
        meta.setPageSize(product.getSize());
        meta.setPages(product.getTotalPages());
        meta.setTotal(product.getTotalElements());

        rs.setMeta(meta);
        List<ResProductDTO> list = product.getContent().stream().map(item -> this.convertToResProductDTO(item))
                .collect(Collectors.toList());
        rs.setResult(list);
        return rs;
    }

    public boolean findByName(String name) {
        return this.productRepository.existsByName(name);
    }

    @Cacheable(value = "related_products", key = "#id")
    public List<ResProductDTO> getRelatedProducts(Long id) {
        Product product = this.handleGetById(id);
        if (product == null || product.getCategory() == null) {
            return new ArrayList<>();
        }

        Long categoryId = product.getCategory().getId();
        Long brandId = product.getBrand() != null ? product.getBrand().getId() : null;

        java.util.LinkedHashSet<Product> relatedSet = new java.util.LinkedHashSet<>();

        // 1. Same Brand + Same Category (Top 4)
        if (brandId != null) {
            List<Product> brandRelated = this.productRepository
                    .findTop4ByDeletedFalseAndActiveTrueAndCategoryIdAndBrandIdAndIdNotOrderBySoldCountDesc(categoryId, brandId, id);
            relatedSet.addAll(brandRelated);
        }

        // 2. Best Sellers in Category (Fill up to 8)
        if (relatedSet.size() < 8) {
            List<Product> popularRelated = this.productRepository
                    .findTop8ByDeletedFalseAndActiveTrueAndCategoryIdAndIdNotOrderBySoldCountDesc(categoryId, id);
            for (Product p : popularRelated) {
                if (relatedSet.size() >= 8)
                    break;
                relatedSet.add(p);
            }
        }

        // 3. Fallback: Latest in Category (If still not enough)
        if (relatedSet.size() < 8) {
            List<Product> latestRelated = this.productRepository
                    .findTop8ByDeletedFalseAndActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(categoryId, id);
            for (Product p : latestRelated) {
                if (relatedSet.size() >= 8)
                    break;
                relatedSet.add(p);
            }
        }

        return relatedSet.stream().map(this::convertToResProductDTO).collect(Collectors.toList());
    }

    public ResultPaginationDTO handleGetFlashSale(Pageable pageable) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // Check for Active Flash Sale Campaigns (Strict Mode)
        List<FlashSaleCampaign> activeCampaigns = this.flashSaleCampaignRepository.findActiveCampaigns(now);
        if (!activeCampaigns.isEmpty()) {
            List<Long> campaignProductIds = activeCampaigns.stream()
                    .flatMap(c -> c.getItems().stream())
                    .map(item -> item.getProduct().getId())
                    .distinct()
                    .collect(Collectors.toList());

            if (!campaignProductIds.isEmpty()) {
                Page<Product> flashSaleProducts = this.productRepository.findByDeletedFalseAndActiveTrueAndIdIn(
                        campaignProductIds, pageable);
                return this.convertToResultPaginationDTO(flashSaleProducts);
            }
        }

        // If no active campaign, return empty result with valid Meta
        ResultPaginationDTO emptyRs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        emptyRs.setMeta(mt);
        emptyRs.setResult(new ArrayList<>());
        return emptyRs;
    }

    private ResultPaginationDTO convertToResultPaginationDTO(Page<Product> page) {
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(page.getNumber() + 1);
        mt.setPageSize(page.getSize());
        mt.setPages(page.getTotalPages());
        mt.setTotal(page.getTotalElements());

        rs.setMeta(mt);

        List<ResProductDTO> list = page.getContent().stream()
                .map(item -> this.convertToResProductDTO(item))
                .collect(Collectors.toList());

        rs.setResult(list);
        return rs;
    }

    public double findByOriginalPrice(long id) {
        return this.productRepository.findOriginalPriceById(id).orElse(0.0);
    }

    public ResProductDTO convertToResProductDTO(Product product) {
        ResProductDTO res = new ResProductDTO();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setOriginalPrice(product.getOriginalPrice());
        res.setActive(product.isActive());

        // Map Inventory Details for main product (from its variants or default variant)
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            if (product.getVariants().size() == 1 && product.getVariants().get(0).getSku().startsWith("DEFAULT-")) {
                // If it's a simple product with a default variant, pull its stock directly
                this.inventoryRepository.findByProductVariant(product.getVariants().get(0)).ifPresent(inv -> {
                    res.setStock(inv.getStock());
                    res.setReservedStock(inv.getReservedStock());
                    res.setMaxStock(inv.getMaxStock());
                });
            } else {
                // For products with multiple variants, sum the stock for the dashboard overview
                int totalStock = 0;
                int totalReserved = 0;
                for (ProductVariant v : product.getVariants()) {
                    Inventory inv = this.inventoryRepository.findByProductVariant(v).orElse(null);
                    if (inv != null) {
                        totalStock += inv.getStock();
                        totalReserved += inv.getReservedStock();
                    }
                }
                res.setStock(totalStock);
                res.setReservedStock(totalReserved);
            }
        }

        // Map Thumbnail and Images
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            res.setThumbnail(product.getImages().get(0).getImageUrl());
            List<String> imageUrls = new ArrayList<>();
            List<ResProductDTO.ProductImageInner> productImages = new ArrayList<>();
            for (ProductImage img : product.getImages()) {
                imageUrls.add(img.getImageUrl());
                productImages.add(new ResProductDTO.ProductImageInner(img.getId(), img.getImageUrl()));
            }
            res.setImage(imageUrls);
            res.setProductImages(productImages);
        }

        // Map Description from ProductDetail
        if (product.getProductDetail() != null) {
            res.setDescription(product.getProductDetail().getDescription());
        }

        if (product.getCategory() != null) {
            ResProductDTO.CategoryInner categoryInner = new ResProductDTO.CategoryInner();
            categoryInner.setId(product.getCategory().getId());
            categoryInner.setName(product.getCategory().getName());
            res.setCategory(categoryInner);
        }

        if (product.getBrand() != null) {
            ResProductDTO.BrandInner brandInner = new ResProductDTO.BrandInner();
            brandInner.setId(product.getBrand().getId());
            brandInner.setName(product.getBrand().getName());
            res.setBrand(brandInner);
        }

        if (product.getProductAttributeValues() != null) {
            List<ResProductDTO.ValueInner> valueInners = product.getProductAttributeValues().stream()
                    .filter(pav -> pav.getAttributeValue() != null)
                    .map(pav -> {
                        ResProductDTO.ValueInner v = new ResProductDTO.ValueInner();
                        AttributeValue av = pav.getAttributeValue();
                        v.setId(av.getId());
                        v.setAttributeValue(av.getAttributeValue());
                        if (av.getAttribute() != null) {
                            v.setAttributeId(av.getAttribute().getId());
                            v.setAttributeName(av.getAttribute().getName());
                        }
                        return v;
                    })
                    .collect(Collectors.toList());
            res.setAttributeValue(valueInners);
        }

        // Map Variants
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            List<ResProductDTO.ProductVariantInner> variantInners = product.getVariants().stream()
                    .map(v -> {
                        ResProductDTO.ProductVariantInner vi = new ResProductDTO.ProductVariantInner();
                        vi.setId(v.getId());
                        vi.setSku(v.getSku());
                        // Fallback to product price if variant price is 0 or null
                        if (v.getPrice() == null || v.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                            vi.setPrice(product.getOriginalPrice());
                        } else {
                            vi.setPrice(v.getPrice());
                        }
                        vi.setWeight(v.getWeight());
                        vi.setImage(v.getProductImage() != null ? v.getProductImage().getImageUrl() : null);
                        vi.setProductImageId(v.getProductImage() != null ? v.getProductImage().getId() : null);

                        // Map Variant Inventory
                        this.inventoryRepository.findByProductVariant(v).ifPresent(inv -> {
                            vi.setStock(inv.getStock());
                            vi.setReservedStock(inv.getReservedStock());
                            vi.setMaxStock(inv.getMaxStock());
                        });

                        List<ResProductDTO.VariantAttributeInner> vaInners = v.getAttributeValues().stream()
                                .filter(av -> av != null && av.getAttribute() != null)
                                .map(av -> {
                                    ResProductDTO.VariantAttributeInner va = new ResProductDTO.VariantAttributeInner();
                                    va.setName(av.getAttribute().getName());
                                    va.setAttributeValue(av.getAttributeValue());
                                    return va;
                                })
                                .collect(Collectors.toList());
                        vi.setVariantAttributes(vaInners);

                        // Map Flash Sale Info for this specific variant
                        this.flashSaleService.findActiveFlashSaleItemByVariant(v.getId()).ifPresent(fs -> {
                            if (fs.getCampaign() != null) {
                                ResProductDTO.FlashSaleInner fsInner = new ResProductDTO.FlashSaleInner();
                                fsInner.setPrice(fs.getFlashSalePrice());
                                fsInner.setLimitQuantity(fs.getLimitQuantity());
                                fsInner.setSoldQuantity(fs.getSoldQuantity());
                                fsInner.setEndAt(fs.getCampaign().getEndAt());
                                vi.setFlashSale(fsInner);
                            }
                        });

                        return vi;
                    })
                    .collect(Collectors.toList());
            res.setVariants(variantInners);
        }

        // Calculate pricing for the main product and its variants
        if (this.pricingService != null) {
            // Get all applicable promotions (Global, Category, and Specific)
            List<Promotion> promotions = this.pricingService.getApplicablePromotions(product);

            // Main Product Price
            ResPriceResultDTO mainPriceResult = this.pricingService.calculatePrice(product);
            res.setDiscountPrice(mainPriceResult.getDiscountPrice());
            res.setFinalPrice(mainPriceResult.getFinalPrice());

            // Individual Variant Prices Calculation
            if (res.getVariants() != null) {
                for (ResProductDTO.ProductVariantInner vi : res.getVariants()) {
                    // Find actual variant entity for calculation
                    ProductVariant actualVariant = product.getVariants().stream()
                            .filter(v -> v.getId().equals(vi.getId())).findFirst().orElse(null);

                    if (actualVariant != null) {
                        ResPriceResultDTO vPriceRes = this.pricingService.calculatePriceForVariant(actualVariant,
                                promotions);
                        vi.setDiscountPrice(vPriceRes.getDiscountPrice());
                        vi.setFinalPrice(vPriceRes.getFinalPrice());
                    }
                }
            }
        } else {
            // Fallback if pricing service is missing
            res.setFinalPrice(product.getOriginalPrice());
            res.setDiscountPrice(BigDecimal.ZERO);

            // Set finalPrice for variants as their original price
            if (res.getVariants() != null) {
                for (ResProductDTO.ProductVariantInner vi : res.getVariants()) {
                    vi.setFinalPrice(vi.getPrice());
                    vi.setDiscountPrice(BigDecimal.ZERO);
                }
            }
        }

        res.setAverageRating(this.reviewRepository.findAverageRatingByProductId(product.getId()));
        res.setReviewCount(this.reviewRepository.countByProductId(product.getId()));
        res.setSoldCount(product.getSoldCount());

        // Fill Flash Sale Info
        this.flashSaleService.findActiveFlashSaleForItem(product.getId()).ifPresent(fs -> {
            ResProductDTO.FlashSaleInner fsInner = new ResProductDTO.FlashSaleInner();
            fsInner.setPrice(fs.getFlashSalePrice());
            fsInner.setLimitQuantity(fs.getLimitQuantity());
            fsInner.setSoldQuantity(fs.getSoldQuantity());
            fsInner.setEndAt(fs.getCampaign().getEndAt());
            res.setFlashSale(fsInner);
        });

        return res;
    }

    public Product addImages(ReqUpdateProductDTO req, List<MultipartFile> files) throws IOException {
        Product product = this.handleGetById(req.getId());

        for (MultipartFile file : files) {

            Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);

            ProductImage image = new ProductImage();
            image.setImageUrl(uploadResult.get("secure_url").toString());
            image.setPublicId(uploadResult.get("public_id").toString());
            this.productImageRepository.save(image);
            product.addImage(image);
        }

        return this.productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public String getProductsSummaryForChatbot(String query) {
        StringBuilder sb = new StringBuilder();

        // 1. Fetch Top 5 Bán chạy nhất
        List<Product> topSellers = this.productRepository.findAll(
                org.springframework.data.domain.PageRequest
                        .of(0, 5,
                                org.springframework.data.domain.Sort
                                        .by(org.springframework.data.domain.Sort.Direction.DESC, "soldCount")))
                .getContent();

        if (!topSellers.isEmpty()) {
            sb.append("--- TOÀN TRANG: 5 SẢN PHẨM BÁN CHẠY NHẤT HIỆN GẦN ĐÂY ---\n");
            for (Product p : topSellers) {
                appendChatbotProductInfo(sb, p);
            }
        }

        // 2. Fetch Tìm kiếm nếu có query
        if (query != null && !query.trim().isEmpty()) {
            String sqlQuery = "%" + query.trim() + "%";
            List<Product> searchResults = this.productRepository.searchByNameNative(sqlQuery);
            if (!searchResults.isEmpty()) {
                sb.append("\n--- KẾT QUẢ TÌM KIẾM LIÊN QUAN ---\n");
                for (Product p : searchResults) {
                    appendChatbotProductInfo(sb, p);
                }
            }
        }

        if (sb.length() == 0) {
            return "Hiện tại tôi không tìm thấy sản phẩm nào trong cửa hàng.";
        }

        return sb.toString();
    }

    private void appendChatbotProductInfo(StringBuilder sb, Product p) {
        sb.append("- Tên: ").append(p.getName())
                .append(" (ID: ").append(p.getId()).append(")");

        if (this.pricingService != null) {
            List<Promotion> promotions = this.pricingService.getApplicablePromotions(p);
            ResPriceResultDTO priceResult = this.pricingService.calculatePriceWithPromotions(p.getOriginalPrice(),
                    promotions);
            if (priceResult.getDiscountPrice() != null
                    && priceResult.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(", Giá gốc: ").append(String.format("%,.0f VNĐ", p.getOriginalPrice().doubleValue()))
                        .append(", [ĐANG CÓ FLASHSALE/GIẢM GIÁ] Giá chỉ còn: ")
                        .append(String.format("%,.0f VNĐ", priceResult.getFinalPrice().doubleValue()));
            } else {
                sb.append(", Giá: ").append(String.format("%,.0f VNĐ", p.getOriginalPrice().doubleValue()));
            }
        } else {
            sb.append(", Giá: ").append(String.format("%,.0f VNĐ", p.getOriginalPrice().doubleValue()));
        }

        sb.append(", Danh mục: ").append(p.getCategory() != null ? p.getCategory().getName() : "Khác")
                .append(", Đã bán: ").append(p.getSoldCount());

        if (p.getProductDetail() != null && p.getProductDetail().getDescription() != null) {
            // Loại bỏ HTML tags để đưa vào AI
            String desc = p.getProductDetail().getDescription().replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
            if (desc.length() > 150) {
                desc = desc.substring(0, 150) + "...";
            }
            sb.append(", Mô tả/Tính năng: ").append(desc);
        }
        sb.append("\n");
    }
}
