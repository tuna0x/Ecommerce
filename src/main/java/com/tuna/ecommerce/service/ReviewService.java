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
            @CacheEvict(value = "reviews", allEntries = true),
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
        // SECURITY: Escape HTML to prevent XSS
        String sanitizedComment = org.springframework.web.util.HtmlUtils.htmlEscape(req.getComment());
        review.setComment(sanitizedComment);

        if (files != null && !files.isEmpty()) {
            java.util.List<java.util.Map> uploadResults = cloudinaryService.uploadFiles(files);
            for (java.util.Map uploadResult : uploadResults) {
                ReviewImage image = new ReviewImage();
                image.setImageUrl(uploadResult.get("secure_url").toString());
                image.setPublicId(uploadResult.get("public_id").toString());
                image.setReview(review);
                review.getImages().add(image);
            }
        }

        Review savedReview = reviewRepository.save(review);

        // Update product statistics
        updateProductStatistics(product.getId());

        return savedReview;
    }

    private void updateProductStatistics(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
            Long count = reviewRepository.countByProductId(productId);
            product.setAverageRating(avgRating != null ? avgRating : 0.0);
            product.setReviewCount(count != null ? count : 0L);
            productRepository.save(product);
        }
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
            @CacheEvict(value = "reviews", allEntries = true),
            @CacheEvict(value = { "product", "products", "related_products", "flash_sale" }, allEntries = true)
    })
    public void deleteReview(Long id) throws IdInvalidException {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Đánh giá không tồn tại"));

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        if (!review.getUser().getEmail().equals(email)) {
            throw new IdInvalidException("Bạn không có quyền xóa đánh giá này");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);

        // Update product statistics after deletion
        updateProductStatistics(productId);
    }

    public String getReviewsSummaryForChatbot(Long productId) {
        Page<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId,
                org.springframework.data.domain.PageRequest.of(0, 10));
        if (reviews.isEmpty())
            return "Sản phẩm này chưa có đánh giá nào từ khách hàng.";

        StringBuilder sb = new StringBuilder("Đánh giá thực tế từ khách hàng (10 cái gần nhất):\n");
        double avg = reviews.getContent().stream().mapToInt(Review::getRating).average().orElse(0.0);
        sb.append("- Đánh giá trung bình: ").append(String.format("%.1f", avg)).append("/5 ⭐️\n");

        for (Review r : reviews.getContent()) {
            sb.append("- [").append(r.getRating()).append("⭐️] ").append(r.getComment()).append("\n");
        }
        return sb.toString();
    }
}
