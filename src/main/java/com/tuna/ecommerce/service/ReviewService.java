package com.tuna.ecommerce.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.Review;
import com.tuna.ecommerce.domain.ReviewImage;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.review.ReqCreateReviewDTO;
import com.tuna.ecommerce.domain.response.review.ResReviewDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.ProductRepository;
import com.tuna.ecommerce.repository.ReviewRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CloudinaryService cloudinaryService;

    @Caching(evict = {
            @CacheEvict(value = "reviews", key = "#req.productId"),
            @CacheEvict(value = { "product", "products", "related_products", "flash_sale" }, allEntries = true)
    })
    public Review createReview(ReqCreateReviewDTO req,
            java.util.List<org.springframework.web.multipart.MultipartFile> files)
            throws IdInvalidException, java.io.IOException {
        // 1. Check product exists
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IdInvalidException("Sản phẩm không tồn tại"));

        // 2. Check user exists (from security context)
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Người dùng chưa đăng nhập"));
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        // 3. Check if user already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new IdInvalidException("Bạn đã đánh giá sản phẩm này rồi");
        }

        // 4. Check if user purchased the product
        if (!orderRepository.hasPurchasedProduct(user.getId(), product.getId())) {
            throw new IdInvalidException("Bạn chỉ có thể đánh giá sản phẩm đã mua và nhận hàng");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(req.getRating());
        review.setComment(req.getComment());

        if (files != null && !files.isEmpty()) {
            for (org.springframework.web.multipart.MultipartFile file : files) {
                java.util.Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
                ReviewImage image = new ReviewImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                image.setReview(review);
                review.getImages().add(image);
            }
        }

        return reviewRepository.save(review);
    }

    @Cacheable(value = "reviews", key = "#productId + '-' + #pageable.pageNumber")
    public ResultPaginationDTO getReviewsByProduct(Long productId, Pageable pageable) {
        Page<Review> pageReview = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageReview.getTotalPages());
        mt.setTotal(pageReview.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageReview.getContent().stream().map(this::convertToDTO).toList());

        return rs;
    }

    public ResReviewDTO convertToDTO(Review review) {
        ResReviewDTO res = new ResReviewDTO();
        res.setId(review.getId());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        if (review.getUser().getUserProfile() != null) {
            res.setUserName(review.getUser().getUserProfile().getName());
            res.setUserImage(review.getUser().getUserProfile().getImage());
        }
        res.setCreatedAt(review.getCreatedAt());
        if (review.getImages() != null) {
            res.setImages(review.getImages().stream().map(com.tuna.ecommerce.domain.ReviewImage::getImageUrl).toList());
        }
        return res;
    }

    @Caching(evict = {
            @CacheEvict(value = "reviews", key = "#result.product.id", condition = "#result != null"),
            @CacheEvict(value = { "product", "products", "related_products", "flash_sale" }, allEntries = true)
    })
    public void deleteReview(Long id) throws IdInvalidException {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        if (!review.getUser().getEmail().equals(email)) {
            throw new IdInvalidException("Bạn không có quyền xóa đánh giá này");
        }

        reviewRepository.delete(review);
    }
}
