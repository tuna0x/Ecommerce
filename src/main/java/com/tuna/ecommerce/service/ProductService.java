package com.tuna.ecommerce.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tuna.ecommerce.domain.Inventory;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.response.promotion.ResPriceResultDTO;
import jakarta.persistence.criteria.JoinType;
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
import com.tuna.ecommerce.domain.request.product.ReqCreateProductDTO;
import com.tuna.ecommerce.domain.request.product.ReqUpdateProductDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.category.ResCategoryDTO;
import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.repository.ProductAttributeValueRepository;
import com.tuna.ecommerce.repository.ProductImageRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.ReviewRepository;
import com.tuna.ecommerce.domain.response.promotion.ResPriceResultDTO;
import com.tuna.ecommerce.domain.ProductVariant;
import com.tuna.ecommerce.repository.ProductVariantRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.tuna.ecommerce.domain.ProductPromotion;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.repository.ProductPromotionRepository;
import com.tuna.ecommerce.repository.PromotionRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import com.tuna.ecommerce.repository.InventoryRepository;

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
            ProductVariantRepository productVariantRepository1, ProductPromotionRepository productPromotionRepository,
            PromotionRepository promotionRepository,
            @Lazy InventoryService inventoryService,
            InventoryRepository inventoryRepository) {
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
    }

    @jakarta.annotation.PostConstruct
    public void initPrices() {
        // One-time sync for existing products that don't have a final price yet
        List<Product> products = this.productRepository.findAll();
        for (Product p : products) {
            if (p.getPrice() == null) {
                this.updateProductPrice(p);
                this.productRepository.save(p);
            }
        }
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

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
                ProductImage image = new ProductImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                image.setProduct(newProduct);
                this.productImageRepository.save(image);
                newProduct.addImage(image);
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
        return this.productRepository.findById(id).orElse(null);
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
        } else {
            // If attributeValue is null in the DTO, it means we might want to clear them.
            // However, to be safe and compatible with old behavior where null means "don't change",
            // we could check if we want to clear them.
            // Let's assume for now that if the user sends null, we clear it TO FIX THE BUG
            // "choosing no attributes fails or doesn't update".
            cur.getProductAttributeValues().clear();
        }

        // Update Variants
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
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

                if (vDto.getAttributeValues() != null) {
                    List<AttributeValue> avs = vDto.getAttributeValues().stream()
                            .map(id -> this.attributeValueRepository.findById(id).orElse(null))
                            .filter(av -> av != null)
                            .collect(Collectors.toList());
                    variant.setAttributeValues(avs);
                }
                updatedVariants.add(variant);
            }

            cur.getVariants().clear();
            cur.getVariants().addAll(updatedVariants);
        } else {
            // Simple product: Ensure only default variant exists
            cur.getVariants().removeIf(v -> !v.getSku().startsWith("DEFAULT-"));
            if (cur.getVariants().isEmpty()) {
                ProductVariant defaultVariant = new ProductVariant();
                defaultVariant.setProduct(cur);
                defaultVariant.setSku("DEFAULT-" + cur.getId());
                defaultVariant.setPrice(cur.getOriginalPrice());
                this.productVariantRepository.save(defaultVariant);
                cur.getVariants().add(defaultVariant);
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

    private void updateProductPrice(Product product) {
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

    public void handleDelete(long id) throws IOException {
        Product product = this.handleGetById(id);
        if (product != null) {
            for (ProductImage img : product.getImages()) {
                this.cloudinaryService.deleteFile(img.getPublicId());
            }
            // Cascade.ALL + orphanRemoval handles ProductImage and ProductAttributeValue
            // deletion
            this.productRepository.delete(product);
        }
    }

    public ResultPaginationDTO handleGetAll(Specification<Product> spec, Long categoryId, String search, Pageable page) {
        if (categoryId != null) {
            List<Long> categoryIds = this.categoryService.getAllIdsInHierarchy(categoryId);
            Specification<Product> categorySpec = (root, query, cb) -> root.get("category").get("id")
                    .in(categoryIds);
            spec = (spec == null) ? categorySpec : spec.and(categorySpec);
        }

        // Unaccented search: if search param exists, add OR condition for nameUnsigned
        if (search != null && !search.trim().isEmpty()) {
            String unsignedSearch = Product.removeVietnameseAccents(search.trim());
            Specification<Product> unsignedSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("nameUnsigned")), "%" + unsignedSearch + "%");
            // OR with the original spec from springfilter (which searches `name`)
            if (spec != null) {
                spec = spec.or(unsignedSpec);
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
        List<Product> related = this.productRepository
                .findTop8ByCategoryIdAndIdNotOrderByCreatedAtDesc(product.getCategory().getId(), id);
        return related.stream().map(this::convertToResProductDTO).collect(Collectors.toList());
    }

    @Cacheable(value = "flash_sale", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public ResultPaginationDTO handleGetFlashSale(Pageable pageable) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // 1. Check for Global Promotions
        List<Promotion> globalPromos = this.promotionRepository.findActiveGlobal(now);
        if (globalPromos != null && !globalPromos.isEmpty()) {
            // If any global promotion is active, all products are in "Flash Sale"
            Page<Product> allProducts = this.productRepository.findAll(pageable);
            return this.convertToResultPaginationDTO(allProducts);
        }

        // 2. Collect Category IDs with active promotions
        List<ResCategoryDTO> allCategories = this.categoryService.handleGetAll();
        List<Long> categoryIds = new ArrayList<>();
        if (allCategories != null) {
            for (ResCategoryDTO cat : allCategories) {
                List<Promotion> catPromos = this.promotionRepository.findActiveByCategoryId(cat.getId(), now);
                if (catPromos != null && !catPromos.isEmpty()) {
                    categoryIds.add(cat.getId());
                }
            }
        }

        // 3. Collect Product IDs with active specific promotions
        List<ProductPromotion> productPromos = this.productPromotionRepository.findAllActive(now);
        List<Long> productIds = productPromos.stream()
                .filter(pp -> pp.getProduct() != null)
                .map(pp -> pp.getProduct().getId())
                .distinct()
                .collect(Collectors.toList());

        // 4. Query products by Category OR Product ID
        if (categoryIds.isEmpty() && productIds.isEmpty()) {
            return new ResultPaginationDTO(); // Empty result
        }

        // Ensure lists are not empty for IN clause if one is empty
        if (categoryIds.isEmpty())
            categoryIds.add(-1L);
        if (productIds.isEmpty())
            productIds.add(-1L);

        Page<Product> flashSaleProducts = this.productRepository.findByCategoryIdInOrIdIn(categoryIds, productIds,
                pageable);
        return this.convertToResultPaginationDTO(flashSaleProducts);
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
            List<String> imageUrls = product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());
            res.setImage(imageUrls);
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
                        vi.setPrice(v.getPrice());
                        vi.setWeight(v.getWeight());

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
            ResPriceResultDTO mainPriceResult = this.pricingService
                    .calculatePriceWithPromotions(product.getOriginalPrice(), promotions);
            res.setDiscountPrice(mainPriceResult.getDiscountPrice());
            res.setFinalPrice(mainPriceResult.getFinalPrice());

            // Individual Variant Prices Calculation
            if (res.getVariants() != null) {
                for (ResProductDTO.ProductVariantInner vi : res.getVariants()) {
                    // Calculate discount for this variant individual price
                    ResPriceResultDTO vPriceRes = this.pricingService.calculatePriceWithPromotions(vi.getPrice(),
                            promotions);
                    vi.setDiscountPrice(vPriceRes.getDiscountPrice());
                    vi.setFinalPrice(vPriceRes.getFinalPrice());
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
                org.springframework.data.domain.PageRequest.of(0, 5, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "soldCount"))
        ).getContent();

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
            ResPriceResultDTO priceResult = this.pricingService.calculatePriceWithPromotions(p.getOriginalPrice(), promotions);
            if (priceResult.getDiscountPrice() != null && priceResult.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
                 sb.append(", Giá gốc: ").append(String.format("%,.0f VNĐ", p.getOriginalPrice().doubleValue()))
                   .append(", [ĐANG CÓ FLASHSALE/GIẢM GIÁ] Giá chỉ còn: ").append(String.format("%,.0f VNĐ", priceResult.getFinalPrice().doubleValue()));
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
