package com.tuna.ecommerce.domain.response.review;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResReviewDTO {
    private Long id;
    private int rating;
    private String comment;
    private String userName;
    private String userImage;
    private Instant createdAt;
}
