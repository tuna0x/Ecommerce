package com.tuna.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Review;
import com.tuna.ecommerce.domain.request.review.ReqCreateReviewDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.ReviewService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    @APIMessage("Đánh giá sản phẩm thành công")
    public ResponseEntity<com.tuna.ecommerce.domain.response.review.ResReviewDTO> createReview(@Valid @RequestBody ReqCreateReviewDTO req) throws IdInvalidException {
        Review review = reviewService.createReview(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.convertToDTO(review));
    }

    @GetMapping("/reviews/product/{productId}")
    @APIMessage("Lấy danh sách đánh giá thành công")
    public ResponseEntity<ResultPaginationDTO> getReviewsByProduct(@PathVariable Long productId, Pageable pageable) {
        return ResponseEntity.ok().body(reviewService.getReviewsByProduct(productId, pageable));
    }

    @DeleteMapping("/reviews/{id}")
    @APIMessage("Xóa đánh giá thành công")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) throws IdInvalidException {
        reviewService.deleteReview(id);
        return ResponseEntity.ok().body(null);
    }
}
