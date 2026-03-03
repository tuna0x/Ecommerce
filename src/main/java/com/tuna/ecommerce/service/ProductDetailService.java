package com.tuna.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.ProductDetail;
import com.tuna.ecommerce.domain.request.productDetail.ReqCreateProductDetailDTO;
import com.tuna.ecommerce.domain.request.productDetail.ReqUpdateProductDetailDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.ProductDetailRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductDetailService {
    private final ProductDetailRepository productDetailRepository;
    private final ProductRepository productRepository;

        public ProductDetail createProductDetail(ReqCreateProductDetailDTO req) throws IdInvalidException{
            ProductDetail productDetail=new ProductDetail();
            productDetail.setDescription(req.getDescription());
            productDetail.setIngredient(productDetail.getIngredient());
            productDetail.setUsageGuide(productDetail.getUsageGuide());
            productDetail.setSpecification(productDetail.getSpecification());

            Product product= this.productRepository.findById(req.getProductId()).orElse(null);
            if (product!=null) {
                productDetail.setProduct(product);
            }else{
                throw new IdInvalidException("product not found");
            }
        return this.productDetailRepository.save(productDetail);
    }

    public ProductDetail getById(long id){
        return this.productDetailRepository.findById(id).orElse(null);
    }

    public ProductDetail updateProductDetail(ReqUpdateProductDetailDTO req) throws IdInvalidException{
        ProductDetail cur= this.getById(req.getId());
        if (cur!=null) {
            cur.setDescription(req.getDescription());
            cur.setIngredient(req.getIngredient());
            cur.setUsageGuide(req.getUsageGuide());
            cur.setSpecification(req.getSpecification());
            Product product= this.productRepository.findById(req.getProductId()).orElse(null);
            if (product!=null) {
                cur.setProduct(product);
            }else{
                throw new IdInvalidException("product not found");
            }
            cur=this.productDetailRepository.save(cur);
        }
        return cur;

    }

    public ResultPaginationDTO handleGetAll(Specification<ProductDetail> spec,Pageable page){
        Page<ProductDetail> productDetail= this.productDetailRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(productDetail.getNumber() + 1);
        meta.setPageSize(productDetail.getSize());
        meta.setPages(productDetail.getTotalPages());
        meta.setTotal(productDetail.getTotalElements());

        rs.setMeta(meta);
        rs.setResult(productDetail.getContent());
        return rs;
    }

    public void deleteProductDetail(long id){
        this.productDetailRepository.deleteById(id);
    }

}
