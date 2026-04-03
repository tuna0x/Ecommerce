package com.tuna.ecommerce.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.repository.ProductAttributeValueRepository;
import com.tuna.ecommerce.repository.ProductImageRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.ReviewRepository;
import com.tuna.ecommerce.domain.response.promotion.ResPriceResultDTO;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
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

    public Product handleCreate(ReqCreateProductDTO product, List<MultipartFile> files)
            throws IdInvalidException, IOException {
        Category category = this.categoryService.handleGetById(product.getCategoryId());
        if (category == null) {
            throw new IdInvalidException("Category not found with id: " + product.getCategoryId());
        }

        Product newProduct = new Product();
        newProduct.setName(product.getName());
        newProduct.setOriginalPrice(product.getOriginalPrice());
        newProduct.setStock(product.getStock());
        newProduct.setCategory(category);
        newProduct.setWeight(product.getWeight());

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
        cur.setStock(product.getStock());
        cur.setCategory(category);
        cur.setWeight(product.getWeight());

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
        }

        if (files != null && !files.isEmpty()) {
            // Delete old images only if new images are provided (optional logic, could be
            // different)
            for (ProductImage img : new ArrayList<>(cur.getImages())) {
                this.cloudinaryService.deleteFile(img.getPublicId());
                this.productImageRepository.delete(img);
            }
            cur.getImages().clear();

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

        return this.productRepository.save(cur);
    }

    public void handleDelete(long id) throws IOException {
        Product product = this.handleGetById(id);
        if (product != null) {
            for (ProductImage img : product.getImages()) {
                this.cloudinaryService.deleteFile(img.getPublicId());
            }
            // Cascade.ALL + orphanRemoval handles ProductImage and ProductAttributeValue deletion
            this.productRepository.delete(product);
        }
    }

    public ResultPaginationDTO handleGetAll(Specification<Product> spec, Pageable page) {
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

    public List<ResProductDTO> getRelatedProducts(Long id) {
        Product product = this.handleGetById(id);
        if (product == null || product.getCategory() == null) {
            return new ArrayList<>();
        }
        List<Product> related = this.productRepository.findTop8ByCategoryIdAndIdNotOrderByCreatedAtDesc(product.getCategory().getId(), id);
        return related.stream().map(this::convertToResProductDTO).collect(Collectors.toList());
    }

    public double findByOriginalPrice(long id) {
        return this.productRepository.findOriginalPriceById(id).orElse(0.0);
    }

    public ResProductDTO convertToResProductDTO(Product product) {
        ResProductDTO res = new ResProductDTO();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setOriginalPrice(product.getOriginalPrice());
        res.setStock(product.getStock());
        res.setWeight(product.getWeight());

        if (product.getImages() != null) {
            List<String> imageUrls = product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());
            res.setImage(imageUrls);
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
                    .map(pav -> {
                        ResProductDTO.ValueInner v = new ResProductDTO.ValueInner();
                        v.setId(pav.getAttributeValue().getId());
                        v.setValue(pav.getAttributeValue().getValue());
                        return v;
                    })
                    .collect(Collectors.toList());
            res.setAttributeValue(valueInners);
        }

        res.setAverageRating(this.reviewRepository.findAverageRatingByProductId(product.getId()));
        res.setReviewCount(this.reviewRepository.countByProductId(product.getId()));
        res.setSoldCount(product.getSoldCount());

        if (this.pricingService != null) {
            ResPriceResultDTO priceResult = this.pricingService.calculatePrice(product);
            res.setDiscountPrice(priceResult.getDiscountPrice());
            res.setFinalPrice(priceResult.getFinalPrice());
        }

        return res;
    }

    public Product addImages(ReqUpdateProductDTO req, List<MultipartFile> files) throws IOException {
        Product product = this.handleGetById(req.getId());

        for (MultipartFile file : files) {

            Map uploadResult = cloudinaryService.uploadFile(file);

            ProductImage image = new ProductImage();
            image.setImageUrl(uploadResult.get("secure_url").toString());
            image.setPublicId(uploadResult.get("public_id").toString());
            this.productImageRepository.save(image);
            product.addImage(image);
        }

        return this.productRepository.save(product);
    }
}
