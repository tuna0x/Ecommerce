package com.tuna.ecommerce.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tuna.ecommerce.domain.AttributeValue;
import com.tuna.ecommerce.domain.Brand;
import com.tuna.ecommerce.domain.Category;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductAttributeValue;
import com.tuna.ecommerce.domain.ProductDetail;
import com.tuna.ecommerce.domain.ProductImage;
import com.tuna.ecommerce.domain.request.product.ReqCreateProductDTO;
import com.tuna.ecommerce.domain.request.product.ReqUpdateProductDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.product.ResProductDTO;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.repository.AttributeValueRepository;
import com.tuna.ecommerce.repository.ProductAttributeValueRepository;
import com.tuna.ecommerce.repository.ProductImageRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {
      private final ProductRepository productRepository;
      private final AttributeValueRepository attributeValueRepository;
      private final ProductAttributeValueRepository productAttributeValueRepository;
      private final CategoryService categoryService;
      private final CloudinaryService cloudinaryService;
      private final ProductImageRepository productImageRepository;
      private final BrandService brandService;
      private final AttributeValueService attributeValueService;

    public Product handleCreate(ReqCreateProductDTO product, List<MultipartFile>files) throws IdInvalidException, IOException{
            Product newProduct=new Product();

        Category category=this.categoryService.handleGetById(product.getCategoryId());
        if (category==null) {
            throw new IdInvalidException("Id invalid");
        }

        Brand brand= this.brandService.getById(product.getBrandId());
        if (brand!=null) {
            newProduct.setBrand(brand);
        }
        newProduct.setName(product.getName());
        newProduct.setOriginalPrice(product.getOriginalPrice());
        newProduct.setStock(product.getStock());
        newProduct.setCategory(category);
        newProduct.setWeight(product.getWeight());
        this.productRepository.save(newProduct);

                if (files!=null && !files.isEmpty()) {
            for(MultipartFile file:files){
                Map uploadResult = cloudinaryService.uploadFile(file);

                ProductImage image=new ProductImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                newProduct.addImage(image);
            }
        }

        for(Long id: product.getAttributeValue()){
            AttributeValue attributeValue=this.attributeValueRepository.findById(id).orElse(null);
              if (this.productAttributeValueRepository.existsByProductIdAndAttributeValueId(newProduct.getId(), id)) {
                throw new RuntimeException("Duplicate attribute value");
              }
              ProductAttributeValue pav=new ProductAttributeValue();
                pav.setProduct(newProduct);
                pav.setAttributeValue(attributeValue);
                this.productAttributeValueRepository.save(pav);
                newProduct.getProductAttributeValues().add(pav);
            }
        return newProduct;
    }

    public Product handleGetById(long id){
        return this.productRepository.findById(id).orElse(null);
    }

    public Product handleUpdate(ReqUpdateProductDTO product, List<MultipartFile> files) throws IdInvalidException, IOException{
                Product cur = this.handleGetById(product.getId());

                for(ProductImage img: cur.getImages()){
                    this.cloudinaryService.deleteFile(img.getPublicId());
                }

                cur.getImages().clear();

            for(MultipartFile file:files){
                Map uploadResult = cloudinaryService.uploadFile(file);

                ProductImage image=new ProductImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                this.productImageRepository.save(image);
                cur.addImage(image);
            }
        Category category = this.categoryService.handleGetById(product.getCategoryId());
        if (category == null) {
            throw new IdInvalidException("Id invalid");
        }

        if (cur != null) {
            cur.setId(product.getId());
            cur.setName(product.getName());
            cur.setOriginalPrice(product.getOriginalPrice());
            cur.setStock(product.getStock());
            cur.setCategory(category);
            cur.setWeight(product.getWeight());
            Brand brand= this.brandService.getById(product.getBrandId());
            if (brand!=null) {
                cur.setBrand(brand);
        }
        }
        return this.productRepository.save(cur);
    }

    public void handleDelete(long id) throws IOException{
        Product product= this.handleGetById(id);
        for(ProductImage img:product.getImages()){
            this.cloudinaryService.deleteFile(img.getPublicId());
        }
        this.productRepository.deleteById(id);
    }

    public ResultPaginationDTO handleGetAll(Specification<Product> spec,Pageable page){
        Page<Product> product= this.productRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(product.getNumber() + 1);
        meta.setPageSize(product.getSize());
        meta.setPages(product.getTotalPages());
        meta.setTotal(product.getTotalElements());

        rs.setMeta(meta);
        List<ResProductDTO> list=product.getContent().stream().map(item->this.convertToResProductDTO(item)).collect(Collectors.toList());
        rs.setResult(list);
        return rs;
    }

    public boolean findByName(String name){
        return this.productRepository.existsByName(name);
    }

    public double findByOriginalPrice(long id){
        return this.findByOriginalPrice(id);
    }


    public ResProductDTO convertToResProductDTO(Product product){
        ResProductDTO res=new ResProductDTO();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setOriginalPrice(product.getOriginalPrice());
        res.setStock(product.getStock());
        res.setWeight(product.getWeight());
        List<String> imageUrl= product.getImages().stream().map(img->img.getImageUrl()).collect(Collectors.toList());
        res.setImage(imageUrl);

        ResProductDTO.CategoryInner categoryInner=new ResProductDTO.CategoryInner();
        Category category= this.categoryService.handleGetById(product.getCategory().getId());
        categoryInner.setId(category.getId());
        categoryInner.setName(category.getName());
        res.setCategory(categoryInner);

        ResProductDTO.BrandInner brandInner=new ResProductDTO.BrandInner();
        if (product.getBrand()!=null) {
        Brand brand= this.brandService.getById(product.getBrand().getId());
        brandInner.setId(brand.getId());
        brandInner.setName(brand.getName());
        res.setBrand(brandInner);

        List<ResProductDTO.ValueInner> valueInner= new ArrayList<>();
        List<ProductAttributeValue> pav= product.getProductAttributeValues();
        List<Long> values= pav.stream().map(item-> item.getAttributeValue().getId()).collect(Collectors.toList());
        List<AttributeValue> attributeValues= this.attributeValueRepository.findAllById(values);
        for(AttributeValue a: attributeValues){
            ResProductDTO.ValueInner v= new ResProductDTO.ValueInner();
            v.setId(a.getId());
            v.setValue(a.getValue());
            valueInner.add(v);
        }
        res.setAttributeValue(valueInner);

        }

        return res;
    }

    public Product addImages(ReqUpdateProductDTO req, List<MultipartFile> files) throws IOException{
        Product product= this.handleGetById(req.getId());

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
